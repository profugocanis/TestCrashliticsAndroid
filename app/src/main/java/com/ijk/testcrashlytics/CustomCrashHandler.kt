package com.ijk.testcrashlytics

import com.ijk.testcrashlytics.extention.disableSslVerification
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread

object CustomCrashHandler {

    const val URL = "http://mf.bot.nu:5678/webhook-test/d72f5c25-0ae5-4d5b-b349-42a1f52f05b7"

    fun setup() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val error = """
                {
                    "stackTrace": "${throwable.fillInStackTrace().stackTraceToString()}"
                }
            """.trimIndent()
            logger("error ${throwable.fillInStackTrace().stackTraceToString()}")
            sendPostRequest(JSONObject(error))
        }
    }

    fun sendPostRequest(jsonBody: JSONObject) {
        thread {
            sendPostRequestInThread(jsonBody)
        }
    }

    fun sendPostRequestInThread(jsonBody: JSONObject): String? {
        val client = OkHttpClient.Builder()
            .disableSslVerification()
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(URL)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                response.body?.string()
            } else {
                "Error: ${response.code} - ${response.message}"
            }
        }
    }
}