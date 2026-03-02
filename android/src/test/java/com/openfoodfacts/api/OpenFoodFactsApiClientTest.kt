package com.openfoodfacts.api

import com.openfoodfacts.model.ConfigPatch
import com.openfoodfacts.model.OffUser
import com.openfoodfacts.model.OffUserAgent
import com.openfoodfacts.model.ProductQuery
import com.openfoodfacts.model.SaveProductField
import com.openfoodfacts.model.SaveProductRequest
import com.openfoodfacts.model.SuggestionsQuery
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class OpenFoodFactsApiClientTest {
  private lateinit var server: MockWebServer
  private lateinit var configStore: OpenFoodFactsConfigStore
  private lateinit var apiClient: OpenFoodFactsApiClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()

    configStore =
      OpenFoodFactsConfigStore(
        generatedUserAgentProvider = {
          OffUserAgent(
            name = "TestApp",
            version = "1.0.0",
            system = "Android 15",
            comment = "TestApp - 1.0.0 - Android 15 - test-uuid",
          )
        },
        uuidProvider = { "test-uuid" },
      )

    apiClient =
      OpenFoodFactsApiClient(
        configStore = configStore,
        endpoints = OpenFoodFactsEndpoints(
          worldBaseUrlOverride = server.url("/"),
          taxonomiesBaseUrlOverride = server.url("/"),
        ),
      )
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun getProduct_mapsTypedFieldsAndAppendsUserAgentQuery() = runBlocking {
    server.enqueue(
      MockResponse().setBody(
        """
        {
          "code": "1234567890123",
          "status": "product_found",
          "product": {
            "code": "1234567890123",
            "product_name": "Test Product",
            "product_name_en": "Test Product",
            "brands": "OFF",
            "lang": "en",
            "quantity": "100 g",
            "product_quantity": "100",
            "serving_size": "10 g",
            "serving_quantity": "10",
            "nutrition_data_per": "100g",
            "categories": "Snacks",
            "image_front_url": "https://example.com/front.jpg",
            "image_ingredients_url": "https://example.com/ingredients.jpg",
            "image_nutrition_url": "https://example.com/nutrition.jpg",
            "_keywords": ["test", "product"],
            "nutriments": {
              "fat_100g": 1.5,
              "energy_unit": "kcal"
            }
          }
        }
        """.trimIndent()
      )
    )

    val response = apiClient.getProduct(ProductQuery(barcode = "1234567890123", languages = emptyList(), country = null, fields = emptyList()))
    val request = server.takeRequest()

    assertTrue(response.hasProduct)
    assertEquals(100.0, response.product?.packagingQuantity)
    assertEquals(10.0, response.product?.servingQuantity)
    assertEquals(2, response.product?.nutriments?.size)
    assertTrue(request.requestUrl?.queryParameterNames?.contains("app_name") == true)
    assertEquals("/api/v3/product/1234567890123", request.requestUrl?.encodedPath)
  }

  @Test
  fun getSuggestions_usesDefaults() = runBlocking {
    server.enqueue(MockResponse().setBody("""{"suggestions":["Alpha","Beta"]}"""))

    val result = apiClient.getSuggestions(SuggestionsQuery(query = "", country = "us", language = "en", limit = 15))
    val request = server.takeRequest()

    assertEquals(listOf("Alpha", "Beta"), result)
    assertEquals("15", request.requestUrl?.queryParameter("limit"))
    assertEquals("", request.requestUrl?.queryParameter("string"))
  }

  @Test
  fun getNutrientMetadata_stripsTaxonomyPrefix() = runBlocking {
    server.enqueue(
      MockResponse().setBody(
        """
        {
          "zz:energy-kcal": {
            "name": { "en": "Energy" },
            "unit": { "en": "kcal" }
          }
        }
        """.trimIndent()
      )
    )

    val metadata = apiClient.getNutrientMetadata()

    assertEquals(1, metadata.nutrients.size)
    assertEquals("energy-kcal", metadata.nutrients.first().key)
    assertEquals("kcal", metadata.nutrients.first().meta.unit)
  }

  @Test
  fun saveProduct_requiresCredentialsInProduction() = runBlocking {
    try {
      apiClient.saveProduct(
        SaveProductRequest(fields = listOf(SaveProductField(key = "code", value = "1234567890123")))
      )
    } catch (error: OpenFoodFactsException) {
      assertEquals("E_CONFIG", error.errorCode)
      return@runBlocking
    }

    throw AssertionError("Expected saveProduct to fail without credentials.")
  }

  @Test
  fun configure_stagingInjectsDefaultCredentials() {
    val state = configStore.configure(ConfigPatch(environment = com.openfoodfacts.model.OffEnvironment.STAGING))

    assertEquals("staging", state.environment.wireValue)
    assertEquals("off", state.globalUser?.userId)
    assertFalse(state.globalUser?.password.isNullOrBlank())
  }

  @Test
  fun saveProduct_returnsApiStatusOnSuccess() = runBlocking {
    configStore.configure(
      ConfigPatch(
        hasGlobalUser = true,
        globalUser = OffUser(comment = null, userId = "demo", password = "demo"),
      )
    )
    server.enqueue(MockResponse().setBody("""{"status":1,"status_verbose":"saved"}"""))

    val status =
      apiClient.saveProduct(
        SaveProductRequest(
          fields = listOf(
            SaveProductField(key = "code", value = "1234567890123"),
            SaveProductField(key = "product_name", value = "Test Product"),
          )
        )
      )
    val request = server.takeRequest()
    val body = request.body.readUtf8()

    assertEquals(1, status.status)
    assertTrue(body.contains("user_id=demo"))
    assertTrue(body.contains("product_name=Test+Product"))
  }
}
