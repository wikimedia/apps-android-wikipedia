# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Cellar/android-sdk/22.6.2/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

###
# from https://github.com/ACRA/acra/wiki/ProGuard
#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

# ACRA needs "annotations" so add this...
# Note: This may already be defined in the default "proguard-android-optimize.txt"
# file in the SDK. If it is, then you don't need to duplicate it. See your
# "project.properties" file to get the path to the default "proguard-android-optimize.txt".
-keepattributes *Annotation*

-keepattributes Signature

## instead of the remaining ACRA proguard config: just keep all classes for now
-keep class org.acra.** {*;}

###
# Other libraries:

# https://github.com/square/okio/issues/60
-dontwarn okio.**

-dontwarn com.squareup.okhttp.**
-dontnote com.squareup.okhttp.internal.Platform


-keep class com.mobsandgeeks.saripaar.** {*;}

-keep class com.squareup.picasso.** {*;}

-keep class uk.co.senab.photoview.** {*;}

-keep class com.github.kevinsawicki.http.** {*;}

###
# Android support libraries:

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

-dontnote android.support.v4.text.ICUCompatIcs

#-dontwarn android.support.**

# --- Retrofit ---
# https://github.com/square/retrofit/issues/117
-dontwarn retrofit.**
-keep class retrofit.** { *; }
#-keepattributes Signature (already specified)
-keepattributes Exceptions

# --- Gson ---
# https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
#-keepattributes Signature (already specified)

# For using GSON @Expose annotation (already specified)
#-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
# --- /Gson ---

###
# Our code:
-keep class org.wikipedia.** { <init>(...); *; }
-keep enum org.wikipedia.** { <init>(...); *; }
-keep class org.mediawiki.api.json.** {*;}
-keep enum org.wikipedia.api.json.** { <init>(...); *; }

# Mapbox
# TODO: use official rules when https://github.com/mapbox/mapbox-android-sdk/issues/363 is resolved.
-dontwarn java.awt.**
-keep class com.nineoldandroids.animation.** {*;}
-keep class com.nineoldandroids.util.Property {*;}
