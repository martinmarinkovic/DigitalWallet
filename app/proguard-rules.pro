# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# CameraX PreviewView uses internal implementation classes during live-preview startup.
# Keep them stable in release so the default Add Card scanner path is not broken by shrinking.
-keep class androidx.camera.view.PreviewView { *; }
-keep class androidx.camera.view.PreviewView$* { *; }
-keep class androidx.camera.view.PreviewTransformation { *; }
-keep class androidx.camera.view.PreviewViewImplementation { *; }
-keep class androidx.camera.view.SurfaceViewImplementation { *; }
-keep class androidx.camera.view.SurfaceViewImplementation$* { *; }
-keep class androidx.camera.view.TextureViewImplementation { *; }
-keep class androidx.camera.view.TextureViewImplementation$* { *; }

# Preserve ML Kit barcode initialization entry points used by the live scanner.
-keep class com.google.mlkit.common.internal.MlKitInitProvider { *; }
-keep class com.google.mlkit.common.internal.CommonComponentRegistrar { *; }
-keep class com.google.mlkit.vision.common.internal.VisionCommonRegistrar { *; }
-keep class com.google.mlkit.vision.barcode.internal.BarcodeRegistrar { *; }
