package io.github.behnamrm.watertracker.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class DriveException(val code: Int, message: String) : IOException(message)

data class DriveFile(val id: String, val modifiedTime: String?)

/** Minimal Google Drive v3 REST client for a single JSON backup file. */
class DriveClient(private val http: OkHttpClient = OkHttpClient()) {

    suspend fun findBackup(token: String): DriveFile? = withContext(Dispatchers.IO) {
        val url = "$API/files".toHttpUrl().newBuilder()
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME' and trashed = false")
            .addQueryParameter("spaces", "drive")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("fields", "files(id,modifiedTime)")
            .build()
        val body = execute(Request.Builder().url(url).auth(token).build())
        val files = JSONObject(body).optJSONArray("files")
        if (files == null || files.length() == 0) null else files.getJSONObject(0).toDriveFile()
    }

    /** Creates the backup file, or overwrites it when [existingId] is given. */
    suspend fun upload(token: String, existingId: String?, json: String): DriveFile = withContext(Dispatchers.IO) {
        if (existingId != null) {
            try {
                val request = Request.Builder()
                    .url("$UPLOAD/files/$existingId?uploadType=media&fields=id,modifiedTime")
                    .auth(token)
                    .patch(json.toRequestBody(JSON))
                    .build()
                return@withContext JSONObject(execute(request)).toDriveFile()
            } catch (e: DriveException) {
                if (e.code != 404) throw e
                // The file was deleted from Drive in the meantime; create a new one below.
            }
        }
        val metadata = JSONObject()
            .put("name", BACKUP_FILE_NAME)
            .put("mimeType", "application/json")
            .toString()
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody(JSON))
            .addPart(json.toRequestBody(JSON))
            .build()
        val request = Request.Builder()
            .url("$UPLOAD/files?uploadType=multipart&fields=id,modifiedTime")
            .auth(token)
            .post(multipart)
            .build()
        JSONObject(execute(request)).toDriveFile()
    }

    suspend fun download(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        execute(Request.Builder().url("$API/files/$fileId?alt=media").auth(token).build())
    }

    private fun execute(request: Request): String = http.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw DriveException(response.code, response.errorMessage(body))
        body
    }

    private fun Response.errorMessage(body: String): String =
        runCatching { JSONObject(body).getJSONObject("error").getString("message") }
            .getOrDefault("HTTP $code")

    private fun Request.Builder.auth(token: String) = header("Authorization", "Bearer $token")

    private fun JSONObject.toDriveFile() = DriveFile(getString("id"), optString("modifiedTime").ifEmpty { null })

    companion object {
        const val BACKUP_FILE_NAME = "WaterTracker-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private val JSON = "application/json; charset=UTF-8".toMediaType()
    }
}
