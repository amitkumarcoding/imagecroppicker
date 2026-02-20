//
//  NativeImageCropperModule.swift
//
import AVFoundation
import CoreText
import Foundation
import ImageIO
import PhotosUI
import React
import TOCropViewController
import UniformTypeIdentifiers
import UIKit

@objc(NativeImageCropperModule)
class NativeImageCropperModule: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate, PHPickerViewControllerDelegate, TOCropViewControllerDelegate {
  private let ePickerCancelled = "E_PICKER_CANCELLED"
  private let ePickerCancelledMsg = "User cancelled image selection"
  private let ePickerError = "E_PICKER_ERROR"
  private let ePermissionMissing = "E_PERMISSION_MISSING"
  private let eNoImageDataFound = "E_NO_IMAGE_DATA_FOUND"
  private let eNoAppAvailable = "E_NO_APP_AVAILABLE"
  private let sourceCamera = "camera"

  private var pendingResolve: RCTPromiseResolveBlock?
  private var pendingReject: RCTPromiseRejectBlock?
  private var cropperConfig = CropperConfig()
  private var isFlowInProgress = false

  deinit {
    cleanupFlow()
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    true
  }

  @objc
  func openImagePreview(
    _ options: NSDictionary,
    resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      guard !self.isFlowInProgress else {
        reject(self.ePickerError, "Image picker is already in progress", nil)
        return
      }
      guard let presenter = self.topViewController() else {
        reject(self.ePickerError, "Unable to find visible view controller", nil)
        return
      }

      self.isFlowInProgress = true
      self.pendingResolve = resolve
      self.pendingReject = reject
      self.cropperConfig = CropperConfig.parse(from: options)

      if self.cropperConfig.pickerSource == self.sourceCamera {
        self.launchCamera(from: presenter)
      } else {
        self.launchGallery(from: presenter)
      }
    }
  }

  private func launchCamera(from presenter: UIViewController) {
    guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
      rejectPending(code: eNoAppAvailable, message: "Camera is not available on this device")
      return
    }

    let cameraPermission = AVCaptureDevice.authorizationStatus(for: .video)
    if cameraPermission == .denied || cameraPermission == .restricted {
      rejectPending(code: ePermissionMissing, message: "Camera permission is not granted")
      return
    }
    if cameraPermission == .notDetermined {
      AVCaptureDevice.requestAccess(for: .video) { [weak self, weak presenter] granted in
        DispatchQueue.main.async {
          guard let self, let presenter else { return }
          if !granted {
            self.rejectPending(code: self.ePermissionMissing, message: "Camera permission is not granted")
            return
          }
          self.launchCamera(from: presenter)
        }
      }
      return
    }

    let picker = UIImagePickerController()
    picker.delegate = self
    picker.sourceType = .camera
    picker.mediaTypes = ["public.image"]
    picker.modalPresentationStyle = .fullScreen
    presenter.present(picker, animated: true)
  }

  private func launchGallery(from presenter: UIViewController) {
    if #available(iOS 14.0, *) {
      var configuration = PHPickerConfiguration()
      configuration.filter = .images
      configuration.selectionLimit = 1

      let picker = PHPickerViewController(configuration: configuration)
      picker.delegate = self
      picker.modalPresentationStyle = .fullScreen
      presenter.present(picker, animated: true)
      return
    }

    let picker = UIImagePickerController()
    picker.delegate = self
    picker.sourceType = .photoLibrary
    picker.mediaTypes = ["public.image"]
    picker.modalPresentationStyle = .fullScreen
    // iOS 13 and below uses UIImagePickerController which requires photo library permission.
    if #available(iOS 14.0, *) {
      presenter.present(picker, animated: true)
      return
    }
    let status = PHPhotoLibrary.authorizationStatus()
    if status == .denied || status == .restricted {
      rejectPending(code: ePermissionMissing, message: "Photo library permission is not granted")
      return
    }
    if status == .notDetermined {
      PHPhotoLibrary.requestAuthorization { [weak self, weak presenter] newStatus in
        DispatchQueue.main.async {
          guard let self, let presenter else { return }
          if newStatus == .authorized {
            presenter.present(picker, animated: true)
          } else {
            self.rejectPending(code: self.ePermissionMissing, message: "Photo library permission is not granted")
          }
        }
      }
      return
    }
    presenter.present(picker, animated: true)
  }

  private func presentCropper(for image: UIImage) {
    guard let presenter = topViewController() else {
      rejectPending(code: ePickerError, message: "Unable to show cropper screen")
      return
    }
    guard let preparedImage = prepareImageForCropper(image) else {
      rejectPending(code: eNoImageDataFound, message: "Selected image is invalid for cropping")
      return
    }

    let cropper = NativeImageCropperViewController(
      image: preparedImage,
      config: cropperConfig,
      colorParser: { [weak self] hex, fallback in
        self?.parseColor(hex, fallback: fallback) ?? fallback
      }
    )
    cropper.delegate = self
    cropper.modalPresentationStyle = .fullScreen
    if presenter.isBeingDismissed || presenter.isBeingPresented {
      rejectPending(code: ePickerError, message: "Unable to present cropper right now")
      return
    }
    presenter.present(cropper, animated: false)
  }

  func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    picker.dismiss(animated: true) { [weak self] in
      self?.rejectPending(code: self?.ePickerCancelled ?? "E_PICKER_CANCELLED", message: self?.ePickerCancelledMsg ?? "User cancelled image selection")
    }
  }

  func imagePickerController(
    _ picker: UIImagePickerController,
    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
  ) {
    let selectedImage = (info[.editedImage] ?? info[.originalImage]) as? UIImage
    picker.dismiss(animated: true) { [weak self] in
      guard let self else { return }
      guard let image = selectedImage else {
        self.rejectPending(code: self.eNoImageDataFound, message: "Cannot resolve selected image")
        return
      }
      self.presentCropper(for: image)
    }
  }

  @available(iOS 14.0, *)
  func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
    picker.dismiss(animated: true) { [weak self] in
      guard let self else { return }
      guard let provider = results.first?.itemProvider else {
        self.rejectPending(code: self.ePickerCancelled, message: self.ePickerCancelledMsg)
        return
      }
      guard provider.canLoadObject(ofClass: UIImage.self) else {
        self.rejectPending(code: self.eNoImageDataFound, message: "Selected item is not an image")
        return
      }

      if provider.hasItemConformingToTypeIdentifier(UTType.image.identifier) {
        provider.loadDataRepresentation(forTypeIdentifier: UTType.image.identifier) { [weak self] data, error in
          guard let self else { return }
          if let error {
            self.rejectPending(code: self.ePickerError, message: error.localizedDescription)
            return
          }
          guard let data, let selectedImage = UIImage(data: data) else {
            self.rejectPending(code: self.eNoImageDataFound, message: "Cannot decode selected image")
            return
          }
          DispatchQueue.main.async {
            self.presentCropper(for: selectedImage)
          }
        }
        return
      }

      provider.loadObject(ofClass: UIImage.self) { [weak self] image, error in
        guard let self else { return }
        if let error {
          self.rejectPending(code: self.ePickerError, message: error.localizedDescription)
          return
        }
        guard let selectedImage = image as? UIImage else {
          self.rejectPending(code: self.eNoImageDataFound, message: "Cannot resolve selected image")
          return
        }
        DispatchQueue.main.async {
          self.presentCropper(for: selectedImage)
        }
      }
    }
  }

  func cropViewController(_ cropViewController: TOCropViewController, didFinishCancelled cancelled: Bool) {
    cropViewController.dismiss(animated: true) { [weak self] in
      self?.rejectPending(code: self?.ePickerCancelled ?? "E_PICKER_CANCELLED", message: self?.ePickerCancelledMsg ?? "User cancelled image selection")
    }
  }

  func cropViewController(
    _ cropViewController: TOCropViewController,
    didCropTo image: UIImage,
    with cropRect: CGRect,
    angle: Int
  ) {
    cropViewController.dismiss(animated: true) { [weak self] in
      guard let self else { return }
      self.encodeAndResolve(image: image)
    }
  }

  private func encodeAndResolve(image: UIImage) {
    DispatchQueue.global(qos: .userInitiated).async { [weak self] in
      guard let self else { return }
      guard self.isFlowInProgress else { return }
      autoreleasepool {
        guard let (imageData, fileExt) = self.encodeImage(image: image) else {
          self.rejectPending(code: self.eNoImageDataFound, message: "Cannot encode selected image")
          return
        }
        let tempPath = self.writeTempImage(data: imageData, fileExt: fileExt)
        var payload: [String: Any] = [
          "path": tempPath ?? "",
        ]
        if self.cropperConfig.includeBase64 {
          payload["base64"] = imageData.base64EncodedString()
        }
        self.resolvePending(payload: payload)
      }
    }
  }

  private func encodeImage(image: UIImage) -> (Data, String)? {
    let format = cropperConfig.compressFormat
    let quality = min(max(cropperConfig.compressQuality, 0.0), 1.0)

    if format == "png" {
      guard let data = image.pngData() else { return nil }
      return (data, "png")
    }

    if format == "webp" {
      // Best-effort WebP encoding (iOS 14+). Falls back to JPEG if not available.
      if #available(iOS 14.0, *), let cg = image.cgImage {
        let type = UTType("org.webmproject.webp")
        if let type {
          let out = NSMutableData()
          if let dest = CGImageDestinationCreateWithData(out, type.identifier as CFString, 1, nil) {
            let options: [CFString: Any] = [
              kCGImageDestinationLossyCompressionQuality: quality,
            ]
            CGImageDestinationAddImage(dest, cg, options as CFDictionary)
            if CGImageDestinationFinalize(dest) {
              return (out as Data, "webp")
            }
          }
        }
      }
      // fallback
      guard let data = image.jpegData(compressionQuality: CGFloat(quality)) else { return nil }
      return (data, "jpg")
    }

    // jpeg (default)
    guard let data = image.jpegData(compressionQuality: CGFloat(quality)) else { return nil }
    return (data, "jpg")
  }

  private func writeTempImage(data: Data, fileExt: String) -> String? {
    do {
      let directory = FileManager.default.temporaryDirectory
      let ext = fileExt.isEmpty ? "jpg" : fileExt
      let url = directory.appendingPathComponent("native-cropped-\(UUID().uuidString).\(ext)")
      try data.write(to: url, options: .atomic)
      return url.path
    } catch {
      return nil
    }
  }

  private func parseColor(_ value: String, fallback: UIColor) -> UIColor {
    guard value.hasPrefix("#") else { return fallback }
    var hex = String(value.dropFirst())
    if hex.count == 6 {
      hex = "FF" + hex
    }
    guard hex.count == 8, let intVal = UInt64(hex, radix: 16) else {
      return fallback
    }
    let a = CGFloat((intVal >> 24) & 0xFF) / 255.0
    let r = CGFloat((intVal >> 16) & 0xFF) / 255.0
    let g = CGFloat((intVal >> 8) & 0xFF) / 255.0
    let b = CGFloat(intVal & 0xFF) / 255.0
    return UIColor(red: r, green: g, blue: b, alpha: a)
  }

  private func prepareImageForCropper(_ image: UIImage) -> UIImage? {
    guard image.size.width > 0, image.size.height > 0 else {
      return nil
    }
    if image.cgImage != nil {
      return image
    }
    let format = UIGraphicsImageRendererFormat.default()
    format.scale = image.scale
    let renderer = UIGraphicsImageRenderer(size: image.size, format: format)
    let normalized = renderer.image { _ in
      image.draw(in: CGRect(origin: .zero, size: image.size))
    }
    guard normalized.size.width > 0, normalized.size.height > 0 else {
      return nil
    }
    return normalized
  }

  private func topViewController() -> UIViewController? {
    let keyWindow: UIWindow? = {
      if #available(iOS 13.0, *) {
        let scenes = UIApplication.shared.connectedScenes
          .compactMap { $0 as? UIWindowScene }
          .filter { $0.activationState == .foregroundActive || $0.activationState == .foregroundInactive }
        for scene in scenes {
          for window in scene.windows where window.isKeyWindow {
            return window
          }
        }
        return scenes.first?.windows.first
      }
      // iOS 12 and below
      return UIApplication.shared.keyWindow
    }()
    var controller = keyWindow?.rootViewController
    while let presented = controller?.presentedViewController {
      controller = presented
    }
    return controller
  }

  private func resolvePending(payload: [String: Any]) {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      guard self.isFlowInProgress else { return }
      self.pendingResolve?(payload)
      self.cleanupFlow()
    }
  }

  private func rejectPending(code: String, message: String) {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      guard self.isFlowInProgress else { return }
      self.pendingReject?(code, message, nil)
      self.cleanupFlow()
    }
  }

  private func cleanupFlow() {
    pendingResolve = nil
    pendingReject = nil
    isFlowInProgress = false
  }
}

