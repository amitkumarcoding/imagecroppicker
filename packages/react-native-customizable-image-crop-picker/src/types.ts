import type * as React from 'react';
import type {
  ImageSourcePropType,
  ImageStyle,
  StyleProp,
  TextStyle,
  ViewStyle,
} from 'react-native';

export type PickerSource = 'camera' | 'gallery';
export type HeaderAlignment = 'left' | 'center' | 'right';

export type CropCompressionFormat = 'jpeg' | 'png' | 'webp';

export type CropResult = {
  path: string;
  width?: number;
  height?: number;
  mime?: string;
  size?: number;
  base64?: string;
  exif?: Record<string, unknown>;
};

export type PickedImage = {
  path: string;
  width?: number;
  height?: number;
  mime?: string;
  size?: number;
};

export type IconElement = React.ReactNode;

export type HeaderStyle = {
  containerStyle?: StyleProp<ViewStyle>;
  titleStyle?: StyleProp<TextStyle>;
  leftIconContainerStyle?: StyleProp<ViewStyle>;
  rightIconContainerStyle?: StyleProp<ViewStyle>;
};

export type FooterStyle = {
  containerStyle?: StyleProp<ViewStyle>;
  buttonRowStyle?: StyleProp<ViewStyle>;
  cancelButtonStyle?: StyleProp<ViewStyle>;
  uploadButtonStyle?: StyleProp<ViewStyle>;
  cancelTextStyle?: StyleProp<TextStyle>;
  uploadTextStyle?: StyleProp<TextStyle>;
  cancelIconContainerStyle?: StyleProp<ViewStyle>;
  uploadIconContainerStyle?: StyleProp<ViewStyle>;
};

export type CropperOverlayStyle = {
  containerStyle?: StyleProp<ViewStyle>;
  previewContainerStyle?: StyleProp<ViewStyle>;
  imageStyle?: StyleProp<ImageStyle>;
  cropOverlayStyle?: StyleProp<ViewStyle>;
};

