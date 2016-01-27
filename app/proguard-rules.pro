-dontobfuscate

# --- Fresco ---
# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}
# Keep native methods
-keepclassmembers class * {
    native <methods>;
}
-dontwarn javax.annotation.**
-dontwarn com.android.volley.toolbox.**
# --- /Fresco ---

# https://github.com/square/okio/issues/60
-dontwarn okio.**

-dontwarn com.squareup.okhttp.**
-dontnote com.squareup.okhttp.internal.Platform


-keep class com.mobsandgeeks.saripaar.** {*;}

-keep class uk.co.senab.photoview.** {*;}

-keep class com.github.kevinsawicki.http.** {*;}

# --- Retrofit ---
# https://github.com/square/retrofit/issues/117
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# --- /Retrofit ---

# --- Butter Knife ---
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
# --- /Butter Knife ---

# --- Gson ---
# https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
#-keepattributes Signature (already specified)

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
# --- /Gson ---

# --- Mapbox ---
# TODO: remove when upstream patch is merged: https://phabricator.wikimedia.org/T120388
-keep class com.mapbox.mapboxsdk.views.** { *; }

# --- /Mapbox ---

# --- Wikipedia ---
-keep class org.wikipedia.** { <init>(...); *; }
-keep enum org.wikipedia.** { <init>(...); *; }
-keep class org.mediawiki.api.json.** {*;}
-keep enum org.wikipedia.api.json.** { <init>(...); *; }
# --- /Wikipedia ---