private struct HeaderStyleConfig {
  var backgroundColor = "#FFFFFF"
  var color = "#2D2D2D"
  var fontSize = 22
  var fontFamily = ""
  // When controls are in the footer ("bottom"), keep the header compact by default.
  // When controls are in the header ("top"), header is taller to fit the action buttons.
  var height: Int = 56
  var paddingHorizontal: Int = 20
  var paddingTop: Int = 12
  var paddingBottom: Int = 12
}

private struct ButtonContainerStyleConfig {
  var backgroundColor = "#FFFFFF"
  var paddingHorizontal: Int = 20
  var paddingTop: Int = 16
  var paddingBottom: Int = 24
  var gap: Int = 12
  var buttonHeight: Int = 54
  var layout: String = "vertical"
}

private struct ButtonStyleConfig {
  var textColor: String
  var backgroundColor: String
  var borderColor: String
  var borderWidth: Int
  var fontSize: Int
  var fontFamily: String
  var borderRadius: Int = 27
  var content: String = "text" // text | icon | iconText | textIcon (also accepts icon+text, text+icon, TextIcon)
  var iconUri: String = ""
  var iconBase64: String = ""
  var iconTintColor: String = ""
  var iconSize: Int = 18
  var iconGap: Int = 8
  var paddingHorizontal: Int = 12
  var paddingVertical: Int = 0
}

