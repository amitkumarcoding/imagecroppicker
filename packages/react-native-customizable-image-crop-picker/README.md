# react-native-customizable-image-crop-picker

[![License](https://img.shields.io/npm/l/react-native-customizable-image-crop-picker)](./LICENSE)
[![NPM](https://img.shields.io/badge/npm-package-CB3837?logo=npm&logoColor=white)](https://www.npmjs.com/package/react-native-customizable-image-crop-picker)
[![Downloads](https://img.shields.io/npm/dm/react-native-customizable-image-crop-picker)](https://www.npmjs.com/package/react-native-customizable-image-crop-picker)
![Platform - Android](https://img.shields.io/badge/platform-android-3DDC84?logo=android&logoColor=white)
![Platform - iOS](https://img.shields.io/badge/platform-iOS-000000?logo=apple&logoColor=white)

This library is developed by **Amit Kumar**.

A high performance, beautiful and fully customizable **image crop picker** for React Native.

iOS/Android image picker + cropper with support for **camera**, **gallery**, optional **base64**, and a **highly customizable native full-screen crop UI** (header/footer/buttons/icons/layout) driven by JS props.

- **Android**: system photo picker / camera + **uCrop**
- **iOS**: `UIImagePickerController` / camera + **TOCropViewController**

## Platforms Supported

- [x] iOS
- [x] Android

## Features

- Camera + gallery
- Cropping with configurable aspect ratio (`cropWidth` / `cropHeight`)
- Circular crop (`circularCrop`)
- Crop overlays: dimmed layer + grid (`dimmedLayerColor`, `cropGridEnabled`)
- Native crop controls (rotate/reset + aspect ratio picker) via `showNativeCropControls`
- Output compression controls (`compressQuality`, `compressFormat`)
- Optional base64 output (`includeBase64`)
- Native full-screen crop UI customization from JS (header/footer/buttons/icons/layout)
- Icons: remote (http/https) + local (`file://` / Android `content://`) + base64 + bundled assets (`require(...)`)
- Events: `onCropStart`, `onCropEnd`, `onProgress` (Android base64 progress)

## Roadmap (planned)

- **Rotation + flip parity**: add flip support, finer control over native toolbars
- **Multiple aspect ratio presets**: configurable preset list (1:1, 4:3, 16:9, free) beyond the native defaults
- **Full RN UI mode**: “JS chrome + native crop engine” component so every UI detail is styleable with real RN styles
- **Richer error codes** and permission UX
- **Multiple selection** (`multiple`, `maxFiles`)

## Demo

**Android**

</br></br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/1.png" alt="Android demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/2.png" alt="Android demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/3.png" alt="Android demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/4.png" alt="Android demo" width="320" />
</p>
</br>
</br>

**iOS**

</br>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/5.png" alt="iOS demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/6.png" alt="iOS demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/7.png" alt="iOS demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/8.png" alt="iOS demo" width="320" />
</p>
</br>
<p>
  <img src="https://github.com/amitkumarcoding/imagecroppicker/blob/main/images/9.png" alt="iOS demo" width="320" />
</p>
</br>
</br>


## Important notes

- **Camera permission**
  - **Android**: you must request `CAMERA` permission at runtime (helper included).
  - **iOS**: you must add `NSCameraUsageDescription` to `Info.plist`.
- **iOS Simulator**: camera isn’t available on the simulator. Test camera on a real device.

## Getting started

```bash
npm install react-native-customizable-image-crop-picker --save
```

or

```bash
yarn add react-native-customizable-image-crop-picker
```

### iOS

```bash
cd ios
pod install
cd ..
```

#### Permissions (Info.plist)

Add:

- `NSCameraUsageDescription`
- `NSPhotoLibraryUsageDescription`
- `NSPhotoLibraryAddUsageDescription` (only if saving to library)

#### If CocoaPods fails: TOCropViewController modular headers

If you see this error during `pod install`:

- `The following Swift pods cannot yet be integrated as static libraries...`
- `RNCustomizableImageCropPicker depends upon TOCropViewController, which does not define modules`

Add this to your **app** `ios/Podfile` (inside your app target):

```ruby
pod 'TOCropViewController', :modular_headers => true
```

Then run:

```bash
cd ios
pod install
cd ..
```

#### If you see an rsync error about missing simulator slice (RN 0.84+ prebuilt pods)

```bash
cd ios
RCT_USE_PREBUILT_RNCORE=0 pod install
```

### Android

Autolinking handles it. For camera flows, ensure this exists (manifest merge usually brings it from the library):

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

If you use **remote icons** (`http/https`) for buttons, your app must also have:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### SVG icons (Android native UI)

On **Android**, button icon props like `uploadButtonIconUri` / `cancelButtonIconUri` can also be **SVG**:

- Remote: `https://.../icon.svg`
- Local: `file://.../icon.svg` or `content://.../icon.svg`
- Data URI: `data:image/svg+xml;utf8,<svg ...>`
- Base64: `data:image/svg+xml;base64,PHN2Zy4uLg==`

On **iOS native UI**, SVG icons are not supported (use PNG/JPG/WebP), or use the JS UI (`ImageCropPickerModal`) with `react-native-svg`.

## Usage

Import:

```js
import {
  openImageCropPicker,
} from 'react-native-customizable-image-crop-picker';
```

### Select from gallery

```js
const result = await openImageCropPicker({
  source: 'gallery',
  cropWidth: 1,
  cropHeight: 1,
});

console.log(result.path);
```

### Select from camera

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

## Optional: JS UI chrome (`ImageCropPickerModal`)

This package also exports a **fully customizable React Native modal** (`ImageCropPickerModal`) that you can use as your own UI layer (custom header/footer/buttons). It’s independent from the native crop flow.

## Examples

- [Basic Example](https://github.com/amitkumarcoding/imagecroppicker/blob/main/App.js)
- [Full Example](https://github.com/amitkumarcoding/imagecroppicker/blob/main/packages/react-native-customizable-image-crop-picker/example/App.js)


### Usage

```js
import React, { useState } from 'react';
import { View, Button } from 'react-native';
import { ImageCropPickerModal } from 'react-native-customizable-image-crop-picker';

export default function Screen() {
  const [visible, setVisible] = useState(false);

  return (
    <View style={{ flex: 1 }}>
      <Button title="Open preview UI" onPress={() => setVisible(true)} />

      <ImageCropPickerModal
        visible={visible}
        imageUri="file:///path/to/image.jpg"
        headerTitle="Preview Image"
        cancelText="Cancel"
        uploadText="Upload"
        onCancel={() => setVisible(false)}
        onConfirm={() => setVisible(false)}
      />
    </View>
  );
}
```

### Modal props

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| visible | boolean | Yes | - | Show/hide the modal |
| imageUri | string | No | - | Image to preview |
| onCancel | `() => void` | Yes | - | Cancel callback |
| onConfirm | `() => void` | Yes | - | Confirm callback |
| safeAreaEnabled | boolean | No | `true` | Wrap modal in `SafeAreaView` |
| backgroundColor | string | No | `#000` | Preview background |
| showHeader | boolean | No | `true` | Show header |
| showFooter | boolean | No | `true` | Show footer |
| headerTitle | string | No | `Preview Image` | Header title |
| headerAlignment | `'left' \| 'center' \| 'right'` | No | `'center'` | Title alignment |
| leftIcon | `React.ReactNode` | No | - | Left icon element |
| rightIcon | `React.ReactNode` | No | - | Right icon element |
| showLeftIcon | boolean | No | `headerAlignment === 'center'` | Show/hide left icon |
| showRightIcon | boolean | No | `headerAlignment === 'center'` | Show/hide right icon |
| headerStyle | `{ containerStyle?, titleStyle?, leftIconContainerStyle?, rightIconContainerStyle? }` | No | - | Header styling |
| cancelText | string | No | `Cancel` | Cancel label |
| uploadText | string | No | `Upload` | Upload label |
| cancelIcon | `React.ReactNode` | No | - | Cancel icon element (footer) |
| uploadIcon | `React.ReactNode` | No | - | Upload icon element (footer) |
| footerStyle | `{ containerStyle?, buttonRowStyle?, cancelButtonStyle?, uploadButtonStyle?, cancelTextStyle?, uploadTextStyle?, cancelIconContainerStyle?, uploadIconContainerStyle? }` | No | - | Footer styling |
| overlayStyle | `{ containerStyle?, previewContainerStyle?, imageStyle?, cropOverlayStyle? }` | No | - | Preview + overlay styling |

### Render overrides

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| renderHeader | `(ctx) => React.ReactNode` | No | - | Full custom header |
| renderFooter | `(ctx) => React.ReactNode` | No | - | Full custom footer |
| renderCancelButton | `(ctx) => React.ReactNode` | No | - | Custom cancel button |
| renderUploadButton | `(ctx) => React.ReactNode` | No | - | Custom upload button |

## Props / Options (`openImageCropPicker(options)`)

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

## Platform-specific props / behavior

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

### JS-only props (ignored by `openImageCropPicker()`)

These exist in exported types to support the `ImageCropPickerModal` component. They are currently **not used** by the native `openImageCropPicker()` flow (or its alias `open()`).

| Prop | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| showHeader | boolean | No | - | `ImageCropPickerModal` only |
| headerAlignment | `'left' \| 'center' \| 'right'` | No | - | `ImageCropPickerModal` only |
| leftIcon | `React.ReactNode` | No | - | `ImageCropPickerModal` only |
| rightIcon | `React.ReactNode` | No | - | `ImageCropPickerModal` only |
| showLeftIcon | boolean | No | - | `ImageCropPickerModal` only |
| showRightIcon | boolean | No | - | `ImageCropPickerModal` only |
| showFooter | boolean | No | - | `ImageCropPickerModal` only |
| cancelIcon | `React.ReactNode` | No | - | `ImageCropPickerModal` only |
| uploadIcon | `React.ReactNode` | No | - | `ImageCropPickerModal` only |
| overlayStyle | `CropperOverlayStyle` | No | - | `ImageCropPickerModal` only |
| backgroundColor | string | No | - | `ImageCropPickerModal` only |
| safeAreaEnabled | boolean | No | - | `ImageCropPickerModal` only |

## Response Object

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

## Peer Dependencies

| Package | Version |
| --- | --- |
| react | `>=18` |
| react-native | `>=0.72` |

## Contributing

Contributions are welcome. Please open an issue with reproduction details or submit a PR with a clear description + test plan.

## License

MIT