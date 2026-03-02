package com.openfoodfacts.api

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.openfoodfacts.model.ApiStatus
import com.openfoodfacts.model.ConfigPatch
import com.openfoodfacts.model.LocalizedStringEntry
import com.openfoodfacts.model.NutrientMeta
import com.openfoodfacts.model.NutrientMetadata
import com.openfoodfacts.model.NutrientMetadataEntry
import com.openfoodfacts.model.NutrimentEntry
import com.openfoodfacts.model.OffConfigState
import com.openfoodfacts.model.OffEnvironment
import com.openfoodfacts.model.OffUser
import com.openfoodfacts.model.OffUserAgent
import com.openfoodfacts.model.OrderedNutrient
import com.openfoodfacts.model.ProductImageDeleteRequest
import com.openfoodfacts.model.ProductImageUploadRequest
import com.openfoodfacts.model.ProductPatchRequest
import com.openfoodfacts.model.Product
import com.openfoodfacts.model.ProductQuery
import com.openfoodfacts.model.ProductRevertRequest
import com.openfoodfacts.model.ProductResponse
import com.openfoodfacts.model.SaveProductField
import com.openfoodfacts.model.SaveProductRequest
import com.openfoodfacts.model.SuggestionsQuery
import com.openfoodfacts.model.TagKnowledgeQuery
import com.openfoodfacts.model.TaxonomyCanonicalizeQuery
import com.openfoodfacts.model.TaxonomyCanonicalizeResponse
import com.openfoodfacts.model.TaxonomyDisplayQuery
import com.openfoodfacts.model.TaxonomyDisplayResponse
import java.util.Locale

internal object OpenFoodFactsMappers {
  private val validCountries =
    Locale.getISOCountries().map { it.lowercase(Locale.ROOT) }.toMutableSet().apply {
      add("uk")
    }.toSet()

  private val validLanguages =
    Locale.getISOLanguages().map { it.lowercase(Locale.ROOT) }.toMutableSet().apply {
      addAll(listOf("tl", "mo", "cu"))
    }.toSet()

  fun parseConfigPatch(config: ReadableMap): ConfigPatch =
    ConfigPatch(
      environment =
        if (config.hasKey("environment") && !config.isNull("environment")) {
          OffEnvironment.fromWire(config.getString("environment"))
        } else {
          null
        },
      useRequired =
        if (config.hasKey("useRequired") && !config.isNull("useRequired")) {
          config.getBoolean("useRequired")
        } else {
          null
        },
      hasGlobalUser = config.hasKey("globalUser"),
      globalUser =
        if (config.hasKey("globalUser") && !config.isNull("globalUser")) {
          parseUser(requireMap(config, "globalUser"))
        } else {
          null
        },
      hasUserAgent = config.hasKey("userAgent"),
      userAgent =
        if (config.hasKey("userAgent") && !config.isNull("userAgent")) {
          parseUserAgent(requireMap(config, "userAgent"))
        } else {
          null
        },
      country =
        if (config.hasKey("country") && !config.isNull("country")) {
          validateCountry(requireTrimmedString(config, "country"))
        } else {
          null
        },
      uiLanguage =
        if (config.hasKey("uiLanguage") && !config.isNull("uiLanguage")) {
          validateLanguage(requireTrimmedString(config, "uiLanguage"), "uiLanguage")
        } else {
          null
        },
      productsLanguage =
        if (config.hasKey("productsLanguage") && !config.isNull("productsLanguage")) {
          validateLanguage(requireTrimmedString(config, "productsLanguage"), "productsLanguage")
        } else {
          null
        },
    )

