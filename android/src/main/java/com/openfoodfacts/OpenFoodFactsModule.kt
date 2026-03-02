package com.openfoodfacts

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.turbomodule.core.interfaces.TurboModule
import com.openfoodfacts.api.OpenFoodFactsApiClient
import com.openfoodfacts.api.OpenFoodFactsConfigStore
import com.openfoodfacts.api.OpenFoodFactsException
import com.openfoodfacts.api.OpenFoodFactsMappers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@ReactModule(name = OpenFoodFactsModule.NAME)
class OpenFoodFactsModule(reactContext: ReactApplicationContext) :
  NativeOpenFoodFactsSpec(reactContext), TurboModule {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val configStore = OpenFoodFactsConfigStore.create(reactContext)
  private val apiClient = OpenFoodFactsApiClient(configStore = configStore)

  override fun getName(): String = NAME

  override fun getRuntimeInfo(promise: Promise) {
    val runtimeInfo = Arguments.createMap().apply {
      putString("platform", "android")
      putString("moduleName", NAME)
      putBoolean("newArchitecture", true)
      putString("nativeVersion", NATIVE_VERSION)
    }

    promise.resolve(runtimeInfo)
  }

  override fun configure(config: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.configure(OpenFoodFactsMappers.parseConfigPatch(config)))
    }
  }

  override fun getConfig(promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.getConfig())
    }
  }

  override fun resetConfig(promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.resetConfig())
    }
  }

  override fun getProduct(query: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.getProduct(OpenFoodFactsMappers.parseProductQuery(query)))
    }
  }

  override fun getOrderedNutrients(promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableOrderedNutrients(apiClient.getOrderedNutrients())
    }
  }

  override fun getNutrientMetadata(promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.getNutrientMetadata())
    }
  }

  override fun getSuggestions(query: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableStringArray(apiClient.getSuggestions(OpenFoodFactsMappers.parseSuggestionsQuery(query)))
    }
  }

  override fun saveProduct(request: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.saveProduct(OpenFoodFactsMappers.parseSaveProductRequest(request)))
    }
  }

  override fun patchProduct(request: ReadableMap, promise: Promise) {
    runPromise(promise) {
      apiClient.patchProduct(OpenFoodFactsMappers.parseProductPatchRequest(request))
    }
  }

  override fun uploadProductImage(request: ReadableMap, promise: Promise) {
    runPromise(promise) {
      apiClient.uploadProductImage(OpenFoodFactsMappers.parseProductImageUploadRequest(request))
    }
  }

  override fun deleteProductImage(request: ReadableMap, promise: Promise) {
    runPromise(promise) {
      apiClient.deleteProductImage(OpenFoodFactsMappers.parseProductImageDeleteRequest(request))
    }
  }

  override fun canonicalizeTags(query: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.canonicalizeTags(OpenFoodFactsMappers.parseTaxonomyCanonicalizeQuery(query)))
    }
  }

  override fun displayTags(query: ReadableMap, promise: Promise) {
    runPromise(promise) {
      OpenFoodFactsMappers.toWritableMap(apiClient.displayTags(OpenFoodFactsMappers.parseTaxonomyDisplayQuery(query)))
    }
  }

  override fun getTagKnowledge(query: ReadableMap, promise: Promise) {
    runPromise(promise) {
      apiClient.getTagKnowledge(OpenFoodFactsMappers.parseTagKnowledgeQuery(query))
    }
  }

  override fun revertProduct(request: ReadableMap, promise: Promise) {
    runPromise(promise) {
      apiClient.revertProduct(OpenFoodFactsMappers.parseProductRevertRequest(request))
    }
  }

  override fun getExternalSources(promise: Promise) {
    runPromise(promise) {
      apiClient.getExternalSources()
    }
  }

  override fun getPreferences(promise: Promise) {
    runPromise(promise) {
      apiClient.getPreferences()
    }
  }

  override fun getAttributeGroups(promise: Promise) {
    runPromise(promise) {
      apiClient.getAttributeGroups()
    }
  }

  override fun invalidate() {
    scope.cancel()
    super.invalidate()
  }

  private fun runPromise(promise: Promise, block: suspend () -> Any) {
    scope.launch {
      try {
        promise.resolve(block())
      } catch (error: OpenFoodFactsException) {
        promise.reject(error.errorCode, error.message ?: "Open Food Facts request failed.", error)
      } catch (error: Exception) {
        promise.reject("E_NETWORK", error.message ?: "Unexpected Open Food Facts failure.", error)
      }
    }
  }

  companion object {
    const val NAME = "OpenFoodFacts"
    private const val NATIVE_VERSION = "0.1.0"
  }
}
