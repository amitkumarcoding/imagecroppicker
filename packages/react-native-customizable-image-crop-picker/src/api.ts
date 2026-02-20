import { normalizeNativeError } from './errors';
import type { CropResult, OpenOptions } from './types';
import { NativeImageCropperModule } from './native/NativeImageCropperModule';
import { mapOpenOptionsToNative } from './native/mapOptions';
import { NativeEventEmitter, Platform } from 'react-native';

export async function openImageCropPicker(options: OpenOptions): Promise<CropResult> {
  options.onCropStart?.();
  options.onProgress?.(0);
  const emitter =
    Platform.OS === 'android' ? new NativeEventEmitter(NativeImageCropperModule as any) : null;
  const sub =
    options.onProgress && emitter
      ? emitter.addListener('NativeImageCropperProgress', (evt: any) => {
          const p = typeof evt?.progress === 'number' ? evt.progress : undefined;
          if (typeof p === 'number') options.onProgress?.(Math.max(0, Math.min(1, p)));
        })
      : null;
  try {
    const nativeOptions = mapOpenOptionsToNative(options);
    const result = await NativeImageCropperModule.openImagePreview(nativeOptions);
    const path = result.path ?? '';
    if (!path) {
      throw new Error('Native cropper returned empty path');
    }
    const payload = { path, base64: options.includeBase64 ? result.base64 : undefined };
    options.onProgress?.(1);
    options.onCropEnd?.(payload, undefined);
    return payload;
  } catch (e) {
    const err = normalizeNativeError(e);
    options.onCropEnd?.(undefined, err);
    throw err;
  } finally {
    sub?.remove();
  }
}

// Backwards compatible alias.
export const open = openImageCropPicker;

