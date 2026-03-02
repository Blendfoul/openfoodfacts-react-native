package com.openfoodfacts.api

import android.util.Base64
import com.openfoodfacts.model.ApiStatus
import com.openfoodfacts.model.ConfigPatch
import com.openfoodfacts.model.LocalizedStringEntry
import com.openfoodfacts.model.NutrientMeta
import com.openfoodfacts.model.NutrientMetadata
import com.openfoodfacts.model.NutrientMetadataEntry
import com.openfoodfacts.model.NutrimentEntry
import com.openfoodfacts.model.OffConfigState
import com.openfoodfacts.model.OffEnvironment
import com.openfoodfacts.model.OrderedNutrient
import com.openfoodfacts.model.ProductImageDeleteRequest
import com.openfoodfacts.model.ProductImageUploadRequest
import com.openfoodfacts.model.ProductPatchRequest
import com.openfoodfacts.model.Product
import com.openfoodfacts.model.ProductQuery
import com.openfoodfacts.model.ProductRevertRequest
import com.openfoodfacts.model.ProductResponse
import com.openfoodfacts.model.SaveProductRequest
import com.openfoodfacts.model.SuggestionsQuery
import com.openfoodfacts.model.TagKnowledgeQuery
import com.openfoodfacts.model.TaxonomyCanonicalizeQuery
import com.openfoodfacts.model.TaxonomyCanonicalizeResponse
import com.openfoodfacts.model.TaxonomyDisplayQuery
import com.openfoodfacts.model.TaxonomyDisplayResponse
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class OpenFoodFactsApiClient(
  private val configStore: OpenFoodFactsConfigStore,
  private val httpClient: OpenFoodFactsHttpClient = OpenFoodFactsHttpClient(),
  private val endpoints: OpenFoodFactsEndpoints = OpenFoodFactsEndpoints(),
) {
  fun configure(patch: ConfigPatch): OffConfigState = configStore.configure(patch)

  fun getConfig(): OffConfigState = configStore.get()

  fun resetConfig(): OffConfigState = configStore.reset()

  suspend fun getProduct(query: ProductQuery): ProductResponse {
    val config = configStore.get()
    val requestQuery = mutableMapOf<String, String>()
    requestQuery["cc"] = query.country ?: config.country
    if (query.languages.isNotEmpty()) {
      requestQuery["lc"] = query.languages.joinToString(",")
      requestQuery["tags_lc"] = query.languages.first()
    }
    if (query.fields.isNotEmpty()) {
      requestQuery["fields"] = query.fields.joinToString(",")
    }
    requestQuery.putAll(userAgentQuery(config))

    val payload = httpClient.get(
      url = endpoints.world(config, "/api/v3/product/${query.barcode}", requestQuery),
      headers = jsonHeaders(config),
    )
    ensureSuccess(payload.statusCode, "/api/v3/product/${query.barcode}")
    return parseProductResponse(payload.body)
  }

  suspend fun getOrderedNutrients(): List<OrderedNutrient> {
    val config = configStore.get()
    val body = linkedMapOf(
      "cc" to config.country,
      "lc" to config.uiLanguage,
    ).apply {
      putAll(userAgentBody(config))
    }

    val payload = httpClient.postForm(
      url = endpoints.world(config, "/cgi/nutrients.pl"),
      headers = formHeaders(config),
      body = body,
    )
    ensureSuccess(payload.statusCode, "/cgi/nutrients.pl")

    try {
      val root = JSONObject(payload.body)
      val nutrients = mutableListOf<OrderedNutrient>()
      flattenOrderedNutrients(root.optJSONArray("nutrients"), nutrients)
      return nutrients
    } catch (error: JSONException) {
      throw deserialization(error, "/cgi/nutrients.pl")
    }
  }

  suspend fun getNutrientMetadata(): NutrientMetadata {
    val config = configStore.get()
    val payload = httpClient.get(
      url = endpoints.taxonomies(config, "/data/taxonomies/nutrients.json"),
      headers = jsonHeaders(config, addAuthHeader = true),
    )
    ensureSuccess(payload.statusCode, "/data/taxonomies/nutrients.json")

    try {
      val root = JSONObject(payload.body)
      val entries = mutableListOf<NutrientMetadataEntry>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val rawKey = keys.next()
        val metaObject = root.optJSONObject(rawKey) ?: continue
        val namesObject = metaObject.optJSONObject("name")
        val localizedNames = mutableListOf<LocalizedStringEntry>()
        if (namesObject != null) {
          val nameKeys = namesObject.keys()
          while (nameKeys.hasNext()) {
            val languageCode = nameKeys.next()
            val value = namesObject.optString(languageCode, "")
            if (value.isNotBlank()) {
              localizedNames += LocalizedStringEntry(languageCode = languageCode, value = value)
            }
          }
        }
        val unit = metaObject.optJSONObject("unit")?.optString("en")?.takeIf { it.isNotBlank() } ?: "missing"
        entries +=
          NutrientMetadataEntry(
            key = rawKey.removePrefix("zz:"),
            meta = NutrientMeta(unit = normalizeUnit(unit), name = localizedNames),
          )
      }
      return NutrientMetadata(nutrients = entries.sortedBy { it.key })
    } catch (error: JSONException) {
      throw deserialization(error, "/data/taxonomies/nutrients.json")
    }
  }

  suspend fun getSuggestions(query: SuggestionsQuery): List<String> {
    val config = configStore.get()
    val requestQuery = linkedMapOf(
      "tagtype" to query.tagtype,
      "cc" to query.country,
      "lc" to query.language,
      "string" to query.query,
      "limit" to query.limit.toString(),
    ).apply {
      query.categories?.takeIf { it.isNotBlank() }?.let { put("categories", it) }
      query.shape?.takeIf { it.isNotBlank() }?.let { put("shape", it) }
      query.term?.takeIf { it.isNotBlank() }?.let { put("term", it) }
      if (query.getSynonyms) {
        put("get_synonyms", "1")
      }
      putAll(userAgentQuery(config))
    }

    val payload = httpClient.get(
      url = endpoints.world(config, "/api/v3/taxonomy_suggestions", requestQuery),
      headers = jsonHeaders(config),
    )
    ensureSuccess(payload.statusCode, "/api/v3/taxonomy_suggestions")

    try {
      val root = JSONObject(payload.body)
      val suggestions = root.optJSONArray("suggestions") ?: JSONArray()
      return buildList {
        for (index in 0 until suggestions.length()) {
          val value = suggestions.optString(index, "")
          if (value.isNotBlank()) {
            add(value)
          }
        }
      }
    } catch (error: JSONException) {
      throw deserialization(error, "/api/v3/taxonomy_suggestions")
    }
  }

  suspend fun patchProduct(request: ProductPatchRequest): String {
    val config = configStore.get()
    val endpoint = "/api/v3/product/${request.code}"
    val payload =
      httpClient.request(
        url = endpoints.world(config, endpoint),
        method = "PATCH",
        headers = jsonRequestHeaders(config, addAuthHeader = true),
        body = request.bodyJson,
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun uploadProductImage(request: ProductImageUploadRequest): String {
    val config = configStore.get()
    val endpoint = "/api/v3/product/${request.code}/images"
    val payload =
      httpClient.request(
        url = endpoints.world(config, endpoint),
        method = "POST",
        headers = jsonRequestHeaders(config, addAuthHeader = true),
        body = request.bodyJson,
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun deleteProductImage(request: ProductImageDeleteRequest): String {
    val config = configStore.get()
    val endpoint = "/api/v3/product/${request.code}/images/uploaded/${request.imageId}"
    val payload =
      httpClient.request(
        url = endpoints.world(config, endpoint),
        method = "DELETE",
        headers = jsonHeaders(config, addAuthHeader = true),
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun canonicalizeTags(query: TaxonomyCanonicalizeQuery): TaxonomyCanonicalizeResponse {
    val config = configStore.get()
    val requestQuery =
      linkedMapOf(
        "tagtype" to query.tagtype,
        "local_tags_list" to query.localTags.joinToString(","),
      ).apply {
        query.language?.let { put("lc", it) }
      }
    val endpoint = "/api/v3/taxonomy_canonicalize_tags"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint, requestQuery),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)

    try {
      val root = JSONObject(payload.body)
      val canonicalTags =
        root.optString("canonical_tags_list", "")
          .split(",")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
      return TaxonomyCanonicalizeResponse(
        canonicalTags = canonicalTags,
        rawJson = payload.body,
      )
    } catch (error: JSONException) {
      throw deserialization(error, endpoint)
    }
  }

  suspend fun displayTags(query: TaxonomyDisplayQuery): TaxonomyDisplayResponse {
    val config = configStore.get()
    val requestQuery =
      linkedMapOf(
        "tagtype" to query.tagtype,
        "canonical_tags_list" to query.canonicalTags.joinToString(","),
      ).apply {
        query.language?.let { put("lc", it) }
      }
    val endpoint = "/api/v3/taxonomy_display_tags"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint, requestQuery),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)

    try {
      val root = JSONObject(payload.body)
      val displayTags = mutableListOf<String>()
      val values = root.optJSONArray("display_tags") ?: JSONArray()
      for (index in 0 until values.length()) {
        val value = values.optString(index, "").trim()
        if (value.isNotEmpty()) {
          displayTags += value
        }
      }
      return TaxonomyDisplayResponse(
        displayTags = displayTags,
        rawJson = payload.body,
      )
    } catch (error: JSONException) {
      throw deserialization(error, endpoint)
    }
  }

  suspend fun getTagKnowledge(query: TagKnowledgeQuery): String {
    val config = configStore.get()
    val requestQuery = linkedMapOf<String, String>().apply {
      query.country?.let { put("cc", it) }
      query.language?.let { put("lc", it) }
    }
    val endpoint = "/api/v3/tag/${query.tagtype}/${query.tag}"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint, requestQuery),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun revertProduct(request: ProductRevertRequest): String {
    val config = configStore.get()
    val endpoint = "/api/v3/product_revert"
    val body =
      JSONObject().apply {
        put("code", request.code)
        put("rev", request.revision)
        if (request.fields.isNotEmpty()) {
          put("fields", request.fields.joinToString(","))
        }
        request.tagsLanguage?.let { put("tags_lc", it) }
      }.toString()

    val payload =
      httpClient.request(
        url = endpoints.world(config, endpoint),
        method = "POST",
        headers = jsonRequestHeaders(config, addAuthHeader = true),
        body = body,
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun getExternalSources(): String {
    val config = configStore.get()
    val endpoint = "/api/v3/external_sources"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun getPreferences(): String {
    val config = configStore.get()
    val endpoint = "/api/v3/preferences"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun getAttributeGroups(): String {
    val config = configStore.get()
    val endpoint = "/api/v3.4/attribute_groups"
    val payload =
      httpClient.get(
        url = endpoints.world(config, endpoint),
        headers = jsonHeaders(config),
      )
    ensureSuccess(payload.statusCode, endpoint)
    return payload.body
  }

  suspend fun saveProduct(request: SaveProductRequest): ApiStatus {
    val config = configStore.get()
    if (config.environment == OffEnvironment.PRODUCTION && config.globalUser == null) {
      throw OpenFoodFactsException(
        errorCode = "E_CONFIG",
        message = "Production writes require configured credentials.",
        endpoint = "/cgi/product_jqm2.pl",
      )
    }

    val body = linkedMapOf<String, String>()
    request.fields.forEach { body[it.key] = it.value }
    body["lc"] = config.productsLanguage
    body["cc"] = config.country
    body.putAll(globalUserBody(config))
    body.putAll(userAgentBody(config))

    val payload = httpClient.postForm(
      url = endpoints.world(config, "/cgi/product_jqm2.pl"),
      headers = formHeaders(config),
      body = body,
    )
    ensureSuccess(payload.statusCode, "/cgi/product_jqm2.pl")

    val status = parseApiStatus(payload.body)
    if (status.status != 1) {
      throw OpenFoodFactsException(
        errorCode = "E_API_STATUS",
        message = status.statusVerbose ?: status.error ?: "Open Food Facts rejected the save request.",
        endpoint = "/cgi/product_jqm2.pl",
        apiStatus = status,
      )
    }
    return status
  }

  private fun userAgentQuery(config: OffConfigState): Map<String, String> = userAgentBody(config)

  private fun userAgentBody(config: OffConfigState): Map<String, String> =
    linkedMapOf(
      "app_name" to (config.userAgent.name ?: ""),
      "app_version" to (config.userAgent.version ?: ""),
      "app_uuid" to config.appUuid,
      "app_platform" to (config.userAgent.system ?: ""),
      "comment" to (config.userAgent.comment ?: ""),
    )

  private fun globalUserBody(config: OffConfigState): Map<String, String> =
    config.globalUser?.let {
      linkedMapOf(
        "user_id" to it.userId,
        "password" to it.password,
        "comment" to (it.comment ?: ""),
      )
    } ?: emptyMap()

  private fun jsonHeaders(
    config: OffConfigState,
    addAuthHeader: Boolean = false,
  ): Map<String, String> =
    mutableMapOf<String, String>().apply {
      put("Accept", "application/json")
      put("User-Agent", userAgentHeader(config))
      put("From", config.globalUser?.userId ?: "anonymous")
      if (addAuthHeader && config.globalUser != null) {
        val encoded =
          Base64.encodeToString(
            "${config.globalUser.userId}:${config.globalUser.password}".toByteArray(),
            Base64.NO_WRAP,
          )
        put("Authorization", "Basic $encoded")
      }
    }

  private fun formHeaders(config: OffConfigState): Map<String, String> =
    jsonHeaders(config) + ("Content-Type" to "application/x-www-form-urlencoded")

  private fun jsonRequestHeaders(
    config: OffConfigState,
    addAuthHeader: Boolean = false,
  ): Map<String, String> =
    jsonHeaders(config, addAuthHeader) + ("Content-Type" to "application/json")

  private fun userAgentHeader(config: OffConfigState): String {
    val parts =
      listOf(
        config.userAgent.name,
        config.userAgent.version,
        config.userAgent.system,
        config.userAgent.comment,
      ).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }

    return parts.joinToString(" - ").ifEmpty { "OpenFoodFacts Android" }
  }

  private fun ensureSuccess(statusCode: Int, endpoint: String) {
    if (statusCode in 200..299) {
      return
    }

    throw OpenFoodFactsException(
      errorCode = "E_HTTP_STATUS",
      message = "Request failed with HTTP status $statusCode.",
      httpStatus = statusCode,
      endpoint = endpoint,
    )
  }

  private fun flattenOrderedNutrients(source: JSONArray?, destination: MutableList<OrderedNutrient>) {
    if (source == null) {
      return
    }

    for (index in 0 until source.length()) {
      val item = source.optJSONObject(index) ?: continue
      destination +=
        OrderedNutrient(
          id = item.optString("id", ""),
          name = item.optString("name", ""),
          important = item.optBoolean("important", false),
          displayInEditForm = item.optBoolean("display_in_edit_form", false),
        )
      flattenOrderedNutrients(item.optJSONArray("nutrients"), destination)
    }
  }

  private fun parseProductResponse(payload: String): ProductResponse =
    try {
      val root = JSONObject(payload)
      val barcode = root.optStringOrNull("code")
      val status = root.optStringOrNull("status")
      val product = root.optJSONObject("product")?.let(::parseProduct)
      val hasProduct =
        status in setOf("product_found", "success", "success_with_warnings")

      ProductResponse(
        barcode = barcode,
        status = status,
        product = product,
        hasProduct = hasProduct,
      )
    } catch (error: JSONException) {
      throw deserialization(error, "/api/v3/product")
    }

  private fun parseProduct(json: JSONObject): Product =
    Product(
      code = json.optString("code"),
      productName = json.optStringOrNull("product_name"),
      productNameEn = json.optStringOrNull("product_name_en"),
      brands = json.optStringOrNull("brands"),
      lang = json.optStringOrNull("lang")?.lowercase(Locale.ROOT),
      quantity = json.optStringOrNull("quantity"),
      packagingQuantity = json.optNumericOrNull("product_quantity"),
      servingSize = json.optStringOrNull("serving_size"),
      servingQuantity = json.optNumericOrNull("serving_quantity"),
      dataPer = json.optStringOrNull("nutrition_data_per"),
      categories = json.optStringOrNull("categories"),
      imageFront = json.optStringOrNull("image_front_url"),
      imageIngredients = json.optStringOrNull("image_ingredients_url"),
      imageNutrition = json.optStringOrNull("image_nutrition_url"),
      keywords = json.optStringArray("_keywords"),
      nutriments = json.optNutriments("nutriments"),
    )

  private fun parseApiStatus(payload: String): ApiStatus =
    try {
      val root = JSONObject(payload)
      ApiStatus(
        status = if (root.has("status") && !root.isNull("status")) root.optInt("status") else null,
        statusVerbose = root.optStringOrNull("status_verbose"),
        body = root.optStringOrNull("body"),
        error = root.optStringOrNull("error"),
        imageId = if (root.has("imgid") && !root.isNull("imgid")) root.optInt("imgid") else null,
      )
    } catch (_: JSONException) {
      ApiStatus(
        status = 400,
        statusVerbose =
          if (payload.contains("Incorrect user name or password")) {
            "Incorrect user name or password"
          } else {
            "Unexpected non-JSON response"
          },
        body = payload,
        error = null,
        imageId = null,
      )
    }

  private fun normalizeUnit(value: String): String =
    when (value.lowercase(Locale.ROOT)) {
      "kcal" -> "kcal"
      "kj" -> "kj"
      "g" -> "g"
      "mg", "milli-gram", "mg." -> "mg"
      "mcg", "µg", "μg", "&#181;g", "&micro;g", "&#xb5;g" -> "µg"
      "ml", "milli-liter" -> "ml"
      "l", "liter" -> "l"
      "% vol" -> "% vol"
      "%", "percent", "per cent" -> "%"
      "mmol/l" -> "mmol/l"
      else -> "missing"
    }

  private fun deserialization(error: Throwable, endpoint: String): OpenFoodFactsException =
    OpenFoodFactsException(
      errorCode = "E_DESERIALIZATION",
      message = error.message ?: "Failed to parse Open Food Facts response.",
      endpoint = endpoint,
      cause = error,
    )
}

private fun JSONObject.optStringOrNull(key: String): String? =
  if (has(key) && !isNull(key)) {
    optString(key).takeIf { it.isNotBlank() }
  } else {
    null
  }

private fun JSONObject.optNumericOrNull(key: String): Double? {
  if (!has(key) || isNull(key)) {
    return null
  }

  val value = opt(key)
  return when (value) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
  }
}

private fun JSONObject.optStringArray(key: String): List<String>? {
  val array = optJSONArray(key) ?: return null
  val values = mutableListOf<String>()
  for (index in 0 until array.length()) {
    val item = array.optString(index, "")
    if (item.isNotBlank()) {
      values += item
    }
  }
  return values.ifEmpty { null }
}

private fun JSONObject.optNutriments(key: String): List<NutrimentEntry>? {
  val obj = optJSONObject(key) ?: return null
  val entries = mutableListOf<NutrimentEntry>()
  val keys = obj.keys()
  while (keys.hasNext()) {
    val nutrientKey = keys.next()
    val value = obj.opt(nutrientKey)
    when (value) {
      is Number ->
        entries += NutrimentEntry(key = nutrientKey, numberValue = value.toDouble(), stringValue = null)
      is String ->
        entries += NutrimentEntry(key = nutrientKey, numberValue = value.toDoubleOrNull(), stringValue = value)
    }
  }
  return entries.sortedBy { it.key }.ifEmpty { null }
}