  fun parseProductQuery(query: ReadableMap): ProductQuery {
    val barcode = requireTrimmedString(query, "barcode")
    if (barcode.isEmpty()) {
      throw validation("barcode must not be empty.")
    }

    val languages = readStringList(query, "languages").map { validateLanguage(it, "languages") }
    val country =
      if (query.hasKey("country") && !query.isNull("country")) {
        validateCountry(requireTrimmedString(query, "country"))
      } else {
        null
      }
    val fields = readStringList(query, "fields")

    return ProductQuery(
      barcode = barcode,
      languages = languages,
      country = country,
      fields = fields,
    )
  }

  fun parseSuggestionsQuery(query: ReadableMap): SuggestionsQuery {
    val country = validateCountry(requireTrimmedString(query, "country"))
    val language = validateLanguage(requireTrimmedString(query, "language"), "language")
    val userQuery =
      if (query.hasKey("query") && !query.isNull("query")) {
        query.getString("query")?.trim().orEmpty()
      } else {
        ""
      }
    val tagtype =
      if (query.hasKey("tagtype") && !query.isNull("tagtype")) {
        requireTrimmedString(query, "tagtype")
      } else {
        "categories"
      }
    val categories = optionalTrimmedString(query, "categories")
    val shape = optionalTrimmedString(query, "shape")
    val getSynonyms =
      if (query.hasKey("getSynonyms") && !query.isNull("getSynonyms")) {
        query.getBoolean("getSynonyms")
      } else {
        false
      }
    val term = optionalTrimmedString(query, "term")
    val limit =
      if (query.hasKey("limit") && !query.isNull("limit")) {
        query.getDouble("limit").toInt()
      } else {
        25
      }

    if (limit < 1) {
      throw validation("limit must be greater than 0.")
    }
    if (limit > 400) {
      throw validation("limit must be less than or equal to 400.")
    }

    return SuggestionsQuery(
      query = userQuery,
      tagtype = tagtype,
      country = country,
      language = language,
      categories = categories,
      shape = shape,
      getSynonyms = getSynonyms,
      term = term,
      limit = limit,
    )
  }

  fun parseProductPatchRequest(request: ReadableMap): ProductPatchRequest =
    ProductPatchRequest(
      code = requiredBarcode(request, "code"),
      bodyJson = requireNonEmptyString(request, "bodyJson"),
    )

  fun parseProductImageUploadRequest(request: ReadableMap): ProductImageUploadRequest =
    ProductImageUploadRequest(
      code = requiredBarcode(request, "code"),
      bodyJson = requireNonEmptyString(request, "bodyJson"),
    )

  fun parseProductImageDeleteRequest(request: ReadableMap): ProductImageDeleteRequest {
    val code = requiredBarcode(request, "code")
    val imageId =
      if (request.hasKey("imageId") && !request.isNull("imageId")) {
        request.getDouble("imageId").toInt()
      } else {
        throw validation("imageId must be a number.")
      }
    if (imageId < 0) {
      throw validation("imageId must be greater than or equal to 0.")
    }

    return ProductImageDeleteRequest(
      code = code,
      imageId = imageId,
    )
  }

  fun parseTaxonomyCanonicalizeQuery(query: ReadableMap): TaxonomyCanonicalizeQuery {
    val tagtype = requireNonEmptyString(query, "tagtype")
    val localTags = readStringList(query, "localTags").map { it.trim() }.filter { it.isNotEmpty() }
    if (localTags.isEmpty()) {
      throw validation("localTags must contain at least one tag.")
    }

    val language =
      if (query.hasKey("language") && !query.isNull("language")) {
        validateLanguage(requireTrimmedString(query, "language"), "language")
      } else {
        null
      }

    return TaxonomyCanonicalizeQuery(
      tagtype = tagtype,
      localTags = localTags,
      language = language,
    )
  }

