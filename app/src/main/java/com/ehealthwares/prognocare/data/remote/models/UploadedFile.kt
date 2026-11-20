package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

/**
 * Result of `POST /uploads` (EMR stores to Cloudinary + S3). Mirrors the web
 * `UploadedFile` shape so `file` field values stay compatible.
 */
@Serializable
data class UploadedFile(
    val url: String = "",
    val cloudinaryUrl: String? = null,
    val s3Url: String? = null,
    val key: String? = null,
    val name: String = "",
    val mimeType: String? = null,
    val size: Long? = null,
) {
    fun toFieldValue(): Map<String, Any?> = buildMap {
        put("url", url)
        put("name", name)
        cloudinaryUrl?.let { put("cloudinaryUrl", it) }
        s3Url?.let { put("s3Url", it) }
        key?.let { put("key", it) }
        mimeType?.let { put("mimeType", it) }
        size?.let { put("size", it) }
    }
}
