module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath:
          'import com.rncustomizableimagecroppicker.NativeImageCropperPackage;',
        packageInstance: 'new NativeImageCropperPackage()',
      },
    },
  },
};

