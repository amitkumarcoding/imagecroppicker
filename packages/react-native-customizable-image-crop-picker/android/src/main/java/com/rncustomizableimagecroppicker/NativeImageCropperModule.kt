package com.rncustomizableimagecroppicker

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64OutputStream
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.module.annotations.ReactModule
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ReactModule(name = NativeImageCropperModule.NAME)
class NativeImageCropperModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {

  companion object {
    const val NAME = "NativeImageCropperModule"
    private const val REQUEST_PICK_IMAGE = 63201
    private const val REQUEST_CAPTURE_IMAGE = 63202

    private const val E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST"
    private const val E_PICKER_CANCELLED = "E_PICKER_CANCELLED"
    private const val E_PICKER_CANCELLED_MSG = "User cancelled image selection"
    private const val E_NO_IMAGE_DATA_FOUND = "E_NO_IMAGE_DATA_FOUND"
    private const val E_MODULE_DESTROYED = "E_MODULE_DESTROYED"
    private const val E_PICKER_ERROR = "E_PICKER_ERROR"
    private const val E_PERMISSION_MISSING = "E_PERMISSION_MISSING"
    private const val E_NO_APP_AVAILABLE = "E_NO_APP_AVAILABLE"

    private const val DEFAULT_CROP_SIZE = 100
    private const val SOURCE_CAMERA = "camera"
    private const val SOURCE_GALLERY = "gallery"
  }

  override fun getName(): String = NAME

  // Required by React Native when using NativeEventEmitter on Android.
  // (No-op; we emit events opportunistically during base64 encoding.)
  @ReactMethod
  fun addListener(eventName: String) = Unit

  @ReactMethod
  fun removeListeners(count: Int) = Unit

  private var pendingPromise: Promise? = null
  private val encodeExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  @Volatile private var isInvalidated = false

  private var cropWidth = DEFAULT_CROP_SIZE
  private var cropHeight = DEFAULT_CROP_SIZE
  private var headerTitle = "Preview Image"
  private var cancelText = "Cancel"
  private var uploadText = "Upload"
  // Defaults aligned with the "demo button layout":
  // - Cancel is a light button with dark text
  // - Upload is a black button with white text
  private var cancelColor = "#111111" // text color
  private var uploadColor = "#111111" // background color
  private var dimmedLayerColor = "#B3000000"
  private var statusBarColor = "#FFFFFF"
  private var statusBarStyle = "auto" // "dark" | "light" | "auto"
  private var isDarkTheme = false
  private var drawUnderStatusBar = false
  private var pickerSource = SOURCE_GALLERY
  private var pendingCameraUri: Uri? = null
  private var includeBase64 = false
  private var freeStyleCropEnabled = false
  private var compressQuality = 100 // 0..100
  private var compressFormat = "jpeg" // jpeg | png | webp
  private var circularCrop = false
  private var rotationEnabled = false
  private var cropGridEnabled = false
  private var cropFrameColor = ""
  private var cropGridColor = ""
  private var showNativeCropControls = false

  private var headerBackgroundColor = "#FFFFFF"
  private var headerTitleColor = "#111111"
  private var headerTitleFontSize = 22
  private var headerTitleFontFamily = ""
  private var headerHeight = 84
  private var headerPaddingHorizontal = 20
  private var headerPaddingTop = 20
  private var headerPaddingBottom = 20

  private var buttonContainerBackgroundColor = "#FFFFFF"
  private var buttonContainerPaddingHorizontal = 20
  private var buttonContainerPaddingTop = 16
  private var buttonContainerPaddingBottom = 24
  private var buttonGap = 12
  private var buttonHeight = 54
  private var buttonLayout = "vertical"

  private var cancelButtonBackgroundColor = "#EFEFF4"
  private var cancelButtonBorderColor = "#EFEFF4"
  private var cancelButtonBorderWidth = 0
  private var cancelButtonFontSize = 18
  private var cancelButtonFontFamily = ""
  private var cancelButtonRadius = 28

