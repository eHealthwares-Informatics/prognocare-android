package com.ehealthinformatics.prognocare.data.remote.api

import com.ehealthinformatics.prognocare.data.remote.models.AuthResponse
import com.ehealthinformatics.prognocare.data.remote.models.FacebookAuthRequest
import com.ehealthinformatics.prognocare.data.remote.models.GoogleAuthRequest
import com.ehealthinformatics.prognocare.data.remote.models.LoginDto
import com.ehealthinformatics.prognocare.data.remote.models.MeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Login-proxy endpoints. The EMR exposes /api/auth/* which forwards to the
// rxsoft-identity service and returns the shared JWT.
interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(@Body dto: LoginDto): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    // Google OAuth (Credential Manager ID token) — proxied to rxsoft-identity,
    // which verifies the token and returns the shared JWT.
    @POST("api/auth/oauth/google")
    suspend fun googleSignIn(@Body dto: GoogleAuthRequest): Response<AuthResponse>

    // Facebook OAuth access token — same proxied identity exchange.
    @POST("api/auth/oauth/facebook")
    suspend fun facebookSignIn(@Body dto: FacebookAuthRequest): Response<AuthResponse>
}