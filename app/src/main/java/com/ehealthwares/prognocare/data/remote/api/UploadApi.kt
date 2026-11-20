package com.ehealthwares.prognocare.data.remote.api

import com.ehealthwares.prognocare.data.remote.models.UploadedFile
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {

    @Multipart
    @POST("uploads")
    suspend fun upload(@Part file: MultipartBody.Part): Response<UploadedFile>
}