  private var uploadButtonTextColor = "#FFFFFF"
  private var uploadButtonBackgroundColor = "#111111"
  private var uploadButtonBorderColor = "#111111"
  private var uploadButtonBorderWidth = 0
  private var uploadButtonFontSize = 18
  private var uploadButtonFontFamily = ""
  private var uploadButtonRadius = 28

  private var controlsPlacement = "bottom"
  private var topLeftControl = "cancel"
  private var topRightControl = "upload"
  private var footerButtonOrder = "uploadFirst"

  private var cancelButtonContent = "text"
  private var cancelButtonIconUri = ""
  private var cancelButtonIconBase64 = ""
  private var cancelButtonIconTintColor = ""
  private var cancelButtonIconSize = 18
  private var cancelButtonIconGap = 8
  private var cancelButtonPaddingHorizontal = 12
  private var cancelButtonPaddingVertical = 0

  private var uploadButtonContent = "text"
  private var uploadButtonIconUri = ""
  private var uploadButtonIconBase64 = ""
  private var uploadButtonIconTintColor = ""
  private var uploadButtonIconSize = 18
  private var uploadButtonIconGap = 8
  private var uploadButtonPaddingHorizontal = 12
  private var uploadButtonPaddingVertical = 0

  private var headerAlignment = "left" // "left" | "center" | "right"

  init {
    reactContext.addActivityEventListener(this)
  }

  @ReactMethod
  fun openImagePreview(options: ReadableMap, promise: Promise) {
    if (isInvalidated) {
      promise.reject(E_MODULE_DESTROYED, "Native image cropper module is destroyed")
      return
    }
    val activity = reactApplicationContext.currentActivity
    if (activity == null) {
      promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist")
      return
    }
    if (pendingPromise != null) {
      promise.reject(E_PICKER_ERROR, "Image picker is already in progress")
      return
    }

    applyConfig(options)
    pendingPromise = promise

    try {
      if (pickerSource == SOURCE_CAMERA) {
        launchCamera(activity)
      } else {
        launchSystemPhotoPicker(activity)
      }
    } catch (error: Exception) {
      rejectPending(E_PICKER_ERROR, error.message ?: "Unable to open image picker")
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_PICK_IMAGE -> handleImagePickResult(activity, resultCode, data)
      REQUEST_CAPTURE_IMAGE -> handleCameraResult(activity, resultCode, data)
      UCrop.REQUEST_CROP -> handleCropResult(resultCode, data)
    }
  }

  override fun onNewIntent(intent: Intent) = Unit

  override fun invalidate() {
    isInvalidated = true
    reactContext.removeActivityEventListener(this)
    pendingCameraUri = null
    rejectPending(E_MODULE_DESTROYED, "Native image cropper module is destroyed")
    encodeExecutor.shutdownNow()
    super.invalidate()
  }

