package com.ehealthwares.prognocare.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the tenant context headers used by the backends:
 * `X-Organization-Id` (omitted for a super admin = all organisations) and
 * `X-Location-Id` (omitted when the user has no location).
 */
@Singleton
class TenantInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AuthInterceptor.getToken(context)
        val claims = JwtClaims.decode(token) ?: return chain.proceed(chain.request())

        val request = chain.request().newBuilder().apply {
            claims.organizationId?.let { header("X-Organization-Id", it) }
            claims.locationId?.let { header("X-Location-Id", it) }
        }.build()

        return chain.proceed(request)
    }
}