  fun parseTaxonomyDisplayQuery(query: ReadableMap): TaxonomyDisplayQuery {
    val tagtype = requireNonEmptyString(query, "tagtype")
    val canonicalTags =
      readStringList(query, "canonicalTags").map { it.trim() }.filter { it.isNotEmpty() }
    if (canonicalTags.isEmpty()) {
      throw validation("canonicalTags must contain at least one tag.")
    }

    val language =
      if (query.hasKey("language") && !query.isNull("language")) {
        validateLanguage(requireTrimmedString(query, "language"), "language")
      } else {
        null
      }

    return TaxonomyDisplayQuery(
      tagtype = tagtype,
      canonicalTags = canonicalTags,
      language = language,
    )
  }

  fun parseTagKnowledgeQuery(query: ReadableMap): TagKnowledgeQuery {
    val tagtype = requireNonEmptyString(query, "tagtype")
    val tag = requireNonEmptyString(query, "tag")
    val country =
      if (query.hasKey("country") && !query.isNull("country")) {
        validateCountry(requireTrimmedString(query, "country"))
      } else {
        null
      }
    val language =
      if (query.hasKey("language") && !query.isNull("language")) {
        validateLanguage(requireTrimmedString(query, "language"), "language")
      } else {
        null
      }

    return TagKnowledgeQuery(
      tagtype = tagtype,
      tag = tag,
      country = country,
      language = language,
    )
  }

  fun parseProductRevertRequest(request: ReadableMap): ProductRevertRequest {
    val code = requiredBarcode(request, "code")
    val revision =
      if (request.hasKey("revision") && !request.isNull("revision")) {
        request.getDouble("revision").toInt()
      } else {
        throw validation("revision must be a number.")
      }
    if (revision < 0) {
      throw validation("revision must be greater than or equal to 0.")
    }

    val fields = readStringList(request, "fields").map { it.trim() }.filter { it.isNotEmpty() }
    val tagsLanguage =
      if (request.hasKey("tagsLanguage") && !request.isNull("tagsLanguage")) {
        validateLanguage(requireTrimmedString(request, "tagsLanguage"), "tagsLanguage")
      } else {
        null
      }

    return ProductRevertRequest(
      code = code,
      revision = revision,
      fields = fields,
      tagsLanguage = tagsLanguage,
    )
  }

  fun parseSaveProductRequest(request: ReadableMap): SaveProductRequest {
    val fieldsArray = requireArray(request, "fields")
    val fields = mutableListOf<SaveProductField>()
    val seen = mutableSetOf<String>()

    for (index in 0 until fieldsArray.size()) {
      if (fieldsArray.getType(index) != ReadableType.Map) {
        throw validation("fields[$index] must be an object.")
      }

      val entry = fieldsArray.getMap(index) ?: throw validation("fields[$index] must be an object.")
      val key = requireTrimmedString(entry, "key")
      val value = requireTrimmedString(entry, "value")

      if (key.isEmpty()) {
        throw validation("fields[$index].key must not be empty.")
      }
      if (!seen.add(key)) {
        throw validation("fields contains duplicate key '$key'.")
      }

      fields += SaveProductField(key = key, value = value)
    }

    return SaveProductRequest(fields = fields)
  }

  fun toWritableMap(config: OffConfigState): WritableMap =
    Arguments.createMap().apply {
      putString("environment", config.environment.wireValue)
      putBoolean("useRequired", config.useRequired)
      putString("country", config.country)
      putString("uiLanguage", config.uiLanguage)
      putString("productsLanguage", config.productsLanguage)
      putMap("userAgent", toWritableMap(config.userAgent))
      if (config.globalUser != null) {
        putMap("globalUser", toWritableMap(config.globalUser))
      } else {
        putNull("globalUser")
      }
    }

  fun toWritableMap(response: ProductResponse): WritableMap =
    Arguments.createMap().apply {
      putNullableString("barcode", response.barcode)
      putNullableString("status", response.status)
      putBoolean("hasProduct", response.hasProduct)
      if (response.product != null) {
        putMap("product", toWritableMap(response.product))
      } else {
        putNull("product")
      }
    }