export type CropperUIProps = {
  showHeader?: boolean;
  headerTitle?: string;
  headerAlignment?: HeaderAlignment;
  leftIcon?: IconElement;
  rightIcon?: IconElement;
  showLeftIcon?: boolean;
  showRightIcon?: boolean;
  headerStyle?: HeaderStyle;
  /**
   * Native-only layout knobs (used by the native full-screen crop UI).
   * For fully custom React components, use `ImageCropPickerModal`.
   */
  headerHeight?: number;
  headerPaddingHorizontal?: number;
  headerPaddingTop?: number;
  headerPaddingBottom?: number;
  /**
   * Android native UI only:
   * - false (default): header starts below the status bar (not "stuck" under it)
   * - true: draw header under status bar (edge-to-edge)
   */
  drawUnderStatusBar?: boolean;

  showFooter?: boolean;
  cancelText?: string;
  uploadText?: string;
  cancelIcon?: IconElement;
  uploadIcon?: IconElement;
  footerStyle?: FooterStyle;
  footerPaddingHorizontal?: number;
  footerPaddingTop?: number;
  footerPaddingBottom?: number;
  footerButtonGap?: number;
  footerButtonHeight?: number;
  /**
   * Native full-screen footer button layout.
   * - vertical: Upload on top, Cancel below
   * - horizontal: Cancel left, Upload right
   */
  footerButtonLayout?: 'vertical' | 'horizontal';
  /**
   * Native full-screen controls placement.
   * - bottom: render actions in the footer (default)
   * - top: render actions in the header (title stays centered)
   */
  controlsPlacement?: 'bottom' | 'top';
  /**
   * Only used when `controlsPlacement: 'top'`.
   * Decide which control appears on the left/right side.
   */
  topLeftControl?: 'cancel' | 'upload' | 'none';
  topRightControl?: 'cancel' | 'upload' | 'none';
  /**
   * Only used when `controlsPlacement: 'bottom'`.
   * Controls ordering within the footer.
   * - uploadFirst: Upload then Cancel (default)
   * - cancelFirst: Cancel then Upload
   */
  footerButtonOrder?: 'uploadFirst' | 'cancelFirst';

  /**
   * Native full-screen: control content mode per button.
   * - text: label only
   * - icon: icon only
   * - iconText | icon+text: icon then label
   * - textIcon | TextIcon | text+icon: label then icon
   */
  cancelButtonContent?:
    | 'text'
    | 'icon'
    | 'iconText'
    | 'textIcon'
    | 'TextIcon'
    | 'icon+text'
    | 'text+icon';
  uploadButtonContent?:
    | 'text'
    | 'icon'
    | 'iconText'
    | 'textIcon'
    | 'TextIcon'
    | 'icon+text'
    | 'text+icon';
  cancelButtonIconUri?: string | ImageSourcePropType;
  uploadButtonIconUri?: string | ImageSourcePropType;
  cancelButtonIconBase64?: string;
  uploadButtonIconBase64?: string;
  cancelButtonIconTintColor?: string;
  uploadButtonIconTintColor?: string;
  cancelButtonIconSize?: number;
  uploadButtonIconSize?: number;
  buttonIconGap?: number;

  /**
   * Native full-screen: internal content padding for the action buttons.
   * These apply to both buttons unless you override per-button.
   */
  buttonContentPaddingHorizontal?: number;
  buttonContentPaddingVertical?: number;
  cancelButtonContentPaddingHorizontal?: number;
  cancelButtonContentPaddingVertical?: number;
  uploadButtonContentPaddingHorizontal?: number;
  uploadButtonContentPaddingVertical?: number;
  cancelButtonRadius?: number;
  uploadButtonRadius?: number;

  overlayStyle?: CropperOverlayStyle;
  backgroundColor?: string;
  safeAreaEnabled?: boolean;

  /**
   * Native full-screen UI appearance.
   */
  isDarkTheme?: boolean;
  statusBarColor?: string;
  /**
   * Android native UI only: status bar icon/text color.
   * - dark: dark icons (for light backgrounds)
   * - light: light icons (for dark backgrounds)
   */
  statusBarStyle?: 'dark' | 'light';
  /**
   * Native dimmed overlay color around the crop box (Android uses this directly).
   * Example: "#CC0B1B3A"
   */
  dimmedLayerColor?: string;

  /**
   * Show native crop controls (rotation/aspect ratio toolbar).
   * - Android: shows uCrop bottom controls and forces actions into the header.
   * - iOS: shows TOCropViewController toolbar controls.
   */
  showNativeCropControls?: boolean;
};

export type CropOptions = {
  cropWidth?: number;
  cropHeight?: number;
  aspectRatio?: { width: number; height: number };
  freeStyleCropEnabled?: boolean;
  circularCrop?: boolean;
  rotationEnabled?: boolean;
  cropGridEnabled?: boolean;
  cropOverlayColor?: string;
  cropFrameColor?: string;
  cropGridColor?: string;

  compressQuality?: number; // 0..1
  compressFormat?: CropCompressionFormat;
  includeBase64?: boolean;
};

export type PickerOptions = {
  source: PickerSource;
  multiple?: boolean;
  maxFiles?: number;
  mediaType?: 'photo';
};

export type OpenOptions = PickerOptions &
  CropOptions &
  CropperUIProps & {
    /**
     * If true, use the native full-screen UI flow (if your native layer supports it).
     * If false, you can render JS chrome and use a native cropper *view* (future step).
     */
    preferNativeUI?: boolean;

    /**
     * JS callbacks (not passed to native).
     */
    onCropStart?: () => void;
    onCropEnd?: (result?: CropResult, error?: unknown) => void;
    /**
     * Progress for slow work (currently best-effort; Android emits real base64 progress).
     * 0..1
     */
    onProgress?: (progress: number) => void;
  };

export type CropperRenderContext = {
  title: string;
  headerAlignment: HeaderAlignment;
  onCancel: () => void;
  onConfirm: () => void;
};

export type CropperRenderHeader = (ctx: CropperRenderContext) => React.ReactNode;
export type CropperRenderFooter = (ctx: CropperRenderContext) => React.ReactNode;
export type CropperRenderButton = (ctx: CropperRenderContext) => React.ReactNode;

export type CropperRenderOverrides = {
  renderHeader?: CropperRenderHeader;
  renderFooter?: CropperRenderFooter;
  renderCancelButton?: CropperRenderButton;
  renderUploadButton?: CropperRenderButton;
};

