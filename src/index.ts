import NativeOpenFoodFacts, { LINKING_ERROR } from './NativeOpenFoodFacts';

import type {
  ApiStatus,
  NutrientMetadata,
  OffConfigInput,
  OffConfigState,
  OrderedNutrient,
  ProductImageDeleteRequest,
  ProductImageUploadRequest,
  ProductPatchRequest,
  ProductQuery,
  ProductRevertRequest,
  ProductResponse,
  RuntimeInfo,
  SaveProductRequest,
  SearchQuery,
  SuggestionsQuery,
  TagKnowledgeQuery,
  TaxonomyCanonicalizeQuery,
  TaxonomyCanonicalizeResponse,
  TaxonomyDisplayQuery,
  TaxonomyDisplayResponse,
  SearchResult,
} from './types';

export type {
  ApiStatus,
  CountryCode,
  LanguageCode,
  LocalizedStringEntry,
  NutrientMeta,
  NutrientMetadata,
  NutrientMetadataEntry,
  NutrientUnit,
  NutrimentEntry,
  OffConfigInput,
  OffConfigState,
  OffEnvironment,
  OffUser,
  OffUserAgent,
  OrderedNutrient,
  ProductImageDeleteRequest,
  ProductImageUploadRequest,
  ProductPatchRequest,
  Product,
  ProductQuery,
  ProductRevertRequest,
  ProductResponse,
  RuntimeInfo,
  SaveProductField,
  SaveProductRequest,
  SearchQuery,
  SearchQueryScalar,
  SearchQueryValue,
  SuggestionsQuery,
  TagKnowledgeQuery,
  TaxonomyCanonicalizeQuery,
  TaxonomyCanonicalizeResponse,
  TaxonomyDisplayQuery,
  TaxonomyDisplayResponse,
} from './types';

function getNativeModule() {
  if (!NativeOpenFoodFacts) {
    throw new Error(LINKING_ERROR);
  }

  return NativeOpenFoodFacts;
}

export function getRuntimeInfo(): Promise<RuntimeInfo> {
  return getNativeModule().getRuntimeInfo() as Promise<RuntimeInfo>;
}

export function configure(config: OffConfigInput): Promise<OffConfigState> {
  return getNativeModule().configure(config) as Promise<OffConfigState>;
}

export function getConfig(): Promise<OffConfigState> {
  return getNativeModule().getConfig() as Promise<OffConfigState>;
}

export function resetConfig(): Promise<OffConfigState> {
  return getNativeModule().resetConfig() as Promise<OffConfigState>;
}

export function getProduct(query: ProductQuery): Promise<ProductResponse> {
  return getNativeModule().getProduct(query);
}

export function fetchProductByCode(code: string): Promise<ProductResponse> {
  return getProduct({ barcode: code });
}

export function getOrderedNutrients(): Promise<ReadonlyArray<OrderedNutrient>> {
  return getNativeModule().getOrderedNutrients();
}

export function getNutrientMetadata(): Promise<NutrientMetadata> {
  return getNativeModule().getNutrientMetadata();
}

export function getSuggestions(
  query: SuggestionsQuery
): Promise<ReadonlyArray<string>> {
  return getNativeModule().getSuggestions(query);
}

export function search(query: SearchQuery): Promise<SearchResult> {
  return getNativeModule().search(query);
}

export function saveProduct(request: SaveProductRequest): Promise<ApiStatus> {
  return getNativeModule().saveProduct(request);
}

export function patchProduct(request: ProductPatchRequest): Promise<string> {
  return getNativeModule().patchProduct(request);
}

export function uploadProductImage(
  request: ProductImageUploadRequest
): Promise<string> {
  return getNativeModule().uploadProductImage(request);
}

export function deleteProductImage(
  request: ProductImageDeleteRequest
): Promise<string> {
  return getNativeModule().deleteProductImage(request);
}

export function canonicalizeTags(
  query: TaxonomyCanonicalizeQuery
): Promise<TaxonomyCanonicalizeResponse> {
  return getNativeModule().canonicalizeTags(query);
}

export function displayTags(
  query: TaxonomyDisplayQuery
): Promise<TaxonomyDisplayResponse> {
  return getNativeModule().displayTags(query);
}

export function getTagKnowledge(query: TagKnowledgeQuery): Promise<string> {
  return getNativeModule().getTagKnowledge(query);
}

export function revertProduct(request: ProductRevertRequest): Promise<string> {
  return getNativeModule().revertProduct(request);
}

export function getExternalSources(): Promise<string> {
  return getNativeModule().getExternalSources();
}

export function getPreferences(): Promise<string> {
  return getNativeModule().getPreferences();
}

export function getAttributeGroups(): Promise<string> {
  return getNativeModule().getAttributeGroups();
}
