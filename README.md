# react-native-openfoodfacts

`react-native-openfoodfacts` is now scaffolded as a New Architecture React Native library, built as a TurboModule for Open Food Facts mobile integrations. Phase 1 focuses on project structure, codegen wiring, Android/iOS native registration, and a minimal example app that validates native linking with a placeholder method.

## Phase 1 Status

This repository now includes:

- A publishable library scaffold with TypeScript entrypoints
- A TurboModule contract defined in `src/NativeOpenFoodFacts.ts`
- Android and iOS native implementations returning runtime metadata
- An `example/` consumer app for smoke testing the package

The current public API is intentionally small:

```ts
getRuntimeInfo(): Promise<RuntimeInfo>
```

This is a handshake method used to confirm that the package is linked and running through the React Native New Architecture.

## What Is Still Legacy

The existing sample files under `API/`, `screens/`, and `assets/` remain in the repository as legacy reference material. They are not part of the package entrypoint and are not considered the source of truth for the new library.

## Local Bootstrap

Install dependencies at the repo root:

```bash
npm install
```

Install example dependencies:

```bash
npm install --prefix example
```

Run the smoke test app:

```bash
npm run example
npm run example:android
npm run example:ios
```

Run basic library checks:

```bash
npm run typecheck
npm run lint
npm run test
```

## Project Layout

```text
.
|-- android/
|-- ios/
|-- src/
|-- example/
|-- API/
|-- screens/
`-- assets/
```

## Architecture

- React Native New Architecture only
- TurboModule first
- No Fabric UI component in phase 1
- No legacy bridge fallback

## Phase Boundaries

- Phase 1: scaffolding, native registration, codegen, smoke test example
- Phase 2: first real Open Food Facts read API, likely product lookup by barcode
- Later phases: authenticated writes, uploads, image workflows, and richer domain APIs

## Reference Links

- [Open Food Facts API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/)
- [Open Food Facts JavaScript wiki](https://wiki.openfoodfacts.org/API/Javascript)

## License

Apache 2.0. See [LICENSE](./LICENSE).
