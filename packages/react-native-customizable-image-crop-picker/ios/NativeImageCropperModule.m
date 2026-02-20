@import Foundation;
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(NativeImageCropperModule, NSObject)

RCT_EXTERN_METHOD(
  openImagePreview:(NSDictionary *)options
  resolver:(RCTPromiseResolveBlock)resolver
  rejecter:(RCTPromiseRejectBlock)rejecter
)

@end

