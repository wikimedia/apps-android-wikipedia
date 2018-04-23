-dontobfuscate

# --- Butter Knife ---
# Finder.castParam() is stripped when not needed and ProGuard notes it
# unnecessarily. When castParam() is needed, it's not stripped. e.g.:
#
#  @OnItemSelected(value = R.id.history_entry_list)
#  void foo(ListView bar) {
#      L.d("baz");
#  }

-dontnote butterknife.internal.**
# --- /Butter Knife ---

# --- Retrofit2 ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
# --- /Retrofit ---

# --- OkHttp + Okio ---
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.*
# --- /OkHttp + Okio ---

# --- Commons Lang ---
-keep class org.apache.commons.lang3.** { <init>(...); *; }
-keep enum org.apache.commons.lang3.** { <init>(...); *; }
# --- /Commons Lang ---

# --- Gson ---
# https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
#-keepattributes Signature (already specified)

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
# --- /Gson ---

# --- Wikipedia ---
-keep class org.wikipedia.** { <init>(...); *; }
-keep enum org.wikipedia.** { <init>(...); *; }
# --- /Wikipedia ---
