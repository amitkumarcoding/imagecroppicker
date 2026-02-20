import React, { useMemo } from 'react';
import {
  Image,
  Modal,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import type {
  CropperRenderOverrides,
  CropperUIProps,
  HeaderAlignment,
} from '../types';

type Props = CropperUIProps &
  CropperRenderOverrides & {
    visible: boolean;
    imageUri?: string;
    onCancel: () => void;
    onConfirm: () => void;
  };

function resolveAlignment(alignment?: HeaderAlignment): HeaderAlignment {
  if (alignment === 'left' || alignment === 'right' || alignment === 'center') {
    return alignment;
  }
  return 'center';
}

export function ImageCropPickerModal(props: Props) {
  const {
    visible,
    imageUri,
    onCancel,
    onConfirm,
    backgroundColor,
    safeAreaEnabled = true,
  } = props;

  const title = props.headerTitle ?? 'Preview Image';
  const headerAlignment = resolveAlignment(props.headerAlignment);

  const ctx = useMemo(
    () => ({
      title,
      headerAlignment,
      onCancel,
      onConfirm,
    }),
    [title, headerAlignment, onCancel, onConfirm],
  );

  const content = (
    <View style={[styles.root, { backgroundColor: backgroundColor ?? '#000' }]}>
      {props.showHeader !== false &&
        (props.renderHeader ? (
          props.renderHeader(ctx)
        ) : (
          <DefaultHeader {...props} {...ctx} />
        ))}

      <View style={[styles.previewOuter, props.overlayStyle?.previewContainerStyle]}>
        <View style={[styles.previewInner, props.overlayStyle?.containerStyle]}>
          {imageUri ? (
            <Image
              source={{ uri: imageUri }}
              resizeMode="contain"
              style={[styles.image, props.overlayStyle?.imageStyle]}
            />
          ) : (
            <View style={styles.placeholder} />
          )}

          <View
            pointerEvents="none"
            style={[styles.cropOverlay, props.overlayStyle?.cropOverlayStyle]}
          />
        </View>
      </View>

      {props.showFooter !== false &&
        (props.renderFooter ? (
          props.renderFooter(ctx)
        ) : (
          <DefaultFooter {...props} {...ctx} />
        ))}
    </View>
  );

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="fullScreen">
      {safeAreaEnabled ? (
        <SafeAreaView style={styles.safe}>{content}</SafeAreaView>
      ) : (
        content
      )}
    </Modal>
  );
}

function DefaultHeader(
  props: CropperUIProps &
    CropperRenderOverrides & {
      title: string;
      headerAlignment: HeaderAlignment;
      onCancel: () => void;
      onConfirm: () => void;
    },
) {
  const {
    title,
    headerAlignment,
    onCancel,
    onConfirm,
    leftIcon,
    rightIcon,
    showLeftIcon,
    showRightIcon,
    headerStyle,
  } = props;

  const titleAlignStyle =
    headerAlignment === 'left'
      ? styles.headerTitleLeft
      : headerAlignment === 'right'
        ? styles.headerTitleRight
        : styles.headerTitleCenter;

  const canShowLeft = showLeftIcon ?? headerAlignment === 'center';
  const canShowRight = showRightIcon ?? headerAlignment === 'center';

  return (
    <View style={[styles.header, headerStyle?.containerStyle]}>
      <View style={[styles.headerSide, headerStyle?.leftIconContainerStyle]}>
        {canShowLeft ? (
          <Pressable accessibilityRole="button" onPress={onCancel} hitSlop={12}>
            {leftIcon ?? <Text style={styles.defaultIcon}>×</Text>}
          </Pressable>
        ) : null}
      </View>

      <Text
        numberOfLines={1}
        style={[styles.headerTitle, titleAlignStyle, headerStyle?.titleStyle]}
      >
        {title}
      </Text>

      <View style={[styles.headerSide, headerStyle?.rightIconContainerStyle]}>
        {canShowRight ? (
          <Pressable accessibilityRole="button" onPress={onConfirm} hitSlop={12}>
            {rightIcon ?? <Text style={styles.defaultIcon}>✓</Text>}
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}

function DefaultFooter(
  props: CropperUIProps &
    CropperRenderOverrides & {
      onCancel: () => void;
      onConfirm: () => void;
    },
) {
  const {
    onCancel,
    onConfirm,
    cancelText = 'Cancel',
    uploadText = 'Upload',
    cancelIcon,
    uploadIcon,
    footerStyle,
    renderCancelButton,
    renderUploadButton,
  } = props;

  const ctx = {
    title: props.headerTitle ?? 'Preview Image',
    headerAlignment: resolveAlignment(props.headerAlignment),
    onCancel,
    onConfirm,
  };

  return (
    <View style={[styles.footer, footerStyle?.containerStyle]}>
      <View style={[styles.buttonRow, footerStyle?.buttonRowStyle]}>
        {renderCancelButton ? (
          renderCancelButton(ctx)
        ) : (
          <Pressable
            accessibilityRole="button"
            onPress={onCancel}
            style={[styles.cancelButton, footerStyle?.cancelButtonStyle]}
          >
            <View style={[styles.buttonIconWrap, footerStyle?.cancelIconContainerStyle]}>
              {cancelIcon}
            </View>
            <Text style={[styles.cancelText, footerStyle?.cancelTextStyle]}>
              {cancelText}
            </Text>
          </Pressable>
        )}

        {renderUploadButton ? (
          renderUploadButton(ctx)
        ) : (
          <Pressable
            accessibilityRole="button"
            onPress={onConfirm}
            style={[styles.uploadButton, footerStyle?.uploadButtonStyle]}
          >
            <View style={[styles.buttonIconWrap, footerStyle?.uploadIconContainerStyle]}>
              {uploadIcon}
            </View>
            <Text style={[styles.uploadText, footerStyle?.uploadTextStyle]}>
              {uploadText}
            </Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  root: { flex: 1 },

  header: {
    minHeight: 56,
    paddingHorizontal: 16,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  headerSide: {
    width: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    flex: 1,
    fontSize: 18,
    fontWeight: '700',
    color: '#111',
  },
  headerTitleLeft: { textAlign: 'left' },
  headerTitleCenter: { textAlign: 'center' },
  headerTitleRight: { textAlign: 'right' },
  defaultIcon: { fontSize: 22, color: '#111' },

  previewOuter: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  previewInner: {
    flex: 1,
    justifyContent: 'center',
  },
  image: {
    flex: 1,
    width: '100%',
  },
  placeholder: {
    flex: 1,
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  cropOverlay: {
    position: 'absolute',
    left: 24,
    right: 24,
    top: 24,
    bottom: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.8)',
    borderRadius: 12,
  },

  footer: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 16,
    backgroundColor: '#fff',
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 12,
  },
  cancelButton: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#D9D9D9',
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  uploadButton: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#2E7D32',
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  cancelText: { fontSize: 16, fontWeight: '600', color: '#111' },
  uploadText: { fontSize: 16, fontWeight: '700', color: '#fff' },
  buttonIconWrap: { alignItems: 'center', justifyContent: 'center' },
});

