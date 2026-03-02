package com.openfoodfacts.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.HttpUrl

internal data class HttpResponsePayload(
  val statusCode: Int,
  val body: String,
)

internal class OpenFoodFactsHttpClient(
  private val client: OkHttpClient = OkHttpClient(),
) {
  suspend fun get(
    url: HttpUrl,
    headers: Map<String, String>,
  ): HttpResponsePayload = execute(
    Request.Builder()
      .url(url)
      .get()
      .applyHeaders(headers)
      .build()
  )

  suspend fun postForm(
    url: HttpUrl,
    headers: Map<String, String>,
    body: Map<String, String>,
  ): HttpResponsePayload {
    val formBody =
      FormBody.Builder().apply {
        body.forEach { (key, value) -> add(key, value) }
      }.build()

    return execute(
      Request.Builder()
        .url(url)
        .post(formBody)
        .applyHeaders(headers)
        .build()
    )
  }

  suspend fun request(
    url: HttpUrl,
    method: String,
    headers: Map<String, String>,
    body: String? = null,
  ): HttpResponsePayload {
    val requestBuilder =
      Request.Builder()
        .url(url)
        .applyHeaders(headers)

    val requestBody =
      body?.toRequestBody(
        (headers["Content-Type"] ?: "application/json").toMediaType()
      )

    requestBuilder.method(method, requestBody)

    return execute(requestBuilder.build())
  }

  private suspend fun execute(request: Request): HttpResponsePayload =
    withContext(Dispatchers.IO) {
      try {
        client.newCall(request).execute().use { response ->
          val payload = response.body?.string().orEmpty()
          HttpResponsePayload(
            statusCode = response.code,
            body = payload,
          )
        }
      } catch (error: Exception) {
        throw OpenFoodFactsException(
          errorCode = "E_NETWORK",
          message = error.message ?: "Network request failed.",
          endpoint = request.url.encodedPath,
          cause = error,
        )
      }
    }
}

private fun Request.Builder.applyHeaders(headers: Map<String, String>): Request.Builder = apply {
  headers.forEach { (key, value) ->
    header(key, value)
  }
}
