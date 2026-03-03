import Foundation
import React

@objc(OpenFoodFactsModule)
public final class OpenFoodFactsModule: NSObject {
  private let client = OpenFoodFactsIOSClient()

  @objc
  public static func requiresMainQueueSetup() -> Bool {
    false
  }

  @objc(getRuntimeInfo:reject:)
  public func getRuntimeInfo(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    resolve([
      "platform": "ios",
      "moduleName": "OpenFoodFacts",
      "newArchitecture": true,
      "nativeVersion": "0.1.0",
    ])
  }

  @objc(configure:resolve:reject:)
  public func configure(
    _ config: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.configure(asSwiftDictionary(config)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getConfig:reject:)
  public func getConfig(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      resolve(await client.getConfig())
    }
  }

  @objc(resetConfig:reject:)
  public func resetConfig(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      resolve(await client.resetConfig())
    }
  }

  @objc(getProduct:resolve:reject:)
  public func getProduct(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getProduct(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getOrderedNutrients:reject:)
  public func getOrderedNutrients(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getOrderedNutrients())
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getNutrientMetadata:reject:)
  public func getNutrientMetadata(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getNutrientMetadata())
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getSuggestions:resolve:reject:)
  public func getSuggestions(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getSuggestions(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(search:resolve:reject:)
  public func search(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.search(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(saveProduct:resolve:reject:)
  public func saveProduct(
    _ request: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.saveProduct(asSwiftDictionary(request)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(patchProduct:resolve:reject:)
  public func patchProduct(
    _ request: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.patchProduct(asSwiftDictionary(request)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(uploadProductImage:resolve:reject:)
  public func uploadProductImage(
    _ request: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.uploadProductImage(asSwiftDictionary(request)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(deleteProductImage:resolve:reject:)
  public func deleteProductImage(
    _ request: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.deleteProductImage(asSwiftDictionary(request)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(canonicalizeTags:resolve:reject:)
  public func canonicalizeTags(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.canonicalizeTags(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(displayTags:resolve:reject:)
  public func displayTags(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.displayTags(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getTagKnowledge:resolve:reject:)
  public func getTagKnowledge(
    _ query: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getTagKnowledge(asSwiftDictionary(query)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(revertProduct:resolve:reject:)
  public func revertProduct(
    _ request: NSDictionary,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.revertProduct(asSwiftDictionary(request)))
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getExternalSources:reject:)
  public func getExternalSources(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getExternalSources())
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getPreferences:reject:)
  public func getPreferences(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getPreferences())
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }

  @objc(getAttributeGroups:reject:)
  public func getAttributeGroups(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      do {
        resolve(try await client.getAttributeGroups())
      } catch let error as OpenFoodFactsModuleError {
        reject(error.code, error.message, error.asNSError())
      } catch {
        reject("E_NETWORK", error.localizedDescription, error)
      }
    }
  }
}

private func asSwiftDictionary(_ dictionary: NSDictionary) -> [String: Any] {
  dictionary as? [String: Any] ?? [:]
}
