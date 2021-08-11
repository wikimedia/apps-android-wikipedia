-dontobfuscate

# --- Retrofit2 ---
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
# --- /Retrofit ---

# --- OkHttp + Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
# --- /OkHttp + Okio ---

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# --- Wikipedia ---
-keep class org.wikipedia.** { <init>(...); *; }
-keep enum org.wikipedia.** { <init>(...); *; }
# --- /Wikipedia ---
