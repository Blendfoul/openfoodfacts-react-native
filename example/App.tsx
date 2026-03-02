import React, { useState } from 'react';
import {
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import { canonicalizeTags, configure, deleteProductImage, displayTags, fetchProductByCode, getAttributeGroups, getConfig, getExternalSources, getNutrientMetadata, getOrderedNutrients, getPreferences, getProduct, getRuntimeInfo, getSuggestions, getTagKnowledge, patchProduct, resetConfig, revertProduct, saveProduct, uploadProductImage,  } from 'react-native-openfoodfacts'
import type { OffConfigInput } from 'react-native-openfoodfacts';

type ExampleResult = {
  error?: string;
  output?: string;
  status: 'error' | 'loading' | 'success';
};

type ApiExample = {
  caution?: string;
  description: string;
  environment: 'Local state' | 'Production API' | 'Staging API';
  id: string;
  snippet: string;
  title: string;
  run: () => Promise<unknown>;
};

type ApiSection = {
  description: string;
  title: string;
  examples: ReadonlyArray<ApiExample>;
};

const CODE_FONT = Platform.select({
  android: 'monospace',
  default: 'monospace',
  ios: 'Menlo',
}) ?? 'monospace';

const READ_BARCODE = '3017620422003';
const DEMO_WRITE_BARCODE = '2000000000001';
const TINY_PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO5/0X8AAAAASUVORK5CYII=';

const PRODUCTION_CONFIG: OffConfigInput = {
  country: 'us',
  environment: 'production',
  productsLanguage: 'en',
  uiLanguage: 'en',
  userAgent: {
    comment: 'Example API explorer',
    name: 'react-native-openfoodfacts-example',
    system: 'react-native',
    version: '0.1.0',
  },
  useRequired: true,
};

const STAGING_CONFIG: OffConfigInput = {
  ...PRODUCTION_CONFIG,
  environment: 'staging',
};

const PRODUCT_QUERY = {
  barcode: READ_BARCODE,
  country: 'us',
  fields: ['code', 'product_name', 'brands', 'image_front_url'],
  languages: ['en'],
} as const;

const SUGGESTIONS_QUERY = {
  country: 'us',
  getSynonyms: true,
  language: 'en',
  limit: 5,
  query: 'chocolate',
  tagtype: 'categories',
} as const;

const CANONICALIZE_QUERY = {
  language: 'en',
  localTags: ['Chocolate bars', 'Organic'],
  tagtype: 'categories',
} as const;

const DISPLAY_QUERY = {
  canonicalTags: ['en:chocolate-bars', 'en:organic'],
  language: 'fr',
  tagtype: 'categories',
} as const;

const TAG_KNOWLEDGE_QUERY = {
  country: 'us',
  language: 'en',
  tag: 'en:palm-oil',
  tagtype: 'ingredients',
} as const;

const SAVE_REQUEST = {
  fields: [
    { key: 'code', value: DEMO_WRITE_BARCODE },
    { key: 'product_name', value: 'React Native Demo Product' },
    { key: 'brands', value: 'Open Food Facts Example' },
    { key: 'quantity', value: '100 g' },
  ],
} as const;

const PATCH_REQUEST = {
  bodyJson: JSON.stringify(
    {
      product: {
        brands: 'Open Food Facts Example',
        product_name: 'React Native Demo Product (patched)',
      },
    },
    null,
    2
  ),
  code: DEMO_WRITE_BARCODE,
} as const;

const IMAGE_UPLOAD_REQUEST = {
  bodyJson: JSON.stringify(
    {
      imagefield: 'front',
      imgupload_front: TINY_PNG_BASE64,
    },
    null,
    2
  ),
  code: DEMO_WRITE_BARCODE,
} as const;

const REVERT_REQUEST = {
  code: DEMO_WRITE_BARCODE,
  fields: ['product_name'],
  revision: 1,
  tagsLanguage: 'en',
} as const;

async function runInEnvironment<T>(
  config: OffConfigInput,
  task: () => Promise<T>
): Promise<T> {
  await configure(config);
  return task();
}

async function ensureDemoProduct() {
  await runInEnvironment(STAGING_CONFIG, async () => {
    await saveProduct(SAVE_REQUEST);
  });
}

function parseMaybeJson(value: string): unknown {
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}

function parseUploadedImageId(value: string): number | undefined {
  const parsed = parseMaybeJson(value);
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return undefined;
  }

  const candidate = (parsed as { imgid?: unknown }).imgid;
  return typeof candidate === 'number' ? candidate : undefined;
}

