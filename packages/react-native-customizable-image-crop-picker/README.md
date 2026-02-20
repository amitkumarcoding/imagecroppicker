# react-native-customizable-image-crop-picker

[![License](https://img.shields.io/npm/l/react-native-customizable-image-crop-picker)](./LICENSE)
[![NPM](https://img.shields.io/badge/npm-package-CB3837?logo=npm&logoColor=white)](https://www.npmjs.com/package/react-native-customizable-image-crop-picker)
[![Downloads](https://img.shields.io/npm/dm/react-native-customizable-image-crop-picker)](https://www.npmjs.com/package/react-native-customizable-image-crop-picker)
![Platform - Android](https://img.shields.io/badge/platform-android-3DDC84?logo=android&logoColor=white)
![Platform - iOS](https://img.shields.io/badge/platform-iOS-000000?logo=apple&logoColor=white)

A high-performance, fully customizable **image crop picker** for React Native (iOS + Android).

- **Android**: system photo picker / camera + **uCrop**
- **iOS**: `UIImagePickerController` / camera + **TOCropViewController**

This library is developed and maintained by **Amit Kumar**.

## Table of contents

- [Features](#features)
- [Roadmap / future features](#roadmap--future-features)
- [Demo](#demo)
- [Installation](#installation)
- [Permissions](#permissions)
- [Quick start](#quick-start)
- [Icons (including SVG on Android)](#icons-including-svg-on-android)
- [API: `openImageCropPicker(options)`](#api-openimagecroppickeroptions)
- [Response object](#response-object)
- [Errors](#errors)
- [Known limitations](#known-limitations)
- [Peer dependencies](#peer-dependencies)
- [Contributing](#contributing)
- [Support](#support)
- [License](#license)

## Features

- Camera + gallery
- Cropping with configurable aspect ratio (`cropWidth` / `cropHeight` or `aspectRatio`)
- Circular crop (`circularCrop`)
- Crop overlays: dimmed layer + grid (`dimmedLayerColor`, `cropGridEnabled`)
- Native crop controls (rotate/reset + aspect ratio picker) via `showNativeCropControls`
- Output compression controls (`compressQuality`, `compressFormat`)
- Optional base64 output (`includeBase64`)
- Native full-screen crop UI customization from JS (header/footer/buttons/icons/layout)
- Icons: remote (http/https) + local (`file://` / Android `content://`) + base64 + bundled assets (`require(...)`)
- Events: `onCropStart`, `onCropEnd`, `onProgress` (Android base64 progress)

## Roadmap / future features

- **Multiple selection**: `multiple`, `maxFiles`
- **Flip support** (to complement rotation)
- **More aspect ratio presets** (configurable preset list)
- **Richer errors + permission UX** (more granular error codes and clearer guidance)

## Demo

<details>
  <summary>Android screenshots</summary>
  <br />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/1.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/2.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/3.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/4.png" width="240" />
</details>

<details>
  <summary>iOS screenshots</summary>
  <br />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/5.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/6.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/7.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/8.png" width="240" />
  <img src="https://raw.githubusercontent.com/amitkumarcoding/imagecroppicker/main/images/9.png" width="240" />
</details>

## Installation

```bash
npm install react-native-customizable-image-crop-picker
```

or

```bash
yarn add react-native-customizable-image-crop-picker
```

### iOS

```bash
cd ios && pod install && cd ..
```

#### If CocoaPods fails: TOCropViewController modular headers

If `pod install` fails with a Swift modular headers error (TOCropViewController), add this to your **app** `ios/Podfile` (inside your app target):

```ruby
pod 'TOCropViewController', :modular_headers => true
```

Then run `pod install` again.

#### If you see an rsync error about missing simulator slice (RN 0.84+ prebuilt pods)

```bash
cd ios
RCT_USE_PREBUILT_RNCORE=0 pod install
cd ..
```

## Permissions

### iOS (`Info.plist`)

Add:

- `NSCameraUsageDescription`
- `NSPhotoLibraryUsageDescription`
- `NSPhotoLibraryAddUsageDescription` (only if saving to library)

### Android

- Request `CAMERA` permission at runtime before opening `source: 'camera'`.
- Ensure the following is present (usually merged from the library):

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

If you use **remote icons** (`http/https`) for buttons:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Notes

- **iOS Simulator**: camera isn’t available on the simulator. Test camera on a real device.

## Quick start

```js
import { openImageCropPicker } from 'react-native-customizable-image-crop-picker';

const result = await openImageCropPicker({
  source: 'gallery',
  cropWidth: 1,
  cropHeight: 1,
});

console.log(result.path);
```

### Camera

```js
const result = await openImageCropPicker({
  source: 'camera',
  cropWidth: 1,
  cropHeight: 1,
});
```

### Callbacks / progress (base64)

```js
const result = await openImageCropPicker({
  source: 'gallery',
  includeBase64: true,
  onCropStart: () => console.log('crop started'),
  onProgress: (p) => console.log('progress', p), // Android: real base64 progress
  onCropEnd: (res, err) => console.log('crop end', { res, err }),
});
```

### Examples (recommended)

- [Basic Example](https://github.com/amitkumarcoding/imagecroppicker/blob/main/App.js)
- [Full Example](https://github.com/amitkumarcoding/imagecroppicker/blob/main/packages/react-native-customizable-image-crop-picker/example/App.js)

## Icons (including SVG on Android)

Button icon props like `uploadButtonIconUri` / `cancelButtonIconUri` accept:

- Remote URI: `https://...`
- Local file: `file://...`
- Android content URI: `content://...`
- Bundled asset: `require('./icon.png')` (recommended)

### SVG

- **Android native UI** supports SVG for these icon URIs.
- **iOS native UI** does **not** support SVG (use PNG/JPG/WebP).

## API: `openImageCropPicker(options)`

> Backwards compatible alias: `open(options)`

Defaults shown below are the current native defaults.

### Picker options

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| source | `'camera' \| 'gallery'` | Yes | - | Open camera or gallery |
| multiple | boolean | No | `false` | Pick multiple images (**planned**, not implemented yet) |
| maxFiles | number | No | - | Max items when `multiple: true` (**planned**) |
| mediaType | `'photo'` | No | `'photo'` | Media type (photos only) |

### Crop options

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| cropWidth | number | No | `100` | Aspect ratio width (native crop) |
| cropHeight | number | No | `100` | Aspect ratio height (native crop) |
| aspectRatio | `{ width: number; height: number }` | No | - | Alternative to `cropWidth/cropHeight` |
| freeStyleCropEnabled | boolean | No | `false` | If `true`, user can resize the crop box (free-style) |
| circularCrop | boolean | No | `false` | Circular crop (iOS/Android) |
| rotationEnabled | boolean | No | `false` | Enables rotation controls when `showNativeCropControls: true` |
| cropGridEnabled | boolean | No | `false` | Show crop grid (iOS/Android) |
| cropOverlayColor | string | No | - | Alias for `dimmedLayerColor` |
| cropFrameColor | string | No | - | Android: crop frame color |
| cropGridColor | string | No | - | Android: crop grid color. iOS: used for **circular** grid overlay |
| compressQuality | number | No | `1` | 0..1 (output encoding quality) |
| compressFormat | `'jpeg' \| 'png' \| 'webp'` | No | `'jpeg'` | Output encoding format (iOS WebP is best-effort; falls back to JPEG if unavailable) |
| includeBase64 | boolean | No | `false` | If `true`, resolves base64 (slower) |

### Native full-screen crop UI (header/footer/buttons)

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| preferNativeUI | boolean | No | `true` | Prefer native full-screen UI (current implementation uses native UI) |
| headerTitle | string | No | `Preview Image` | Native header title |
| headerAlignment | `'left' \| 'center' \| 'right'` | No | `'center'` | Header title alignment |
| headerHeight | number | No | `84` | Header height |
| headerPaddingHorizontal | number | No | `20` | Header padding |
| headerPaddingTop | number | No | `20` | Header padding |
| headerPaddingBottom | number | No | `20` | Header padding |
| controlsPlacement | `'bottom' \| 'top'` | No | `'bottom'` | Render actions in footer or header |
| topLeftControl | `'cancel' \| 'upload' \| 'none'` | No | `'cancel'` | Left header control when `controlsPlacement: 'top'` |
| topRightControl | `'cancel' \| 'upload' \| 'none'` | No | `'upload'` | Right header control when `controlsPlacement: 'top'` |
| cancelText | string | No | `Cancel` | Cancel label |
| uploadText | string | No | `Upload` | Upload label |
| footerPaddingHorizontal | number | No | `20` | Footer padding |
| footerPaddingTop | number | No | `16` | Footer padding |
| footerPaddingBottom | number | No | `24` | Footer padding |
| footerButtonGap | number | No | `12` | Gap between footer buttons |
| footerButtonHeight | number | No | `54` | Button height |
| footerButtonLayout | `'vertical' \| 'horizontal'` | No | `'vertical'` | Footer layout |
| footerButtonOrder | `'uploadFirst' \| 'cancelFirst'` | No | `'uploadFirst'` | Footer order |
| cancelButtonRadius | number | No | `28` | Cancel button radius |
| uploadButtonRadius | number | No | `28` | Upload button radius |

### Button content + icons

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| uploadButtonContent | `'text' \| 'icon' \| 'iconText' \| 'textIcon' \| 'TextIcon' \| 'icon+text' \| 'text+icon'` | No | `'text'` | Upload content mode |
| cancelButtonContent | same as above | No | `'text'` | Cancel content mode |
| uploadButtonIconUri | `string \| ImageSourcePropType` | No | `''` | Icon URI (http/https/file/content) or bundled asset `require(...)` |
| cancelButtonIconUri | `string \| ImageSourcePropType` | No | `''` | Icon URI (http/https/file/content) or bundled asset `require(...)` |
| uploadButtonIconBase64 | string | No | `''` | Icon base64 (optionally `data:image/...;base64,`) |
| cancelButtonIconBase64 | string | No | `''` | Icon base64 |
| uploadButtonIconSize | number | No | `18` | Icon size |
| cancelButtonIconSize | number | No | `18` | Icon size |
| buttonIconGap | number | No | `8` | Gap between icon and text |
| uploadButtonIconTintColor | string | No | `''` | Tint (only applied if provided) |
| cancelButtonIconTintColor | string | No | `''` | Tint (only applied if provided) |
| buttonContentPaddingHorizontal | number | No | `12` | Internal button padding (both buttons) |
| buttonContentPaddingVertical | number | No | `0` | Internal button padding (both buttons) |
| uploadButtonContentPaddingHorizontal | number | No | - | Upload internal padding override |
| uploadButtonContentPaddingVertical | number | No | - | Upload internal padding override |
| cancelButtonContentPaddingHorizontal | number | No | - | Cancel internal padding override |
| cancelButtonContentPaddingVertical | number | No | - | Cancel internal padding override |

#### Using bundled images (`require(...)`)

`uploadButtonIconUri` / `cancelButtonIconUri` accept:

- a **string URI** (`https://`, `file://`, etc.)
- or a **bundled asset** via `require(...)` (recommended)

```js
import { openImageCropPicker } from 'react-native-customizable-image-crop-picker';

await openImageCropPicker({
  source: 'gallery',
  uploadButtonContent: 'icon',
  uploadButtonIconUri: require('./upload.jpg'),
});
```

### Theme / status bar / overlay

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| isDarkTheme | boolean | No | `false` | Native theme hint |
| statusBarColor | string | No | `#FFFFFF` (light), `#000000` (dark) | Status bar color |
| statusBarStyle | `'dark' \| 'light'` | No | - | Android only: status bar icon/text color |
| dimmedLayerColor | string | No | `#B3000000` (light), `#E0000000` (dark) | Dimmed overlay around crop box |
| showNativeCropControls | boolean | No | `false` | Show native crop controls (rotation/aspect ratio toolbar) |

#### `showNativeCropControls` + `controlsPlacement`

When `showNativeCropControls: true`, the native crop toolbar is visible.

- To avoid the footer covering the native toolbar, `showNativeCropControls` works **only with** `controlsPlacement: 'top'`.
- If you pass `controlsPlacement: 'bottom'`, the library will **auto-force it to `'top'`**.

### Platform-specific props / behavior

- **Android only**
  - **`drawUnderStatusBar`**: edge-to-edge header under the status bar.
  - **`statusBarStyle`**: controls status bar icons/text (`'dark' | 'light'`).
  - **`cropFrameColor`**: crop frame color.
  - **`cropGridColor`**: crop grid line color (rectangle mode).
  - **SVG button icons**: `uploadButtonIconUri` / `cancelButtonIconUri` support SVG on Android native UI.

- **iOS only**
  - **`compressFormat: 'webp'`**: best-effort (falls back to JPEG if the encoder/type is unavailable).

- **Both (with small differences)**
  - **`cropGridEnabled`**: rectangle uses native grid on iOS; circular uses a custom grid overlay.
  - **`showNativeCropControls`**: requires header controls (`controlsPlacement: 'top'`), library auto-forces when enabled.

### Style objects (partial support)

These are read from React Native styles and mapped into native values (subset only).

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| headerStyle | `{ containerStyle?, titleStyle? }` | No | - | Supports `containerStyle.backgroundColor`, `titleStyle.color/fontSize/fontFamily` |
| footerStyle | `{ containerStyle?, cancelButtonStyle?, uploadButtonStyle?, cancelTextStyle?, uploadTextStyle? }` | No | - | Supports background/border/textColor/fontSize/fontFamily/borderRadius subset |

## Response object

| Property | Type | Description |
| --- | --- | --- |
| path | string | Cropped image URI/path |
| width | number | Image width (if available) |
| height | number | Image height (if available) |
| mime | string | MIME type (if available) |
| size | number | Size in bytes (if available) |
| exif | object | EXIF metadata (if available) |
| base64 | string | Only when `includeBase64: true` |

## Errors

The library throws `CropPickerError` with a `code`:

- `E_PICKER_CANCELLED`
- `E_PERMISSION_MISSING`
- `E_NO_IMAGE_DATA_FOUND`
- `E_NO_APP_AVAILABLE`
- `E_PICKER_ERROR`
- `E_ACTIVITY_DOES_NOT_EXIST`
- `E_MODULE_DESTROYED`
- `E_UNAVAILABLE`
- `E_INVALID_OPTIONS`

## Known limitations

- Multiple selection options exist in types but **native multiple picking is not implemented yet**.
- iOS grid/frame color customization is limited (rectangle grid uses the native overlay; circular grid uses a custom overlay).

## Peer dependencies

| Package | Version |
| --- | --- |
| react | `>=18` |
| react-native | `>=0.72` |

## Contributing

Contributions are welcome. Please open an issue with reproduction details or submit a PR with a clear description + test plan.

## Support

- **Bug reports / feature requests**: open an issue with a minimal repro (device/OS, RN version, and logs).
- **Questions / help**: start a GitHub discussion or issue (whichever you prefer) with your use-case and desired UI.

## License

MIT