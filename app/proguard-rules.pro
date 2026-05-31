-dontobfuscate

-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern

# TODO: remove after reading challenge widget is removed
-keep class * implements androidx.glance.appwidget.action.ActionCallback { public <init>(); }