  fun toWritableOrderedNutrients(values: List<OrderedNutrient>): WritableArray =
    Arguments.createArray().apply {
      values.forEach { nutrient ->
        pushMap(
          Arguments.createMap().apply {
            putString("id", nutrient.id)
            putString("name", nutrient.name)
            putBoolean("important", nutrient.important)
            putBoolean("displayInEditForm", nutrient.displayInEditForm)
          }
        )
      }
    }

  fun toWritableMap(metadata: NutrientMetadata): WritableMap =
    Arguments.createMap().apply {
      putArray(
        "nutrients",
        Arguments.createArray().apply {
          metadata.nutrients.forEach { entry ->
            pushMap(
              Arguments.createMap().apply {
                putString("key", entry.key)
                putMap(
                  "meta",
                  Arguments.createMap().apply {
                    putNullableString("unit", entry.meta.unit)
                    putArray("name", toLocalizedStringEntryArray(entry.meta.name))
                  }
                )
              }
            )
          }
        }
      )
    }

  fun toWritableStringArray(values: List<String>): WritableArray =
    Arguments.createArray().apply {
      values.forEach(::pushString)
    }

  fun toWritableMap(status: ApiStatus): WritableMap =
    Arguments.createMap().apply {
      putNullableInt("status", status.status)
      putNullableString("statusVerbose", status.statusVerbose)
      putNullableString("body", status.body)
      putNullableString("error", status.error)
      putNullableInt("imageId", status.imageId)
    }

  fun toWritableMap(response: TaxonomyCanonicalizeResponse): WritableMap =
    Arguments.createMap().apply {
      putArray("canonicalTags", toWritableStringArray(response.canonicalTags))
      putString("rawJson", response.rawJson)
    }

  fun toWritableMap(response: TaxonomyDisplayResponse): WritableMap =
    Arguments.createMap().apply {
      putArray("displayTags", toWritableStringArray(response.displayTags))
      putString("rawJson", response.rawJson)
    }

  private fun toWritableMap(user: OffUser): WritableMap =
    Arguments.createMap().apply {
      putNullableString("comment", user.comment)
      putString("userId", user.userId)
      putString("password", user.password)
    }

  private fun toWritableMap(userAgent: OffUserAgent): WritableMap =
    Arguments.createMap().apply {
      putNullableString("name", userAgent.name)
      putNullableString("version", userAgent.version)
      putNullableString("system", userAgent.system)
      putNullableString("comment", userAgent.comment)
    }

  private fun toWritableMap(product: Product): WritableMap =
    Arguments.createMap().apply {
      putString("code", product.code)
      putNullableString("productName", product.productName)
      putNullableString("productNameEn", product.productNameEn)
      putNullableString("brands", product.brands)
      putNullableString("lang", product.lang)
      putNullableString("quantity", product.quantity)
      putNullableDouble("packagingQuantity", product.packagingQuantity)
      putNullableString("servingSize", product.servingSize)
      putNullableDouble("servingQuantity", product.servingQuantity)
      putNullableString("dataPer", product.dataPer)
      putNullableString("categories", product.categories)
      putNullableString("imageFront", product.imageFront)
      putNullableString("imageIngredients", product.imageIngredients)
      putNullableString("imageNutrition", product.imageNutrition)
      putArray("keywords", toStringArrayOrEmpty(product.keywords))
      putArray("nutriments", toNutrimentEntryArray(product.nutriments))
    }

  private fun toLocalizedStringEntryArray(entries: List<LocalizedStringEntry>): WritableArray =
    Arguments.createArray().apply {
      entries.forEach { entry ->
        pushMap(
          Arguments.createMap().apply {
            putString("languageCode", entry.languageCode)
            putString("value", entry.value)
          }
        )
      }
    }

  private fun toStringArrayOrEmpty(entries: List<String>?): WritableArray =
    Arguments.createArray().apply {
      entries?.forEach(::pushString)
    }

