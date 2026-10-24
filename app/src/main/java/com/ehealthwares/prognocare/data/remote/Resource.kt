package com.ehealthwares.prognocare.data.remote

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> {
    return when (this) {
        is Resource.Success -> Resource.Success(transform(data))
        is Resource.Error -> Resource.Error(message, code)
        is Resource.Loading -> Resource.Loading
    }
}

suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Resource.Success(body)
            } else {
                Resource.Success(Unit as T)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
            Resource.Error(errorMsg, response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network error occurred")
    }
}