private struct CropperConfig {
  var cropWidth = 100
  var cropHeight = 100
  var freeStyleCropEnabled = false
  var compressQuality: Double = 1.0 // 0..1
  var compressFormat: String = "jpeg" // jpeg | png | webp
  var circularCrop = false
  var rotationEnabled = false
  var cropGridEnabled = false
  var cropGridColor: String = ""
  var dimmedLayerColor: String = "#B3000000"
  var showNativeCropControls = false
  var isDarkTheme = false
  var statusBarColor = "#FFFFFF"
  var includeBase64 = false
  var headerTitle = "Preview Image"
  var headerAlignment = "left" // left | center | right
  var cancelText = "Cancel"
  var uploadText = "Upload"
  // Defaults aligned with the "demo button layout":
  // - Cancel is a light button with dark text
  // - Upload is a black button with white text
  var cancelColor = "#111111"
  var uploadColor = "#111111"
  var pickerSource = "gallery"
  var controlsPlacement = "bottom" // bottom | top
  var topLeftControl = "cancel" // cancel | upload | none
  var topRightControl = "upload" // cancel | upload | none
  var footerButtonOrder = "uploadFirst" // uploadFirst | cancelFirst
  var headerStyle = HeaderStyleConfig()
  var buttonContainerStyle = ButtonContainerStyleConfig()
  var cancelButtonStyle = ButtonStyleConfig(
    textColor: "#111111",
    backgroundColor: "#EFEFF4",
    borderColor: "#EFEFF4",
    borderWidth: 0,
    fontSize: 16,
    fontFamily: ""
  )
  var uploadButtonStyle = ButtonStyleConfig(
    textColor: "#FFFFFF",
    backgroundColor: "#111111",
    borderColor: "#111111",
    borderWidth: 0,
    fontSize: 16,
    fontFamily: ""
  )

