import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

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
  SuggestionsQuery,
  TagKnowledgeQuery,
  TaxonomyCanonicalizeQuery,
  TaxonomyCanonicalizeResponse,
  TaxonomyDisplayQuery,
  TaxonomyDisplayResponse,
} from './types';

export interface Spec extends TurboModule {
  getRuntimeInfo(): Promise<RuntimeInfo>;
  configure(config: OffConfigInput): Promise<OffConfigState>;
  getConfig(): Promise<OffConfigState>;
  resetConfig(): Promise<OffConfigState>;
  getProduct(query: ProductQuery): Promise<ProductResponse>;
  getOrderedNutrients(): Promise<ReadonlyArray<OrderedNutrient>>;
  getNutrientMetadata(): Promise<NutrientMetadata>;
  getSuggestions(query: SuggestionsQuery): Promise<ReadonlyArray<string>>;
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

export default TurboModuleRegistry.get<Spec>('OpenFoodFacts');
