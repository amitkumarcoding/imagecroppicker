<p align="center">
  <img src="./packages/react-native-customizable-image-crop-picker/assets/logo.png" alt="react-native-customizable-image-crop-picker" width="140" />
</p>

# Customizable Image Crop Picker (Monorepo)

This repo contains:

- **Library (publish to NPM)**: `packages/react-native-customizable-image-crop-picker/`
- **Example app (to test the library)**: repo root (`android/`, `ios/`)

The example app UI lives in `example/App.js` and is re-exported by root `App.js`.

## Important notes

- **Android camera**: add camera permission + request runtime permission before opening camera.
- **iOS camera**: add `NSCameraUsageDescription` to `Info.plist`.
- **iOS Simulator**: camera is not available; test camera on a real device.

## Demo

**Android / iOS** demo screenshots are in the library package README:

- `packages/react-native-customizable-image-crop-picker/README.md`

## Run the example app

### Step 1: Start Metro

```sh
npm start
```

### Step 2: Android

```sh
npm run android
```

### Step 3: iOS

Install Ruby gems + Pods (first time or after native dependency changes):

```sh
bundle install
cd ios && bundle exec pod install && cd ..
```

Run:

```sh
npm run ios
```

## Library docs

See the package README:

- `packages/react-native-customizable-image-crop-picker/README.md`

## Publishing to NPM (maintainers)

```sh
cd packages/react-native-customizable-image-crop-picker
npm run build
npm pack --dry-run
npm publish --access public
```

## iOS troubleshooting (common)

### TOCropViewController modular headers

If CocoaPods reports a Swift modular headers error, add this to the app `ios/Podfile`:

```ruby
pod 'TOCropViewController', :modular_headers => true
```

### RN 0.84+ prebuilt RNCore simulator rsync error

If you see an rsync error about missing `ios-arm64_x86_64-simulator`, run:

```sh
cd ios
RCT_USE_PREBUILT_RNCORE=0 bundle exec pod install
cd ..
```
