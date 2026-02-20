package com.rncustomizableimagecroppicker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.view.OverlayView
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NativeImageCropperActivity : UCropActivity() {

  companion object {
    const val EXTRA_HEADER_TITLE = "native_cropper_header_title"
    const val EXTRA_CANCEL_TEXT = "native_cropper_cancel_text"
    const val EXTRA_UPLOAD_TEXT = "native_cropper_upload_text"
    const val EXTRA_CANCEL_COLOR = "native_cropper_cancel_color"
    const val EXTRA_UPLOAD_COLOR = "native_cropper_upload_color"
    const val EXTRA_STATUS_BAR_COLOR = "native_cropper_status_bar_color"
    const val EXTRA_IS_DARK_THEME = "native_cropper_is_dark_theme"
    const val EXTRA_DRAW_UNDER_STATUS_BAR = "native_cropper_draw_under_status_bar"
    const val EXTRA_STATUS_BAR_STYLE = "native_cropper_status_bar_style"
    const val EXTRA_HEADER_BACKGROUND_COLOR = "native_cropper_header_background_color"
    const val EXTRA_HEADER_TITLE_COLOR = "native_cropper_header_title_color"
    const val EXTRA_HEADER_TITLE_FONT_SIZE = "native_cropper_header_title_font_size"
    const val EXTRA_HEADER_TITLE_FONT_FAMILY = "native_cropper_header_title_font_family"
    const val EXTRA_HEADER_HEIGHT = "native_cropper_header_height"
    const val EXTRA_HEADER_PADDING_HORIZONTAL = "native_cropper_header_padding_horizontal"
    const val EXTRA_HEADER_PADDING_TOP = "native_cropper_header_padding_top"
    const val EXTRA_HEADER_PADDING_BOTTOM = "native_cropper_header_padding_bottom"
    const val EXTRA_HEADER_ALIGNMENT = "native_cropper_header_alignment"
    const val EXTRA_BOTTOM_BACKGROUND_COLOR = "native_cropper_bottom_background_color"
    const val EXTRA_BOTTOM_PADDING_HORIZONTAL = "native_cropper_bottom_padding_horizontal"
    const val EXTRA_BOTTOM_PADDING_TOP = "native_cropper_bottom_padding_top"
    const val EXTRA_BOTTOM_PADDING_BOTTOM = "native_cropper_bottom_padding_bottom"
    const val EXTRA_BOTTOM_BUTTON_GAP = "native_cropper_bottom_button_gap"
    const val EXTRA_BOTTOM_BUTTON_HEIGHT = "native_cropper_bottom_button_height"
    const val EXTRA_BOTTOM_BUTTON_LAYOUT = "native_cropper_bottom_button_layout"
    const val EXTRA_CONTROLS_PLACEMENT = "native_cropper_controls_placement"
    const val EXTRA_TOP_LEFT_CONTROL = "native_cropper_top_left_control"
    const val EXTRA_TOP_RIGHT_CONTROL = "native_cropper_top_right_control"
    const val EXTRA_FOOTER_BUTTON_ORDER = "native_cropper_footer_button_order"

    const val EXTRA_CANCEL_BUTTON_CONTENT = "native_cropper_cancel_button_content"
    const val EXTRA_CANCEL_BUTTON_ICON_URI = "native_cropper_cancel_button_icon_uri"
    const val EXTRA_CANCEL_BUTTON_ICON_BASE64 = "native_cropper_cancel_button_icon_base64"
    const val EXTRA_CANCEL_BUTTON_ICON_TINT = "native_cropper_cancel_button_icon_tint"
    const val EXTRA_CANCEL_BUTTON_ICON_SIZE = "native_cropper_cancel_button_icon_size"
    const val EXTRA_CANCEL_BUTTON_ICON_GAP = "native_cropper_cancel_button_icon_gap"
    const val EXTRA_CANCEL_BUTTON_PADDING_HORIZONTAL = "native_cropper_cancel_button_padding_horizontal"
    const val EXTRA_CANCEL_BUTTON_PADDING_VERTICAL = "native_cropper_cancel_button_padding_vertical"

    const val EXTRA_UPLOAD_BUTTON_CONTENT = "native_cropper_upload_button_content"
    const val EXTRA_UPLOAD_BUTTON_ICON_URI = "native_cropper_upload_button_icon_uri"
    const val EXTRA_UPLOAD_BUTTON_ICON_BASE64 = "native_cropper_upload_button_icon_base64"
    const val EXTRA_UPLOAD_BUTTON_ICON_TINT = "native_cropper_upload_button_icon_tint"
    const val EXTRA_UPLOAD_BUTTON_ICON_SIZE = "native_cropper_upload_button_icon_size"
    const val EXTRA_UPLOAD_BUTTON_ICON_GAP = "native_cropper_upload_button_icon_gap"
    const val EXTRA_UPLOAD_BUTTON_PADDING_HORIZONTAL = "native_cropper_upload_button_padding_horizontal"
    const val EXTRA_UPLOAD_BUTTON_PADDING_VERTICAL = "native_cropper_upload_button_padding_vertical"
    const val EXTRA_CANCEL_BACKGROUND_COLOR = "native_cropper_cancel_background_color"
    const val EXTRA_CANCEL_BORDER_COLOR = "native_cropper_cancel_border_color"
    const val EXTRA_CANCEL_BORDER_WIDTH = "native_cropper_cancel_border_width"
    const val EXTRA_CANCEL_FONT_SIZE = "native_cropper_cancel_font_size"
    const val EXTRA_CANCEL_FONT_FAMILY = "native_cropper_cancel_font_family"
    const val EXTRA_CANCEL_BORDER_RADIUS = "native_cropper_cancel_border_radius"
    const val EXTRA_UPLOAD_TEXT_COLOR = "native_cropper_upload_text_color"
    const val EXTRA_UPLOAD_BORDER_COLOR = "native_cropper_upload_border_color"
    const val EXTRA_UPLOAD_BORDER_WIDTH = "native_cropper_upload_border_width"
    const val EXTRA_UPLOAD_FONT_SIZE = "native_cropper_upload_font_size"
    const val EXTRA_UPLOAD_FONT_FAMILY = "native_cropper_upload_font_family"
    const val EXTRA_UPLOAD_BORDER_RADIUS = "native_cropper_upload_border_radius"

    const val EXTRA_CROP_GRID_ENABLED = "native_cropper_crop_grid_enabled"
    const val EXTRA_CROP_FRAME_COLOR = "native_cropper_crop_frame_color"
    const val EXTRA_CROP_GRID_COLOR = "native_cropper_crop_grid_color"

    // Simple in-memory caches for remote icon bitmaps.
    // Key includes URL + target size to avoid re-scaling every time.
    private val remoteBitmapCache = LruCache<String, Bitmap>(64)
  }

  private var headerHeightDp = 84
  private var bottomInsetDp = 120

  private var headerBackgroundColor = Color.WHITE
  private var headerTitleColor = Color.parseColor("#2D2D2D")
  private var headerTitleFontSizeSp = 22
  private var headerTitleFontFamily = ""
  private var headerAlignment: String = "left" // "left" | "center" | "right"
  private var headerPaddingHorizontalDp = 20
  private var headerPaddingTopDp = 28
  private var headerPaddingBottomDp = 20

  private var bottomBackgroundColor = Color.WHITE
  private var bottomPaddingHorizontalDp = 20
  private var bottomPaddingTopDp = 16
  private var bottomPaddingBottomDp = 24
  private var bottomButtonGapDp = 12
  private var bottomButtonHeightDp = 54
  private var bottomButtonLayout: String = "vertical"
  private var controlsPlacement: String = "bottom"
  private var topLeftControl: String = "cancel"
  private var topRightControl: String = "upload"
  private var footerButtonOrder: String = "uploadFirst"

  private var cancelButtonContent: String = "text"
  private var cancelButtonIconUri: String = ""
  private var cancelButtonIconBase64: String = ""
  private var cancelButtonIconTintColor: String = ""
  private var cancelButtonIconSizeDp: Int = 18
  private var cancelButtonIconGapDp: Int = 8
  private var cancelButtonPaddingHorizontalDp: Int = 12
  private var cancelButtonPaddingVerticalDp: Int = 0

  private var uploadButtonContent: String = "text"
  private var uploadButtonIconUri: String = ""
  private var uploadButtonIconBase64: String = ""
  private var uploadButtonIconTintColor: String = ""
  private var uploadButtonIconSizeDp: Int = 18
  private var uploadButtonIconGapDp: Int = 8
  private var uploadButtonPaddingHorizontalDp: Int = 12
  private var uploadButtonPaddingVerticalDp: Int = 0

  // Same layout vibe as demo screen:
  // - Primary: black pill (Upload)
  // - Secondary: light pill (Cancel)
  private var cancelTextColor = Color.parseColor("#111111")
  private var cancelBackgroundColor = Color.parseColor("#EFEFF4")
  private var cancelBorderColor = Color.parseColor("#EFEFF4")
  private var cancelBorderWidthDp = 0
  private var cancelFontSizeSp = 18
  private var cancelFontFamily = ""
  private var cancelCornerRadiusDp = 28

  private var uploadTextColor = Color.WHITE
  private var uploadBackgroundColor = Color.parseColor("#111111")
  private var uploadBorderColor = Color.parseColor("#111111")
  private var uploadBorderWidthDp = 0
  private var uploadFontSizeSp = 18
  private var uploadFontFamily = ""
  private var uploadCornerRadiusDp = 28

  private var isDarkTheme = false
  private var isCropActionInProgress = false
  private var drawUnderStatusBar = false
  private var statusBarStyle: String = "auto" // "dark" | "light" | "auto"
  private var statusBarColorString: String = "#FFFFFF"

  private var cropGridEnabled: Boolean = false
  private var cropFrameColorString: String = ""
  private var cropGridColorString: String = ""

  private var headerView: FrameLayout? = null
  private var bottomActionsView: LinearLayout? = null
  private var baseBottomActionsPaddingBottomPx: Int? = null

  private var baseCropTopMarginPx: Int? = null
  private var baseCropBottomMarginPx: Int? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    overridePendingTransition(0, 0)

    isDarkTheme = intent.getBooleanExtra(EXTRA_IS_DARK_THEME, false)
    drawUnderStatusBar = intent.getBooleanExtra(EXTRA_DRAW_UNDER_STATUS_BAR, false)
    statusBarStyle = intent.getStringExtra(EXTRA_STATUS_BAR_STYLE).orEmpty().ifBlank { "auto" }
    statusBarColorString = intent.getStringExtra(EXTRA_STATUS_BAR_COLOR).orEmpty().ifBlank { "#FFFFFF" }
    // We always go edge-to-edge and apply insets ourselves.
    // This is the most reliable behavior across Android versions (esp. Android 15+).
    WindowCompat.setDecorFitsSystemWindows(window, false)
    readStyleExtras()
    applyStatusBarColor()

    findViewById<Toolbar>(com.yalantis.ucrop.R.id.toolbar)?.visibility = View.GONE

    val root = findViewById<FrameLayout>(android.R.id.content) ?: return
    enforceOverlayStyle()
    addHeader(root)
    if (controlsPlacement != "top") {
      addBottomActions(root)
    }
    setupInsetsHandling(root)
  }

  override fun onResume() {
    super.onResume()
    // uCrop / system can re-apply UI flags on resume; re-assert our status bar settings.
    applyStatusBarColor()
    enforceOverlayStyle()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  private fun enforceOverlayStyle() {
    val overlay = findViewById<OverlayView>(com.yalantis.ucrop.R.id.view_overlay) ?: return
    overlay.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    overlay.setShowCropGrid(cropGridEnabled)
    overlay.setShowCropFrame(true)
    if (cropGridColorString.isNotBlank()) {
      overlay.setCropGridColor(parseColor(cropGridColorString, Color.WHITE))
    }
    if (cropFrameColorString.isNotBlank()) {
      overlay.setCropFrameColor(parseColor(cropFrameColorString, Color.WHITE))
    }
    overlay.invalidate()
  }

  private fun applyCropInsets(statusBarTopInsetPx: Int) {
    val cropFrame = findViewById<View>(com.yalantis.ucrop.R.id.ucrop_frame) ?: return
    val params = cropFrame.layoutParams as? ViewGroup.MarginLayoutParams ?: return
    if (baseCropTopMarginPx == null) baseCropTopMarginPx = params.topMargin
    if (baseCropBottomMarginPx == null) baseCropBottomMarginPx = params.bottomMargin

    params.topMargin = (baseCropTopMarginPx ?: 0) + dp(headerHeightDp) + statusBarTopInsetPx
    params.bottomMargin = (baseCropBottomMarginPx ?: 0) + dp(bottomInsetDp)
    cropFrame.layoutParams = params
  }

  private fun addHeader(root: FrameLayout) {
    val titleText = intent.getStringExtra(EXTRA_HEADER_TITLE).orEmpty().ifBlank { "Preview Image" }
    val header = FrameLayout(this).apply {
      setBackgroundColor(headerBackgroundColor)
      setPadding(
        dp(headerPaddingHorizontalDp),
        dp(headerPaddingTopDp),
        dp(headerPaddingHorizontalDp),
        dp(headerPaddingBottomDp),
      )
    }
    val headerLp = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      dp(headerHeightDp),
    ).apply { gravity = Gravity.TOP }
    root.addView(header, headerLp)
    headerView = header

    if (controlsPlacement == "top") {
      val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      }
      header.addView(row)

      val left = FrameLayout(this)
      val right = FrameLayout(this)
      val title = TextView(this).apply {
        text = titleText
        setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTitleFontSizeSp.toFloat())
        setTextColor(headerTitleColor)
        gravity = Gravity.CENTER
        typeface = getTypefaceByName(headerTitleFontFamily, Typeface.DEFAULT_BOLD)
      }

      row.addView(left, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
      row.addView(
        title,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
          gravity = Gravity.CENTER_VERTICAL
        },
      )
      row.addView(right, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))

      createHeaderControlView(topLeftControl)?.let { v ->
        left.addView(
          v,
          FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START),
        )
      }
      createHeaderControlView(topRightControl)?.let { v ->
        right.addView(
          v,
          FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END),
        )
      }
    } else {
      val title = TextView(this).apply {
        text = titleText
        setTextSize(TypedValue.COMPLEX_UNIT_SP, headerTitleFontSizeSp.toFloat())
        setTextColor(headerTitleColor)
        gravity = when (headerAlignment) {
          "center" -> Gravity.CENTER
          "right" -> Gravity.END or Gravity.CENTER_VERTICAL
          else -> Gravity.START or Gravity.CENTER_VERTICAL
        }
        typeface = getTypefaceByName(headerTitleFontFamily, Typeface.DEFAULT_BOLD)
      }
      header.addView(
        title,
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
          Gravity.CENTER_VERTICAL,
        ),
      )
    }
  }

  private fun createHeaderControlView(which: String): View? {
    val normalized = which.trim()
    if (normalized == "none" || normalized.isEmpty()) return null
    val heightDp = bottomButtonHeightDp.coerceIn(36, 44)
    return when (normalized) {
      "cancel" -> createActionView(
        text = intent.getStringExtra(EXTRA_CANCEL_TEXT).orEmpty().ifBlank { "Cancel" },
        content = cancelButtonContent,
        iconUri = cancelButtonIconUri,
        iconBase64 = cancelButtonIconBase64,
        iconTintColor = cancelButtonIconTintColor,
        iconSizeDp = cancelButtonIconSizeDp,
        iconGapDp = cancelButtonIconGapDp,
        contentPaddingHorizontalDp = cancelButtonPaddingHorizontalDp,
        contentPaddingVerticalDp = cancelButtonPaddingVerticalDp,
        textColor = cancelTextColor,
        fillColor = cancelBackgroundColor,
        strokeColor = cancelBorderColor,
        borderWidthDp = cancelBorderWidthDp,
        fontSizeSp = cancelFontSizeSp,
        fontFamily = cancelFontFamily,
        cornerRadiusDp = cancelCornerRadiusDp,
        fixedHeightDp = heightDp,
      ).apply {
        setOnClickListener { onBackPressedDispatcher.onBackPressed() }
      }
      "upload" -> createActionView(
        text = intent.getStringExtra(EXTRA_UPLOAD_TEXT).orEmpty().ifBlank { "Upload" },
        content = uploadButtonContent,
        iconUri = uploadButtonIconUri,
        iconBase64 = uploadButtonIconBase64,
        iconTintColor = uploadButtonIconTintColor,
        iconSizeDp = uploadButtonIconSizeDp,
        iconGapDp = uploadButtonIconGapDp,
        contentPaddingHorizontalDp = uploadButtonPaddingHorizontalDp,
        contentPaddingVerticalDp = uploadButtonPaddingVerticalDp,
        textColor = uploadTextColor,
        fillColor = uploadBackgroundColor,
        strokeColor = uploadBorderColor,
        borderWidthDp = uploadBorderWidthDp,
        fontSizeSp = uploadFontSizeSp,
        fontFamily = uploadFontFamily,
        cornerRadiusDp = uploadCornerRadiusDp,
        fixedHeightDp = heightDp,
      ).apply {
        setOnClickListener {
          if (isCropActionInProgress || isFinishing || isDestroyed) return@setOnClickListener
          isCropActionInProgress = true
          isEnabled = false
          triggerCropAction()
        }
      }
      else -> null
    }
  }

  private fun addBottomActions(root: FrameLayout) {
    val cancelText = intent.getStringExtra(EXTRA_CANCEL_TEXT).orEmpty().ifBlank { "Cancel" }
    val uploadText = intent.getStringExtra(EXTRA_UPLOAD_TEXT).orEmpty().ifBlank { "Upload" }

    val wrapper = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(bottomBackgroundColor)
      setPadding(
        dp(bottomPaddingHorizontalDp),
        dp(bottomPaddingTopDp),
        dp(bottomPaddingHorizontalDp),
        dp(bottomPaddingBottomDp),
      )
    }
    val wrapperLp = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { gravity = Gravity.BOTTOM }
    root.addView(wrapper, wrapperLp)
    bottomActionsView = wrapper
    baseBottomActionsPaddingBottomPx = wrapper.paddingBottom

    val cancelButton = createActionView(
      text = cancelText,
      content = cancelButtonContent,
      iconUri = cancelButtonIconUri,
      iconBase64 = cancelButtonIconBase64,
      iconTintColor = cancelButtonIconTintColor,
      iconSizeDp = cancelButtonIconSizeDp,
      iconGapDp = cancelButtonIconGapDp,
      contentPaddingHorizontalDp = cancelButtonPaddingHorizontalDp,
      contentPaddingVerticalDp = cancelButtonPaddingVerticalDp,
      textColor = cancelTextColor,
      fillColor = cancelBackgroundColor,
      strokeColor = cancelBorderColor,
      borderWidthDp = cancelBorderWidthDp,
      fontSizeSp = cancelFontSizeSp,
      fontFamily = cancelFontFamily,
      cornerRadiusDp = cancelCornerRadiusDp,
      fixedHeightDp = bottomButtonHeightDp,
    )
    val uploadButton = createActionView(
      text = uploadText,
      content = uploadButtonContent,
      iconUri = uploadButtonIconUri,
      iconBase64 = uploadButtonIconBase64,
      iconTintColor = uploadButtonIconTintColor,
      iconSizeDp = uploadButtonIconSizeDp,
      iconGapDp = uploadButtonIconGapDp,
      contentPaddingHorizontalDp = uploadButtonPaddingHorizontalDp,
      contentPaddingVerticalDp = uploadButtonPaddingVerticalDp,
      textColor = uploadTextColor,
      fillColor = uploadBackgroundColor,
      strokeColor = uploadBorderColor,
      borderWidthDp = uploadBorderWidthDp,
      fontSizeSp = uploadFontSizeSp,
      fontFamily = uploadFontFamily,
      cornerRadiusDp = uploadCornerRadiusDp,
      fixedHeightDp = bottomButtonHeightDp,
    )

    val firstIsCancel = footerButtonOrder == "cancelFirst"
    val first = if (firstIsCancel) cancelButton else uploadButton
    val second = if (firstIsCancel) uploadButton else cancelButton

    if (bottomButtonLayout == "horizontal") {
      val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
      }
      wrapper.addView(
        row,
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
      )

      // Horizontal: order is configurable.
      row.addView(first, LinearLayout.LayoutParams(0, dp(bottomButtonHeightDp), 1f))
      row.addView(
        second,
        LinearLayout.LayoutParams(0, dp(bottomButtonHeightDp), 1f).apply {
          marginStart = dp(bottomButtonGapDp)
        },
      )
    } else {
      // Vertical: order is configurable.
      wrapper.addView(
        first,
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          dp(bottomButtonHeightDp),
        ),
      )
      wrapper.addView(
        second,
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          dp(bottomButtonHeightDp),
        ).apply { topMargin = dp(bottomButtonGapDp) },
      )
    }

    cancelButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    uploadButton.setOnClickListener {
      if (isCropActionInProgress || isFinishing || isDestroyed) return@setOnClickListener
      isCropActionInProgress = true
      uploadButton.isEnabled = false
      triggerCropAction()
    }
  }

  private fun createActionView(
    text: String,
    content: String,
    iconUri: String,
    iconBase64: String,
    iconTintColor: String,
    iconSizeDp: Int,
    iconGapDp: Int,
    contentPaddingHorizontalDp: Int,
    contentPaddingVerticalDp: Int,
    textColor: Int,
    fillColor: Int,
    strokeColor: Int,
    borderWidthDp: Int,
    fontSizeSp: Int,
    fontFamily: String,
    cornerRadiusDp: Int,
    fixedHeightDp: Int,
  ): View {
    val normalizedContent = content.trim()
    val resolvedContent = when (normalizedContent) {
      "icon", "text" -> normalizedContent
      "iconText", "icon+text" -> "iconText"
      "textIcon", "TextIcon", "text+icon" -> "textIcon"
      else -> "text"
    }
    val hasText = resolvedContent != "icon"
    val wantsIcon = resolvedContent != "text"

    val container = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
      isClickable = true
      isFocusable = true
      setPadding(
        dp(contentPaddingHorizontalDp.coerceAtLeast(0)),
        dp(contentPaddingVerticalDp.coerceAtLeast(0)),
        dp(contentPaddingHorizontalDp.coerceAtLeast(0)),
        dp(contentPaddingVerticalDp.coerceAtLeast(0)),
      )
      minimumHeight = dp(fixedHeightDp.coerceAtLeast(36))
      background = GradientDrawable().apply {
        setColor(fillColor)
        cornerRadius = dp(cornerRadiusDp.coerceAtLeast(0)).toFloat()
        setStroke(dp(borderWidthDp.coerceAtLeast(0)), strokeColor)
      }
    }

    val labelView = TextView(this).apply {
      this.text = text
      gravity = Gravity.CENTER
      setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp.toFloat())
      setTextColor(textColor)
      typeface = getTypefaceByName(fontFamily, Typeface.DEFAULT_BOLD)
    }

    val iconView = ImageView(this).apply {
      scaleType = ImageView.ScaleType.FIT_CENTER
      if (iconTintColor.isNotBlank()) {
        setColorFilter(parseColor(iconTintColor, textColor))
      } else {
        clearColorFilter()
      }
      val size = dp(iconSizeDp.coerceIn(12, 64))
      layoutParams = LinearLayout.LayoutParams(size, size)
    }

    val iconInputsPresent = iconUri.trim().isNotEmpty() || iconBase64.trim().isNotEmpty()
    val shouldRenderIconSlot = wantsIcon && iconInputsPresent
    if (shouldRenderIconSlot) {
      setIconImageAsync(iconView, iconUri, iconBase64)
    }

    val gap = dp(iconGapDp.coerceIn(0, 48))
    fun addIcon() {
      if (!shouldRenderIconSlot) return
      container.addView(iconView, iconView.layoutParams)
    }
    fun addText(withStartGap: Boolean) {
      if (!hasText) return
      val lp = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
      ).apply {
        if (container.childCount > 0 && withStartGap && gap > 0) {
          marginStart = gap
        }
      }
      container.addView(labelView, lp)
    }

    when (resolvedContent) {
      "icon" -> addIcon()
      "text" -> addText(withStartGap = false)
      "textIcon" -> {
        addText(withStartGap = false)
        if (shouldRenderIconSlot) {
          val lp = LinearLayout.LayoutParams(iconView.layoutParams as LinearLayout.LayoutParams).apply {
            marginStart = gap
          }
          container.addView(iconView, lp)
        }
      }
      else /* iconText */ -> {
        addIcon()
        addText(withStartGap = true)
      }
    }

    if (container.childCount == 0) {
      container.addView(labelView)
    }
    return container
  }

  private fun setIconImageAsync(target: ImageView, iconUri: String, iconBase64: String) {
    val targetW = (target.layoutParams?.width ?: 0).coerceAtLeast(1)
    val targetH = (target.layoutParams?.height ?: 0).coerceAtLeast(1)

    val base64 = iconBase64.trim()
    if (base64.isNotEmpty()) {
      val clean = base64.substringAfter("base64,", base64)
      val bitmap = runCatching {
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        decodeSvgBytesToBitmap(bytes, targetW, targetH)
          ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      }.getOrNull()
      if (bitmap != null) target.setImageBitmap(bitmap)
      return
    }

    val uriString = iconUri.trim()
    if (uriString.isEmpty()) return

    if (uriString.startsWith("data:image/svg+xml", ignoreCase = true)) {
      val svgBitmap = decodeSvgDataUriToBitmap(uriString, targetW, targetH)
      if (svgBitmap != null) target.setImageBitmap(svgBitmap)
      return
    }

    val scheme = runCatching { Uri.parse(uriString).scheme.orEmpty() }.getOrDefault("")
    if (scheme == "http" || scheme == "https") {
      val cacheKey = "$uriString|$targetW|$targetH"
      val cached = remoteBitmapCache.get(cacheKey)
      if (cached != null) {
        target.setImageBitmap(cached)
        return
      }
      // Load remote icons asynchronously (best-effort).
      Thread {
        val bmp = if (looksLikeSvgUri(uriString)) {
          downloadSvgBitmap(uriString, targetW, targetH)
        } else {
          downloadBitmap(uriString)
        }
        if (bmp != null && !isFinishing && !isDestroyed) {
          remoteBitmapCache.put(cacheKey, bmp)
          runOnUiThread { target.setImageBitmap(bmp) }
        }
      }.start()
      return
    }

    val bitmap = runCatching {
      val uri = Uri.parse(uriString)
      when (uri.scheme) {
        "content", "file" -> {
          contentResolver.openInputStream(uri)?.use { stream ->
            if (looksLikeSvgUri(uriString)) {
              decodeSvgStreamToBitmap(stream, targetW, targetH)
            } else {
              BitmapFactory.decodeStream(stream)
            }
          }
        }
        else -> {
          if (!uriString.startsWith("/")) return@runCatching null
          if (looksLikeSvgUri(uriString)) {
            contentResolver.openInputStream(Uri.fromFile(java.io.File(uriString)))?.use { stream ->
              decodeSvgStreamToBitmap(stream, targetW, targetH)
            }
          } else {
            BitmapFactory.decodeFile(uriString)
          }
        }
      }
    }.getOrNull()
    if (bitmap != null) target.setImageBitmap(bitmap)
  }

  private fun looksLikeSvgUri(uri: String): Boolean {
    val s = uri.trim().lowercase()
    return s.endsWith(".svg") || s.contains(".svg?") || s.contains(".svg#")
  }

  private fun decodeSvgBytesToBitmap(bytes: ByteArray, w: Int, h: Int): Bitmap? {
    val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return null
    if (!text.contains("<svg", ignoreCase = true)) return null
    val svg = runCatching { SVG.getFromString(text) }.getOrNull() ?: return null
    return renderSvgToBitmap(svg, w, h)
  }

  private fun decodeSvgStreamToBitmap(stream: InputStream, w: Int, h: Int): Bitmap? {
    val svg = runCatching { SVG.getFromInputStream(stream) }.getOrNull() ?: return null
    return renderSvgToBitmap(svg, w, h)
  }

  private fun decodeSvgDataUriToBitmap(dataUri: String, w: Int, h: Int): Bitmap? {
    val commaIdx = dataUri.indexOf(',')
    if (commaIdx < 0) return null
    val meta = dataUri.substring(0, commaIdx)
    val data = dataUri.substring(commaIdx + 1)
    val isBase64 = meta.contains(";base64", ignoreCase = true)
    val svgText = if (isBase64) {
      runCatching {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        bytes.toString(Charsets.UTF_8)
      }.getOrNull()
    } else {
      runCatching { URLDecoder.decode(data, StandardCharsets.UTF_8.name()) }.getOrNull()
    } ?: return null

    val svg = runCatching { SVG.getFromString(svgText) }.getOrNull() ?: return null
    return renderSvgToBitmap(svg, w, h)
  }

  private fun renderSvgToBitmap(svg: SVG, w: Int, h: Int): Bitmap? {
    val picture = runCatching { svg.renderToPicture() }.getOrNull() ?: return null
    val pictureW =
      (picture.width.takeIf { it > 0 }?.toFloat()
        ?: svg.documentViewBox?.width()
        ?: w.toFloat())
        .coerceAtLeast(1f)
    val pictureH =
      (picture.height.takeIf { it > 0 }?.toFloat()
        ?: svg.documentViewBox?.height()
        ?: h.toFloat())
        .coerceAtLeast(1f)

    val scale = kotlin.math.min(w / pictureW, h / pictureH).coerceAtLeast(0.01f)
    val dx = (w - (pictureW * scale)) / 2f
    val dy = (h - (pictureH * scale)) / 2f

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.translate(dx, dy)
    canvas.scale(scale, scale)
    canvas.drawPicture(picture)
    return bitmap
  }

  private fun downloadSvgBitmap(urlString: String, w: Int, h: Int): Bitmap? {
    return runCatching {
      val url = URL(urlString)
      val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 5000
        readTimeout = 7000
        instanceFollowRedirects = true
      }
      connection.connect()
      if (connection.responseCode !in 200..299) return@runCatching null
      connection.inputStream.use { input ->
        BufferedInputStream(input).use { bis ->
          decodeSvgStreamToBitmap(bis, w, h)
        }
      }
    }.getOrNull()
  }

  private fun downloadBitmap(urlString: String): android.graphics.Bitmap? {
    return runCatching {
      val url = URL(urlString)
      val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 5000
        readTimeout = 7000
        instanceFollowRedirects = true
      }
      connection.connect()
      if (connection.responseCode !in 200..299) return@runCatching null
      connection.inputStream.use { input ->
        BufferedInputStream(input).use { bis ->
          BitmapFactory.decodeStream(bis)
        }
      }
    }.getOrNull()
  }

  private fun triggerCropAction() {
    val toolbar = findViewById<Toolbar>(com.yalantis.ucrop.R.id.toolbar) ?: return
    val menuItem = toolbar.menu?.findItem(com.yalantis.ucrop.R.id.menu_crop) ?: return
    onOptionsItemSelected(menuItem)
  }

  private fun parseColor(color: String, fallback: Int): Int {
    val normalized = normalizeColorString(color)
    return runCatching { Color.parseColor(normalized) }.getOrElse { fallback }
  }

  private fun normalizeColorString(raw: String): String {
    val s = raw.trim()
    if (!s.startsWith("#")) return s
    // Expand shorthand hex colors:
    // - #RGB -> #RRGGBB
    // - #ARGB -> #AARRGGBB
    return when (s.length) {
      4 -> {
        val r = s[1]
        val g = s[2]
        val b = s[3]
        "#$r$r$g$g$b$b"
      }
      5 -> {
        val a = s[1]
        val r = s[2]
        val g = s[3]
        val b = s[4]
        "#$a$a$r$r$g$g$b$b"
      }
      else -> s
    }
  }

  private fun applyStatusBarColor() {
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.statusBarColor = parseColor(statusBarColorString, Color.WHITE)
    if (Build.VERSION.SDK_INT >= 29) {
      // Prevent the system from applying a contrast scrim that can "wash out" custom colors.
      window.isStatusBarContrastEnforced = false
    }
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.isAppearanceLightStatusBars = when (statusBarStyle) {
      "dark" -> true
      "light" -> false
      else -> !isDarkTheme
    }
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  }

  private fun readStyleExtras() {
    // Read placement early because it affects sensible header defaults.
    controlsPlacement = intent.getStringExtra(EXTRA_CONTROLS_PLACEMENT).orEmpty().ifBlank { "bottom" }
    topLeftControl = intent.getStringExtra(EXTRA_TOP_LEFT_CONTROL).orEmpty().ifBlank { "cancel" }
    topRightControl = intent.getStringExtra(EXTRA_TOP_RIGHT_CONTROL).orEmpty().ifBlank { "upload" }

    headerBackgroundColor = parseColor(
      intent.getStringExtra(EXTRA_HEADER_BACKGROUND_COLOR).orEmpty().ifBlank { if (isDarkTheme) "#0B0B0F" else "#FFFFFF" },
      if (isDarkTheme) Color.parseColor("#0B0B0F") else Color.WHITE,
    )
    headerTitleColor = parseColor(
      intent.getStringExtra(EXTRA_HEADER_TITLE_COLOR).orEmpty().ifBlank { if (isDarkTheme) "#F5F5F7" else "#111111" },
      if (isDarkTheme) Color.parseColor("#F5F5F7") else Color.parseColor("#111111"),
    )
    headerTitleFontSizeSp = intent.getIntExtra(EXTRA_HEADER_TITLE_FONT_SIZE, 22).coerceIn(10, 48)
    headerTitleFontFamily = intent.getStringExtra(EXTRA_HEADER_TITLE_FONT_FAMILY).orEmpty()
    headerAlignment = intent.getStringExtra(EXTRA_HEADER_ALIGNMENT).orEmpty().ifBlank { "left" }.let { raw ->
      if (raw == "center" || raw == "right" || raw == "left") raw else "left"
    }
    headerHeightDp = intent.getIntExtra(EXTRA_HEADER_HEIGHT, 84).coerceIn(48, 240)
    headerPaddingHorizontalDp = intent.getIntExtra(EXTRA_HEADER_PADDING_HORIZONTAL, 20).coerceAtLeast(0)
    val defaultHeaderPaddingTop = if (controlsPlacement == "top") 0 else 28
    headerPaddingTopDp = intent.getIntExtra(EXTRA_HEADER_PADDING_TOP, defaultHeaderPaddingTop).coerceAtLeast(0)
    headerPaddingBottomDp = intent.getIntExtra(EXTRA_HEADER_PADDING_BOTTOM, 20).coerceAtLeast(0)

    bottomBackgroundColor = parseColor(
      intent.getStringExtra(EXTRA_BOTTOM_BACKGROUND_COLOR).orEmpty().ifBlank { if (isDarkTheme) "#121212" else "#FFFFFF" },
      if (isDarkTheme) Color.parseColor("#121212") else Color.WHITE,
    )
    bottomPaddingHorizontalDp = intent.getIntExtra(EXTRA_BOTTOM_PADDING_HORIZONTAL, 20).coerceAtLeast(0)
    bottomPaddingTopDp = intent.getIntExtra(EXTRA_BOTTOM_PADDING_TOP, 16).coerceAtLeast(0)
    bottomPaddingBottomDp = intent.getIntExtra(EXTRA_BOTTOM_PADDING_BOTTOM, 24).coerceAtLeast(0)
    bottomButtonGapDp = intent.getIntExtra(EXTRA_BOTTOM_BUTTON_GAP, 12).coerceAtLeast(0)
    bottomButtonHeightDp = intent.getIntExtra(EXTRA_BOTTOM_BUTTON_HEIGHT, 54).coerceAtLeast(1)
    bottomButtonLayout = intent.getStringExtra(EXTRA_BOTTOM_BUTTON_LAYOUT).orEmpty().ifBlank { "vertical" }
    footerButtonOrder = intent.getStringExtra(EXTRA_FOOTER_BUTTON_ORDER).orEmpty().ifBlank { "uploadFirst" }

    cancelButtonContent = intent.getStringExtra(EXTRA_CANCEL_BUTTON_CONTENT).orEmpty().ifBlank { "text" }
    cancelButtonIconUri = intent.getStringExtra(EXTRA_CANCEL_BUTTON_ICON_URI).orEmpty()
    cancelButtonIconBase64 = intent.getStringExtra(EXTRA_CANCEL_BUTTON_ICON_BASE64).orEmpty()
    cancelButtonIconTintColor = intent.getStringExtra(EXTRA_CANCEL_BUTTON_ICON_TINT).orEmpty()
    cancelButtonIconSizeDp = intent.getIntExtra(EXTRA_CANCEL_BUTTON_ICON_SIZE, 18).coerceIn(12, 64)
    cancelButtonIconGapDp = intent.getIntExtra(EXTRA_CANCEL_BUTTON_ICON_GAP, 8).coerceIn(0, 48)
    cancelButtonPaddingHorizontalDp = intent.getIntExtra(EXTRA_CANCEL_BUTTON_PADDING_HORIZONTAL, 12).coerceAtLeast(0)
    cancelButtonPaddingVerticalDp = intent.getIntExtra(EXTRA_CANCEL_BUTTON_PADDING_VERTICAL, 0).coerceAtLeast(0)

    uploadButtonContent = intent.getStringExtra(EXTRA_UPLOAD_BUTTON_CONTENT).orEmpty().ifBlank { "text" }
    uploadButtonIconUri = intent.getStringExtra(EXTRA_UPLOAD_BUTTON_ICON_URI).orEmpty()
    uploadButtonIconBase64 = intent.getStringExtra(EXTRA_UPLOAD_BUTTON_ICON_BASE64).orEmpty()
    uploadButtonIconTintColor = intent.getStringExtra(EXTRA_UPLOAD_BUTTON_ICON_TINT).orEmpty()
    uploadButtonIconSizeDp = intent.getIntExtra(EXTRA_UPLOAD_BUTTON_ICON_SIZE, 18).coerceIn(12, 64)
    uploadButtonIconGapDp = intent.getIntExtra(EXTRA_UPLOAD_BUTTON_ICON_GAP, 8).coerceIn(0, 48)
    uploadButtonPaddingHorizontalDp = intent.getIntExtra(EXTRA_UPLOAD_BUTTON_PADDING_HORIZONTAL, 12).coerceAtLeast(0)
    uploadButtonPaddingVerticalDp = intent.getIntExtra(EXTRA_UPLOAD_BUTTON_PADDING_VERTICAL, 0).coerceAtLeast(0)

    bottomInsetDp = if (controlsPlacement == "top") {
      0
    } else if (bottomButtonLayout == "horizontal") {
      (bottomPaddingTopDp + bottomPaddingBottomDp + bottomButtonHeightDp).coerceAtLeast(100)
    } else {
      (bottomPaddingTopDp + bottomPaddingBottomDp + bottomButtonGapDp + (bottomButtonHeightDp * 2))
        .coerceAtLeast(120)
    }

    cancelTextColor = parseColor(intent.getStringExtra(EXTRA_CANCEL_COLOR).orEmpty().ifBlank { "#111111" }, Color.parseColor("#111111"))
    cancelBackgroundColor = parseColor(intent.getStringExtra(EXTRA_CANCEL_BACKGROUND_COLOR).orEmpty().ifBlank { "#EFEFF4" }, Color.parseColor("#EFEFF4"))
    cancelBorderColor = parseColor(intent.getStringExtra(EXTRA_CANCEL_BORDER_COLOR).orEmpty().ifBlank { "#EFEFF4" }, Color.parseColor("#EFEFF4"))
    cancelBorderWidthDp = intent.getIntExtra(EXTRA_CANCEL_BORDER_WIDTH, 0).coerceAtLeast(0)
    cancelFontSizeSp = intent.getIntExtra(EXTRA_CANCEL_FONT_SIZE, 18).coerceIn(10, 40)
    cancelFontFamily = intent.getStringExtra(EXTRA_CANCEL_FONT_FAMILY).orEmpty()
    cancelCornerRadiusDp = intent.getIntExtra(EXTRA_CANCEL_BORDER_RADIUS, 28).coerceAtLeast(0)

    uploadTextColor = parseColor(intent.getStringExtra(EXTRA_UPLOAD_TEXT_COLOR).orEmpty().ifBlank { "#FFFFFF" }, Color.WHITE)
    uploadBackgroundColor = parseColor(intent.getStringExtra(EXTRA_UPLOAD_COLOR).orEmpty().ifBlank { "#111111" }, Color.parseColor("#111111"))
    uploadBorderColor = parseColor(intent.getStringExtra(EXTRA_UPLOAD_BORDER_COLOR).orEmpty().ifBlank { "#111111" }, Color.parseColor("#111111"))
    uploadBorderWidthDp = intent.getIntExtra(EXTRA_UPLOAD_BORDER_WIDTH, 0).coerceAtLeast(0)
    uploadFontSizeSp = intent.getIntExtra(EXTRA_UPLOAD_FONT_SIZE, 18).coerceIn(10, 40)
    uploadFontFamily = intent.getStringExtra(EXTRA_UPLOAD_FONT_FAMILY).orEmpty()
    uploadCornerRadiusDp = intent.getIntExtra(EXTRA_UPLOAD_BORDER_RADIUS, 28).coerceAtLeast(0)

    cropGridEnabled = intent.getBooleanExtra(EXTRA_CROP_GRID_ENABLED, false)
    cropFrameColorString = intent.getStringExtra(EXTRA_CROP_FRAME_COLOR).orEmpty()
    cropGridColorString = intent.getStringExtra(EXTRA_CROP_GRID_COLOR).orEmpty()
  }

  private fun getTypefaceByName(fontFamily: String, fallback: Typeface): Typeface {
    val cleaned = fontFamily.trim()
    if (cleaned.isEmpty()) return fallback
    val fontResId = resources.getIdentifier(cleaned, "font", packageName)
    if (fontResId != 0) {
      return ResourcesCompat.getFont(this, fontResId) ?: fallback
    }
    // Fallback to system font lookup by family name (best-effort).
    return Typeface.create(cleaned, fallback.style) ?: fallback
  }

  private fun dp(value: Int): Int {
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value.toFloat(),
      resources.displayMetrics,
    ).toInt()
  }

  private fun setupInsetsHandling(root: FrameLayout) {
    val decor = window.decorView
    ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
      val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val topInsetPx = maxOf(sysBars.top, getStatusBarHeightPx())
      applySystemInsets(
        statusBarTopInsetPx = topInsetPx,
        navigationBarBottomInsetPx = sysBars.bottom,
      )
      insets
    }
    ViewCompat.requestApplyInsets(decor)
  }

  private fun applySystemInsets(
    statusBarTopInsetPx: Int,
    navigationBarBottomInsetPx: Int,
  ) {
    headerView?.let { header ->
      val lp = header.layoutParams as? FrameLayout.LayoutParams
      if (lp != null) {
        val desiredTopMargin = if (drawUnderStatusBar) 0 else statusBarTopInsetPx
        val desiredHeight = if (drawUnderStatusBar) dp(headerHeightDp) + statusBarTopInsetPx else dp(headerHeightDp)
        var changed = false
        if (lp.topMargin != desiredTopMargin) {
          lp.topMargin = desiredTopMargin
          changed = true
        }
        if (lp.height != desiredHeight) {
          lp.height = desiredHeight
          changed = true
        }
        if (changed) header.layoutParams = lp
      }

      header.setPadding(
        dp(headerPaddingHorizontalDp),
        dp(headerPaddingTopDp) + if (drawUnderStatusBar) statusBarTopInsetPx else 0,
        dp(headerPaddingHorizontalDp),
        dp(headerPaddingBottomDp),
      )
    }

    bottomActionsView?.let { wrapper ->
      if (baseBottomActionsPaddingBottomPx == null) baseBottomActionsPaddingBottomPx = wrapper.paddingBottom
      wrapper.setPadding(
        wrapper.paddingLeft,
        wrapper.paddingTop,
        wrapper.paddingRight,
        (baseBottomActionsPaddingBottomPx ?: wrapper.paddingBottom) + navigationBarBottomInsetPx,
      )
    }

    // Crop frame always needs to sit below: status bar + header height.
    applyCropInsets(statusBarTopInsetPx)
  }

  private fun getStatusBarHeightPx(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId <= 0) return 0
    return resources.getDimensionPixelSize(resourceId).coerceAtLeast(0)
  }
}

