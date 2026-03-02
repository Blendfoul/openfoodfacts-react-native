package com.openfoodfacts.api

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.openfoodfacts.model.ConfigPatch
import com.openfoodfacts.model.OffConfigState
import com.openfoodfacts.model.OffEnvironment
import com.openfoodfacts.model.OffUser
import com.openfoodfacts.model.OffUserAgent
import java.util.UUID

internal class OpenFoodFactsConfigStore(
  private val generatedUserAgentProvider: () -> OffUserAgent,
  private val uuidProvider: () -> String,
) {
  @Volatile
  private var state = defaultState()

  @Synchronized
  fun configure(patch: ConfigPatch): OffConfigState {
    val merged = state.copy(
      environment = patch.environment ?: state.environment,
      useRequired = patch.useRequired ?: state.useRequired,
      globalUser = if (patch.hasGlobalUser) patch.globalUser else state.globalUser,
      userAgent = if (patch.hasUserAgent) patch.userAgent ?: generatedUserAgentProvider() else state.userAgent,
      country = patch.country ?: state.country,
      uiLanguage = patch.uiLanguage ?: state.uiLanguage,
      productsLanguage = patch.productsLanguage ?: state.productsLanguage,
    )

    state = materialize(merged)
    return state
  }

  @Synchronized
  fun get(): OffConfigState = state

  @Synchronized
  fun reset(): OffConfigState {
    state = defaultState()
    return state
  }

  private fun defaultState(): OffConfigState =
    materialize(
      OffConfigState(
        environment = OffEnvironment.PRODUCTION,
        useRequired = true,
        globalUser = null,
        userAgent = generatedUserAgentProvider(),
        country = "us",
        uiLanguage = "en",
        productsLanguage = "en",
        appUuid = uuidProvider(),
      )
    )

  private fun materialize(candidate: OffConfigState): OffConfigState {
    val normalizedEnvironment = candidate.environment
    val normalizedUser =
      if (normalizedEnvironment == OffEnvironment.STAGING && candidate.globalUser == null) {
        OffUser(comment = null, userId = "off", password = "off")
      } else {
        candidate.globalUser
      }

    return candidate.copy(
      country = candidate.country.trim().lowercase(),
      uiLanguage = candidate.uiLanguage.trim().lowercase(),
      productsLanguage = candidate.productsLanguage.trim().lowercase(),
      globalUser = normalizedUser,
    )
  }

  companion object {
    fun create(context: Context): OpenFoodFactsConfigStore {
      val appContext = context.applicationContext
      return OpenFoodFactsConfigStore(
        generatedUserAgentProvider = { generateUserAgent(appContext) },
        uuidProvider = { getOrCreateUuid(appContext) },
      )
    }

    private fun getOrCreateUuid(context: Context): String {
      val prefs = context.getSharedPreferences("openfoodfacts", Context.MODE_PRIVATE)
      val existing = prefs.getString("app_uuid", null)
      if (!existing.isNullOrBlank()) {
        return existing
      }

      val created = UUID.randomUUID().toString()
      prefs.edit().putString("app_uuid", created).apply()
      return created
    }

    private fun generateUserAgent(context: Context): OffUserAgent {
      val packageInfo = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0),
          )
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getPackageInfo(context.packageName, 0)
        }
      }.getOrNull()

      val appName =
        context.applicationInfo.loadLabel(context.packageManager)?.toString()?.takeIf { it.isNotBlank() }
      val version = packageInfo?.versionName?.takeIf { !it.isNullOrBlank() } ?: "0.1.0"
      val system = "Android ${Build.VERSION.RELEASE ?: "unknown"}"
      val comment = listOfNotNull(appName, version, system, getOrCreateUuid(context)).joinToString(" - ")

      return OffUserAgent(
        name = appName,
        version = version,
        system = system,
        comment = comment,
      )
    }
  }
}
