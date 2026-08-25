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

# release 已开启 R8 混淆：保留行号，崩溃堆栈可用
# build/outputs/mapping/release/mapping.txt 还原混淆名
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- StockMaster 依赖 Keep 规则 ---
# kotlinx.serialization 反序列化需保留无参构造与字段名
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class com.stockmaster.app.data.** {
    <init>(...);
    <fields>;
}

# Coil 图片加载（反射 + OkHttp）避免被裁切
-keep class coil.** { *; }

# ML Kit BarcodeScanning 动态加载模型
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# ZXing CODE128 生成仅在反射式调用时需保留核心类
-keep class com.google.zxing.** { *; }

# CameraX 依赖的 androidx.* 已由 AAR 自带 keep，无需重复
