export type CropPickerErrorCode =
  | 'E_ACTIVITY_DOES_NOT_EXIST'
  | 'E_PICKER_CANCELLED'
  | 'E_NO_IMAGE_DATA_FOUND'
  | 'E_MODULE_DESTROYED'
  | 'E_PICKER_ERROR'
  | 'E_PERMISSION_MISSING'
  | 'E_NO_APP_AVAILABLE'
  | 'E_UNAVAILABLE'
  | 'E_INVALID_OPTIONS';

export class CropPickerError extends Error {
  code: CropPickerErrorCode;
  details?: unknown;

  constructor(code: CropPickerErrorCode, message: string, details?: unknown) {
    super(message);
    this.name = 'CropPickerError';
    this.code = code;
    this.details = details;
  }
}

export function normalizeNativeError(error: unknown): CropPickerError {
  if (error instanceof CropPickerError) return error;

  const anyErr = error as any;
  const code =
    (typeof anyErr?.code === 'string' ? anyErr.code : undefined) ??
    (typeof anyErr?.nativeErrorCode === 'string' ? anyErr.nativeErrorCode : undefined) ??
    'E_PICKER_ERROR';

  const message =
    (typeof anyErr?.message === 'string' && anyErr.message) ||
    'Image picker failed';

  return new CropPickerError(code as CropPickerErrorCode, message, error);
}

