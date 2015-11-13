-dontobfuscate

# https://github.com/square/okio/issues/60
-dontwarn okio.**

-dontwarn com.squareup.okhttp.**
-dontnote com.squareup.okhttp.internal.Platform


-keep class com.mobsandgeeks.saripaar.** {*;}

-keep class com.squareup.picasso.** {*;}

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

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
# --- /Gson ---

# --- HockeyApp ---
# http://support.hockeyapp.net/kb/client-integration-android/hockeyapp-and-proguard

# Restore source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless.
-keepattributes SourceFile,LineNumberTable

-keep public class javax.net.ssl.**
-keepclassmembers public class javax.net.ssl.** {
  *;
}

-keep public class org.apache.http.**
-keepclassmembers public class org.apache.http.** {
  *;
}

-keepclassmembers class net.hockeyapp.android.UpdateFragment {
  *;
}

# This is undocumented but seems to work.
-dontwarn uk.co.senab.photoview.**
-keep class net.hockeyapp.** { <init>(...); *; }
-keep enum net.hockeyapp.** { <init>(...); *; }
# --- /HockeyApp ---

# --- Mapbox ---
# TODO: use official rules when https://github.com/mapbox/mapbox-android-sdk/issues/363 is resolved.
-dontwarn java.awt.**
-keep class com.nineoldandroids.animation.** {*;}
-keep class com.nineoldandroids.util.Property {*;}
# --- /Mapbox ---

# --- Wikipedia ---
-keep class org.wikipedia.** { <init>(...); *; }
-keep enum org.wikipedia.** { <init>(...); *; }
-keep class org.mediawiki.api.json.** {*;}
-keep enum org.wikipedia.api.json.** { <init>(...); *; }
# --- /Wikipedia ---
