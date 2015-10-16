-dontobfuscate

# https://github.com/square/okio/issues/60
-dontwarn okio.**

-dontwarn com.squareup.okhttp.**
-dontnote com.squareup.okhttp.internal.Platform


-keep class com.mobsandgeeks.saripaar.** {*;}

-keep class com.squareup.picasso.** {*;}

-keep class uk.co.senab.photoview.** {*;}

-keep class com.github.kevinsawicki.http.** {*;}

# --- Android Support libraries ---

# https://phabricator.wikimedia.org/T78197:
# NoClassDefFoundError: android.support.v7.internal.view.menu.MenuBuilder
# http://stackoverflow.com/questions/26657348/appcompat-v7-v21-0-0-causing-crash-on-samsung-devices-with-android-v4-2-2
# Allow obfuscation of android.support.v7.internal.view.menu.** but not the rest of android.support
# to avoid problem on Samsung 4.2.2 devices with appcompat v21
# see https://code.google.com/p/android/issues/detail?id=78377#hc150
-keep class !android.support.v7.internal.view.menu.MenuBuilder
-keep class !android.support.v7.internal.view.menu.SubMenuBuilder

# Make sure we keep android.support.v4.widget.DrawerLayout and friends
# because we're using reflection.
# Thus, the annoying feature of the nav and ToC drawers peeking in still works.
# This works but not sure what our other libraries do in terms of reflection with support-v4:
#-keep class android.support.v4.widget.DrawerLayout {*;}
#-keep class android.support.v4.widget.FixedDrawerLayout {*;}
#-keep class android.support.v4.widget.ViewDragHelper {*;}

# For now just keep all of v4, just to be on the safe side
-keep class android.support.v4.** {*;}

# This is undocumented but seems to work. Required after upgrading from v23.0.1 to v23.1.0.
-keep class android.support.v7.** { <init>(...); *; }

-dontnote android.support.v4.text.ICUCompatIcs
# --- /Android Support libraries ---

# --- Retrofit ---
# https://github.com/square/retrofit/issues/117
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# --- /Retrofit ---

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
