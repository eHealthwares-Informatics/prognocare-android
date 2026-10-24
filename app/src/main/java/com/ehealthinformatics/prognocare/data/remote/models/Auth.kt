package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val username: String,
    val password: String,
)

@Serializable
data class FacebookAuthRequest(
    val accessToken: String,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String? = null,
    val accessToken: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresIn: Int = 0,
    val refreshTokenExpiresIn: Int = 0,
)

@Serializable
data class MeResponse(
    val id: String = "",
    val username: String = "",
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val modules: List<ModuleInfo> = emptyList(),
)

@Serializable
data class ModuleInfo(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val root: String = "",
)