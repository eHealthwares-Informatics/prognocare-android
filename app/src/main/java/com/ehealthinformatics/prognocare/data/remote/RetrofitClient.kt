package com.ehealthinformatics.prognocare.data.remote

import com.ehealthinformatics.prognocare.data.config.AppConfigStore
import com.ehealthinformatics.prognocare.data.remote.api.AppointmentApi
import com.ehealthinformatics.prognocare.data.remote.api.DashboardApi
import com.ehealthinformatics.prognocare.data.remote.api.EncounterApi
import com.ehealthinformatics.prognocare.data.remote.api.FormApi
import com.ehealthinformatics.prognocare.data.remote.api.HealthApi
import com.ehealthinformatics.prognocare.data.remote.api.LocationApi
import com.ehealthinformatics.prognocare.data.remote.api.MedicationApi
import com.ehealthinformatics.prognocare.data.remote.api.PatientApi
import com.ehealthinformatics.prognocare.data.remote.api.PaymentProviderApi
import com.ehealthinformatics.prognocare.data.remote.api.RequestApi
import com.ehealthinformatics.prognocare.data.remote.api.StaffApi
import com.ehealthinformatics.prognocare.data.remote.api.ReferralApi
import com.ehealthinformatics.prognocare.data.remote.api.VisitApi
import com.ehealthinformatics.prognocare.data.remote.api.AuthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Retrofit API services bound to the current EMR base URL.
 */
data class ApiBundle(
    val patientApi: PatientApi,
    val staffApi: StaffApi,
    val appointmentApi: AppointmentApi,
    val visitApi: VisitApi,
    val encounterApi: EncounterApi,
    val requestApi: RequestApi,
    val formApi: FormApi,
    val dashboardApi: DashboardApi,
    val paymentProviderApi: PaymentProviderApi,
    val locationApi: LocationApi,
    val healthApi: HealthApi,
    val authApi: AuthApi,
    val referralApi: ReferralApi,
    val medicationApi: MedicationApi,
)

@Singleton
class RetrofitClient @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val configStore: AppConfigStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        // Never send explicit nulls: the EMR uses strict whitelist validation
        // and rejects unknown properties like items[].id/requestId.
        explicitNulls = false
    }

    private fun buildHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(buildHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun buildBundle(baseUrl: String): ApiBundle {
        val retrofit = buildRetrofit(baseUrl)
        return ApiBundle(
            patientApi = retrofit.create(PatientApi::class.java),
            staffApi = retrofit.create(StaffApi::class.java),
            appointmentApi = retrofit.create(AppointmentApi::class.java),
            visitApi = retrofit.create(VisitApi::class.java),
            encounterApi = retrofit.create(EncounterApi::class.java),
            requestApi = retrofit.create(RequestApi::class.java),
            formApi = retrofit.create(FormApi::class.java),
            dashboardApi = retrofit.create(DashboardApi::class.java),
            paymentProviderApi = retrofit.create(PaymentProviderApi::class.java),
            locationApi = retrofit.create(LocationApi::class.java),
            healthApi = retrofit.create(HealthApi::class.java),
            authApi = retrofit.create(AuthApi::class.java),
            referralApi = retrofit.create(ReferralApi::class.java),
            medicationApi = retrofit.create(MedicationApi::class.java),
        )
    }

    /** Current API bundle, rebuilt whenever the EMR base URL config changes. */
    val apis: StateFlow<ApiBundle> = configStore.config
        .map { config -> buildBundle(config.emrBaseUrl) }
        .stateIn(scope, SharingStarted.Eagerly, buildBundle(AppConfigStore.DEFAULT_EMR_URL))
}