package com.ehealthwares.prognocare.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Tenant information carried by the identity access token.
 *
 * `organizationId` is blank for a super admin and is normalised to null so the
 * interceptor omits the header ("all organisations" server-side).
 */
data class TenantClaims(
    val organizationId: String? = null,
    val locationId: String? = null,
    val roles: List<String> = emptyList(),
)

object JwtClaims {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun decode(token: String?): TenantClaims? {
        if (token.isNullOrBlank()) return null
        val parts = token.split('.')
        if (parts.size < 2) return null
        return try {
            val payload = String(
                Base64.getUrlDecoder().decode(pad(parts[1])),
                Charsets.UTF_8,
            )
            val obj = json.parseToJsonElement(payload).jsonObject
            TenantClaims(
                organizationId = obj["organizationId"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() },
                locationId = obj["locationId"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() },
                roles = (obj["roles"] as? JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: emptyList(),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun pad(value: String): String {
        val remainder = value.length % 4
        return if (remainder == 0) value else value + "=".repeat(4 - remainder)
    }
}
