# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep custom Model classes used by Gson
-keep class com.example.sennaccess.data.Models { *; }
-keep class com.example.sennaccess.data.** { *; }
-keep class com.example.sennaccess.ui.LoginUiState { *; }

# Keep Retrofit and Gson related classes
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class * {
    @retrofit2.http.* <methods>;
}
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Keep classes used by ZXing for QR generation
-keep class com.google.zxing.** { *; }

# Keep classes used by Coil for image loading
-keep class coil.** { *; }

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