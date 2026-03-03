export type RuntimePlatform = 'android' | 'ios';

export interface RuntimeInfo {
  platform: RuntimePlatform;
  moduleName: string;
  newArchitecture: true;
  nativeVersion: string;
}

export type OffEnvironment = 'production' | 'staging';

export type CountryCode = string;

export type LanguageCode = string;

export interface OffUser {
  comment?: string;
  userId: string;
  password: string;
}

export interface OffUserAgent {
  name?: string;
  version?: string;
  system?: string;
  comment?: string;
}

export interface OffConfigInput {
  environment?: OffEnvironment;
  useRequired?: boolean;
  globalUser?: OffUser;
  userAgent?: OffUserAgent;
  country?: CountryCode;
  uiLanguage?: LanguageCode;
  productsLanguage?: LanguageCode;
}

export interface OffConfigState {
  environment: OffEnvironment;
  useRequired: boolean;
  globalUser?: OffUser;
  userAgent: OffUserAgent;
  country: CountryCode;
  uiLanguage: LanguageCode;
  productsLanguage: LanguageCode;
}

export interface ProductQuery {
  barcode: string;
  languages?: ReadonlyArray<LanguageCode>;
  country?: CountryCode;
  fields?: ReadonlyArray<string>;
}

export interface NutrimentEntry {
  key: string;
  numberValue?: number;
  stringValue?: string;
}

export interface Product {
  code: string;
  productName?: string;
  productNameEn?: string;
  brands?: string;
  lang?: LanguageCode;
  quantity?: string;
  packagingQuantity?: number;
  servingSize?: string;
  servingQuantity?: number;
  dataPer?: string;
  categories?: string;
  imageFront?: string;
  imageIngredients?: string;
  imageNutrition?: string;
  keywords?: ReadonlyArray<string>;
  nutriments?: ReadonlyArray<NutrimentEntry>;
}

export interface ProductResponse {
  barcode?: string;
  status?: string;
  product?: Product;
  hasProduct: boolean;
}

export interface OrderedNutrient {
  id: string;
  name: string;
  important: boolean;
  displayInEditForm: boolean;
}

export type NutrientUnit = string;

export interface LocalizedStringEntry {
  languageCode: string;
  value: string;
}

export interface NutrientMeta {
  unit?: NutrientUnit;
  name: ReadonlyArray<LocalizedStringEntry>;
}

export interface NutrientMetadataEntry {
  key: string;
  meta: NutrientMeta;
}

export interface NutrientMetadata {
  nutrients: ReadonlyArray<NutrientMetadataEntry>;
}

export interface SuggestionsQuery {
  query?: string;
  tagtype?: string;
  country: CountryCode;
  language: LanguageCode;
  categories?: string;
  shape?: string;
  getSynonyms?: boolean;
  term?: string;
  limit?: number;
}

export type SearchQueryScalar = string | number | boolean;

// Keys are forwarded as-is to /api/v2/search so callers can use the OFF docs directly.
export type SearchQueryValue =
  | SearchQueryScalar
  | ReadonlyArray<SearchQueryScalar>;

export type SearchQuery = Readonly<Record<string, SearchQueryValue>>;

export interface ProductPatchRequest {
  code: string;
  bodyJson: string;
}

export interface ProductImageUploadRequest {
  code: string;
  bodyJson: string;
}

export interface ProductImageDeleteRequest {
  code: string;
  imageId: number;
}

export interface TaxonomyCanonicalizeQuery {
  tagtype: string;
  localTags: ReadonlyArray<string>;
  language?: LanguageCode;
}

export interface TaxonomyCanonicalizeResponse {
  canonicalTags: ReadonlyArray<string>;
  rawJson: string;
}

export interface TaxonomyDisplayQuery {
  tagtype: string;
  canonicalTags: ReadonlyArray<string>;
  language?: LanguageCode;
}

export interface TaxonomyDisplayResponse {
  displayTags: ReadonlyArray<string>;
  rawJson: string;
}

export interface TagKnowledgeQuery {
  tagtype: string;
  tag: string;
  country?: CountryCode;
  language?: LanguageCode;
}

export interface ProductRevertRequest {
  code: string;
  revision: number;
  fields?: ReadonlyArray<string>;
  tagsLanguage?: LanguageCode;
}

export interface SaveProductField {
  key: string;
  value: string;
}

export interface SaveProductRequest {
  fields: ReadonlyArray<SaveProductField>;
}

export interface ApiStatus {
  status?: number;
  statusVerbose?: string;
  body?: string;
  error?: string;
  imageId?: number;
}