function formatValue(value: unknown): string {
  if (typeof value === 'string') {
    const parsed = parseMaybeJson(value);
    return typeof parsed === 'string' ? parsed : JSON.stringify(parsed, null, 2);
  }

  return JSON.stringify(value, null, 2) ?? String(value);
}

function formatError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }

  return String(error);
}

const API_SECTIONS: ReadonlyArray<ApiSection> = [
  {
    description:
      'These methods help you validate native linking and control the global Open Food Facts client state.',
    title: 'Runtime And Config',
    examples: [
      {
        description:
          'Confirms the TurboModule is linked and exposes native platform metadata.',
        environment: 'Local state',
        id: 'getRuntimeInfo',
        run: () => getRuntimeInfo(),
        snippet: 'await getRuntimeInfo();',
        title: 'getRuntimeInfo()',
      },
      {
        description:
          'Applies an explicit client configuration. The sample uses production defaults for read-only requests.',
        environment: 'Local state',
        id: 'configure',
        run: () => configure(PRODUCTION_CONFIG),
        snippet: `await configure(${JSON.stringify(PRODUCTION_CONFIG, null, 2)});`,
        title: 'configure(config)',
      },
      {
        description:
          'Reads back the currently active config, including the last environment selected by other example cards.',
        environment: 'Local state',
        id: 'getConfig',
        run: () => getConfig(),
        snippet: 'await getConfig();',
        title: 'getConfig()',
      },
      {
        description:
          'Restores the native module defaults: production, English, and the generated user agent.',
        environment: 'Local state',
        id: 'resetConfig',
        run: () => resetConfig(),
        snippet: 'await resetConfig();',
        title: 'resetConfig()',
      },
    ],
  },
  {
    description:
      'These are the read-only endpoints most applications start with for product and metadata queries.',
    title: 'Read API',
    examples: [
      {
        description:
          'Fetches a product with a full request object so you can narrow fields and set locale hints.',
        environment: 'Production API',
        id: 'getProduct',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getProduct(PRODUCT_QUERY)
          ),
        snippet: `await getProduct(${JSON.stringify(PRODUCT_QUERY, null, 2)});`,
        title: 'getProduct(query)',
      },
      {
        description:
          'The convenience helper when you only need a barcode and the default configuration.',
        environment: 'Production API',
        id: 'fetchProductByCode',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            fetchProductByCode(READ_BARCODE)
          ),
        snippet: `await fetchProductByCode('${READ_BARCODE}');`,
        title: 'fetchProductByCode(code)',
      },
      {
        description:
          'Returns the nutrient order used by Open Food Facts edit forms and nutrition UI.',
        environment: 'Production API',
        id: 'getOrderedNutrients',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getOrderedNutrients()
          ),
        snippet: 'await getOrderedNutrients();',
        title: 'getOrderedNutrients()',
      },
      {
        description:
          'Loads nutrient metadata, including display names per language and normalized units.',
        environment: 'Production API',
        id: 'getNutrientMetadata',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getNutrientMetadata()
          ),
        snippet: 'await getNutrientMetadata();',
        title: 'getNutrientMetadata()',
      },
      {
        description:
          'Looks up taxonomy suggestions for autocomplete experiences such as category pickers.',
        environment: 'Production API',
        id: 'getSuggestions',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getSuggestions(SUGGESTIONS_QUERY)
          ),
        snippet: `await getSuggestions(${JSON.stringify(SUGGESTIONS_QUERY, null, 2)});`,
        title: 'getSuggestions(query)',
      },
      {
        description:
          'Lists the structured external source providers exposed by the Open Food Facts API.',
        environment: 'Production API',
        id: 'getExternalSources',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getExternalSources()
          ),
        snippet: 'await getExternalSources();',
        title: 'getExternalSources()',
      },
      {
        description:
          'Fetches user-facing preference definitions used by personalized nutrition features.',
        environment: 'Production API',
        id: 'getPreferences',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getPreferences()
          ),
        snippet: 'await getPreferences();',
        title: 'getPreferences()',
      },
      {
        description:
          'Returns attribute group definitions from the v3.4 API for product attribute rendering.',
        environment: 'Production API',
        id: 'getAttributeGroups',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getAttributeGroups()
          ),
        snippet: 'await getAttributeGroups();',
        title: 'getAttributeGroups()',
      },
    ],
  },
  {
    description:
      'The taxonomy helpers normalize local labels, turn canonical tags into display labels, and fetch tag knowledge JSON.',
    title: 'Taxonomy API',
    examples: [
      {
        description:
          'Converts user-facing labels into canonical taxonomy tags that can be stored on products.',
        environment: 'Production API',
        id: 'canonicalizeTags',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            canonicalizeTags(CANONICALIZE_QUERY)
          ),
        snippet: `await canonicalizeTags(${JSON.stringify(CANONICALIZE_QUERY, null, 2)});`,
        title: 'canonicalizeTags(query)',
      },
      {
        description:
          'Turns canonical tags back into display labels in the target UI language.',
        environment: 'Production API',
        id: 'displayTags',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            displayTags(DISPLAY_QUERY)
          ),
        snippet: `await displayTags(${JSON.stringify(DISPLAY_QUERY, null, 2)});`,
        title: 'displayTags(query)',
      },
      {
        description:
          'Fetches the raw tag knowledge payload when you need the server-side details for one tag.',
        environment: 'Production API',
        id: 'getTagKnowledge',
        run: () =>
          runInEnvironment(PRODUCTION_CONFIG, async () =>
            getTagKnowledge(TAG_KNOWLEDGE_QUERY)
          ),
        snippet: `await getTagKnowledge(${JSON.stringify(TAG_KNOWLEDGE_QUERY, null, 2)});`,
        title: 'getTagKnowledge(query)',
      },
    ],
  },
  {
    description:
      'These calls mutate data. Each sample switches to staging first, so you can inspect the request shape without touching production.',
    title: 'Write API',
    examples: [
      {
        caution:
          'This sample writes a demo product to staging. Keep the barcode unique if you adapt it for repeated testing.',
        description:
          'Creates or updates a product with form-style fields. The staging environment auto-fills the demo off/off credentials.',
        environment: 'Staging API',
        id: 'saveProduct',
        run: () =>
          runInEnvironment(STAGING_CONFIG, async () =>
            saveProduct(SAVE_REQUEST)
          ),
        snippet: `await saveProduct(${JSON.stringify(SAVE_REQUEST, null, 2)});`,
        title: 'saveProduct(request)',
      },
      {
        caution:
          'The patch body follows the v3 JSON API. The example creates the demo product first, then patches it.',
        description:
          'Sends a raw JSON PATCH payload to the product endpoint when you need field-level updates.',
        environment: 'Staging API',
        id: 'patchProduct',
        run: async () => {
          await ensureDemoProduct();
          return runInEnvironment(STAGING_CONFIG, async () =>
            patchProduct(PATCH_REQUEST)
          );
        },
        snippet: `await patchProduct(${JSON.stringify(PATCH_REQUEST, null, 2)});`,
        title: 'patchProduct(request)',
      },
      {
        caution:
          'The payload uses a 1x1 PNG base64 string. Replace it with your real encoded image data for actual uploads.',
        description:
          'Uploads an image using the JSON image endpoint instead of the older multipart form flow.',
        environment: 'Staging API',
        id: 'uploadProductImage',
        run: async () => {
          await ensureDemoProduct();
          return runInEnvironment(STAGING_CONFIG, async () =>
            uploadProductImage(IMAGE_UPLOAD_REQUEST)
          );
        },
        snippet: `await uploadProductImage(${JSON.stringify(
          IMAGE_UPLOAD_REQUEST,
          null,
          2
        )});`,
        title: 'uploadProductImage(request)',
      },
      {
        caution:
          'This sample uploads an image first and only deletes it if the response includes an imgid field.',
        description:
          'Deletes one uploaded image by numeric image id. In a real app, use the id returned by the upload response.',
        environment: 'Staging API',
        id: 'deleteProductImage',
        run: async () => {
          await ensureDemoProduct();
          return runInEnvironment(STAGING_CONFIG, async () => {
            const uploadResponse = await uploadProductImage(
              IMAGE_UPLOAD_REQUEST
            );
            const imageId = parseUploadedImageId(uploadResponse);

            if (imageId == null) {
              throw new Error(
                'uploadProductImage did not return an imgid. Replace imageId with a real uploaded image id before deleting.'
              );
            }

            const deleteResponse = await deleteProductImage({
              code: DEMO_WRITE_BARCODE,
              imageId,
            });

            return {
              deleteResponse: parseMaybeJson(deleteResponse),
              imageId,
              uploadResponse: parseMaybeJson(uploadResponse),
            };
          });
        },
        snippet:
          "const upload = await uploadProductImage(request);\nconst imageId = JSON.parse(upload).imgid;\nawait deleteProductImage({ code: '2000000000001', imageId });",
        title: 'deleteProductImage(request)',
      },
      {
        caution:
          'Product revert needs a real revision number from product history. Revision 1 is only a placeholder in this example.',
        description:
          'Reverts one or more fields to an earlier revision using the dedicated v3 revert endpoint.',
        environment: 'Staging API',
        id: 'revertProduct',
        run: () =>
          runInEnvironment(STAGING_CONFIG, async () =>
            revertProduct(REVERT_REQUEST)
          ),
        snippet: `await revertProduct(${JSON.stringify(REVERT_REQUEST, null, 2)});`,
        title: 'revertProduct(request)',
      },
    ],
  },
];