  static func parse(from options: NSDictionary) -> CropperConfig {
    var config = CropperConfig()
    config.cropWidth = max(options["width"] as? Int ?? 100, 1)
    config.cropHeight = max(options["height"] as? Int ?? 100, 1)
    config.freeStyleCropEnabled = options["freeStyleCropEnabled"] as? Bool ?? false
    config.isDarkTheme = options["isDarkTheme"] as? Bool ?? false
    if let q = options["compressQuality"] as? NSNumber {
      config.compressQuality = min(max(q.doubleValue, 0.0), 1.0)
    }
    if let rawFmt = (options["compressFormat"] as? String)?.nonEmpty?.lowercased() {
      config.compressFormat = rawFmt
    }
    if config.compressFormat != "jpeg", config.compressFormat != "png", config.compressFormat != "webp" {
      config.compressFormat = "jpeg"
    }
    config.circularCrop = options["circularCrop"] as? Bool ?? false
    config.rotationEnabled = options["rotationEnabled"] as? Bool ?? false
    config.cropGridEnabled = options["cropGridEnabled"] as? Bool ?? false
    config.cropGridColor = (options["cropGridColor"] as? String)?.nonEmpty ?? ""
    config.showNativeCropControls = options["showNativeCropControls"] as? Bool ?? false
    config.dimmedLayerColor = (options["cropperDimmedLayerColor"] as? String)?.nonEmpty
      ?? (options["cropOverlayColor"] as? String)?.nonEmpty
      ?? (config.isDarkTheme ? "#E0000000" : "#B3000000")
    config.includeBase64 = options["includeBase64"] as? Bool ?? false
    config.statusBarColor = (options["cropperStatusBarColor"] as? String)?.nonEmpty
      ?? (config.isDarkTheme ? "#000000" : "#FFFFFF")
    config.headerTitle = (options["cropperToolbarTitle"] as? String)?.nonEmpty ?? config.headerTitle
    config.headerAlignment = (options["headerAlignment"] as? String)?.nonEmpty ?? config.headerAlignment
    config.cancelText = (options["cropperCancelText"] as? String)?.nonEmpty ?? config.cancelText
    config.uploadText = (options["cropperChooseText"] as? String)?.nonEmpty ?? config.uploadText
    config.cancelColor = (options["cropperCancelColor"] as? String)?.nonEmpty ?? config.cancelColor
    config.uploadColor = (options["cropperChooseColor"] as? String)?.nonEmpty ?? config.uploadColor
    config.pickerSource = (options["pickerSource"] as? String)?.nonEmpty ?? config.pickerSource
    config.controlsPlacement = (options["controlsPlacement"] as? String)?.nonEmpty == "top" ? "top" : "bottom"
    // If native crop controls (toolbar) are visible, we must place action buttons in the header.
    // Otherwise the footer overlaps the toolbar and makes it look "missing" on iOS.
    if config.showNativeCropControls { config.controlsPlacement = "top" }
    config.topLeftControl = (options["topLeftControl"] as? String)?.nonEmpty ?? config.topLeftControl
    config.topRightControl = (options["topRightControl"] as? String)?.nonEmpty ?? config.topRightControl
    config.footerButtonOrder = (options["footerButtonOrder"] as? String)?.nonEmpty ?? config.footerButtonOrder

    // sanitize
    let allowedControl = Set(["cancel", "upload", "none"])
    if !allowedControl.contains(config.topLeftControl) { config.topLeftControl = "cancel" }
    if !allowedControl.contains(config.topRightControl) { config.topRightControl = "upload" }
    if config.topLeftControl == config.topRightControl { config.topRightControl = "none" }
    if config.footerButtonOrder != "cancelFirst" { config.footerButtonOrder = "uploadFirst" }
    let allowedAlignment = Set(["left", "center", "right"])
    if !allowedAlignment.contains(config.headerAlignment) { config.headerAlignment = "left" }

    if let headerStyle = options["headerStyle"] as? NSDictionary {
      config.headerStyle.backgroundColor = (headerStyle["backgroundColor"] as? String)?.nonEmpty ?? config.headerStyle.backgroundColor
      config.headerStyle.color = (headerStyle["color"] as? String)?.nonEmpty
        ?? (headerStyle["titleColor"] as? String)?.nonEmpty
        ?? config.headerStyle.color
      config.headerStyle.fontSize = max(
        (headerStyle["fontSize"] as? Int)
          ?? (headerStyle["titleFontSize"] as? Int)
          ?? config.headerStyle.fontSize,
        10
      )
      config.headerStyle.fontFamily = (headerStyle["fontFamily"] as? String)?.nonEmpty
        ?? (headerStyle["titleFontFamily"] as? String)?.nonEmpty
        ?? config.headerStyle.fontFamily
      let defaultHeaderHeight = (config.controlsPlacement == "top") ? 84 : 56
      config.headerStyle.height = max(headerStyle["height"] as? Int ?? defaultHeaderHeight, 48)
      config.headerStyle.paddingHorizontal = max(headerStyle["paddingHorizontal"] as? Int ?? config.headerStyle.paddingHorizontal, 0)
      let defaultPadTop = (config.controlsPlacement == "top") ? 20 : 12
      let defaultPadBottom = (config.controlsPlacement == "top") ? 20 : 12
      config.headerStyle.paddingTop = max(headerStyle["paddingTop"] as? Int ?? defaultPadTop, 0)
      config.headerStyle.paddingBottom = max(headerStyle["paddingBottom"] as? Int ?? defaultPadBottom, 0)
    } else {
      // Apply compact defaults when controls are in the footer.
      if config.controlsPlacement == "top" {
        config.headerStyle.height = 84
        config.headerStyle.paddingTop = 20
        config.headerStyle.paddingBottom = 20
      } else {
        config.headerStyle.height = 56
        config.headerStyle.paddingTop = 12
        config.headerStyle.paddingBottom = 12
      }
    }

    if let containerStyle = options["buttonContainerStyle"] as? NSDictionary {
      config.buttonContainerStyle.backgroundColor = (containerStyle["backgroundColor"] as? String)?.nonEmpty ?? config.buttonContainerStyle.backgroundColor
      config.buttonContainerStyle.paddingHorizontal = max(containerStyle["paddingHorizontal"] as? Int ?? config.buttonContainerStyle.paddingHorizontal, 0)
      config.buttonContainerStyle.paddingTop = max(containerStyle["paddingTop"] as? Int ?? config.buttonContainerStyle.paddingTop, 0)
      config.buttonContainerStyle.paddingBottom = max(containerStyle["paddingBottom"] as? Int ?? config.buttonContainerStyle.paddingBottom, 0)
      config.buttonContainerStyle.gap = max(containerStyle["gap"] as? Int ?? config.buttonContainerStyle.gap, 0)
      config.buttonContainerStyle.buttonHeight = max(containerStyle["buttonHeight"] as? Int ?? config.buttonContainerStyle.buttonHeight, 1)
      config.buttonContainerStyle.layout =
        ((containerStyle["layout"] as? String)?.nonEmpty == "horizontal") ? "horizontal" : "vertical"
    }

    if let cancelStyle = options["cancelButtonStyle"] as? NSDictionary {
      config.cancelButtonStyle.textColor = (cancelStyle["textColor"] as? String)?.nonEmpty ?? config.cancelButtonStyle.textColor
      config.cancelButtonStyle.backgroundColor = (cancelStyle["backgroundColor"] as? String)?.nonEmpty ?? config.cancelButtonStyle.backgroundColor
      config.cancelButtonStyle.borderColor = (cancelStyle["borderColor"] as? String)?.nonEmpty ?? config.cancelButtonStyle.borderColor
      config.cancelButtonStyle.borderWidth = max(cancelStyle["borderWidth"] as? Int ?? config.cancelButtonStyle.borderWidth, 0)
      config.cancelButtonStyle.fontSize = max(cancelStyle["fontSize"] as? Int ?? config.cancelButtonStyle.fontSize, 10)
      config.cancelButtonStyle.fontFamily = (cancelStyle["fontFamily"] as? String)?.nonEmpty ?? config.cancelButtonStyle.fontFamily
      config.cancelButtonStyle.borderRadius = max(cancelStyle["borderRadius"] as? Int ?? config.cancelButtonStyle.borderRadius, 0)
      config.cancelColor = config.cancelButtonStyle.textColor
      config.cancelButtonStyle.content = (cancelStyle["content"] as? String)?.nonEmpty ?? config.cancelButtonStyle.content
      config.cancelButtonStyle.iconUri = (cancelStyle["iconUri"] as? String)?.nonEmpty ?? config.cancelButtonStyle.iconUri
      config.cancelButtonStyle.iconBase64 = (cancelStyle["iconBase64"] as? String)?.nonEmpty ?? config.cancelButtonStyle.iconBase64
      config.cancelButtonStyle.iconTintColor = (cancelStyle["iconTintColor"] as? String)?.nonEmpty ?? config.cancelButtonStyle.iconTintColor
      config.cancelButtonStyle.iconSize = max(cancelStyle["iconSize"] as? Int ?? config.cancelButtonStyle.iconSize, 12)
      config.cancelButtonStyle.iconGap = max(cancelStyle["iconGap"] as? Int ?? config.cancelButtonStyle.iconGap, 0)
      config.cancelButtonStyle.paddingHorizontal = max(cancelStyle["paddingHorizontal"] as? Int ?? config.cancelButtonStyle.paddingHorizontal, 0)
      config.cancelButtonStyle.paddingVertical = max(cancelStyle["paddingVertical"] as? Int ?? config.cancelButtonStyle.paddingVertical, 0)
    }

    if let uploadStyle = options["uploadButtonStyle"] as? NSDictionary {
      config.uploadButtonStyle.textColor = (uploadStyle["textColor"] as? String)?.nonEmpty ?? config.uploadButtonStyle.textColor
      config.uploadButtonStyle.backgroundColor = (uploadStyle["backgroundColor"] as? String)?.nonEmpty ?? config.uploadButtonStyle.backgroundColor
      config.uploadButtonStyle.borderColor = (uploadStyle["borderColor"] as? String)?.nonEmpty ?? config.uploadButtonStyle.borderColor
      config.uploadButtonStyle.borderWidth = max(uploadStyle["borderWidth"] as? Int ?? config.uploadButtonStyle.borderWidth, 0)
      config.uploadButtonStyle.fontSize = max(uploadStyle["fontSize"] as? Int ?? config.uploadButtonStyle.fontSize, 10)
      config.uploadButtonStyle.fontFamily = (uploadStyle["fontFamily"] as? String)?.nonEmpty ?? config.uploadButtonStyle.fontFamily
      config.uploadButtonStyle.borderRadius = max(uploadStyle["borderRadius"] as? Int ?? config.uploadButtonStyle.borderRadius, 0)
      config.uploadColor = config.uploadButtonStyle.backgroundColor
      config.uploadButtonStyle.content = (uploadStyle["content"] as? String)?.nonEmpty ?? config.uploadButtonStyle.content
      config.uploadButtonStyle.iconUri = (uploadStyle["iconUri"] as? String)?.nonEmpty ?? config.uploadButtonStyle.iconUri
      config.uploadButtonStyle.iconBase64 = (uploadStyle["iconBase64"] as? String)?.nonEmpty ?? config.uploadButtonStyle.iconBase64
      config.uploadButtonStyle.iconTintColor = (uploadStyle["iconTintColor"] as? String)?.nonEmpty ?? config.uploadButtonStyle.iconTintColor
      config.uploadButtonStyle.iconSize = max(uploadStyle["iconSize"] as? Int ?? config.uploadButtonStyle.iconSize, 12)
      config.uploadButtonStyle.iconGap = max(uploadStyle["iconGap"] as? Int ?? config.uploadButtonStyle.iconGap, 0)
      config.uploadButtonStyle.paddingHorizontal = max(uploadStyle["paddingHorizontal"] as? Int ?? config.uploadButtonStyle.paddingHorizontal, 0)
      config.uploadButtonStyle.paddingVertical = max(uploadStyle["paddingVertical"] as? Int ?? config.uploadButtonStyle.paddingVertical, 0)
    }

    return config
  }
}

private extension String {
  var nonEmpty: String? {
    isEmpty ? nil : self
  }
}

private func defaultLabelColor() -> UIColor {
  if #available(iOS 13.0, *) {
    return .label
  }
  return .black
}

private final class NativeImageCropperViewController: TOCropViewController {
  private static let iconCache = NSCache<NSString, UIImage>()
  private let config: CropperConfig
  private let parseColor: (String, UIColor) -> UIColor
  private var statusBarFillHeightConstraint: NSLayoutConstraint?
  private var footerHeightConstraint: NSLayoutConstraint?
  private var isCommittingCrop = false
  private var circularGridOverlay: CircularGridOverlayView?

