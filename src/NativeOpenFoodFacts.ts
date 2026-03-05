import type { TurboModule } from 'react-native';
import { NativeModules, TurboModuleRegistry } from 'react-native';
import type { UnsafeObject } from 'react-native/Libraries/Types/CodegenTypes';

interface OffUser {
  comment?: string;
  userId: string;
  password: string;
}

interface OffUserAgent {
  comment?: string;
  name?: string;
  system?: string;
  version?: string;
}

interface OffConfigInput {
  country?: string;
  environment?: string;
  globalUser?: OffUser;
  productsLanguage?: string;
  uiLanguage?: string;
  useRequired?: boolean;
  userAgent?: OffUserAgent;
}

interface OffConfigState {
  country: string;
  environment: string;
  globalUser?: OffUser;
  productsLanguage: string;
  uiLanguage: string;
  useRequired: boolean;
  userAgent: OffUserAgent;
}

interface RuntimeInfo {
  moduleName: string;
  nativeVersion: string;
  newArchitecture: boolean;
  platform: string;
}

interface ProductQuery {
  barcode: string;
  country?: string;
  fields?: ReadonlyArray<string>;
  languages?: ReadonlyArray<string>;
}

interface NutrimentEntry {
  key: string;
  numberValue?: number;
  stringValue?: string;
}

interface Product {
  brands?: string;
  categories?: string;
  code: string;
  dataPer?: string;
  imageFront?: string;
  imageIngredients?: string;
  imageNutrition?: string;
  keywords?: ReadonlyArray<string>;
  lang?: string;
  nutriments?: ReadonlyArray<NutrimentEntry>;
  packagingQuantity?: number;
  productName?: string;
  productNameEn?: string;
  quantity?: string;
  servingQuantity?: number;
  servingSize?: string;
}

interface ProductResponse {
  barcode?: string;
  hasProduct: boolean;
  product?: Product;
  status?: string;
}

interface OrderedNutrient {
  displayInEditForm: boolean;
  id: string;
  important: boolean;
  name: string;
}

interface LocalizedStringEntry {
  languageCode: string;
  value: string;
}

interface NutrientMeta {
  name: ReadonlyArray<LocalizedStringEntry>;
  unit?: string;
}

interface NutrientMetadataEntry {
  key: string;
  meta: NutrientMeta;
}

interface NutrientMetadata {
  nutrients: ReadonlyArray<NutrientMetadataEntry>;
}

interface SuggestionsQuery {
  categories?: string;
  country: string;
  getSynonyms?: boolean;
  language: string;
  limit?: number;
  query?: string;
  shape?: string;
  tagtype?: string;
  term?: string;
}

// Codegen can't parse our public TS SearchQuery type; keep this permissive in the native spec.
type SearchQuery = UnsafeObject;

interface SearchResult {
  count: number;
  page: number;
  pageCount: number;
  pageSize: number;
  products: ReadonlyArray<Product>;
  skip: number;
}

interface SaveProductField {
  key: string;
  value: string;
}

interface SaveProductRequest {
  fields: ReadonlyArray<SaveProductField>;
}

interface ApiStatus {
  body?: string;
  error?: string;
  imageId?: number;
  status?: number;
  statusVerbose?: string;
}

interface ProductPatchRequest {
  bodyJson: string;
  code: string;
}

interface ProductImageUploadRequest {
  bodyJson: string;
  code: string;
}

interface ProductImageDeleteRequest {
  code: string;
  imageId: number;
}

interface TaxonomyCanonicalizeQuery {
  language?: string;
  localTags: ReadonlyArray<string>;
  tagtype: string;
}

interface TaxonomyCanonicalizeResponse {
  canonicalTags: ReadonlyArray<string>;
  rawJson: string;
}

interface TaxonomyDisplayQuery {
  canonicalTags: ReadonlyArray<string>;
  language?: string;
  tagtype: string;
}

interface TaxonomyDisplayResponse {
  displayTags: ReadonlyArray<string>;
  rawJson: string;
}

interface TagKnowledgeQuery {
  country?: string;
  language?: string;
  tag: string;
  tagtype: string;
}

interface ProductRevertRequest {
  code: string;
  fields?: ReadonlyArray<string>;
  revision: number;
  tagsLanguage?: string;
}

export interface Spec extends TurboModule {
  getRuntimeInfo(): Promise<RuntimeInfo>;
  configure(config: OffConfigInput): Promise<OffConfigState>;
  getConfig(): Promise<OffConfigState>;
  resetConfig(): Promise<OffConfigState>;
  getProduct(query: ProductQuery): Promise<ProductResponse>;
  getOrderedNutrients(): Promise<ReadonlyArray<OrderedNutrient>>;
  getNutrientMetadata(): Promise<NutrientMetadata>;
  getSuggestions(query: SuggestionsQuery): Promise<ReadonlyArray<string>>;
  search(query: SearchQuery): Promise<SearchResult>;
  saveProduct(request: SaveProductRequest): Promise<ApiStatus>;
  patchProduct(request: ProductPatchRequest): Promise<string>;
  uploadProductImage(request: ProductImageUploadRequest): Promise<string>;
  deleteProductImage(request: ProductImageDeleteRequest): Promise<string>;
  canonicalizeTags(
    query: TaxonomyCanonicalizeQuery
  ): Promise<TaxonomyCanonicalizeResponse>;
  displayTags(query: TaxonomyDisplayQuery): Promise<TaxonomyDisplayResponse>;
  getTagKnowledge(query: TagKnowledgeQuery): Promise<string>;
  revertProduct(request: ProductRevertRequest): Promise<string>;
  getExternalSources(): Promise<string>;
  getPreferences(): Promise<string>;
  getAttributeGroups(): Promise<string>;
}

export const LINKING_ERROR =
  "The native module 'OpenFoodFacts' is unavailable. Reinstall dependencies, run codegen, and rebuild the native app.";

function resolveNativeModule(): Spec | null {
  try {
    const turboModule = TurboModuleRegistry.get<Spec>('OpenFoodFacts');
    if (turboModule) {
      return turboModule;
    }
  } catch {
    // Fall through to the bridge module for environments where the turbo registry is not ready.
  }

  return (NativeModules.OpenFoodFacts as Spec | undefined) ?? null;
}

export default resolveNativeModule();