function App() {
  const isDarkMode = useColorScheme() === 'dark';
  const [results, setResults] = useState<Record<string, ExampleResult>>({});
  const [activeId, setActiveId] = useState<string | null>(null);

  const colors = isDarkMode
    ? {
        accent: '#6BAA75',
        background: '#0D1B1E',
        border: '#214046',
        card: '#13282D',
        code: '#0F2024',
        muted: '#A8C2B4',
        primary: '#F3F0E5',
        secondary: '#DBE9DD',
      }
    : {
        accent: '#2F6E49',
        background: '#F4EFE2',
        border: '#D5C9A6',
        card: '#FFF9EC',
        code: '#F2E7CC',
        muted: '#5D6648',
        primary: '#18240F',
        secondary: '#2D4025',
      };

  const runExample = async (example: ApiExample) => {
    setActiveId(example.id);
    setResults((current) => ({
      ...current,
      [example.id]: { status: 'loading' },
    }));

    try {
      const value = await example.run();
      setResults((current) => ({
        ...current,
        [example.id]: {
          output: formatValue(value),
          status: 'success',
        },
      }));
    } catch (error) {
      setResults((current) => ({
        ...current,
        [example.id]: {
          error: formatError(error),
          status: 'error',
        },
      }));
    } finally {
      setActiveId((current) => (current === example.id ? null : current));
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView
        edges={['top', 'left', 'right']}
        style={[styles.safeArea, { backgroundColor: colors.background }]}
      >
        <StatusBar
          backgroundColor={colors.background}
          barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        />
        <ScrollView
          contentContainerStyle={styles.content}
          style={[styles.scrollView, { backgroundColor: colors.background }]}
        >
          <View
            style={[
              styles.heroCard,
              { backgroundColor: colors.card, borderColor: colors.border },
            ]}
          >
            <Text style={[styles.eyebrow, { color: colors.accent }]}>
              Open Food Facts TurboModule
            </Text>
            <Text style={[styles.heroTitle, { color: colors.primary }]}>
              Full API Explorer
            </Text>
            <Text style={[styles.heroBody, { color: colors.secondary }]}>
              Every exported function from the library is listed below with a
              runnable example, the exact request shape, and the inline result.
            </Text>
            <View style={styles.badgeRow}>
              <View
                style={[
                  styles.badge,
                  { backgroundColor: colors.code, borderColor: colors.border },
                ]}
              >
                <Text style={[styles.badgeText, { color: colors.primary }]}>
                  20 exported calls
                </Text>
              </View>
              <View
                style={[
                  styles.badge,
                  { backgroundColor: colors.code, borderColor: colors.border },
                ]}
              >
                <Text style={[styles.badgeText, { color: colors.primary }]}>
                  Read calls force production
                </Text>
              </View>
              <View
                style={[
                  styles.badge,
                  { backgroundColor: colors.code, borderColor: colors.border },
                ]}
              >
                <Text style={[styles.badgeText, { color: colors.primary }]}>
                  Write calls force staging
                </Text>
              </View>
            </View>
            <Text style={[styles.notice, { color: colors.muted }]}>
              `getConfig()` reflects the last example that ran. The write
              examples are self-contained, but `revertProduct()` still needs a
              real revision number from product history before it can succeed.
            </Text>
          </View>

          {API_SECTIONS.map((section) => (
            <View key={section.title} style={styles.section}>
              <Text style={[styles.sectionTitle, { color: colors.primary }]}>
                {section.title}
              </Text>
              <Text style={[styles.sectionDescription, { color: colors.secondary }]}>
                {section.description}
              </Text>

              {section.examples.map((example) => {
                const result = results[example.id];
                const isRunning = activeId === example.id;
                const statusLabel =
                  result?.status === 'error'
                    ? 'Failed'
                    : result?.status === 'success'
                      ? 'Completed'
                      : isRunning
                        ? 'Running'
                        : 'Ready';

                return (
                  <View
                    key={example.id}
                    style={[
                      styles.card,
                      {
                        backgroundColor: colors.card,
                        borderColor: colors.border,
                      },
                    ]}
                  >
                    <View style={styles.cardHeader}>
                      <View style={styles.cardTitleBlock}>
                        <Text style={[styles.cardTitle, { color: colors.primary }]}>
                          {example.title}
                        </Text>
                        <Text style={[styles.cardBody, { color: colors.secondary }]}>
                          {example.description}
                        </Text>
                      </View>
                      <View
                        style={[
                          styles.environmentPill,
                          {
                            backgroundColor: colors.code,
                            borderColor: colors.border,
                          },
                        ]}
                      >
                        <Text
                          style={[styles.environmentText, { color: colors.primary }]}
                        >
                          {example.environment}
                        </Text>
                      </View>
                    </View>

                    <View
                      style={[
                        styles.codeBlock,
                        { backgroundColor: colors.code, borderColor: colors.border },
                      ]}
                    >
                      <Text
                        selectable
                        style={[styles.codeText, { color: colors.primary }]}
                      >
                        {example.snippet}
                      </Text>
                    </View>

                    {example.caution ? (
                      <Text style={[styles.caution, { color: colors.accent }]}>
                        {example.caution}
                      </Text>
                    ) : null}

                    <View style={styles.actionRow}>
                      <Pressable
                        disabled={isRunning}
                        onPress={() => {
                          void runExample(example);
                        }}
                        style={({ pressed }) => [
                          styles.button,
                          {
                            backgroundColor: isRunning
                              ? colors.border
                              : pressed
                                ? colors.secondary
                                : colors.accent,
                          },
                        ]}
                      >
                        <Text style={styles.buttonText}>
                          {isRunning ? 'Running...' : 'Run Example'}
                        </Text>
                      </Pressable>
                      <Text style={[styles.statusText, { color: colors.muted }]}>
                        {statusLabel}
                      </Text>
                    </View>

                    {result ? (
                      <View
                        style={[
                          styles.resultBox,
                          {
                            backgroundColor: colors.code,
                            borderColor:
                              result.status === 'error'
                                ? colors.accent
                                : colors.border,
                          },
                        ]}
                      >
                        <Text
                          style={[
                            styles.resultLabel,
                            {
                              color:
                                result.status === 'error'
                                  ? colors.accent
                                  : colors.muted,
                            },
                          ]}
                        >
                          {result.status === 'loading'
                            ? 'Running'
                            : result.status === 'error'
                              ? 'Error'
                              : 'Result'}
                        </Text>
                        <Text
                          selectable
                          style={[styles.codeText, { color: colors.primary }]}
                        >
                          {result.status === 'loading'
                            ? 'Executing sample call...'
                            : result.status === 'error'
                              ? result.error
                              : result.output}
                        </Text>
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>
          ))}
        </ScrollView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  actionRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 12,
  },
  badge: {
    borderRadius: 999,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  badgeRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 16,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  button: {
    borderRadius: 12,
    minWidth: 124,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
    textAlign: 'center',
  },
  card: {
    borderRadius: 20,
    borderWidth: 1,
    gap: 12,
    padding: 18,
  },
  cardBody: {
    fontSize: 14,
    lineHeight: 20,
  },
  cardHeader: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: 12,
    justifyContent: 'space-between',
  },
  cardTitle: {
    fontSize: 17,
    fontWeight: '800',
    marginBottom: 6,
  },
  cardTitleBlock: {
    flex: 1,
  },
  caution: {
    fontSize: 13,
    fontWeight: '600',
    lineHeight: 18,
  },
  codeBlock: {
    borderRadius: 16,
    borderWidth: 1,
    padding: 14,
  },
  codeText: {
    fontFamily: CODE_FONT,
    fontSize: 12,
    lineHeight: 18,
  },
  content: {
    gap: 24,
    padding: 20,
    paddingBottom: 36,
  },
  environmentPill: {
    borderRadius: 999,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  environmentText: {
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  eyebrow: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1.1,
    textTransform: 'uppercase',
  },
  heroBody: {
    fontSize: 15,
    lineHeight: 22,
    marginTop: 12,
  },
  heroCard: {
    borderRadius: 24,
    borderWidth: 1,
    padding: 20,
  },
  heroTitle: {
    fontSize: 30,
    fontWeight: '900',
    letterSpacing: -0.8,
    marginTop: 8,
  },
  notice: {
    fontSize: 13,
    lineHeight: 19,
    marginTop: 16,
  },
  resultBox: {
    borderRadius: 16,
    borderWidth: 1,
    gap: 8,
    padding: 14,
  },
  resultLabel: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  safeArea: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  section: {
    gap: 12,
  },
  sectionDescription: {
    fontSize: 14,
    lineHeight: 20,
  },
  sectionTitle: {
    fontSize: 23,
    fontWeight: '900',
    letterSpacing: -0.4,
  },
  statusText: {
    flexShrink: 1,
    fontSize: 13,
    fontWeight: '600',
  },
});

export default App;
