// export { default } from './packages/react-native-customizable-image-crop-picker/example/App';

import React from 'react';
import {
    View,
    Text,
    StyleSheet,
    StatusBar,
    TouchableOpacity,
    Alert,
    Platform,
    PermissionsAndroid,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { openImageCropPicker } from 'react-native-customizable-image-crop-picker';

const App = () => {
    async function ensureCameraPermission() {
        if (Platform.OS !== 'android') return true;
        const res = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA);
        return res === PermissionsAndroid.RESULTS.GRANTED;
    }

    const baseConfig = {
        cropWidth: 1,
        cropHeight: 1,
        circularCrop: true,
        cropGridEnabled: true,
        dimmedLayerColor: '#B3000000',
        compressQuality: 0.8,
        compressFormat: 'jpeg',
        headerTitle: 'Preview',
        headerAlignment: 'center',
        controlsPlacement: 'bottom',
        uploadText: 'Upload',
        cancelText: 'Cancel',
        uploadButtonContent: 'text',
        cancelButtonContent: 'text',
    };

    const openCamera = async () => {
        const ok = await ensureCameraPermission();
        if (!ok) return;

        try {
            const result = await openImageCropPicker({
                ...baseConfig,
                source: 'camera',
            });
            console.log('Camera Result:', result);
        } catch (error) {
            handleError(error);
        }
    };

    const openGallery = async () => {
        try {
            const result = await openImageCropPicker({
                ...baseConfig,
                source: 'gallery',
            });

            console.log('Gallery Result:', result);
        } catch (error) {
            handleError(error);
        }
    };

    const handleError = (error) => {
        if (error?.code === 'E_PERMISSION_MISSING') {
            Alert.alert('Permission Required', 'Please allow camera access');
            return;
        }

        if (error?.code === 'E_PICKER_CANCELLED') {
            console.log('User cancelled');
            return;
        }

        console.error('Unhandled Error:', error);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <StatusBar barStyle="dark-content" />

            <View style={styles.container}>
                <TouchableOpacity
                    style={styles.primaryButton}
                    activeOpacity={0.8}
                    onPress={openCamera}
                >
                    <Text style={styles.primaryText}>Open Camera</Text>
                </TouchableOpacity>

                <TouchableOpacity
                    style={styles.secondaryButton}
                    activeOpacity={0.8}
                    onPress={openGallery}
                >
                    <Text style={styles.secondaryText}>Open Gallery</Text>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

export default App;

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F9FAFB',
    },
    container: {
        flex: 1,
        justifyContent: 'center',
        paddingHorizontal: 20,
        gap: 16,
    },
    primaryButton: {
        backgroundColor: '#111827',
        paddingVertical: 14,
        borderRadius: 12,
        alignItems: 'center',
    },
    primaryText: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '600',
    },
    secondaryButton: {
        borderWidth: 1.5,
        borderColor: '#111827',
        paddingVertical: 14,
        borderRadius: 12,
        alignItems: 'center',
    },
    secondaryText: {
        color: '#111827',
        fontSize: 16,
        fontWeight: '600',
    },
});