package com.ecolocal.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ecolocal.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Helper for performing unsigned image uploads to Cloudinary.
 * Strictly uses CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET from BuildConfig/local.properties.
 * Never uses or requires an API secret.
 */
object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Uploads an image from an Android Uri to Cloudinary using unsigned preset.
     * Returns Result with the secure HTTPS URL on success.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
            val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET

            if (cloudName.isBlank() || uploadPreset.isBlank()) {
                val err = "Cloudinary credentials missing in BuildConfig/local.properties"
                Log.e(TAG, err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            // Read image bytes from ContentResolver
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to open stream for URI: $imageUri"))

            val buffer = ByteArrayOutputStream()
            val data = ByteArray(8192)
            var nRead: Int
            while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            inputStream.close()
            val imageBytes = buffer.toByteArray()

            if (imageBytes.isEmpty()) {
                val err = "Selected image file is empty (0 bytes)"
                Log.e(TAG, err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            var detectedMime = contentResolver.getType(imageUri)
            val extension = if (imageBytes.size >= 4) {
                if (imageBytes[0] == 0x89.toByte() && imageBytes[1] == 0x50.toByte() && imageBytes[2] == 0x4E.toByte() && imageBytes[3] == 0x47.toByte()) {
                    detectedMime = "image/png"
                    "png"
                } else if (imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte()) {
                    detectedMime = "image/jpeg"
                    "jpg"
                } else if (imageBytes[0] == 'R'.code.toByte() && imageBytes[1] == 'I'.code.toByte() && imageBytes[2] == 'F'.code.toByte() && imageBytes[3] == 'F'.code.toByte()) {
                    detectedMime = "image/webp"
                    "webp"
                } else {
                    if (detectedMime?.contains("png", true) == true) "png" else "jpg"
                }
            } else {
                if (detectedMime?.contains("png", true) == true) "png" else "jpg"
            }

            val finalMime = detectedMime ?: if (extension == "png") "image/png" else "image/jpeg"
            val filename = "upload_${System.currentTimeMillis()}.$extension"
            val fileRequestBody = imageBytes.toRequestBody(finalMime.toMediaTypeOrNull())

            Log.d(TAG, "Uploading image (${imageBytes.size} bytes) with filename: $filename and MIME: $finalMime")

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("file", filename, fileRequestBody)
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "Upload failed with HTTP ${response.code}: $responseBody"
                }
                Log.e(TAG, "Cloudinary upload error: $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(responseBody)
            val secureUrl = json.optString("secure_url")
            if (secureUrl.isNotEmpty()) {
                Log.d(TAG, "Cloudinary upload success: $secureUrl")
                Result.success(secureUrl)
            } else {
                val url = json.optString("url")
                if (url.isNotEmpty()) {
                    Result.success(url.replace("http://", "https://"))
                } else {
                    Result.failure(Exception("No secure_url returned by Cloudinary"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Cloudinary upload", e)
            Result.failure(e)
        }
    }
}