  private fun applyConfig(options: ReadableMap) {
    isDarkTheme = options.hasKey("isDarkTheme") && options.getBoolean("isDarkTheme")
    includeBase64 = options.hasKey("includeBase64") && options.getBoolean("includeBase64")
    compressQuality = runCatching {
      if (options.hasKey("compressQuality") && !options.isNull("compressQuality")) {
        val q = options.getDouble("compressQuality").coerceIn(0.0, 1.0)
        kotlin.math.round(q * 100.0).toInt().coerceIn(0, 100)
      } else {
        100
      }
    }.getOrDefault(100)
    compressFormat = options.getStringOrDefault("compressFormat", "jpeg").let { raw ->
      when (raw.lowercase()) {
        "png" -> "png"
        "webp" -> "webp"
        else -> "jpeg"
      }
    }
    circularCrop = options.hasKey("circularCrop") && options.getBoolean("circularCrop")
    rotationEnabled = options.hasKey("rotationEnabled") && options.getBoolean("rotationEnabled")
    cropGridEnabled = options.hasKey("cropGridEnabled") && options.getBoolean("cropGridEnabled")
    cropFrameColor = options.getStringOrDefault("cropFrameColor", "")
    cropGridColor = options.getStringOrDefault("cropGridColor", "")
    showNativeCropControls = options.hasKey("showNativeCropControls") && options.getBoolean("showNativeCropControls")
    if (rotationEnabled) showNativeCropControls = true
    drawUnderStatusBar = options.hasKey("drawUnderStatusBar") && options.getBoolean("drawUnderStatusBar")
    freeStyleCropEnabled = options.hasKey("freeStyleCropEnabled") && options.getBoolean("freeStyleCropEnabled")
    cropWidth = sanitizePositive(options.getIntOrDefault("width", DEFAULT_CROP_SIZE), DEFAULT_CROP_SIZE)
    cropHeight = sanitizePositive(options.getIntOrDefault("height", DEFAULT_CROP_SIZE), DEFAULT_CROP_SIZE)
    headerTitle = options.getStringOrDefault("cropperToolbarTitle", "Preview Image")
    cancelText = options.getStringOrDefault("cropperCancelText", "Cancel")
    uploadText = options.getStringOrDefault("cropperChooseText", "Upload")
    cancelColor = options.getStringOrDefault("cropperCancelColor", "#111111")
    uploadColor = options.getStringOrDefault("cropperChooseColor", "#111111")
    dimmedLayerColor = options.getStringOrDefault(
      "cropperDimmedLayerColor",
      if (isDarkTheme) "#E0000000" else "#B3000000",
    )
    pickerSource = options.getStringOrDefault("pickerSource", SOURCE_GALLERY)

    controlsPlacement = options.getStringOrDefault("controlsPlacement", "bottom").let { raw ->
      if (raw == "top") "top" else "bottom"
    }
    if (showNativeCropControls) controlsPlacement = "top"
    topLeftControl = options.getStringOrDefault("topLeftControl", "cancel").let { raw ->
      if (raw == "upload" || raw == "cancel" || raw == "none") raw else "cancel"
    }
    topRightControl = options.getStringOrDefault("topRightControl", "upload").let { raw ->
      if (raw == "upload" || raw == "cancel" || raw == "none") raw else "upload"
    }
    footerButtonOrder = options.getStringOrDefault("footerButtonOrder", "uploadFirst").let { raw ->
      if (raw == "cancelFirst") "cancelFirst" else "uploadFirst"
    }

    headerAlignment = options.getStringOrDefault("headerAlignment", "left").let { raw ->
      if (raw == "center" || raw == "right" || raw == "left") raw else "left"
    }

    val headerStyle = getOptionalMap(options, "headerStyle")
    headerBackgroundColor = getMapString(headerStyle, "backgroundColor", if (isDarkTheme) "#0B0B0F" else "#FFFFFF")
    headerTitleColor = getMapString(
      headerStyle,
      "color",
      getMapString(headerStyle, "titleColor", if (isDarkTheme) "#F5F5F7" else "#111111"),
    )
    headerTitleFontSize = sanitizePositive(getMapInt(headerStyle, "fontSize", 22), 22)
    headerTitleFontFamily = getMapString(headerStyle, "fontFamily", "")
    val defaultHeaderHeight = if (controlsPlacement == "top") 84 else 56
    headerHeight = sanitizePositive(getMapInt(headerStyle, "height", defaultHeaderHeight), defaultHeaderHeight)
    headerPaddingHorizontal = sanitizeNonNegative(getMapInt(headerStyle, "paddingHorizontal", 20), 20)
    val defaultHeaderPadTop = if (controlsPlacement == "top") 20 else 12
    val defaultHeaderPadBottom = if (controlsPlacement == "top") 20 else 12
    headerPaddingTop = sanitizeNonNegative(getMapInt(headerStyle, "paddingTop", defaultHeaderPadTop), defaultHeaderPadTop)
    headerPaddingBottom = sanitizeNonNegative(getMapInt(headerStyle, "paddingBottom", defaultHeaderPadBottom), defaultHeaderPadBottom)

    // Default status bar color to header background to avoid a visible seam.
    // Explicit `statusBarColor` (cropperStatusBarColor) still overrides this.
    statusBarColor = options.getStringOrDefault("cropperStatusBarColor", headerBackgroundColor)
    statusBarStyle = options.getStringOrDefault("cropperStatusBarStyle", "auto").let { raw ->
      if (raw == "dark" || raw == "light") raw else "auto"
    }

    val containerStyle = getOptionalMap(options, "buttonContainerStyle")
    buttonContainerBackgroundColor = getMapString(containerStyle, "backgroundColor", "#FFFFFF")
    buttonContainerPaddingHorizontal = sanitizeNonNegative(getMapInt(containerStyle, "paddingHorizontal", 20), 20)
    buttonContainerPaddingTop = sanitizeNonNegative(getMapInt(containerStyle, "paddingTop", 16), 16)
    buttonContainerPaddingBottom = sanitizeNonNegative(getMapInt(containerStyle, "paddingBottom", 24), 24)
    buttonGap = sanitizeNonNegative(getMapInt(containerStyle, "gap", 12), 12)
    buttonHeight = sanitizePositive(getMapInt(containerStyle, "buttonHeight", 54), 54)
    buttonLayout = getMapString(containerStyle, "layout", "vertical").let { raw ->
      if (raw == "horizontal") "horizontal" else "vertical"
    }

    val cancelStyle = getOptionalMap(options, "cancelButtonStyle")
    cancelButtonBackgroundColor = getMapString(cancelStyle, "backgroundColor", cancelButtonBackgroundColor)
    cancelButtonBorderColor = getMapString(cancelStyle, "borderColor", cancelButtonBorderColor)
    cancelButtonBorderWidth = sanitizeNonNegative(getMapInt(cancelStyle, "borderWidth", cancelButtonBorderWidth), cancelButtonBorderWidth)
    cancelButtonFontSize = sanitizePositive(getMapInt(cancelStyle, "fontSize", cancelButtonFontSize), cancelButtonFontSize)
    cancelButtonFontFamily = getMapString(cancelStyle, "fontFamily", cancelButtonFontFamily)
    cancelButtonRadius = sanitizeNonNegative(getMapInt(cancelStyle, "borderRadius", cancelButtonRadius), cancelButtonRadius)
    cancelColor = getMapString(cancelStyle, "textColor", cancelColor)
    cancelButtonContent = getMapString(cancelStyle, "content", cancelButtonContent)
    cancelButtonIconUri = getMapString(cancelStyle, "iconUri", cancelButtonIconUri)
    cancelButtonIconBase64 = getMapString(cancelStyle, "iconBase64", cancelButtonIconBase64)
    cancelButtonIconTintColor = getMapString(cancelStyle, "iconTintColor", cancelButtonIconTintColor)
    cancelButtonIconSize = sanitizePositive(getMapInt(cancelStyle, "iconSize", cancelButtonIconSize), cancelButtonIconSize)
    cancelButtonIconGap = sanitizeNonNegative(getMapInt(cancelStyle, "iconGap", cancelButtonIconGap), cancelButtonIconGap)
    cancelButtonPaddingHorizontal = sanitizeNonNegative(getMapInt(cancelStyle, "paddingHorizontal", cancelButtonPaddingHorizontal), cancelButtonPaddingHorizontal)
    cancelButtonPaddingVertical = sanitizeNonNegative(getMapInt(cancelStyle, "paddingVertical", cancelButtonPaddingVertical), cancelButtonPaddingVertical)

    val uploadStyle = getOptionalMap(options, "uploadButtonStyle")
    uploadButtonBackgroundColor = getMapString(uploadStyle, "backgroundColor", uploadButtonBackgroundColor)
    uploadButtonBorderColor = getMapString(uploadStyle, "borderColor", uploadButtonBorderColor)
    uploadButtonBorderWidth = sanitizeNonNegative(getMapInt(uploadStyle, "borderWidth", uploadButtonBorderWidth), uploadButtonBorderWidth)
    uploadButtonFontSize = sanitizePositive(getMapInt(uploadStyle, "fontSize", uploadButtonFontSize), uploadButtonFontSize)
    uploadButtonFontFamily = getMapString(uploadStyle, "fontFamily", uploadButtonFontFamily)
    uploadButtonRadius = sanitizeNonNegative(getMapInt(uploadStyle, "borderRadius", uploadButtonRadius), uploadButtonRadius)
    uploadButtonTextColor = getMapString(uploadStyle, "textColor", uploadButtonTextColor)
    uploadColor = uploadButtonBackgroundColor
    uploadButtonContent = getMapString(uploadStyle, "content", uploadButtonContent)
    uploadButtonIconUri = getMapString(uploadStyle, "iconUri", uploadButtonIconUri)
    uploadButtonIconBase64 = getMapString(uploadStyle, "iconBase64", uploadButtonIconBase64)
    uploadButtonIconTintColor = getMapString(uploadStyle, "iconTintColor", uploadButtonIconTintColor)
    uploadButtonIconSize = sanitizePositive(getMapInt(uploadStyle, "iconSize", uploadButtonIconSize), uploadButtonIconSize)
    uploadButtonIconGap = sanitizeNonNegative(getMapInt(uploadStyle, "iconGap", uploadButtonIconGap), uploadButtonIconGap)
    uploadButtonPaddingHorizontal = sanitizeNonNegative(getMapInt(uploadStyle, "paddingHorizontal", uploadButtonPaddingHorizontal), uploadButtonPaddingHorizontal)
    uploadButtonPaddingVertical = sanitizeNonNegative(getMapInt(uploadStyle, "paddingVertical", uploadButtonPaddingVertical), uploadButtonPaddingVertical)
  }

