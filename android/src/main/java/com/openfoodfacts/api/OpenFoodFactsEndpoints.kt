package com.openfoodfacts.api

import com.openfoodfacts.model.OffConfigState
import com.openfoodfacts.model.OffEnvironment
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal class OpenFoodFactsEndpoints(
  private val worldBaseUrlOverride: HttpUrl? = null,
  private val taxonomiesBaseUrlOverride: HttpUrl? = null,
) {
  fun world(
    config: OffConfigState,
    path: String,
    query: Map<String, String> = emptyMap(),
  ): HttpUrl = build(worldBaseUrlOverride ?: defaultBaseUrl(config, "world"), path, query)

  fun taxonomies(
    config: OffConfigState,
    path: String,
    query: Map<String, String> = emptyMap(),
  ): HttpUrl = build(taxonomiesBaseUrlOverride ?: defaultBaseUrl(config, "world"), path, query)

  private fun defaultBaseUrl(config: OffConfigState, subdomain: String): HttpUrl {
    val domain =
      if (config.environment == OffEnvironment.STAGING) {
        "openfoodfacts.net"
      } else {
        "openfoodfacts.org"
      }

    return "https://$subdomain.$domain/".toHttpUrl()
  }

  private fun build(baseUrl: HttpUrl, path: String, query: Map<String, String>): HttpUrl {
    val builder = baseUrl.newBuilder()
    builder.encodedPath(path.ensureLeadingSlash())
    query.forEach { (key, value) ->
      builder.addQueryParameter(key, value)
    }
    return builder.build()
  }
}

private fun String.ensureLeadingSlash(): String = if (startsWith("/")) this else "/$this"
