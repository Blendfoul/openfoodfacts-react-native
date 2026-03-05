# react-native-openfoodfacts

React Native native module for Open Food Facts APIs (iOS + Android), with a typed TypeScript API and runnable example app.

## Features

- Runtime/config management (`getRuntimeInfo`, `configure`, `getConfig`, `resetConfig`)
- Product read APIs (`getProduct`, `fetchProductByCode`, `search`)
- Nutrition metadata APIs (`getOrderedNutrients`, `getNutrientMetadata`)
- Taxonomy APIs (`getSuggestions`, `canonicalizeTags`, `displayTags`, `getTagKnowledge`)
- Write/mutation APIs (`saveProduct`, `patchProduct`, `uploadProductImage`, `deleteProductImage`, `revertProduct`)
- Supporting APIs (`getExternalSources`, `getPreferences`, `getAttributeGroups`)
- TurboModule implementation for React Native New Architecture projects

## Requirements

- `react-native >= 0.76.0` (peer dependency)
- `react` (peer dependency)
- Node.js `>= 18` for this library
- iOS target `>= 15.1`

## Installation

```bash
yarn add react-native-openfoodfacts
```

Then rebuild native apps:

- iOS: `cd ios && pod install && cd ..`
- Android: rebuild with Gradle / `react-native run-android`

## Quick Start

```ts
import {
  configure,
  getRuntimeInfo,
  fetchProductByCode,
} from 'react-native-openfoodfacts';

await configure({
  environment: 'production',
  country: 'us',
  uiLanguage: 'en',
  productsLanguage: 'en',
});

const runtime = await getRuntimeInfo();
const product = await fetchProductByCode('3017620422003');
```

## Configuration

`configure` manages a global native client state.

```ts
await configure({
  environment: 'production', // or 'staging'
  country: 'us',
  uiLanguage: 'en',
  productsLanguage: 'en',
  useRequired: true,
  globalUser: {
    userId: 'your-off-username',
    password: 'your-off-password',
    comment: 'optional',
  },
  userAgent: {
    name: 'my-app',
    version: '1.0.0',
    system: 'react-native',
    comment: 'optional',
  },
});
```

Default state after startup / `resetConfig()`:

- `environment: 'production'`
- `useRequired: true`
- `country: 'us'`
- `uiLanguage: 'en'`
- `productsLanguage: 'en'`
- `globalUser: undefined`
- `userAgent`: generated from app metadata and a persisted app UUID

Important behavior:

- In `staging`, if no `globalUser` is set, the module auto-uses test credentials `off/off`.
- In `production`, write methods require credentials (`globalUser`) and throw `E_CONFIG` otherwise.
- Country/language fields are validated against known ISO codes and throw `E_VALIDATION` on invalid input.

## API Reference

Full types are exported from `src/types.ts`.

### Method Cheat Sheet

| Method | Input | Returns | Purpose |
| --- | --- | --- | --- |
| `getRuntimeInfo` | none | `RuntimeInfo` | Verify native linking/platform info. |
| `configure` | `OffConfigInput` | `OffConfigState` | Set global client config. |
| `getConfig` | none | `OffConfigState` | Read current config. |
| `resetConfig` | none | `OffConfigState` | Reset to default config. |
| `getProduct` | `ProductQuery` | `ProductResponse` | Fetch product by barcode with optional fields/languages. |
| `fetchProductByCode` | `code: string` | `ProductResponse` | Shortcut to `getProduct({ barcode: code })`. |
| `getOrderedNutrients` | none | `OrderedNutrient[]` | Get OFF nutrient ordering list. |
| `getNutrientMetadata` | none | `NutrientMetadata` | Get nutrient names and normalized units. |
| `getSuggestions` | `SuggestionsQuery` | `string[]` | Taxonomy autocomplete suggestions. |
| `search` | `SearchQuery` | `SearchResult` | Product search (`/api/v2/search`). |
| `saveProduct` | `SaveProductRequest` | `ApiStatus` | Create/update product using form-style fields. |
| `patchProduct` | `ProductPatchRequest` | `string` (raw JSON) | Patch product with JSON payload. |
| `uploadProductImage` | `ProductImageUploadRequest` | `string` (raw JSON) | Upload image via JSON endpoint. |
| `deleteProductImage` | `ProductImageDeleteRequest` | `string` (raw JSON) | Delete uploaded image by image id. |
| `canonicalizeTags` | `TaxonomyCanonicalizeQuery` | `TaxonomyCanonicalizeResponse` | Convert local labels to canonical OFF tags. |
| `displayTags` | `TaxonomyDisplayQuery` | `TaxonomyDisplayResponse` | Convert canonical tags to display labels. |
| `getTagKnowledge` | `TagKnowledgeQuery` | `string` (raw JSON) | Fetch raw knowledge payload for one tag. |
| `revertProduct` | `ProductRevertRequest` | `string` (raw JSON) | Revert fields to an earlier revision. |
| `getExternalSources` | none | `string` (raw JSON) | List OFF external source definitions. |
| `getPreferences` | none | `string` (raw JSON) | Get OFF preferences payload. |
| `getAttributeGroups` | none | `string` (raw JSON) | Get OFF attribute group definitions. |

