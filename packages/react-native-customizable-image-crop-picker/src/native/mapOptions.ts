import type { OpenOptions } from '../types';
import { Image, StyleSheet } from 'react-native';

export function mapOpenOptionsToNative(options: OpenOptions): Record<string, unknown> {
  const cropWidth = options.cropWidth ?? options.aspectRatio?.width;
  const cropHeight = options.cropHeight ?? options.aspectRatio?.height;

  const headerTitle = options.headerTitle ?? 'Preview Image';
  const cancelText = options.cancelText ?? 'Cancel';
  const uploadText = options.uploadText ?? 'Upload';

  const headerStyle = options.headerStyle;
  const footerStyle = options.footerStyle;

  const resolveIconUri = (
    input: unknown,
  ): string | undefined => {
    if (typeof input === 'string') return input;
    if (input == null) return undefined;
    // Accept RN assets (require()) and resolve to a URI string.
    const resolved = Image.resolveAssetSource(input as any);
    return resolved?.uri;
  };

  const headerStylePayload: Record<string, unknown> = {};
  if (headerStyle) {
    headerStylePayload.backgroundColor = (headerStyle.containerStyle as any)?.backgroundColor;
    headerStylePayload.color = (headerStyle.titleStyle as any)?.color;
    headerStylePayload.fontSize = (headerStyle.titleStyle as any)?.fontSize;
    headerStylePayload.fontFamily = (headerStyle.titleStyle as any)?.fontFamily;
  }
  if (options.headerHeight != null) headerStylePayload.height = options.headerHeight;
  if (options.headerPaddingHorizontal != null)
    headerStylePayload.paddingHorizontal = options.headerPaddingHorizontal;
  if (options.headerPaddingTop != null) headerStylePayload.paddingTop = options.headerPaddingTop;
  if (options.headerPaddingBottom != null)
    headerStylePayload.paddingBottom = options.headerPaddingBottom;

  const buttonContainerStylePayload: Record<string, unknown> = {};
  if (footerStyle) {
    const footerContainer = StyleSheet.flatten(footerStyle.containerStyle as any);
    buttonContainerStylePayload.backgroundColor = footerContainer?.backgroundColor;
    // Allow React Native-like spacing props on the native footer container.
    if (typeof footerContainer?.paddingHorizontal === 'number')
      buttonContainerStylePayload.paddingHorizontal = footerContainer.paddingHorizontal;
    if (typeof footerContainer?.paddingTop === 'number')
      buttonContainerStylePayload.paddingTop = footerContainer.paddingTop;
    if (typeof footerContainer?.paddingBottom === 'number')
      buttonContainerStylePayload.paddingBottom = footerContainer.paddingBottom;
    if (typeof footerContainer?.gap === 'number') buttonContainerStylePayload.gap = footerContainer.gap;
  }
  if (options.footerPaddingHorizontal != null)
    buttonContainerStylePayload.paddingHorizontal = options.footerPaddingHorizontal;
  if (options.footerPaddingTop != null)
    buttonContainerStylePayload.paddingTop = options.footerPaddingTop;
  if (options.footerPaddingBottom != null)
    buttonContainerStylePayload.paddingBottom = options.footerPaddingBottom;
  if (options.footerButtonGap != null) buttonContainerStylePayload.gap = options.footerButtonGap;
  if (options.footerButtonHeight != null)
    buttonContainerStylePayload.buttonHeight = options.footerButtonHeight;
  if (options.footerButtonLayout != null)
    buttonContainerStylePayload.layout = options.footerButtonLayout;

  const cancelButtonStylePayload: Record<string, unknown> = {};
  if (footerStyle) {
    cancelButtonStylePayload.backgroundColor = (footerStyle.cancelButtonStyle as any)?.backgroundColor;
    cancelButtonStylePayload.borderColor = (footerStyle.cancelButtonStyle as any)?.borderColor;
    cancelButtonStylePayload.borderWidth = (footerStyle.cancelButtonStyle as any)?.borderWidth;
    cancelButtonStylePayload.fontFamily = (footerStyle.cancelTextStyle as any)?.fontFamily;
    cancelButtonStylePayload.fontSize = (footerStyle.cancelTextStyle as any)?.fontSize;
    cancelButtonStylePayload.textColor = (footerStyle.cancelTextStyle as any)?.color;
    cancelButtonStylePayload.borderRadius = (footerStyle.cancelButtonStyle as any)?.borderRadius;
  }
  if (options.cancelButtonRadius != null && cancelButtonStylePayload.borderRadius == null) {
    cancelButtonStylePayload.borderRadius = options.cancelButtonRadius;
  }
  if (options.cancelButtonContent != null) cancelButtonStylePayload.content = options.cancelButtonContent;
  const cancelIconUri = resolveIconUri(options.cancelButtonIconUri);
  if (cancelIconUri != null) cancelButtonStylePayload.iconUri = cancelIconUri;
  if (options.cancelButtonIconBase64 != null) cancelButtonStylePayload.iconBase64 = options.cancelButtonIconBase64;
  if (options.cancelButtonIconTintColor != null)
    cancelButtonStylePayload.iconTintColor = options.cancelButtonIconTintColor;
  if (options.cancelButtonIconSize != null) cancelButtonStylePayload.iconSize = options.cancelButtonIconSize;
  if (options.buttonIconGap != null) cancelButtonStylePayload.iconGap = options.buttonIconGap;
  const cancelPadH =
    options.cancelButtonContentPaddingHorizontal ?? options.buttonContentPaddingHorizontal;
  const cancelPadV =
    options.cancelButtonContentPaddingVertical ?? options.buttonContentPaddingVertical;
  if (cancelPadH != null) cancelButtonStylePayload.paddingHorizontal = cancelPadH;
  if (cancelPadV != null) cancelButtonStylePayload.paddingVertical = cancelPadV;

  const uploadButtonStylePayload: Record<string, unknown> = {};
  if (footerStyle) {
    uploadButtonStylePayload.backgroundColor = (footerStyle.uploadButtonStyle as any)?.backgroundColor;
    uploadButtonStylePayload.borderColor = (footerStyle.uploadButtonStyle as any)?.borderColor;
    uploadButtonStylePayload.borderWidth = (footerStyle.uploadButtonStyle as any)?.borderWidth;
    uploadButtonStylePayload.fontFamily = (footerStyle.uploadTextStyle as any)?.fontFamily;
    uploadButtonStylePayload.fontSize = (footerStyle.uploadTextStyle as any)?.fontSize;
    uploadButtonStylePayload.textColor = (footerStyle.uploadTextStyle as any)?.color;
    uploadButtonStylePayload.borderRadius = (footerStyle.uploadButtonStyle as any)?.borderRadius;
  }
  if (options.uploadButtonRadius != null && uploadButtonStylePayload.borderRadius == null) {
    uploadButtonStylePayload.borderRadius = options.uploadButtonRadius;
  }
  if (options.uploadButtonContent != null) uploadButtonStylePayload.content = options.uploadButtonContent;
  const uploadIconUri = resolveIconUri(options.uploadButtonIconUri);
  if (uploadIconUri != null) uploadButtonStylePayload.iconUri = uploadIconUri;
  if (options.uploadButtonIconBase64 != null) uploadButtonStylePayload.iconBase64 = options.uploadButtonIconBase64;
  if (options.uploadButtonIconTintColor != null)
    uploadButtonStylePayload.iconTintColor = options.uploadButtonIconTintColor;
  if (options.uploadButtonIconSize != null) uploadButtonStylePayload.iconSize = options.uploadButtonIconSize;
  if (options.buttonIconGap != null) uploadButtonStylePayload.iconGap = options.buttonIconGap;
  const uploadPadH =
    options.uploadButtonContentPaddingHorizontal ?? options.buttonContentPaddingHorizontal;
  const uploadPadV =
    options.uploadButtonContentPaddingVertical ?? options.buttonContentPaddingVertical;
  if (uploadPadH != null) uploadButtonStylePayload.paddingHorizontal = uploadPadH;
  if (uploadPadV != null) uploadButtonStylePayload.paddingVertical = uploadPadV;

  return {
    pickerSource: options.source,
    width: typeof cropWidth === 'number' ? Math.round(cropWidth) : undefined,
    height: typeof cropHeight === 'number' ? Math.round(cropHeight) : undefined,
    freeStyleCropEnabled: options.freeStyleCropEnabled === true,
    includeBase64: options.includeBase64 === true,
    compressQuality:
      typeof options.compressQuality === 'number' ? options.compressQuality : undefined,
    compressFormat: options.compressFormat,
    circularCrop: options.circularCrop === true,
    rotationEnabled: options.rotationEnabled === true,
    cropGridEnabled: options.cropGridEnabled === true,
    cropFrameColor: options.cropFrameColor,
    cropGridColor: options.cropGridColor,
    showNativeCropControls: options.showNativeCropControls === true,
    isDarkTheme: options.isDarkTheme === true,
    drawUnderStatusBar: options.drawUnderStatusBar === true,
    cropperStatusBarColor: options.statusBarColor,
    cropperStatusBarStyle: options.statusBarStyle,
    cropperDimmedLayerColor:
      options.dimmedLayerColor ?? options.cropOverlayColor,

    cropperToolbarTitle: headerTitle,
    cropperCancelText: cancelText,
    cropperChooseText: uploadText,
    headerAlignment: options.headerAlignment,
    controlsPlacement: options.controlsPlacement,
    topLeftControl: options.topLeftControl,
    topRightControl: options.topRightControl,
    footerButtonOrder: options.footerButtonOrder,

    // Best-effort mapping to existing native keys.
    headerStyle: Object.keys(headerStylePayload).length ? headerStylePayload : undefined,
    buttonContainerStyle: Object.keys(buttonContainerStylePayload).length
      ? buttonContainerStylePayload
      : undefined,
    cancelButtonStyle: Object.keys(cancelButtonStylePayload).length
      ? cancelButtonStylePayload
      : undefined,
    uploadButtonStyle: Object.keys(uploadButtonStylePayload).length
      ? uploadButtonStylePayload
      : undefined,
  };
}

