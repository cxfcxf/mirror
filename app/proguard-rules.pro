# Keep JNI callback interface (called from native code by name)
-keep class com.cxfcxf.mirror.bridge.RaopCallbackHandler { *; }
-keep class * implements com.cxfcxf.mirror.bridge.RaopCallbackHandler { *; }

# Keep NativeBridge native methods
-keep class com.cxfcxf.mirror.bridge.NativeBridge { *; }
