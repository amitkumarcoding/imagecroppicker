import React from 'react';
import {
  Alert,
  Platform,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  openImageCropPicker,
  requestCameraPermissionAndroid,
  requestCameraPermissionIOS,
  requestPhotoLibraryPermissionIOS,
  requestPhotoPermissionAndroid,
} from 'react-native-customizable-image-crop-picker';

const App = () => {
  const uploadIconUri = require('../../../upload.jpg');

  const commonOptions = {
    cropWidth: 1,
    cropHeight: 1,
    includeBase64: true,
    compressQuality: 0.8, // 0..1
    compressFormat: 'jpeg', // 'jpeg' | 'png' | 'webp' (iOS webp best-effort)
    freeStyleCropEnabled: true,
    circularCrop: true,
    cropGridEnabled: true,
    cropGridColor: '#FFFFFF', // Android + iOS (circular overlay)
    cropFrameColor: '#FFFFFF', // Android only
    dimmedLayerColor: '#B3000000',
    cropOverlayColor: '#B3000000', // alias for dimmedLayerColor
    showNativeCropControls: true, // shows rotate/reset + aspect ratio picker toolbar
    rotationEnabled: true,
    isDarkTheme: false,
    statusBarColor: '#ffffff',
    statusBarStyle: 'dark', // Android only
    drawUnderStatusBar: false, // Android only
    headerTitle: 'Preview',
    headerAlignment: 'center',
    headerHeight: 84,
    headerPaddingHorizontal: 20,
    headerPaddingTop: 20,
    headerPaddingBottom: 20,
    footerPaddingHorizontal: 20,
    footerPaddingTop: 16,
    footerPaddingBottom: 24,
    footerButtonGap: 16,
    footerButtonHeight: 54,
    footerButtonLayout: 'horizontal',
    footerButtonOrder: 'uploadFirst',
    controlsPlacement: 'top', // required when showNativeCropControls: true
    topLeftControl: 'upload',
    topRightControl: 'cancel',
    cancelText: 'Cancel',
    uploadText: 'Upload',
    uploadButtonContent: 'text',
    cancelButtonContent: 'text',
    uploadButtonIconUri: uploadIconUri, // bundled asset via require(...)
    cancelButtonIconUri: 'https://img.icons8.com/sf-regular-filled/1200/cancel.jpg',
    uploadButtonIconSize: 22,
    cancelButtonIconSize: 22,
    uploadButtonIconTintColor: '',
    cancelButtonIconTintColor: '',
    buttonIconGap: 12,
    buttonContentPaddingHorizontal: 20,
    buttonContentPaddingVertical: 10,
    uploadButtonContentPaddingHorizontal: 24,
    uploadButtonContentPaddingVertical: 10,
    cancelButtonContentPaddingHorizontal: 20,
    cancelButtonContentPaddingVertical: 10,
    uploadButtonRadius: 28,
    cancelButtonRadius: 28,
    headerStyle: {
      containerStyle: { backgroundColor: '#ffffff', height: 84, paddingTop: 20, paddingBottom: 20, paddingHorizontal: 20 },
      titleStyle: { color: '#111111', fontSize: 20, fontFamily: '' },
    },
    footerStyle: {
      containerStyle: { backgroundColor: '#ffffff', paddingTop: 16, paddingHorizontal: 20, paddingBottom: 24, gap: 16 },
    },
    onCropStart: () => console.log('crop started'),
    onProgress: (p) => console.log('progress', p), // 0..1 (Android emits real base64 progress)
    onCropEnd: (res, err) => console.log('crop end', { res, err }),
  };

  return (
    <View style={styles.screen}>
      <StatusBar barStyle="dark-content" />

      <View style={styles.card}>
        <Text style={styles.h1}>Customizable Crop Picker (Demo)</Text>
        <Text style={styles.p}>
          This demonstrates the native flow options (Android native UI with
          customizable controls).
        </Text>

        <Pressable
          onPress={async () => {
            try {
              if (Platform.OS === 'android') {
                const granted = await requestCameraPermissionAndroid();
                // if (!granted) {
                //   Alert.alert(
                //     'Permission required',
                //     'Camera permission is required to continue.',
                //   );
                //   return;
                // }
              }
              if (Platform.OS === 'ios') {
                const granted = await requestCameraPermissionIOS();
                if (!granted) {
                  Alert.alert('Permission required', 'Camera permission is required to continue.');
                  return;
                }
              }
              const result = await openImageCropPicker({
                source: 'camera',
                ...commonOptions,
              });
              Alert.alert('Camera result', result.path || '(no path returned)');
            } catch (e) {
              Alert.alert('Camera flow failed', String(e?.message ?? e));
            }
          }}
          style={styles.primary}
        >
          <Text style={styles.primaryText}>Open Camera</Text>
        </Pressable>

        <Pressable
          onPress={async () => {
            try {
              if (Platform.OS === 'android') {
                // const granted = await requestPhotoPermissionAndroid();
                // if (!granted) {
                //   Alert.alert('Permission required', 'Photos permission is required to continue.');
                //   return;
                // }
              }
              if (Platform.OS === 'ios') {
                const granted = await requestPhotoLibraryPermissionIOS();
                if (!granted) {
                  Alert.alert('Permission required', 'Photos permission is required to continue.');
                  return;
                }
              }
              const result = await openImageCropPicker({
                source: 'gallery',
                ...commonOptions,
              });
              Alert.alert('Native result', result.path || '(no path returned)');
            } catch (e) {
              Alert.alert('Native bridge not wired', String(e?.message ?? e));
            }
          }}
          style={styles.secondary}
        >
          <Text style={styles.secondaryText}>Open gallery</Text>
        </Pressable>
      </View>
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F5F5F7', justifyContent: 'center' },
  card: {
    margin: 16,
    padding: 16,
    borderRadius: 16,
    backgroundColor: '#fff',
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
  },
  h1: { fontSize: 18, fontWeight: '900', color: '#111' },
  p: { marginTop: 8, color: '#444', lineHeight: 20 },
  primary: {
    marginTop: 16,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#111',
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryText: { color: '#fff', fontWeight: '800' },
  secondary: {
    marginTop: 12,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#EFEFF4',
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryText: { color: '#111', fontWeight: '700' },
});
