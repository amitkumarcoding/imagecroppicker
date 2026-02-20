import { NativeModules, Platform } from 'react-native';

export type NativeOpenImagePreviewOptions = Record<string, unknown>;

export type NativeOpenImagePreviewResult = {
  path?: string;
  base64?: string;
};

type NativeModuleShape = {
  openImagePreview(
    options: NativeOpenImagePreviewOptions,
  ): Promise<NativeOpenImagePreviewResult>;
};

const LINKING_ERROR =
  `The package "react-native-customizable-image-crop-picker" doesn't seem to be linked. ` +
  Platform.select({
    ios: "Make sure you have run 'pod install'. ",
    default: '',
  }) +
  'Make sure you rebuilt the app after installing the package.';

export const NativeImageCropperModule: NativeModuleShape =
  NativeModules.NativeImageCropperModule ??
  new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    },
  ) as NativeModuleShape;

