package com.ehealthwares.prognocare.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }

    companion object {
        fun saveToken(context: Context, token: String) {
            context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
                .edit()
                .putString("auth_token", token)
                .apply()
        }

        fun getToken(context: Context): String? {
            return context.getSharedPreferences("prognocare_auth", Context.MODE_PRIVATE)
                .getString("auth_token", null)
        }
    }
}
