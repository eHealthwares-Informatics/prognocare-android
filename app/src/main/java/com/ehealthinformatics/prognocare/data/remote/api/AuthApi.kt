package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.AuthResponse
import com.ehealthinformatics.prognocare.data.remote.models.LoginDto
import com.ehealthinformatics.prognocare.data.remote.models.MeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Login-proxy endpoints. The EMR exposes /api/auth/* which forwards to the
// rxsoft-identity service and returns the shared JWT.
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body dto: LoginDto): Response<AuthResponse>

    @GET("auth/me")
    suspend fun me(): Response<MeResponse>
}