  private fun handleImagePickResult(activity: Activity, resultCode: Int, data: Intent?) {
    if (pendingPromise == null) return
    if (resultCode == Activity.RESULT_CANCELED) {
      rejectPending(E_PICKER_CANCELLED, E_PICKER_CANCELLED_MSG)
      return
    }
    val sourceUri = data?.data
    if (resultCode != Activity.RESULT_OK || sourceUri == null) {
      rejectPending(E_NO_IMAGE_DATA_FOUND, "Cannot resolve image url")
      return
    }
    startCrop(activity, sourceUri)
  }

  private fun handleCameraResult(activity: Activity, resultCode: Int, data: Intent?) {
    if (pendingPromise == null) return
    if (resultCode == Activity.RESULT_CANCELED) {
      cleanupCameraUriPermissions(activity)
      pendingCameraUri = null
      rejectPending(E_PICKER_CANCELLED, E_PICKER_CANCELLED_MSG)
      return
    }
    val sourceUri = pendingCameraUri ?: data?.data
    cleanupCameraUriPermissions(activity)
    pendingCameraUri = null
    if (resultCode != Activity.RESULT_OK || sourceUri == null) {
      rejectPending(E_NO_IMAGE_DATA_FOUND, "Cannot resolve image url")
      return
    }
    startCrop(activity, sourceUri)
  }

