package com.ehealthinformatics.prognocare.data.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the EMR/gateway on the phone's local networks by probing
 * `GET /api/health` on every host of each reachable /24.
 *
 * Works on Wi-Fi and while the phone is running a mobile hotspot: the scan is
 * driven by the phone's own private IPv4 addresses (so the hotspot AP address
 * such as 192.168.43.1 is included), and the default gateway is added when it
 * sits on one of those subnets. The mobile-network gateway is never scanned.
 */
@Singleton
class ServerDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** A backend that answered the health probe. */
    data class DiscoveredServer(val url: String, val ip: String, val port: Int)

    data class ScanResult(
        val servers: List<DiscoveredServer>,
        val subnets: List<String>,
        val gateway: String?,
    )

    /** Short timeouts: an absent host fails fast and the sweep stays quick. */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.MILLISECONDS)
        .readTimeout(1_200, TimeUnit.MILLISECONDS)
        .callTimeout(1_800, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /** All private IPv4 addresses across up, non-loopback interfaces. */
    fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { iface ->
                iface.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .mapNotNull { it.hostAddress }
                    .map { it to iface.name }
            }
            .filter { (ip, _) -> ip.isPrivate() && !ip.startsWith("169.254.") }
            .distinctBy { it.first }
            .sortedBy { (_, iface) -> priority(iface) }
            .map { it.first }
    }.getOrNull().orEmpty()

    /**
     * Default gateway of the active network. Null when the active network is
     * cellular (its gateway is not on any LAN) or when the platform withholds
     * it (some hotspot modes).
     */
    fun defaultGateway(): String? = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager ?: return@runCatching null
        val network = manager.activeNetwork ?: return@runCatching null
        manager.getLinkProperties(network)
            ?.routes
            ?.firstOrNull { it.isDefaultRoute }
            ?.gateway
            ?.hostAddress
    }.getOrNull()

    /**
     * Scan every reachable /24 for a backend, most likely host first. The
     * result carries the scanned subnets so the UI can explain an empty sweep.
     */
    suspend fun scan(ports: List<Int> = DEFAULT_PORTS): ScanResult = withContext(Dispatchers.IO) {
        val locals = localAddresses()
        val gateway = defaultGateway()?.takeIf { gw ->
            gw.isPrivate() && locals.any { it.substringBeforeLast('.') == gw.substringBeforeLast('.') }
        }
        val prefixes = (locals + listOfNotNull(gateway))
            .filter { it.isPrivate() }
            .map { it.substringBeforeLast('.') }
            .distinct()
        if (prefixes.isEmpty()) return@withContext ScanResult(emptyList(), emptyList(), gateway)

        val targets = buildList {
            gateway?.let { add(it) }
            prefixes.forEach { prefix -> (1..254).forEach { add("$prefix.$it") } }
        }.distinct()

        val semaphore = Semaphore(MAX_CONCURRENCY)
        val seen = mutableSetOf<String>()
        val found = coroutineScope {
            targets.map { ip ->
                async {
                    semaphore.withPermit {
                        val server = probeHost(ip, ports)
                        if (server != null) {
                            synchronized(seen) { if (seen.add(server.url)) server else null }
                        } else null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        ScanResult(
            servers = found.sortedBy { it.url },
            subnets = prefixes,
            gateway = gateway,
        )
    }

    /** First port on [ip] that answers like the EMR, if any. */
    private fun probeHost(ip: String, ports: List<Int>): DiscoveredServer? {
        for (port in ports) {
            if (isEmr(ip, port)) {
                // The app's EMR base URL has no path suffix; endpoints add /api.
                return DiscoveredServer("http://$ip:$port", ip, port)
            }
        }
        return null
    }

    private fun isEmr(ip: String, port: Int): Boolean = runCatching {
        val request = Request.Builder()
            .url("http://$ip:$port/api/health")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use false
            val json = JSONObject(response.body?.string().orEmpty())
            json.optString("status") == "ok" &&
                json.optString("service").lowercase() in KNOWN_SERVICES
        }
    }.getOrDefault(false)

    private fun String.isPrivate(): Boolean {
        if (!matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) return false
        val parts = split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        return parts[0] == 10 ||
            (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31)
    }

    /** Lower is scanned first: hotspot/tether interfaces, then Wi-Fi, then wired. */
    private fun priority(iface: String): Int = when {
        iface.startsWith("ap") || iface.contains("softap") -> 0
        iface.contains("swlan") || iface.contains("rndis") -> 1
        iface.startsWith("wlan") -> 2
        iface.startsWith("eth") -> 3
        else -> 4
    }

    companion object {
        /** Health `service` markers that mean "this is a PrognoCare/rxsoft backend". */
        private val KNOWN_SERVICES = setOf("emr", "prognocare", "rxsoft-backend", "")

        /** Ports to try per host: dev gateway (8080), docs port (8000), backend default (3000). */
        val DEFAULT_PORTS = listOf(8080, 8000, 3000)

        private const val MAX_CONCURRENCY = 64
    }
}
