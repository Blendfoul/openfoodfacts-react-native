package com.openfoodfacts.model

internal enum class OffEnvironment(val wireValue: String) {
  PRODUCTION("production"),
  STAGING("staging");

  companion object {
    fun fromWire(value: String?): OffEnvironment =
      when (value?.trim()?.lowercase()) {
        STAGING.wireValue -> STAGING
        else -> PRODUCTION
      }
  }
}

internal data class OffUser(
  val comment: String?,
  val userId: String,
  val password: String,
)

internal data class OffUserAgent(
  val name: String?,
  val version: String?,
  val system: String?,
  val comment: String?,
)

internal data class OffConfigState(
  val environment: OffEnvironment,
  val useRequired: Boolean,
  val globalUser: OffUser?,
  val userAgent: OffUserAgent,
  val country: String,
  val uiLanguage: String,
  val productsLanguage: String,
  val appUuid: String,
)

internal data class ConfigPatch(
  val environment: OffEnvironment? = null,
  val useRequired: Boolean? = null,
  val hasGlobalUser: Boolean = false,
  val globalUser: OffUser? = null,
  val hasUserAgent: Boolean = false,
  val userAgent: OffUserAgent? = null,
  val country: String? = null,
  val uiLanguage: String? = null,
  val productsLanguage: String? = null,
)

internal data class ProductQuery(
  val barcode: String,
  val languages: List<String>,
  val country: String?,
  val fields: List<String>,
)

internal data class NutrimentEntry(
  val key: String,
  val numberValue: Double?,
  val stringValue: String?,
)

internal data class Product(
  val code: String,
  val productName: String?,
  val productNameEn: String?,
  val brands: String?,
  val lang: String?,
  val quantity: String?,
  val packagingQuantity: Double?,
  val servingSize: String?,
  val servingQuantity: Double?,
  val dataPer: String?,
  val categories: String?,
  val imageFront: String?,
  val imageIngredients: String?,
  val imageNutrition: String?,
  val keywords: List<String>?,
  val nutriments: List<NutrimentEntry>?,
)

internal data class ProductResponse(
  val barcode: String?,
  val status: String?,
  val product: Product?,
  val hasProduct: Boolean,
)

internal data class SearchResponse(
  val count: Int,
  val page: Int,
  val pageSize: Int,
  val pageCount: Int,
  val skip: Int,
  val products: List<Product>,
)

internal data class OrderedNutrient(
  val id: String,
  val name: String,
  val important: Boolean,
  val displayInEditForm: Boolean,
)

internal data class LocalizedStringEntry(
  val languageCode: String,
  val value: String,
)

internal data class NutrientMeta(
  val unit: String?,
  val name: List<LocalizedStringEntry>,
)

internal data class NutrientMetadataEntry(
  val key: String,
  val meta: NutrientMeta,
)

internal data class NutrientMetadata(
  val nutrients: List<NutrientMetadataEntry>,
)

internal data class SuggestionsQuery(
  val query: String,
  val tagtype: String,
  val country: String,
  val language: String,
  val categories: String?,
  val shape: String?,
  val getSynonyms: Boolean,
  val term: String?,
  val limit: Int,
)

internal data class SearchQuery(
  val parameters: Map<String, String>,
)

internal data class ProductPatchRequest(
  val code: String,
  val bodyJson: String,
)

internal data class ProductImageUploadRequest(
  val code: String,
  val bodyJson: String,
)

internal data class ProductImageDeleteRequest(
  val code: String,
  val imageId: Int,
)

internal data class TaxonomyCanonicalizeQuery(
  val tagtype: String,
  val localTags: List<String>,
  val language: String?,
)

internal data class TaxonomyCanonicalizeResponse(
  val canonicalTags: List<String>,
  val rawJson: String,
)

internal data class TaxonomyDisplayQuery(
  val tagtype: String,
  val canonicalTags: List<String>,
  val language: String?,
)

internal data class TaxonomyDisplayResponse(
  val displayTags: List<String>,
  val rawJson: String,
)

internal data class TagKnowledgeQuery(
  val tagtype: String,
  val tag: String,
  val country: String?,
  val language: String?,
)

internal data class ProductRevertRequest(
  val code: String,
  val revision: Int,
  val fields: List<String>,
  val tagsLanguage: String?,
)

internal data class SaveProductField(
  val key: String,
  val value: String,
)

internal data class SaveProductRequest(
  val fields: List<SaveProductField>,
)

internal data class ApiStatus(
  val status: Int?,
  val statusVerbose: String?,
  val body: String?,
  val error: String?,
  val imageId: Int?,
)
