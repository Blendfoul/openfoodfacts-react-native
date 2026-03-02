import Foundation
import UIKit

enum OpenFoodFactsModuleError: Error {
  case validation(String)
  case config(String, endpoint: String? = nil)
  case network(String, endpoint: String? = nil, underlying: Error? = nil)
  case httpStatus(Int, endpoint: String)
  case deserialization(String, endpoint: String, underlying: Error? = nil)
  case apiStatus(String, endpoint: String, apiStatus: [String: Any])

  var code: String {
    switch self {
    case .validation:
      return "E_VALIDATION"
    case .config:
      return "E_CONFIG"
    case .network:
      return "E_NETWORK"
    case .httpStatus:
      return "E_HTTP_STATUS"
    case .deserialization:
      return "E_DESERIALIZATION"
    case .apiStatus:
      return "E_API_STATUS"
    }
  }

  var message: String {
    switch self {
    case let .validation(message),
      let .config(message, _),
      let .network(message, _, _),
      let .deserialization(message, _, _),
      let .apiStatus(message, _, _):
      return message
    case let .httpStatus(statusCode, _):
      return "Request failed with HTTP status \(statusCode)."
    }
  }

  var details: [String: Any] {
    switch self {
    case let .validation(message):
      return [
        "code": code,
        "message": message,
      ]
    case let .config(message, endpoint):
      return baseDetails(message: message, endpoint: endpoint)
    case let .network(message, endpoint, _):
      return baseDetails(message: message, endpoint: endpoint)
    case let .httpStatus(statusCode, endpoint):
      return [
        "code": code,
        "message": message,
        "httpStatus": statusCode,
        "endpoint": endpoint,
      ]
    case let .deserialization(message, endpoint, _):
      return baseDetails(message: message, endpoint: endpoint)
    case let .apiStatus(message, endpoint, apiStatus):
      return [
        "code": code,
        "message": message,
        "endpoint": endpoint,
        "apiStatus": apiStatus,
      ]
    }
  }

  func asNSError() -> NSError {
    var userInfo: [String: Any] = [
      NSLocalizedDescriptionKey: message,
      "details": details,
    ]

    switch self {
    case let .network(_, _, underlying),
      let .deserialization(_, _, underlying):
      if let underlying {
        userInfo[NSUnderlyingErrorKey] = underlying
      }
    default:
      break
    }

    return NSError(domain: "OpenFoodFacts", code: 1, userInfo: userInfo)
  }

  private func baseDetails(message: String, endpoint: String?) -> [String: Any] {
    var result: [String: Any] = [
      "code": code,
      "message": message,
    ]

    if let endpoint {
      result["endpoint"] = endpoint
    }

    return result
  }
}

private struct OffUser {
  let comment: String?
  let userId: String
  let password: String

  func toDictionary() -> [String: Any] {
    [
      "comment": nullableValue(comment),
      "userId": userId,
      "password": password,
    ]
  }
}

private struct OffUserAgent {
  let name: String?
  let version: String?
  let system: String?
  let comment: String?

  func toDictionary() -> [String: Any] {
    [
      "name": nullableValue(name),
      "version": nullableValue(version),
      "system": nullableValue(system),
      "comment": nullableValue(comment),
    ]
  }
}

private enum OffEnvironment: String {
  case production
  case staging

  init(rawValueOrDefault value: String?) {
    if value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == OffEnvironment.staging.rawValue {
      self = .staging
    } else {
      self = .production
    }
  }
}

private struct OffConfigState {
  let environment: OffEnvironment
  let useRequired: Bool
  let globalUser: OffUser?
  let userAgent: OffUserAgent
  let country: String
  let uiLanguage: String
  let productsLanguage: String
  let appUUID: String

  func toDictionary() -> [String: Any] {
    [
      "environment": environment.rawValue,
      "useRequired": useRequired,
      "globalUser": globalUser?.toDictionary() ?? NSNull(),
      "userAgent": userAgent.toDictionary(),
      "country": country,
      "uiLanguage": uiLanguage,
      "productsLanguage": productsLanguage,
    ]
  }
}

private struct ProductQuery {
  let barcode: String
  let languages: [String]
  let country: String?
  let fields: [String]
}

private struct SuggestionsQuery {
  let query: String
  let tagtype: String
  let country: String
  let language: String
  let categories: String?
  let shape: String?
  let getSynonyms: Bool
  let term: String?
  let limit: Int
}

private struct SaveProductField {
  let key: String
  let value: String
}

private struct SaveProductRequest {
  let fields: [SaveProductField]
}

private struct ProductPatchRequest {
  let code: String
  let bodyJson: String
}

private struct ProductImageUploadRequest {
  let code: String
  let bodyJson: String
}

private struct ProductImageDeleteRequest {
  let code: String
  let imageId: Int
}

private struct TaxonomyCanonicalizeQuery {
  let tagtype: String
  let localTags: [String]
  let language: String?
}

private struct TaxonomyDisplayQuery {
  let tagtype: String
  let canonicalTags: [String]
  let language: String?
}

private struct TagKnowledgeQuery {
  let tagtype: String
  let tag: String
  let country: String?
  let language: String?
}

private struct ProductRevertRequest {
  let code: String
  let revision: Int
  let fields: [String]
  let tagsLanguage: String?
}

