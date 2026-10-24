package com.ehealthwares.prognocare.data.remote.api

import com.ehealthwares.prognocare.data.remote.models.AuthResponse
import com.ehealthwares.prognocare.data.remote.models.FacebookAuthRequest
import com.ehealthwares.prognocare.data.remote.models.GoogleAuthRequest
import com.ehealthwares.prognocare.data.remote.models.LoginDto
import com.ehealthwares.prognocare.data.remote.models.MeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Login-proxy endpoints. The EMR exposes /api/auth/* which forwards to the
// rxsoft-identity service and returns the shared JWT.
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body dto: LoginDto): Response<AuthResponse>

    @POST("auth/oauth/google")
    suspend fun googleSignIn(@Body dto: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/oauth/facebook")
    suspend fun facebookSignIn(@Body dto: FacebookAuthRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun me(): Response<MeResponse>
}