### Runtime and Config

- `getRuntimeInfo(): Promise<RuntimeInfo>`
- `configure(config: OffConfigInput): Promise<OffConfigState>`
- `getConfig(): Promise<OffConfigState>`
- `resetConfig(): Promise<OffConfigState>`

### Read APIs

- `getProduct(query: ProductQuery): Promise<ProductResponse>`
- `fetchProductByCode(code: string): Promise<ProductResponse>`
- `getOrderedNutrients(): Promise<ReadonlyArray<OrderedNutrient>>`
- `getNutrientMetadata(): Promise<NutrientMetadata>`
- `getSuggestions(query: SuggestionsQuery): Promise<ReadonlyArray<string>>`
- `search(query: SearchQuery): Promise<SearchResult>`
- `getExternalSources(): Promise<string>`
- `getPreferences(): Promise<string>`
- `getAttributeGroups(): Promise<string>`

Example:

```ts
const product = await getProduct({
  barcode: '3017620422003',
  country: 'us',
  languages: ['en'],
  fields: ['code', 'product_name', 'brands', 'image_front_url'],
});
```

`search` supports documented `/api/v2/search` params such as `fields`, `page`, `page_size`, `sort_by`,
tag filters (`categories_tags`, `labels_tags`, `*_tags_<language_code>`), and nutrient filters (`salt_100g<2`, etc).

Search example:

```ts
const result = await search({
  categories_tags_en: 'en:chocolate-bars',
  page: 1,
  page_size: 5,
  fields: ['code', 'product_name', 'brands'],
  sort_by: 'unique_scans_n',
});
```

### Taxonomy APIs

- `canonicalizeTags(query: TaxonomyCanonicalizeQuery): Promise<TaxonomyCanonicalizeResponse>`
- `displayTags(query: TaxonomyDisplayQuery): Promise<TaxonomyDisplayResponse>`
- `getTagKnowledge(query: TagKnowledgeQuery): Promise<string>`

Example:

```ts
const canonical = await canonicalizeTags({
  tagtype: 'categories',
  localTags: ['Chocolate bars', 'Organic'],
  language: 'en',
});
```

Tag knowledge example:

```ts
const knowledgeJson = await getTagKnowledge({
  tagtype: 'ingredients',
  tag: 'en:palm-oil',
  country: 'us',
  language: 'en',
});
```

### Write APIs

- `saveProduct(request: SaveProductRequest): Promise<ApiStatus>`
- `patchProduct(request: ProductPatchRequest): Promise<string>`
- `uploadProductImage(request: ProductImageUploadRequest): Promise<string>`
- `deleteProductImage(request: ProductImageDeleteRequest): Promise<string>`
- `revertProduct(request: ProductRevertRequest): Promise<string>`

Examples:

```ts
await configure({
  environment: 'staging', // recommended for testing writes
  country: 'us',
  uiLanguage: 'en',
  productsLanguage: 'en',
});

await saveProduct({
  fields: [
    { key: 'code', value: '2000000000001' },
    { key: 'product_name', value: 'Demo product' },
    { key: 'brands', value: 'OFF Demo' },
  ],
});

await patchProduct({
  code: '2000000000001',
  bodyJson: JSON.stringify({
    product: { product_name: 'Demo product (patched)' },
  }),
});

await uploadProductImage({
  code: '2000000000001',
  bodyJson: JSON.stringify({
    imagefield: 'front',
    imgupload_front: '<base64_png>',
  }),
});
```

Notes:

- `saveProduct` returns parsed API status fields (`status`, `statusVerbose`, `error`, `imageId`, `body`).
- `patchProduct`, `uploadProductImage`, `deleteProductImage`, `revertProduct`, `search`, `getTagKnowledge`, `getExternalSources`, `getPreferences`, and `getAttributeGroups` return raw JSON strings.

## Error Handling

All methods reject promises with native error codes:

- `E_VALIDATION`: invalid input shape or values
- `E_CONFIG`: missing/invalid client configuration
- `E_NETWORK`: request/network failure
- `E_HTTP_STATUS`: non-2xx HTTP response
- `E_DESERIALIZATION`: response parsing failure
- `E_API_STATUS`: OFF API returned non-success status (for example from `saveProduct`)

## Local Development (This Repository)

Install dependencies:

```bash
yarn install
```

Note: the `example` workspace currently declares Node.js `>= 22.11.0`.

Run checks:

```bash
yarn lint
yarn typecheck
yarn build
```

Run example app:

```bash
cd example
yarn install
yarn start
yarn android
yarn ios
```

For iOS example setup (first run / native dependency changes):

```bash
cd example
bundle install
cd ios
bundle exec pod install
```

## Project Structure

```text
.
|-- src/      # TypeScript public API and types
|-- android/  # Android native implementation
|-- ios/      # iOS native implementation
`-- example/  # Runnable React Native app showing all exported methods
```

## Reference Links

- [Open Food Facts API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/)
- [Open Food Facts JavaScript wiki](https://wiki.openfoodfacts.org/API/Javascript)

## License

Apache 2.0. See [LICENSE](./LICENSE).