  private fun startCrop(activity: Activity, sourceUri: Uri) {
    try {
      val ext = when (compressFormat) {
        "png" -> "png"
        "webp" -> "webp"
        else -> "jpg"
      }
      val destinationFile = File(activity.cacheDir, "native-cropped-${UUID.randomUUID()}.$ext")
      val destinationUri = Uri.fromFile(destinationFile)

      val cropOptions = UCrop.Options().apply {
        val fmt = when (compressFormat) {
          "png" -> Bitmap.CompressFormat.PNG
          "webp" -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
              Bitmap.CompressFormat.WEBP_LOSSY
            } else {
              @Suppress("DEPRECATION")
              Bitmap.CompressFormat.WEBP
            }
          }
          else -> Bitmap.CompressFormat.JPEG
        }
        setCompressionFormat(fmt)
        // PNG ignores quality but uCrop API requires an int.
        setCompressionQuality(compressQuality.coerceIn(0, 100))
        setToolbarTitle(headerTitle.ifBlank { "Preview Image" })
        setToolbarColor(if (isDarkTheme) Color.parseColor("#121212") else Color.parseColor("#FFFFFF"))
        setStatusBarColor(parseColor(statusBarColor, Color.WHITE))
        setToolbarWidgetColor(if (isDarkTheme) Color.parseColor("#FFFFFF") else Color.parseColor("#111111"))
        setShowCropGrid(cropGridEnabled)
        setShowCropFrame(true)
        if (cropFrameColor.isNotBlank()) setCropFrameColor(parseColor(cropFrameColor, Color.WHITE))
        if (cropGridColor.isNotBlank()) setCropGridColor(parseColor(cropGridColor, Color.WHITE))
        setDimmedLayerColor(parseColor(dimmedLayerColor, Color.parseColor("#B3000000")))
        setCircleDimmedLayer(circularCrop)
        setHideBottomControls(!showNativeCropControls)
        setFreeStyleCropEnabled(freeStyleCropEnabled)
      }