actor OpenFoodFactsIOSClient {
  private static let validCountries: Set<String> = {
    var result = Set(NSLocale.isoCountryCodes.map { $0.lowercased() })
    result.insert("uk")
    return result
  }()

  private static let validLanguages: Set<String> = {
    var result = Set(NSLocale.isoLanguageCodes.map { $0.lowercased() })
    ["tl", "mo", "cu"].forEach { result.insert($0) }
    return result
  }()

  private var state: OffConfigState

  init() {
    let appUUID = Self.appUUID()
    let userAgent = Self.generateUserAgent(appUUID: appUUID)
    self.state = Self.materialize(
      OffConfigState(
        environment: .production,
        useRequired: true,
        globalUser: nil,
        userAgent: userAgent,
        country: "us",
        uiLanguage: "en",
        productsLanguage: "en",
        appUUID: appUUID
      )
    )
  }

  func configure(_ config: [String: Any]) throws -> [String: Any] {
    let hasGlobalUser = config.keys.contains("globalUser")
    let hasUserAgent = config.keys.contains("userAgent")

    let environment =
      if let rawEnvironment = config["environment"] as? String {
        OffEnvironment(rawValueOrDefault: rawEnvironment)
      } else {
        state.environment
      }

    let useRequired =
      if let value = config["useRequired"] as? Bool {
        value
      } else if let value = config["useRequired"] as? NSNumber {
        value.boolValue
      } else {
        state.useRequired
      }

    let globalUser =
      if hasGlobalUser {
        try parseUser(config["globalUser"])
      } else {
        state.globalUser
      }

    let userAgent =
      if hasUserAgent {
        try parseUserAgent(config["userAgent"]) ?? Self.generateUserAgent(appUUID: state.appUUID)
      } else {
        state.userAgent
      }

    let country =
      if let value = config["country"], !(value is NSNull) {
        try validateCountry(value, fieldName: "country")
      } else {
        state.country
      }

    let uiLanguage =
      if let value = config["uiLanguage"], !(value is NSNull) {
        try validateLanguage(value, fieldName: "uiLanguage")
      } else {
        state.uiLanguage
      }

    let productsLanguage =
      if let value = config["productsLanguage"], !(value is NSNull) {
        try validateLanguage(value, fieldName: "productsLanguage")
      } else {
        state.productsLanguage
      }

    state = Self.materialize(
      OffConfigState(
        environment: environment,
        useRequired: useRequired,
        globalUser: globalUser,
        userAgent: userAgent,
        country: country,
        uiLanguage: uiLanguage,
        productsLanguage: productsLanguage,
        appUUID: state.appUUID
      )
    )

    return state.toDictionary()
  }

  func getConfig() -> [String: Any] {
    state.toDictionary()
  }

  func resetConfig() -> [String: Any] {
    let appUUID = state.appUUID
    let userAgent = Self.generateUserAgent(appUUID: appUUID)
    state = Self.materialize(
      OffConfigState(
        environment: .production,
        useRequired: true,
        globalUser: nil,
        userAgent: userAgent,
        country: "us",
        uiLanguage: "en",
        productsLanguage: "en",
        appUUID: appUUID
      )
    )

    return state.toDictionary()
  }

  func getProduct(_ rawQuery: [String: Any]) async throws -> [String: Any] {
    let query = try parseProductQuery(rawQuery)
    var queryItems: [String: String] = [
      "cc": query.country ?? state.country,
    ]
    if !query.languages.isEmpty {
      queryItems["lc"] = query.languages.joined(separator: ",")
      queryItems["tags_lc"] = query.languages[0]
    }
    if !query.fields.isEmpty {
      queryItems["fields"] = query.fields.joined(separator: ",")
    }
    queryItems.merge(userAgentBody()) { current, _ in current }

    let endpoint = "/api/v3/product/\(query.barcode)"
    let url = try buildURL(path: endpoint, query: queryItems)
    let response = try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())

    return try parseProductResponse(response)
  }

  func getOrderedNutrients() async throws -> [[String: Any]] {
    let endpoint = "/cgi/nutrients.pl"
    let url = try buildURL(path: endpoint)
    var body = [
      "cc": state.country,
      "lc": state.uiLanguage,
    ]
    body.merge(userAgentBody()) { current, _ in current }

    let response = try await performRequest(
      url: url,
      method: "POST",
      endpoint: endpoint,
      headers: formHeaders(),
      formBody: body
    )

    let root = try decodeObject(response, endpoint: endpoint)
    var flattened = [[String: Any]]()
    flattenOrderedNutrients(root["nutrients"], into: &flattened)
    return flattened
  }

  func getNutrientMetadata() async throws -> [String: Any] {
    let endpoint = "/data/taxonomies/nutrients.json"
    let url = try buildURL(path: endpoint)
    let response = try await performRequest(
      url: url,
      method: "GET",
      endpoint: endpoint,
      headers: jsonHeaders(addAuthHeader: true)
    )

    let root = try decodeObject(response, endpoint: endpoint)
    let nutrients = root.keys.sorted().compactMap { rawKey -> [String: Any]? in
      guard let meta = root[rawKey] as? [String: Any] else {
        return nil
      }

      let nameEntries =
        ((meta["name"] as? [String: Any]) ?? [:])
          .compactMap { key, value -> [String: Any]? in
            guard let stringValue = value as? String, !stringValue.isEmpty else {
              return nil
            }

            return [
              "languageCode": key,
              "value": stringValue,
            ]
          }
          .sorted { ($0["languageCode"] as? String ?? "") < ($1["languageCode"] as? String ?? "") }

      let rawUnit = ((meta["unit"] as? [String: Any])?["en"] as? String) ?? "missing"

      return [
        "key": rawKey.replacingOccurrences(of: "zz:", with: ""),
        "meta": [
          "unit": normalizeUnit(rawUnit),
          "name": nameEntries,
        ],
      ]
    }

    return [
      "nutrients": nutrients,
    ]
  }

  func getSuggestions(_ rawQuery: [String: Any]) async throws -> [String] {
    let query = try parseSuggestionsQuery(rawQuery)
    var queryItems: [String: String] = [
      "tagtype": query.tagtype,
      "cc": query.country,
      "lc": query.language,
      "string": query.query,
      "limit": String(query.limit),
    ]
    if let categories = query.categories, !categories.isEmpty {
      queryItems["categories"] = categories
    }
    if let shape = query.shape, !shape.isEmpty {
      queryItems["shape"] = shape
    }
    if let term = query.term, !term.isEmpty {
      queryItems["term"] = term
    }
    if query.getSynonyms {
      queryItems["get_synonyms"] = "1"
    }
    queryItems.merge(userAgentBody()) { current, _ in current }

    let endpoint = "/api/v3/taxonomy_suggestions"
    let url = try buildURL(path: endpoint, query: queryItems)
    let response = try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
    let root = try decodeObject(response, endpoint: endpoint)
    let suggestions = (root["suggestions"] as? [Any]) ?? []
    return suggestions.compactMap { value in
      guard let stringValue = value as? String, !stringValue.isEmpty else {
        return nil
      }
      return stringValue
    }
  }

  func saveProduct(_ rawRequest: [String: Any]) async throws -> [String: Any] {
    let request = try parseSaveProductRequest(rawRequest)
    let endpoint = "/cgi/product_jqm2.pl"

    if state.environment == .production, state.globalUser == nil {
      throw OpenFoodFactsModuleError.config("Production writes require configured credentials.", endpoint: endpoint)
    }

    var body = [String: String]()
    request.fields.forEach { body[$0.key] = $0.value }
    body["lc"] = state.productsLanguage
    body["cc"] = state.country
    body.merge(globalUserBody()) { current, _ in current }
    body.merge(userAgentBody()) { current, _ in current }

    let url = try buildURL(path: endpoint)
    let response = try await performRequest(
      url: url,
      method: "POST",
      endpoint: endpoint,
      headers: formHeaders(),
      formBody: body
    )
    let apiStatus = parseApiStatus(response)
    let statusValue = (apiStatus["status"] as? NSNumber)?.intValue
    if statusValue != 1 {
      let message =
        (apiStatus["statusVerbose"] as? String)
        ?? (apiStatus["error"] as? String)
        ?? "Open Food Facts rejected the save request."
      throw OpenFoodFactsModuleError.apiStatus(message, endpoint: endpoint, apiStatus: apiStatus)
    }

    return apiStatus
  }

  func patchProduct(_ rawRequest: [String: Any]) async throws -> String {
    let request = try parseProductPatchRequest(rawRequest)
    let endpoint = "/api/v3/product/\(request.code)"
    let url = try buildURL(path: endpoint)
    return try await performRequest(
      url: url,
      method: "PATCH",
      endpoint: endpoint,
      headers: jsonRequestHeaders(addAuthHeader: true),
      rawBody: request.bodyJson
    )
  }

  func uploadProductImage(_ rawRequest: [String: Any]) async throws -> String {
    let request = try parseProductImageUploadRequest(rawRequest)
    let endpoint = "/api/v3/product/\(request.code)/images"
    let url = try buildURL(path: endpoint)
    return try await performRequest(
      url: url,
      method: "POST",
      endpoint: endpoint,
      headers: jsonRequestHeaders(addAuthHeader: true),
      rawBody: request.bodyJson
    )
  }

  func deleteProductImage(_ rawRequest: [String: Any]) async throws -> String {
    let request = try parseProductImageDeleteRequest(rawRequest)
    let endpoint = "/api/v3/product/\(request.code)/images/uploaded/\(request.imageId)"
    let url = try buildURL(path: endpoint)
    return try await performRequest(
      url: url,
      method: "DELETE",
      endpoint: endpoint,
      headers: jsonHeaders(addAuthHeader: true)
    )
  }

  func canonicalizeTags(_ rawQuery: [String: Any]) async throws -> [String: Any] {
    let query = try parseTaxonomyCanonicalizeQuery(rawQuery)
    var queryItems: [String: String] = [
      "tagtype": query.tagtype,
      "local_tags_list": query.localTags.joined(separator: ","),
    ]
    if let language = query.language {
      queryItems["lc"] = language
    }
    let endpoint = "/api/v3/taxonomy_canonicalize_tags"
    let url = try buildURL(path: endpoint, query: queryItems)
    let response = try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
    let root = try decodeObject(response, endpoint: endpoint)
    let canonicalTags =
      (((root["canonical_tags_list"] as? String) ?? "")
        .split(separator: ",")
        .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty })

    return [
      "canonicalTags": canonicalTags,
      "rawJson": response,
    ]
  }

  func displayTags(_ rawQuery: [String: Any]) async throws -> [String: Any] {
    let query = try parseTaxonomyDisplayQuery(rawQuery)
    var queryItems: [String: String] = [
      "tagtype": query.tagtype,
      "canonical_tags_list": query.canonicalTags.joined(separator: ","),
    ]
    if let language = query.language {
      queryItems["lc"] = language
    }
    let endpoint = "/api/v3/taxonomy_display_tags"
    let url = try buildURL(path: endpoint, query: queryItems)
    let response = try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
    let root = try decodeObject(response, endpoint: endpoint)
    let displayTags = ((root["display_tags"] as? [Any]) ?? []).compactMap { value -> String? in
      guard let stringValue = value as? String else {
        return nil
      }
      let trimmed = stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
      return trimmed.isEmpty ? nil : trimmed
    }

    return [
      "displayTags": displayTags,
      "rawJson": response,
    ]
  }

  func getTagKnowledge(_ rawQuery: [String: Any]) async throws -> String {
    let query = try parseTagKnowledgeQuery(rawQuery)
    var queryItems = [String: String]()
    if let country = query.country {
      queryItems["cc"] = country
    }
    if let language = query.language {
      queryItems["lc"] = language
    }
    let endpoint = "/api/v3/tag/\(query.tagtype)/\(query.tag)"
    let url = try buildURL(path: endpoint, query: queryItems)
    return try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
  }

  func revertProduct(_ rawRequest: [String: Any]) async throws -> String {
    let request = try parseProductRevertRequest(rawRequest)
    let endpoint = "/api/v3/product_revert"
    var body: [String: Any] = [
      "code": request.code,
      "rev": request.revision,
    ]
    if !request.fields.isEmpty {
      body["fields"] = request.fields.joined(separator: ",")
    }
    if let tagsLanguage = request.tagsLanguage {
      body["tags_lc"] = tagsLanguage
    }
    let bodyData =
      try JSONSerialization.data(withJSONObject: body, options: [])
    guard let bodyJson = String(data: bodyData, encoding: .utf8) else {
      throw OpenFoodFactsModuleError.deserialization("Failed to encode revert request.", endpoint: endpoint)
    }
    let url = try buildURL(path: endpoint)
    return try await performRequest(
      url: url,
      method: "POST",
      endpoint: endpoint,
      headers: jsonRequestHeaders(addAuthHeader: true),
      rawBody: bodyJson
    )
  }

  func getExternalSources() async throws -> String {
    let endpoint = "/api/v3/external_sources"
    let url = try buildURL(path: endpoint)
    return try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
  }

  func getPreferences() async throws -> String {
    let endpoint = "/api/v3/preferences"
    let url = try buildURL(path: endpoint)
    return try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
  }

  func getAttributeGroups() async throws -> String {
    let endpoint = "/api/v3.4/attribute_groups"
    let url = try buildURL(path: endpoint)
    return try await performRequest(url: url, method: "GET", endpoint: endpoint, headers: jsonHeaders())
  }

  private static func materialize(_ candidate: OffConfigState) -> OffConfigState {
    let normalizedCountry = candidate.country.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    let normalizedUILanguage = candidate.uiLanguage.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    let normalizedProductsLanguage =
      candidate.productsLanguage.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

    let normalizedUser =
      if candidate.environment == .staging, candidate.globalUser == nil {
        OffUser(comment: nil, userId: "off", password: "off")
      } else {
        candidate.globalUser
      }

    return OffConfigState(
      environment: candidate.environment,
      useRequired: candidate.useRequired,
      globalUser: normalizedUser,
      userAgent: candidate.userAgent,
      country: normalizedCountry,
      uiLanguage: normalizedUILanguage,
      productsLanguage: normalizedProductsLanguage,
      appUUID: candidate.appUUID
    )
  }

  private static func appUUID() -> String {
    let key = "openfoodfacts.appUUID"
    let defaults = UserDefaults.standard
    if let existing = defaults.string(forKey: key), !existing.isEmpty {
      return existing
    }

    let created = UUID().uuidString
    defaults.set(created, forKey: key)
    return created
  }

  private static func generateUserAgent(appUUID: String) -> OffUserAgent {
    let bundle = Bundle.main
    let appName = (bundle.object(forInfoDictionaryKey: "CFBundleName") as? String)?.trimmedNilIfEmpty
    let appVersion =
      (bundle.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String)?.trimmedNilIfEmpty
      ?? "0.1.0"
    let system = "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)".trimmedNilIfEmpty
    let comment = [appName, appVersion, system, appUUID].compactMap { $0 }.joined(separator: " - ")

    return OffUserAgent(
      name: appName,
      version: appVersion,
      system: system,
      comment: comment.isEmpty ? nil : comment
    )
  }

  private func parseUser(_ value: Any?) throws -> OffUser? {
    guard let value else {
      return nil
    }
    if value is NSNull {
      return nil
    }
    guard let dictionary = value as? [String: Any] else {
      throw OpenFoodFactsModuleError.validation("globalUser must be an object.")
    }

    let userId = try trimmedRequiredString(dictionary, key: "userId")
    let password = try trimmedRequiredString(dictionary, key: "password")
    if userId.isEmpty || password.isEmpty {
      throw OpenFoodFactsModuleError.validation("globalUser.userId and globalUser.password must not be empty.")
    }

    let comment = trimmedOptionalString(dictionary, key: "comment")
    return OffUser(comment: comment, userId: userId, password: password)
  }

  private func parseUserAgent(_ value: Any?) throws -> OffUserAgent? {
    guard let value else {
      return nil
    }
    if value is NSNull {
      return nil
    }
    guard let dictionary = value as? [String: Any] else {
      throw OpenFoodFactsModuleError.validation("userAgent must be an object.")
    }

    return OffUserAgent(
      name: trimmedOptionalString(dictionary, key: "name"),
      version: trimmedOptionalString(dictionary, key: "version"),
      system: trimmedOptionalString(dictionary, key: "system"),
      comment: trimmedOptionalString(dictionary, key: "comment")
    )
  }

  private func parseProductQuery(_ value: [String: Any]) throws -> ProductQuery {
    let barcode = try trimmedRequiredString(value, key: "barcode")
    if barcode.isEmpty {
      throw OpenFoodFactsModuleError.validation("barcode must not be empty.")
    }

    let languages = try trimmedStringArray(value: value["languages"], fieldName: "languages").map {
      try validateLanguage($0, fieldName: "languages")
    }

    let country =
      if let rawCountry = value["country"], !(rawCountry is NSNull) {
        try validateCountry(rawCountry, fieldName: "country")
      } else {
        nil
      }

    let fields = try trimmedStringArray(value: value["fields"], fieldName: "fields")

    return ProductQuery(
      barcode: barcode,
      languages: languages,
      country: country,
      fields: fields
    )
  }

  private func parseSuggestionsQuery(_ value: [String: Any]) throws -> SuggestionsQuery {
    let country = try validateCountry(try trimmedRequiredString(value, key: "country"), fieldName: "country")
    let language = try validateLanguage(try trimmedRequiredString(value, key: "language"), fieldName: "language")
    let query = (value["query"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    let tagtype = ((value["tagtype"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap {
      $0.isEmpty ? nil : $0
    } ?? "categories"
    let categories = (value["categories"] as? String)?.trimmedNilIfEmpty
    let shape = (value["shape"] as? String)?.trimmedNilIfEmpty
    let getSynonyms =
      if let rawGetSynonyms = value["getSynonyms"] as? Bool {
        rawGetSynonyms
      } else if let rawGetSynonyms = value["getSynonyms"] as? NSNumber {
        rawGetSynonyms.boolValue
      } else {
        false
      }
    let term = (value["term"] as? String)?.trimmedNilIfEmpty

    let limit: Int
    if let rawLimit = value["limit"] as? NSNumber {
      limit = rawLimit.intValue
    } else {
      limit = 25
    }

    if limit < 1 {
      throw OpenFoodFactsModuleError.validation("limit must be greater than 0.")
    }
    if limit > 400 {
      throw OpenFoodFactsModuleError.validation("limit must be less than or equal to 400.")
    }

    return SuggestionsQuery(
      query: query,
      tagtype: tagtype,
      country: country,
      language: language,
      categories: categories,
      shape: shape,
      getSynonyms: getSynonyms,
      term: term,
      limit: limit
    )
  }

  private func parseProductPatchRequest(_ value: [String: Any]) throws -> ProductPatchRequest {
    let code = try nonEmptyRequiredString(value, key: "code")
    let bodyJson = try nonEmptyRequiredString(value, key: "bodyJson")
    return ProductPatchRequest(code: code, bodyJson: bodyJson)
  }

  private func parseProductImageUploadRequest(_ value: [String: Any]) throws -> ProductImageUploadRequest {
    let code = try nonEmptyRequiredString(value, key: "code")
    let bodyJson = try nonEmptyRequiredString(value, key: "bodyJson")
    return ProductImageUploadRequest(code: code, bodyJson: bodyJson)
  }

  private func parseProductImageDeleteRequest(_ value: [String: Any]) throws -> ProductImageDeleteRequest {
    let code = try nonEmptyRequiredString(value, key: "code")
    let imageId = try requiredInt(value, key: "imageId")
    if imageId < 0 {
      throw OpenFoodFactsModuleError.validation("imageId must be greater than or equal to 0.")
    }
    return ProductImageDeleteRequest(code: code, imageId: imageId)
  }

  private func parseTaxonomyCanonicalizeQuery(_ value: [String: Any]) throws -> TaxonomyCanonicalizeQuery {
    let tagtype = try nonEmptyRequiredString(value, key: "tagtype")
    let localTags = try nonEmptyStringArray(value: value["localTags"], fieldName: "localTags")
    let language =
      if let rawLanguage = value["language"], !(rawLanguage is NSNull) {
        try validateLanguage(rawLanguage, fieldName: "language")
      } else {
        nil
      }

    return TaxonomyCanonicalizeQuery(
      tagtype: tagtype,
      localTags: localTags,
      language: language
    )
  }

  private func parseTaxonomyDisplayQuery(_ value: [String: Any]) throws -> TaxonomyDisplayQuery {
    let tagtype = try nonEmptyRequiredString(value, key: "tagtype")
    let canonicalTags = try nonEmptyStringArray(value: value["canonicalTags"], fieldName: "canonicalTags")
    let language =
      if let rawLanguage = value["language"], !(rawLanguage is NSNull) {
        try validateLanguage(rawLanguage, fieldName: "language")
      } else {
        nil
      }

    return TaxonomyDisplayQuery(
      tagtype: tagtype,
      canonicalTags: canonicalTags,
      language: language
    )
  }

  private func parseTagKnowledgeQuery(_ value: [String: Any]) throws -> TagKnowledgeQuery {
    let tagtype = try nonEmptyRequiredString(value, key: "tagtype")
    let tag = try nonEmptyRequiredString(value, key: "tag")
    let country =
      if let rawCountry = value["country"], !(rawCountry is NSNull) {
        try validateCountry(rawCountry, fieldName: "country")
      } else {
        nil
      }
    let language =
      if let rawLanguage = value["language"], !(rawLanguage is NSNull) {
        try validateLanguage(rawLanguage, fieldName: "language")
      } else {
        nil
      }

    return TagKnowledgeQuery(
      tagtype: tagtype,
      tag: tag,
      country: country,
      language: language
    )
  }

  private func parseProductRevertRequest(_ value: [String: Any]) throws -> ProductRevertRequest {
    let code = try nonEmptyRequiredString(value, key: "code")
    let revision = try requiredInt(value, key: "revision")
    if revision < 0 {
      throw OpenFoodFactsModuleError.validation("revision must be greater than or equal to 0.")
    }
    let fields =
      try trimmedStringArray(value: value["fields"], fieldName: "fields")
        .filter { !$0.isEmpty }
    let tagsLanguage =
      if let rawTagsLanguage = value["tagsLanguage"], !(rawTagsLanguage is NSNull) {
        try validateLanguage(rawTagsLanguage, fieldName: "tagsLanguage")
      } else {
        nil
      }

    return ProductRevertRequest(
      code: code,
      revision: revision,
      fields: fields,
      tagsLanguage: tagsLanguage
    )
  }

  private func parseSaveProductRequest(_ value: [String: Any]) throws -> SaveProductRequest {
    guard let rawFields = value["fields"] as? [Any] else {
      throw OpenFoodFactsModuleError.validation("fields must be an array.")
    }

    var fields = [SaveProductField]()
    var seen = Set<String>()

    for (index, item) in rawFields.enumerated() {
      guard let dictionary = item as? [String: Any] else {
        throw OpenFoodFactsModuleError.validation("fields[\(index)] must be an object.")
      }

      let key = try trimmedRequiredString(dictionary, key: "key")
      let fieldValue = try trimmedRequiredString(dictionary, key: "value")

      if key.isEmpty {
        throw OpenFoodFactsModuleError.validation("fields[\(index)].key must not be empty.")
      }
      if seen.contains(key) {
        throw OpenFoodFactsModuleError.validation("fields contains duplicate key '\(key)'.")
      }

      seen.insert(key)
      fields.append(SaveProductField(key: key, value: fieldValue))
    }

    return SaveProductRequest(fields: fields)
  }

  private func validateCountry(_ rawValue: Any, fieldName: String) throws -> String {
    guard let stringValue = normalizedString(from: rawValue) else {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must be a string.")
    }
    if stringValue.isEmpty {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must not be empty.")
    }
    if !Self.validCountries.contains(stringValue) {
      throw OpenFoodFactsModuleError.validation("\(fieldName) '\(stringValue)' is not supported.")
    }
    return stringValue
  }

  private func validateLanguage(_ rawValue: Any, fieldName: String) throws -> String {
    guard let stringValue = normalizedString(from: rawValue) else {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must be a string.")
    }
    if stringValue.isEmpty {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must not be empty.")
    }
    if !Self.validLanguages.contains(stringValue) {
      throw OpenFoodFactsModuleError.validation("\(fieldName) '\(stringValue)' is not supported.")
    }
    return stringValue
  }

  private func trimmedStringArray(value: Any?, fieldName: String) throws -> [String] {
    guard let value else {
      return []
    }
    if value is NSNull {
      return []
    }
    guard let array = value as? [Any] else {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must be an array.")
    }

    return try array.enumerated().map { index, item in
      guard let stringValue = (item as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) else {
        throw OpenFoodFactsModuleError.validation("\(fieldName)[\(index)] must be a string.")
      }
      return stringValue
    }
  }

  private func trimmedRequiredString(_ dictionary: [String: Any], key: String) throws -> String {
    guard let stringValue = dictionary[key] as? String else {
      throw OpenFoodFactsModuleError.validation("\(key) must be a string.")
    }
    return stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
  }

  private func nonEmptyRequiredString(_ dictionary: [String: Any], key: String) throws -> String {
    let value = try trimmedRequiredString(dictionary, key: key)
    if value.isEmpty {
      throw OpenFoodFactsModuleError.validation("\(key) must not be empty.")
    }
    return value
  }

  private func trimmedOptionalString(_ dictionary: [String: Any], key: String) -> String? {
    (dictionary[key] as? String)?.trimmedNilIfEmpty
  }

  private func normalizedString(from value: Any) -> String? {
    (value as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
  }

  private func requiredInt(_ dictionary: [String: Any], key: String) throws -> Int {
    if let value = dictionary[key] as? NSNumber {
      return value.intValue
    }
    if let value = dictionary[key] as? String, let intValue = Int(value) {
      return intValue
    }
    throw OpenFoodFactsModuleError.validation("\(key) must be a number.")
  }

  private func nonEmptyStringArray(value: Any?, fieldName: String) throws -> [String] {
    let values = try trimmedStringArray(value: value, fieldName: fieldName).filter { !$0.isEmpty }
    if values.isEmpty {
      throw OpenFoodFactsModuleError.validation("\(fieldName) must contain at least one value.")
    }
    return values
  }

  private func buildURL(path: String, query: [String: String] = [:]) throws -> URL {
    var components = URLComponents()
    components.scheme = "https"
    components.host = "world.\(state.environment == .staging ? "openfoodfacts.net" : "openfoodfacts.org")"
    components.path = path.hasPrefix("/") ? path : "/\(path)"
    if !query.isEmpty {
      components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
    }

    guard let url = components.url else {
      throw OpenFoodFactsModuleError.network("Could not compose request URL.", endpoint: path)
    }

    return url
  }

  private func userAgentBody() -> [String: String] {
    [
      "app_name": state.userAgent.name ?? "",
      "app_version": state.userAgent.version ?? "",
      "app_uuid": state.appUUID,
      "app_platform": state.userAgent.system ?? "",
      "comment": state.userAgent.comment ?? "",
    ]
  }

  private func globalUserBody() -> [String: String] {
    guard let globalUser = state.globalUser else {
      return [:]
    }

    return [
      "user_id": globalUser.userId,
      "password": globalUser.password,
      "comment": globalUser.comment ?? "",
    ]
  }

  private func jsonHeaders(addAuthHeader: Bool = false) -> [String: String] {
    var headers: [String: String] = [
      "Accept": "application/json",
      "User-Agent": userAgentHeader(),
      "From": state.globalUser?.userId ?? "anonymous",
    ]

    if addAuthHeader, let globalUser = state.globalUser {
      let token = "\(globalUser.userId):\(globalUser.password)"
      if let data = token.data(using: .utf8) {
        headers["Authorization"] = "Basic \(data.base64EncodedString())"
      }
    }

    return headers
  }

  private func formHeaders() -> [String: String] {
    var headers = jsonHeaders()
    headers["Content-Type"] = "application/x-www-form-urlencoded"
    return headers
  }

  private func jsonRequestHeaders(addAuthHeader: Bool = false) -> [String: String] {
    var headers = jsonHeaders(addAuthHeader: addAuthHeader)
    headers["Content-Type"] = "application/json"
    return headers
  }

  private func userAgentHeader() -> String {
    let parts = [
      state.userAgent.name,
      state.userAgent.version,
      state.userAgent.system,
      state.userAgent.comment,
    ].compactMap { $0?.trimmedNilIfEmpty }

    if parts.isEmpty {
      return "OpenFoodFacts iOS"
    }

    return parts.joined(separator: " - ")
  }

  private func performRequest(
    url: URL,
    method: String,
    endpoint: String,
    headers: [String: String],
    formBody: [String: String]? = nil,
    rawBody: String? = nil
  ) async throws -> String {
    var request = URLRequest(url: url)
    request.httpMethod = method
    headers.forEach { key, value in
      request.setValue(value, forHTTPHeaderField: key)
    }

    if let rawBody {
      request.httpBody = rawBody.data(using: .utf8)
    } else if let formBody {
      var components = URLComponents()
      components.queryItems = formBody.map { URLQueryItem(name: $0.key, value: $0.value) }
      request.httpBody = components.percentEncodedQuery?.data(using: .utf8)
    }

    do {
      let (data, response) = try await URLSession.shared.data(for: request)
      guard let httpResponse = response as? HTTPURLResponse else {
        throw OpenFoodFactsModuleError.network("Request did not return an HTTP response.", endpoint: endpoint)
      }
      guard (200 ... 299).contains(httpResponse.statusCode) else {
        throw OpenFoodFactsModuleError.httpStatus(httpResponse.statusCode, endpoint: endpoint)
      }
      return String(data: data, encoding: .utf8) ?? ""
    } catch let error as OpenFoodFactsModuleError {
      throw error
    } catch {
      throw OpenFoodFactsModuleError.network(
        error.localizedDescription.isEmpty ? "Network request failed." : error.localizedDescription,
        endpoint: endpoint,
        underlying: error
      )
    }
  }

  private func decodeObject(_ payload: String, endpoint: String) throws -> [String: Any] {
    guard let data = payload.data(using: .utf8) else {
      throw OpenFoodFactsModuleError.deserialization("Failed to decode response text.", endpoint: endpoint)
    }

    do {
      guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
        throw OpenFoodFactsModuleError.deserialization("Expected a JSON object response.", endpoint: endpoint)
      }
      return object
    } catch let error as OpenFoodFactsModuleError {
      throw error
    } catch {
      throw OpenFoodFactsModuleError.deserialization(
        error.localizedDescription.isEmpty ? "Failed to parse Open Food Facts response." : error.localizedDescription,
        endpoint: endpoint,
        underlying: error
      )
    }
  }

  private func parseProductResponse(_ payload: String) throws -> [String: Any] {
    let root = try decodeObject(payload, endpoint: "/api/v3/product")
    let status = stringOrNil(root["status"])
    let hasProduct = ["product_found", "success", "success_with_warnings"].contains(status)

    return [
      "barcode": nullableValue(stringOrNil(root["code"])),
      "status": nullableValue(status),
      "product": (root["product"] as? [String: Any]).map(parseProduct) ?? NSNull(),
      "hasProduct": hasProduct,
    ]
  }

  private func parseProduct(_ json: [String: Any]) -> [String: Any] {
    [
      "code": stringOrNil(json["code"]) ?? "",
      "productName": nullableValue(stringOrNil(json["product_name"])),
      "productNameEn": nullableValue(stringOrNil(json["product_name_en"])),
      "brands": nullableValue(stringOrNil(json["brands"])),
      "lang": nullableValue(normalizedString(fromAny: json["lang"])),
      "quantity": nullableValue(stringOrNil(json["quantity"])),
      "packagingQuantity": nullableValue(numberOrNil(json["product_quantity"])),
      "servingSize": nullableValue(stringOrNil(json["serving_size"])),
      "servingQuantity": nullableValue(numberOrNil(json["serving_quantity"])),
      "dataPer": nullableValue(stringOrNil(json["nutrition_data_per"])),
      "categories": nullableValue(stringOrNil(json["categories"])),
      "imageFront": nullableValue(stringOrNil(json["image_front_url"])),
      "imageIngredients": nullableValue(stringOrNil(json["image_ingredients_url"])),
      "imageNutrition": nullableValue(stringOrNil(json["image_nutrition_url"])),
      "keywords": stringList(json["_keywords"]),
      "nutriments": nutrimentEntries(json["nutriments"]),
    ]
  }

  private func flattenOrderedNutrients(_ value: Any?, into destination: inout [[String: Any]]) {
    guard let items = value as? [Any] else {
      return
    }

    for item in items {
      guard let dictionary = item as? [String: Any] else {
        continue
      }

      destination.append([
        "id": stringOrNil(dictionary["id"]) ?? "",
        "name": stringOrNil(dictionary["name"]) ?? "",
        "important": boolValue(dictionary["important"]),
        "displayInEditForm": boolValue(dictionary["display_in_edit_form"]),
      ])
      flattenOrderedNutrients(dictionary["nutrients"], into: &destination)
    }
  }

  private func parseApiStatus(_ payload: String) -> [String: Any] {
    guard let data = payload.data(using: .utf8),
      let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    else {
      let statusVerbose =
        if payload.contains("Incorrect user name or password") {
          "Incorrect user name or password"
        } else {
          "Unexpected non-JSON response"
        }

      return [
        "status": 400,
        "statusVerbose": statusVerbose,
        "body": payload,
        "error": NSNull(),
        "imageId": NSNull(),
      ]
    }

    return [
      "status": nullableValue(intOrNil(object["status"])),
      "statusVerbose": nullableValue(stringOrNil(object["status_verbose"])),
      "body": nullableValue(stringOrNil(object["body"])),
      "error": nullableValue(stringOrNil(object["error"])),
      "imageId": nullableValue(intOrNil(object["imgid"])),
    ]
  }

  private func normalizeUnit(_ value: String) -> String {
    switch value.lowercased() {
    case "kcal":
      return "kcal"
    case "kj":
      return "kj"
    case "g":
      return "g"
    case "mg", "milli-gram", "mg.":
      return "mg"
    case "mcg", "µg", "μg", "&#181;g", "&micro;g", "&#xb5;g":
      return "µg"
    case "ml", "milli-liter":
      return "ml"
    case "l", "liter":
      return "l"
    case "% vol":
      return "% vol"
    case "%", "percent", "per cent":
      return "%"
    case "mmol/l":
      return "mmol/l"
    default:
      return "missing"
    }
  }

  private func boolValue(_ value: Any?) -> Bool {
    if let boolValue = value as? Bool {
      return boolValue
    }
    if let numberValue = value as? NSNumber {
      return numberValue.boolValue
    }
    return false
  }

  private func stringOrNil(_ value: Any?) -> String? {
    guard let stringValue = value as? String else {
      return nil
    }
    return stringValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : stringValue
  }

  private func normalizedString(fromAny value: Any?) -> String? {
    guard let stringValue = value as? String else {
      return nil
    }
    let normalized = stringValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    return normalized.isEmpty ? nil : normalized
  }

  private func numberOrNil(_ value: Any?) -> NSNumber? {
    if let numberValue = value as? NSNumber {
      return numberValue
    }
    if let stringValue = value as? String, let doubleValue = Double(stringValue) {
      return NSNumber(value: doubleValue)
    }
    return nil
  }

  private func intOrNil(_ value: Any?) -> NSNumber? {
    if let numberValue = value as? NSNumber {
      return NSNumber(value: numberValue.intValue)
    }
    if let stringValue = value as? String, let intValue = Int(stringValue) {
      return NSNumber(value: intValue)
    }
    return nil
  }

  private func stringList(_ value: Any?) -> [String] {
    guard let values = value as? [Any] else {
      return []
    }
    return values.compactMap { item in
      guard let stringValue = item as? String, !stringValue.isEmpty else {
        return nil
      }
      return stringValue
    }
  }

  private func nutrimentEntries(_ value: Any?) -> [[String: Any]] {
    guard let dictionary = value as? [String: Any] else {
      return []
    }

    return dictionary.keys.sorted().compactMap { key in
      let rawValue = dictionary[key]
      if let numberValue = rawValue as? NSNumber {
        return [
          "key": key,
          "numberValue": numberValue,
          "stringValue": NSNull(),
        ]
      }
      if let stringValue = rawValue as? String {
        return [
          "key": key,
          "numberValue": nullableValue(numberOrNil(stringValue)),
          "stringValue": stringValue,
        ]
      }
      return nil
    }
  }
}

private func nullableValue(_ value: Any?) -> Any {
  value ?? NSNull()
}

private extension String {
  var trimmedNilIfEmpty: String? {
    let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? nil : trimmed
  }
}
