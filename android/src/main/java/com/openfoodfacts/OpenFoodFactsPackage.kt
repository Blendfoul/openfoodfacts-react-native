package com.openfoodfacts

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class OpenFoodFactsPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
    when (name) {
      OpenFoodFactsModule.NAME -> OpenFoodFactsModule(reactContext)
      else -> null
    }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
    ReactModuleInfoProvider {
      mapOf(
        OpenFoodFactsModule.NAME to ReactModuleInfo(
          OpenFoodFactsModule.NAME,
          OpenFoodFactsModule.NAME,
          false,
          false,
          false,
          false,
          true
        )
      )
    }
}