  override var preferredStatusBarStyle: UIStatusBarStyle {
    if #available(iOS 13.0, *) {
      return config.isDarkTheme ? .lightContent : .darkContent
    }
    return config.isDarkTheme ? .lightContent : .default
  }

  private lazy var headerContainer: UIView = {
    let view = UIView()
    view.translatesAutoresizingMaskIntoConstraints = false
    view.backgroundColor = parseColor(config.headerStyle.backgroundColor, .white)
    return view
  }()

  private lazy var statusBarFillView: UIView = {
    let view = UIView()
    view.translatesAutoresizingMaskIntoConstraints = false
    view.backgroundColor = parseColor(config.statusBarColor, .white)
    return view
  }()

  private lazy var headerTitleLabel: UILabel = {
    let label = UILabel()
    label.translatesAutoresizingMaskIntoConstraints = false
    label.text = config.headerTitle
    if config.controlsPlacement == "top" {
      label.textAlignment = .center
    } else {
      switch config.headerAlignment {
      case "center":
        label.textAlignment = .center
      case "right":
        label.textAlignment = .right
      default:
        label.textAlignment = .left
      }
    }
    label.textColor = parseColor(config.headerStyle.color, defaultLabelColor())
    label.font = resolvedFont(
      family: config.headerStyle.fontFamily,
      size: CGFloat(config.headerStyle.fontSize),
      fallback: UIFont.systemFont(ofSize: CGFloat(config.headerStyle.fontSize), weight: .bold)
    )
    return label
  }()

  private lazy var footerContainer: UIView = {
    let view = UIView()
    view.translatesAutoresizingMaskIntoConstraints = false
    view.backgroundColor = parseColor(config.buttonContainerStyle.backgroundColor, .white)
    return view
  }()

  private lazy var cancelButton: UIButton = createActionButton(
    title: config.cancelText,
    style: config.cancelButtonStyle
  )

  private lazy var uploadButton: UIButton = createActionButton(
    title: config.uploadText,
    style: config.uploadButtonStyle
  )

  init(
    image: UIImage,
    config: CropperConfig,
    colorParser: @escaping (String, UIColor) -> UIColor
  ) {
    self.config = config
    self.parseColor = colorParser
    super.init(croppingStyle: config.circularCrop ? .circular : .default, image: image)
  }

  required override init(croppingStyle style: TOCropViewCroppingStyle, image: UIImage) {
    self.config = CropperConfig()
    self.parseColor = { _, fallback in fallback }
    super.init(croppingStyle: style, image: image)
  }

  @available(*, unavailable)
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    modalPresentationCapturesStatusBarAppearance = true
    setupCropBehavior()
    setupCustomChrome()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    setNeedsStatusBarAppearanceUpdate()
  }

  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    statusBarFillHeightConstraint?.constant = view.safeAreaInsets.top
    footerHeightConstraint?.constant =
      footerBaseHeight() + view.safeAreaInsets.bottom

    // Ensure pill rounding stays correct after AutoLayout updates button sizes.
    applyButtonRounding(button: cancelButton, style: config.cancelButtonStyle)
    applyButtonRounding(button: uploadButton, style: config.uploadButtonStyle)

    // Keep circular grid overlay in sync with layout.
    if let grid = circularGridOverlay {
      grid.frame = cropView.foregroundContainerView.bounds
      grid.setNeedsLayout()
    }
  }

  private func footerBaseHeight() -> CGFloat {
    if config.controlsPlacement == "top" {
      return 0
    }
    let base =
      CGFloat(config.buttonContainerStyle.paddingTop +
              config.buttonContainerStyle.paddingBottom +
              config.buttonContainerStyle.buttonHeight)
    if config.buttonContainerStyle.layout == "horizontal" {
      return base
    }
    return base + CGFloat(config.buttonContainerStyle.gap + config.buttonContainerStyle.buttonHeight)
  }

  private func setupCropBehavior() {
    doneButtonHidden = true
    cancelButtonHidden = true
    rotateButtonsHidden = !config.rotationEnabled
    resetButtonHidden = !config.rotationEnabled
    aspectRatioPickerButtonHidden = !config.showNativeCropControls
    toolbar.isHidden = !config.showNativeCropControls
    showCancelConfirmationDialog = false

    setAspectRatioPreset(CGSize(width: config.cropWidth, height: config.cropHeight), animated: false)
    aspectRatioLockEnabled = !config.freeStyleCropEnabled
    // Keep the selected aspect ratio when user taps reset.
    // Otherwise TOCropViewController resets back to the image's original ratio,
    // which looks especially wrong for circular crops.
    resetAspectRatioEnabled = false
    aspectRatioLockDimensionSwapEnabled = true
    cropView.cropBoxResizeEnabled = config.freeStyleCropEnabled
    cropView.overlayView.backgroundColor = parseColor(config.dimmedLayerColor, .black.withAlphaComponent(0.7))

    let footerBase = footerBaseHeight()
    cropView.cropRegionInsets = UIEdgeInsets(
      top: CGFloat(config.headerStyle.height),
      left: 0,
      bottom: footerBase,
      right: 0
    )
    cropView.performInitialSetup()

    // Grid overlay:
    // - Rectangle mode: use built-in grid overlay.
    // - Circular mode: TOCropViewController doesn't create the built-in grid view, so we render our own.
    if config.circularCrop {
      circularGridOverlay?.removeFromSuperview()
      circularGridOverlay = nil
      if config.cropGridEnabled {
        let color = config.cropGridColor.nonEmpty.flatMap { UIColor(hex: $0) } ?? UIColor.white.withAlphaComponent(0.9)
        let grid = CircularGridOverlayView(frame: cropView.foregroundContainerView.bounds, lineColor: color)
        grid.isUserInteractionEnabled = false
        cropView.foregroundContainerView.addSubview(grid)
        circularGridOverlay = grid
      }
    } else {
      circularGridOverlay?.removeFromSuperview()
      circularGridOverlay = nil
      cropView.gridOverlayHidden = !config.cropGridEnabled
      cropView.alwaysShowCroppingGrid = config.cropGridEnabled
      cropView.setGridOverlayHidden(!config.cropGridEnabled, animated: false)
    }
}

  private func setupCustomChrome() {
    view.addSubview(statusBarFillView)
    view.addSubview(headerContainer)
    headerContainer.addSubview(headerTitleLabel)
    view.addSubview(footerContainer)
    if config.controlsPlacement == "top" {
      headerContainer.addSubview(cancelButton)
      headerContainer.addSubview(uploadButton)
      footerContainer.isHidden = true
    } else {
      footerContainer.addSubview(cancelButton)
      footerContainer.addSubview(uploadButton)
    }

    cancelButton.addTarget(self, action: #selector(onCancelTapped), for: .touchUpInside)
    uploadButton.addTarget(self, action: #selector(onUploadTapped), for: .touchUpInside)

    statusBarFillHeightConstraint = statusBarFillView.heightAnchor.constraint(equalToConstant: view.safeAreaInsets.top)
    footerHeightConstraint = footerContainer.heightAnchor.constraint(equalToConstant: footerBaseHeight() + view.safeAreaInsets.bottom)

    guard let statusHeightConstraint = statusBarFillHeightConstraint,
          let bottomHeightConstraint = footerHeightConstraint else {
      return
    }

    var constraints: [NSLayoutConstraint] = [
      statusBarFillView.topAnchor.constraint(equalTo: view.topAnchor),
      statusBarFillView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      statusBarFillView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      statusHeightConstraint,

      headerContainer.topAnchor.constraint(equalTo: statusBarFillView.bottomAnchor),
      headerContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      headerContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      headerContainer.heightAnchor.constraint(equalToConstant: CGFloat(config.headerStyle.height)),
      headerTitleLabel.topAnchor.constraint(equalTo: headerContainer.topAnchor, constant: CGFloat(config.headerStyle.paddingTop)),
      headerTitleLabel.bottomAnchor.constraint(equalTo: headerContainer.bottomAnchor, constant: -CGFloat(config.headerStyle.paddingBottom)),

      footerContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      footerContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      footerContainer.bottomAnchor.constraint(equalTo: view.bottomAnchor),
      bottomHeightConstraint,
    ]

    if config.controlsPlacement == "top" {
      let left = config.topLeftControl
      let right = config.topRightControl
      cancelButton.isHidden = !(left == "cancel" || right == "cancel")
      uploadButton.isHidden = !(left == "upload" || right == "upload")

      let paddingH = CGFloat(config.headerStyle.paddingHorizontal)
      let gap = CGFloat(max(config.buttonContainerStyle.gap, 8))
      let h = CGFloat(min(max(config.buttonContainerStyle.buttonHeight, 36), 44))

      var leftButton: UIButton?
      var rightButton: UIButton?

      if left == "cancel" {
        leftButton = cancelButton
        constraints += [
          cancelButton.leadingAnchor.constraint(equalTo: headerContainer.leadingAnchor, constant: paddingH),
          cancelButton.centerYAnchor.constraint(equalTo: headerContainer.centerYAnchor),
          cancelButton.heightAnchor.constraint(equalToConstant: h),
        ]
      } else if left == "upload" {
        leftButton = uploadButton
        constraints += [
          uploadButton.leadingAnchor.constraint(equalTo: headerContainer.leadingAnchor, constant: paddingH),
          uploadButton.centerYAnchor.constraint(equalTo: headerContainer.centerYAnchor),
          uploadButton.heightAnchor.constraint(equalToConstant: h),
        ]
      }

      if right == "cancel" {
        rightButton = cancelButton
        constraints += [
          cancelButton.trailingAnchor.constraint(equalTo: headerContainer.trailingAnchor, constant: -paddingH),
          cancelButton.centerYAnchor.constraint(equalTo: headerContainer.centerYAnchor),
          cancelButton.heightAnchor.constraint(equalToConstant: h),
        ]
      } else if right == "upload" {
        rightButton = uploadButton
        constraints += [
          uploadButton.trailingAnchor.constraint(equalTo: headerContainer.trailingAnchor, constant: -paddingH),
          uploadButton.centerYAnchor.constraint(equalTo: headerContainer.centerYAnchor),
          uploadButton.heightAnchor.constraint(equalToConstant: h),
        ]
      }

      // Keep title centered and avoid overlapping the side controls.
      constraints += [
        headerTitleLabel.centerXAnchor.constraint(equalTo: headerContainer.centerXAnchor),
      ]
      if let lb = leftButton {
        constraints.append(headerTitleLabel.leadingAnchor.constraint(greaterThanOrEqualTo: lb.trailingAnchor, constant: gap))
      } else {
        constraints.append(headerTitleLabel.leadingAnchor.constraint(greaterThanOrEqualTo: headerContainer.leadingAnchor, constant: paddingH))
      }
      if let rb = rightButton {
        constraints.append(headerTitleLabel.trailingAnchor.constraint(lessThanOrEqualTo: rb.leadingAnchor, constant: -gap))
      } else {
        constraints.append(headerTitleLabel.trailingAnchor.constraint(lessThanOrEqualTo: headerContainer.trailingAnchor, constant: -paddingH))
      }
    } else {
      constraints += [
        headerTitleLabel.leadingAnchor.constraint(equalTo: headerContainer.leadingAnchor, constant: CGFloat(config.headerStyle.paddingHorizontal)),
        headerTitleLabel.trailingAnchor.constraint(equalTo: headerContainer.trailingAnchor, constant: -CGFloat(config.headerStyle.paddingHorizontal)),
      ]

      let paddingH = CGFloat(config.buttonContainerStyle.paddingHorizontal)
      let paddingTop = CGFloat(config.buttonContainerStyle.paddingTop)
      let gap = CGFloat(config.buttonContainerStyle.gap)
      let buttonHeight = CGFloat(config.buttonContainerStyle.buttonHeight)

      let firstIsCancel = config.footerButtonOrder == "cancelFirst"
      let firstButton = firstIsCancel ? cancelButton : uploadButton
      let secondButton = firstIsCancel ? uploadButton : cancelButton

      if config.buttonContainerStyle.layout == "horizontal" {
        constraints += [
          firstButton.leadingAnchor.constraint(equalTo: footerContainer.leadingAnchor, constant: paddingH),
          firstButton.topAnchor.constraint(equalTo: footerContainer.topAnchor, constant: paddingTop),
          firstButton.heightAnchor.constraint(equalToConstant: buttonHeight),

          secondButton.leadingAnchor.constraint(equalTo: firstButton.trailingAnchor, constant: gap),
          secondButton.trailingAnchor.constraint(equalTo: footerContainer.trailingAnchor, constant: -paddingH),
          secondButton.topAnchor.constraint(equalTo: firstButton.topAnchor),
          secondButton.heightAnchor.constraint(equalTo: firstButton.heightAnchor),

          secondButton.widthAnchor.constraint(equalTo: firstButton.widthAnchor),
        ]
      } else {
        constraints += [
          firstButton.leadingAnchor.constraint(equalTo: footerContainer.leadingAnchor, constant: paddingH),
          firstButton.trailingAnchor.constraint(equalTo: footerContainer.trailingAnchor, constant: -paddingH),
          firstButton.topAnchor.constraint(equalTo: footerContainer.topAnchor, constant: paddingTop),
          firstButton.heightAnchor.constraint(equalToConstant: buttonHeight),

          secondButton.leadingAnchor.constraint(equalTo: firstButton.leadingAnchor),
          secondButton.trailingAnchor.constraint(equalTo: firstButton.trailingAnchor),
          secondButton.topAnchor.constraint(equalTo: firstButton.bottomAnchor, constant: gap),
          secondButton.heightAnchor.constraint(equalTo: firstButton.heightAnchor),
        ]
      }
    }

    NSLayoutConstraint.activate(constraints)
  }

  private func createActionButton(title: String, style: ButtonStyleConfig) -> UIButton {
    let button = UIButton(type: .system)
    button.translatesAutoresizingMaskIntoConstraints = false
    let resolvedTitleColor = parseColor(style.textColor, defaultLabelColor())
    applyButtonContent(button: button, title: title, style: style, fallbackTitleColor: resolvedTitleColor)
    button.setTitleColor(resolvedTitleColor, for: .normal)
    button.titleLabel?.font = resolvedFont(
      family: style.fontFamily,
      size: CGFloat(style.fontSize),
      fallback: UIFont.systemFont(ofSize: CGFloat(style.fontSize), weight: .semibold)
    )
    button.titleLabel?.numberOfLines = 1
    button.titleLabel?.lineBreakMode = .byTruncatingTail
    button.titleLabel?.adjustsFontSizeToFitWidth = true
    button.titleLabel?.minimumScaleFactor = 0.85
    button.titleLabel?.allowsDefaultTighteningForTruncation = true
    if #available(iOS 14.0, *) {
      button.titleLabel?.lineBreakStrategy = []
    }
    button.setContentCompressionResistancePriority(.required, for: .horizontal)
    button.setContentHuggingPriority(.defaultHigh, for: .horizontal)
    if #available(iOS 15.0, *) {
      // Style is applied via `UIButton.Configuration`.
      button.backgroundColor = .clear
      button.layer.borderWidth = 0
      button.layer.borderColor = UIColor.clear.cgColor
    } else {
      button.backgroundColor = parseColor(style.backgroundColor, .clear)
      button.layer.borderColor = parseColor(style.borderColor, .clear).cgColor
      button.layer.borderWidth = CGFloat(style.borderWidth)
    }
    button.clipsToBounds = true
    applyButtonRounding(button: button, style: style)
    return button
  }

  private func applyButtonRounding(button: UIButton, style: ButtonStyleConfig) {
    button.clipsToBounds = true
    button.layer.masksToBounds = true
    button.layer.allowsEdgeAntialiasing = true
    if #available(iOS 13.0, *) {
      button.layer.cornerCurve = .continuous
    }

    // If user provides a radius, respect it (but never exceed half height).
    // Otherwise default to a perfect "pill" radius.
    let half = max(button.bounds.height / 2.0, 0)
    let preferred = CGFloat(max(style.borderRadius, 0))
    let radius = preferred > 0 ? min(preferred, half) : half
    if button.layer.cornerRadius != radius {
      button.layer.cornerRadius = radius
    }
    if #available(iOS 15.0, *), var cfg = button.configuration {
      var bg = cfg.background
      bg.cornerRadius = radius
      cfg.background = bg
      button.configuration = cfg
    }
  }

  private func applyButtonContent(
    button: UIButton,
    title: String,
    style: ButtonStyleConfig,
    fallbackTitleColor: UIColor
  ) {
    let raw = style.content.trimmingCharacters(in: .whitespacesAndNewlines)
    let content: String
    switch raw {
    case "iconText", "icon+text":
      content = "iconText"
    case "textIcon", "TextIcon", "text+icon":
      content = "textIcon"
    case "icon", "text":
      content = raw
    default:
      content = "text"
    }

    let wantsText = content != "icon"
    let wantsIcon = content != "text"

    let icon = wantsIcon ? resolvedIcon(style: style) : nil
    let iconReady = icon != nil

    if #available(iOS 15.0, *) {
      applyButtonConfiguration(
        button: button,
        title: title,
        content: content,
        style: style,
        icon: icon,
        fallbackTitleColor: fallbackTitleColor
      )
      // If icon is not ready, try remote loader (it will update config when done).
      if wantsIcon, !iconReady {
        loadRemoteIconIfNeeded(
          button: button,
          title: title,
          content: content,
          style: style,
          fallbackTitleColor: fallbackTitleColor
        )
      }
      return
    }

    // Fallback for iOS < 15 (best-effort).
    button.titleEdgeInsets = .zero
    button.imageEdgeInsets = .zero
    button.contentHorizontalAlignment = .center
    button.semanticContentAttribute = content == "textIcon" ? .forceRightToLeft : .forceLeftToRight

    if wantsText || !iconReady {
      button.setTitle(title, for: .normal)
    } else {
      button.setTitle(nil, for: .normal)
    }

    let padH = CGFloat(max(style.paddingHorizontal, 0))
    let padV = CGFloat(max(style.paddingVertical, 0))
    button.contentEdgeInsets = UIEdgeInsets(top: padV, left: padH, bottom: padV, right: padH)

    let hasTint = !style.iconTintColor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    if iconReady, var img = icon {
      if hasTint {
        let tintColor = parseColor(style.iconTintColor, fallbackTitleColor)
        img = img.withRenderingMode(.alwaysTemplate)
        button.tintColor = tintColor
      } else {
        img = img.withRenderingMode(.alwaysOriginal)
      }
      let size = CGFloat(max(style.iconSize, 12))
      button.setImage(resizedImageToFit(img, size: size), for: .normal)
      button.imageView?.contentMode = .scaleAspectFit
      let gap = CGFloat(max(style.iconGap, 0))
      if gap > 0 {
        button.titleEdgeInsets = UIEdgeInsets(top: 0, left: gap / 2, bottom: 0, right: -(gap / 2))
        button.imageEdgeInsets = UIEdgeInsets(top: 0, left: -(gap / 2), bottom: 0, right: gap / 2)
      }
    } else if wantsIcon {
      button.setImage(nil, for: .normal)
      loadRemoteIconIfNeeded(
        button: button,
        title: title,
        content: content,
        style: style,
        fallbackTitleColor: fallbackTitleColor
      )
    } else {
      button.setImage(nil, for: .normal)
    }
  }

  @available(iOS 15.0, *)
  private func applyButtonConfiguration(
    button: UIButton,
    title: String,
    content: String,
    style: ButtonStyleConfig,
    icon: UIImage?,
    fallbackTitleColor: UIColor
  ) {
    var config = button.configuration ?? UIButton.Configuration.plain()
    config.titleAlignment = .center
    config.titleLineBreakMode = .byTruncatingTail

    let padH = CGFloat(max(style.paddingHorizontal, 0))
    let padV = CGFloat(max(style.paddingVertical, 0))
    config.contentInsets = NSDirectionalEdgeInsets(top: padV, leading: padH, bottom: padV, trailing: padH)

    let wantsText = content != "icon"
    let wantsIcon = content != "text"

    config.title = wantsText ? title : nil
    config.baseForegroundColor = fallbackTitleColor

    let gap = CGFloat(max(style.iconGap, 0))
    config.imagePadding = gap
    config.imagePlacement = (content == "textIcon") ? .trailing : .leading

    // Keep background/border consistent when using UIButton.Configuration.
    var bg = config.background
    bg.backgroundColor = parseColor(style.backgroundColor, .clear)
    bg.strokeColor = parseColor(style.borderColor, .clear)
    bg.strokeWidth = CGFloat(max(style.borderWidth, 0))
    // Use a generous initial corner radius; `applyButtonRounding` will clamp to a pill after layout.
    bg.cornerRadius = CGFloat(max(style.borderRadius, 999))
    config.background = bg

    if wantsIcon, let icon {
      let hasTint = !style.iconTintColor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
      let size = CGFloat(max(style.iconSize, 12))
      let prepared = resizedImageToFit(icon, size: size)
      if hasTint {
        config.image = prepared.withRenderingMode(.alwaysTemplate)
        button.tintColor = parseColor(style.iconTintColor, fallbackTitleColor)
      } else {
        config.image = prepared.withRenderingMode(.alwaysOriginal)
      }
    } else {
      config.image = nil
    }

    button.configuration = config

    // Ensure title never wraps when using configuration titles.
    button.titleLabel?.numberOfLines = 1
    button.titleLabel?.lineBreakMode = .byTruncatingTail
    button.titleLabel?.adjustsFontSizeToFitWidth = true
    button.titleLabel?.minimumScaleFactor = 0.85
    if #available(iOS 14.0, *) {
      button.titleLabel?.lineBreakStrategy = []
    }
  }

  private func resizedImageToFit(_ image: UIImage, size: CGFloat) -> UIImage {
    guard size > 0 else { return image }
    let target = CGSize(width: size, height: size)
    let renderer = UIGraphicsImageRenderer(size: target)
    return renderer.image { _ in
      // Aspect-fit into a square.
      let iw = image.size.width
      let ih = image.size.height
      if iw <= 0 || ih <= 0 {
        image.draw(in: CGRect(origin: .zero, size: target))
        return
      }
      let scale = min(target.width / iw, target.height / ih)
      let w = iw * scale
      let h = ih * scale
      let x = (target.width - w) / 2.0
      let y = (target.height - h) / 2.0
      image.draw(in: CGRect(x: x, y: y, width: w, height: h))
    }
  }

  private func loadRemoteIconIfNeeded(
    button: UIButton,
    title: String,
    content: String,
    style: ButtonStyleConfig,
    fallbackTitleColor: UIColor
  ) {
    let uri = style.iconUri.trimmingCharacters(in: .whitespacesAndNewlines)
    guard uri.hasPrefix("http://") || uri.hasPrefix("https://") else { return }
    guard let url = URL(string: uri) else { return }

    let cacheKey = "\(uri)|\(max(style.iconSize, 12))" as NSString
    if let cached = Self.iconCache.object(forKey: cacheKey) {
      if #available(iOS 15.0, *) {
        applyButtonConfiguration(
          button: button,
          title: title,
          content: content,
          style: style,
          icon: cached,
          fallbackTitleColor: fallbackTitleColor
        )
      } else {
        button.setImage(cached, for: .normal)
      }
      return
    }

    URLSession.shared.dataTask(with: url) { [weak button] data, _, _ in
      guard let button, let data, let img = UIImage(data: data) else { return }
      DispatchQueue.main.async {
        if #available(iOS 15.0, *) {
          // Re-apply the full configuration so title/icon/layout stay consistent.
          self.applyButtonConfiguration(
            button: button,
            title: title,
            content: content,
            style: style,
            icon: img,
            fallbackTitleColor: fallbackTitleColor
          )
        } else {
          let hasTint = !style.iconTintColor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
          let size = CGFloat(max(style.iconSize, 12))
          let prepared = self.resizedImageToFit(img, size: size)
          if hasTint {
            let tintColor = self.parseColor(style.iconTintColor, fallbackTitleColor)
            button.tintColor = tintColor
            button.setImage(prepared.withRenderingMode(.alwaysTemplate), for: .normal)
          } else {
            button.setImage(prepared.withRenderingMode(.alwaysOriginal), for: .normal)
          }
          button.imageView?.contentMode = .scaleAspectFit
          // If the button is icon-only, remove the temporary/fallback title after icon is ready.
          if content == "icon" {
            button.setTitle(nil, for: .normal)
          } else if button.title(for: .normal) == nil {
            button.setTitle(title, for: .normal)
          }
        }

        // Cache the final rendered image (already aspect-fit sized for our target).
        let size = CGFloat(max(style.iconSize, 12))
        let cached = self.resizedImageToFit(img, size: size)
        Self.iconCache.setObject(cached, forKey: cacheKey)
      }
    }.resume()
  }

  private func resolvedIcon(style: ButtonStyleConfig) -> UIImage? {
    let base64 = style.iconBase64.trimmingCharacters(in: .whitespacesAndNewlines)
    if !base64.isEmpty {
      let cleaned = base64.components(separatedBy: "base64,").last ?? base64
      if let data = Data(base64Encoded: cleaned), let img = UIImage(data: data) {
        return img
      }
    }
    let uri = style.iconUri.trimmingCharacters(in: .whitespacesAndNewlines)
    if uri.isEmpty { return nil }
    if uri.hasPrefix("file://"), let url = URL(string: uri) {
      return UIImage(contentsOfFile: url.path)
    }
    if uri.hasPrefix("/") {
      return UIImage(contentsOfFile: uri)
    }
    return nil
  }

  private func resolvedFont(family: String, size: CGFloat, fallback: UIFont) -> UIFont {
    let cleaned = family.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !cleaned.isEmpty else { return fallback }
    if let font = UIFont(name: cleaned, size: size) {
      return font
    }
    let candidates = [
      cleaned,
      cleaned.replacingOccurrences(of: "_", with: "-"),
      cleaned.replacingOccurrences(of: "_", with: "")
    ]
    for name in candidates {
      if let font = UIFont(name: name, size: size) {
        return font
      }
    }
    return fallback
  }

  @objc private func onCancelTapped() {
    if let delegate {
      delegate.cropViewController?(self, didFinishCancelled: true)
      return
    }
    dismiss(animated: true)
  }

  @objc private func onUploadTapped() {
    if isCommittingCrop { return }
    isCommittingCrop = true
    uploadButton.isEnabled = false
    commitCurrentCrop()
  }
}

