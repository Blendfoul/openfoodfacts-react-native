package com.openfoodfacts.api

import com.openfoodfacts.model.ApiStatus

internal class OpenFoodFactsException(
  val errorCode: String,
  override val message: String,
  val httpStatus: Int? = null,
  val endpoint: String? = null,
  val apiStatus: ApiStatus? = null,
  cause: Throwable? = null,
) : Exception(message, cause)