      val cropIntent = UCrop.of(sourceUri, destinationUri)
        .withOptions(cropOptions)
        .withAspectRatio(cropWidth.toFloat(), cropHeight.toFloat())
        .getIntent(activity)
        .apply {
          setClass(activity, NativeImageCropperActivity::class.java)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_TITLE, headerTitle.ifBlank { "Preview Image" })
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_TEXT, cancelText.ifBlank { "Cancel" })
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_TEXT, uploadText.ifBlank { "Upload" })
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_COLOR, cancelColor.ifBlank { "#111111" })
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_COLOR, uploadColor.ifBlank { "#111111" })
          putExtra(NativeImageCropperActivity.EXTRA_STATUS_BAR_COLOR, statusBarColor.ifBlank { "#FFFFFF" })
          putExtra(NativeImageCropperActivity.EXTRA_IS_DARK_THEME, isDarkTheme)
          putExtra(NativeImageCropperActivity.EXTRA_DRAW_UNDER_STATUS_BAR, drawUnderStatusBar)
          putExtra(NativeImageCropperActivity.EXTRA_STATUS_BAR_STYLE, statusBarStyle)

          putExtra(NativeImageCropperActivity.EXTRA_HEADER_BACKGROUND_COLOR, headerBackgroundColor)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_TITLE_COLOR, headerTitleColor)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_TITLE_FONT_SIZE, headerTitleFontSize)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_TITLE_FONT_FAMILY, headerTitleFontFamily)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_HEIGHT, headerHeight)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_PADDING_HORIZONTAL, headerPaddingHorizontal)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_PADDING_TOP, headerPaddingTop)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_PADDING_BOTTOM, headerPaddingBottom)
          putExtra(NativeImageCropperActivity.EXTRA_HEADER_ALIGNMENT, headerAlignment)

          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_BACKGROUND_COLOR, buttonContainerBackgroundColor)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_PADDING_HORIZONTAL, buttonContainerPaddingHorizontal)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_PADDING_TOP, buttonContainerPaddingTop)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_PADDING_BOTTOM, buttonContainerPaddingBottom)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_BUTTON_GAP, buttonGap)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_BUTTON_HEIGHT, buttonHeight)
          putExtra(NativeImageCropperActivity.EXTRA_BOTTOM_BUTTON_LAYOUT, buttonLayout)
          putExtra(NativeImageCropperActivity.EXTRA_CONTROLS_PLACEMENT, controlsPlacement)
          putExtra(NativeImageCropperActivity.EXTRA_TOP_LEFT_CONTROL, topLeftControl)
          putExtra(NativeImageCropperActivity.EXTRA_TOP_RIGHT_CONTROL, topRightControl)
          putExtra(NativeImageCropperActivity.EXTRA_FOOTER_BUTTON_ORDER, footerButtonOrder)

          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BACKGROUND_COLOR, cancelButtonBackgroundColor)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BORDER_COLOR, cancelButtonBorderColor)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BORDER_WIDTH, cancelButtonBorderWidth)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_FONT_SIZE, cancelButtonFontSize)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_FONT_FAMILY, cancelButtonFontFamily)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BORDER_RADIUS, cancelButtonRadius)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_CONTENT, cancelButtonContent)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_ICON_URI, cancelButtonIconUri)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_ICON_BASE64, cancelButtonIconBase64)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_ICON_TINT, cancelButtonIconTintColor)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_ICON_SIZE, cancelButtonIconSize)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_ICON_GAP, cancelButtonIconGap)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_PADDING_HORIZONTAL, cancelButtonPaddingHorizontal)
          putExtra(NativeImageCropperActivity.EXTRA_CANCEL_BUTTON_PADDING_VERTICAL, cancelButtonPaddingVertical)

          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_TEXT_COLOR, uploadButtonTextColor)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_COLOR, uploadButtonBackgroundColor)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BORDER_COLOR, uploadButtonBorderColor)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BORDER_WIDTH, uploadButtonBorderWidth)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_FONT_SIZE, uploadButtonFontSize)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_FONT_FAMILY, uploadButtonFontFamily)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BORDER_RADIUS, uploadButtonRadius)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_CONTENT, uploadButtonContent)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_ICON_URI, uploadButtonIconUri)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_ICON_BASE64, uploadButtonIconBase64)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_ICON_TINT, uploadButtonIconTintColor)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_ICON_SIZE, uploadButtonIconSize)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_ICON_GAP, uploadButtonIconGap)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_PADDING_HORIZONTAL, uploadButtonPaddingHorizontal)
          putExtra(NativeImageCropperActivity.EXTRA_UPLOAD_BUTTON_PADDING_VERTICAL, uploadButtonPaddingVertical)

          putExtra(NativeImageCropperActivity.EXTRA_CROP_GRID_ENABLED, cropGridEnabled)
          putExtra(NativeImageCropperActivity.EXTRA_CROP_FRAME_COLOR, cropFrameColor)
          putExtra(NativeImageCropperActivity.EXTRA_CROP_GRID_COLOR, cropGridColor)
        }

      if (cropIntent.resolveActivity(activity.packageManager) == null) {
        rejectPending(E_NO_APP_AVAILABLE, "Cropper activity is not available")
        return
      }
      activity.startActivityForResult(cropIntent, UCrop.REQUEST_CROP)
      activity.overridePendingTransition(0, 0)
    } catch (error: Exception) {
      rejectPending(E_PICKER_ERROR, error.message ?: "Unable to start cropper")
    }
  }

  private fun launchCamera(activity: Activity) {
    val granted =
      ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    if (!granted) {
      rejectPending(E_PERMISSION_MISSING, "Camera permission is not granted")
      return
    }
    val outputFile = File(activity.cacheDir, "native-camera-${UUID.randomUUID()}.jpg")
    val authority = activity.packageName + ".nativeimagepicker.provider"
    val outputUri = FileProvider.getUriForFile(activity, authority, outputFile)
    pendingCameraUri = outputUri

    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
      putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
      putExtra("android.intent.extra.quickCapture", true)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    val cameraHandlers = activity.packageManager.queryIntentActivities(cameraIntent, 0)
    if (cameraHandlers.isNullOrEmpty()) {
      pendingCameraUri = null
      rejectPending(E_NO_APP_AVAILABLE, "No camera app available")
      return
    }
    for (handler in cameraHandlers) {
      activity.grantUriPermission(
        handler.activityInfo.packageName,
        outputUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
    activity.startActivityForResult(cameraIntent, REQUEST_CAPTURE_IMAGE)
  }

  private fun launchSystemPhotoPicker(activity: Activity) {
    val pickRequest = PickVisualMediaRequest.Builder()
      .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
      .build()
    val pickerIntent = ActivityResultContracts.PickVisualMedia().createIntent(activity, pickRequest).apply {
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (pickerIntent.resolveActivity(activity.packageManager) == null) {
      rejectPending(E_NO_APP_AVAILABLE, "No gallery app available")
      return
    }
    activity.startActivityForResult(pickerIntent, REQUEST_PICK_IMAGE)
  }

  private fun handleCropResult(resultCode: Int, data: Intent?) {
    if (pendingPromise == null) return
    if (resultCode == Activity.RESULT_CANCELED) {
      rejectPending(E_PICKER_CANCELLED, E_PICKER_CANCELLED_MSG)
      return
    }
    val resultUri = UCrop.getOutput(data ?: return rejectPending(E_NO_IMAGE_DATA_FOUND, "Cannot find image data"))
    if (resultCode != Activity.RESULT_OK || resultUri == null) {
      rejectPending(E_NO_IMAGE_DATA_FOUND, "Cannot find image data")
      return
    }

    encodeExecutor.execute {
      try {
        if (isInvalidated || pendingPromise == null) return@execute
        val result = WritableNativeMap().apply {
          putString("path", resultUri.toString())
          if (includeBase64) {
            val encoded = toBase64String(resultUri)
            if (encoded.isNullOrBlank()) {
              rejectPending(E_NO_IMAGE_DATA_FOUND, "Cannot encode selected image")
              return@execute
            }
            putString("base64", encoded)
          }
        }
        resolvePending(result)
      } catch (error: Exception) {
        rejectPending(E_PICKER_ERROR, error.message ?: "Unable to process image")
      } finally {
        runCatching {
          if ("file".equals(resultUri.scheme, ignoreCase = true)) {
            File(resultUri.path.orEmpty()).delete()
          }
        }
      }
    }
  }

  private fun parseColor(color: String, fallback: Int): Int {
    return runCatching { Color.parseColor(color) }.getOrElse { fallback }
  }

  private fun toBase64String(uri: Uri): String? {
    emitProgress(0.0)
    val output = ByteArrayOutputStream()
    Base64OutputStream(output, Base64.NO_WRAP).use { base64Stream ->
      val resolver = reactContext.contentResolver
      val total = runCatching {
        resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
          val len = afd.length
          if (len > 0) len else -1L
        } ?: -1L
      }.getOrDefault(-1L)
      var readSoFar = 0L
      var lastEmitted = 0.0
      resolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
          val read = input.read(buffer)
          if (read <= 0) break
          base64Stream.write(buffer, 0, read)
          if (total > 0) {
            readSoFar += read.toLong()
            val p = (readSoFar.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
            // Emit only if progress moved enough to matter.
            if (p - lastEmitted >= 0.03) {
              lastEmitted = p
              emitProgress(p)
            }
          }
        }
      } ?: return null
    }
    emitProgress(1.0)
    return output.toString(Charsets.UTF_8.name())
  }

  private fun emitProgress(progress: Double) {
    if (isInvalidated) return
    val map = WritableNativeMap().apply {
      putDouble("progress", progress.coerceIn(0.0, 1.0))
    }
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("NativeImageCropperProgress", map)
  }

  private fun cleanupCameraUriPermissions(activity: Activity) {
    val uri = pendingCameraUri ?: return
    runCatching {
      activity.revokeUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
  }

  private fun resolvePending(data: WritableNativeMap) {
    val promise = pendingPromise ?: return
    pendingPromise = null
    reactContext.runOnUiQueueThread { promise.resolve(data) }
  }

  private fun rejectPending(code: String, message: String) {
    val promise = pendingPromise ?: return
    pendingPromise = null
    reactContext.runOnUiQueueThread { promise.reject(code, message) }
  }

  private fun ReadableMap.getIntOrDefault(key: String, fallback: Int): Int {
    if (!hasKey(key) || isNull(key)) return fallback
    return runCatching { getInt(key) }.getOrDefault(fallback)
  }

  private fun ReadableMap.getStringOrDefault(key: String, fallback: String): String {
    if (!hasKey(key) || isNull(key)) return fallback
    return getString(key).orEmpty().ifBlank { fallback }
  }

  private fun sanitizePositive(value: Int, fallback: Int): Int {
    return if (value > 0) value else fallback
  }

  private fun sanitizeNonNegative(value: Int, fallback: Int): Int {
    return if (value >= 0) value else fallback
  }

  private fun getOptionalMap(map: ReadableMap, key: String): ReadableMap? {
    return if (map.hasKey(key) && !map.isNull(key)) map.getMap(key) else null
  }

  private fun getMapString(map: ReadableMap?, key: String, fallback: String): String {
    if (map == null || !map.hasKey(key) || map.isNull(key)) return fallback
    return map.getString(key).orEmpty().ifBlank { fallback }
  }

  private fun getMapInt(map: ReadableMap?, key: String, fallback: Int): Int {
    if (map == null || !map.hasKey(key) || map.isNull(key)) return fallback
    return runCatching { map.getInt(key) }.getOrDefault(fallback)
  }
}