// MARK: - Circular grid overlay (iOS circular crop)

private final class CircularGridOverlayView: UIView {
  private let h1 = UIView()
  private let h2 = UIView()
  private let v1 = UIView()
  private let v2 = UIView()

  init(frame: CGRect, lineColor: UIColor) {
    super.init(frame: frame)
    [h1, h2, v1, v2].forEach {
      $0.backgroundColor = lineColor
      $0.isUserInteractionEnabled = false
      addSubview($0)
    }
    isUserInteractionEnabled = false
    backgroundColor = .clear
  }

  @available(*, unavailable)
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    let scale = window?.screen.scale ?? UIScreen.main.scale
    let thickness = max(1.0 / scale, 0.5)
    let w = bounds.width
    let h = bounds.height
    let x1 = (w / 3.0)
    let x2 = (w * 2.0 / 3.0)
    let y1 = (h / 3.0)
    let y2 = (h * 2.0 / 3.0)

    h1.frame = CGRect(x: 0, y: y1, width: w, height: thickness)
    h2.frame = CGRect(x: 0, y: y2, width: w, height: thickness)
    v1.frame = CGRect(x: x1, y: 0, width: thickness, height: h)
    v2.frame = CGRect(x: x2, y: 0, width: thickness, height: h)
  }
}

private extension UIColor {
  convenience init?(hex: String) {
    var s = hex.trimmingCharacters(in: .whitespacesAndNewlines)
    guard s.hasPrefix("#") else { return nil }
    s.removeFirst()
    if s.count == 6 { s = "FF" + s }
    guard s.count == 8, let v = UInt64(s, radix: 16) else { return nil }
    let a = CGFloat((v >> 24) & 0xFF) / 255.0
    let r = CGFloat((v >> 16) & 0xFF) / 255.0
    let g = CGFloat((v >> 8) & 0xFF) / 255.0
    let b = CGFloat(v & 0xFF) / 255.0
    self.init(red: r, green: g, blue: b, alpha: a)
  }
}