  private fun toNutrimentEntryArray(entries: List<NutrimentEntry>?): WritableArray =
    Arguments.createArray().apply {
      entries?.forEach { entry ->
        pushMap(
          Arguments.createMap().apply {
            putString("key", entry.key)
            putNullableDouble("numberValue", entry.numberValue)
            putNullableString("stringValue", entry.stringValue)
          }
        )
      }
    }

  private fun parseUser(map: ReadableMap): OffUser {
    val userId = requireTrimmedString(map, "userId")
    val password = requireTrimmedString(map, "password")
    if (userId.isEmpty() || password.isEmpty()) {
      throw validation("globalUser.userId and globalUser.password must not be empty.")
    }

    val comment =
      if (map.hasKey("comment") && !map.isNull("comment")) {
        map.getString("comment")?.trim()
      } else {
        null
      }

    return OffUser(comment = comment, userId = userId, password = password)
  }

  private fun parseUserAgent(map: ReadableMap): OffUserAgent =
    OffUserAgent(
      name = optionalTrimmedString(map, "name"),
      version = optionalTrimmedString(map, "version"),
      system = optionalTrimmedString(map, "system"),
      comment = optionalTrimmedString(map, "comment"),
    )

  private fun readStringList(map: ReadableMap, key: String): List<String> {
    if (!map.hasKey(key) || map.isNull(key)) {
      return emptyList()
    }

    val array = requireArray(map, key)
    val result = mutableListOf<String>()
    for (index in 0 until array.size()) {
      if (array.getType(index) != ReadableType.String) {
        throw validation("$key[$index] must be a string.")
      }
      result += array.getString(index)?.trim().orEmpty()
    }
    return result
  }

  private fun requireTrimmedString(map: ReadableMap, key: String): String =
    map.getString(key)?.trim() ?: throw validation("$key must be a string.")

  private fun requireNonEmptyString(map: ReadableMap, key: String): String {
    val value = requireTrimmedString(map, key)
    if (value.isEmpty()) {
      throw validation("$key must not be empty.")
    }
    return value
  }

  private fun optionalTrimmedString(map: ReadableMap, key: String): String? =
    if (map.hasKey(key) && !map.isNull(key)) {
      map.getString(key)?.trim()
    } else {
      null
    }

  private fun requiredBarcode(map: ReadableMap, key: String): String {
    val value = requireTrimmedString(map, key)
    if (value.isEmpty()) {
      throw validation("$key must not be empty.")
    }
    return value
  }

  private fun requireMap(map: ReadableMap, key: String): ReadableMap =
    map.getMap(key) ?: throw validation("$key must be an object.")

  private fun requireArray(map: ReadableMap, key: String): ReadableArray =
    map.getArray(key) ?: throw validation("$key must be an array.")

  private fun validateCountry(rawValue: String): String {
    val normalized = rawValue.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) {
      throw validation("country must not be empty.")
    }
    if (!validCountries.contains(normalized)) {
      throw validation("country '$normalized' is not supported.")
    }
    return normalized
  }

  private fun validateLanguage(rawValue: String, fieldName: String): String {
    val normalized = rawValue.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) {
      throw validation("$fieldName must not be empty.")
    }
    if (!validLanguages.contains(normalized)) {
      throw validation("$fieldName '$normalized' is not supported.")
    }
    return normalized
  }

  private fun validation(message: String): OpenFoodFactsException =
    OpenFoodFactsException(
      errorCode = "E_VALIDATION",
      message = message,
    )
}

private fun WritableMap.putNullableString(key: String, value: String?) {
  if (value == null) {
    putNull(key)
  } else {
    putString(key, value)
  }
}

private fun WritableMap.putNullableDouble(key: String, value: Double?) {
  if (value == null) {
    putNull(key)
  } else {
    putDouble(key, value)
  }
}

private fun WritableMap.putNullableInt(key: String, value: Int?) {
  if (value == null) {
    putNull(key)
  } else {
    putInt(key, value)
  }
}
