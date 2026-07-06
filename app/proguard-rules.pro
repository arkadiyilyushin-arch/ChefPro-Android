-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.chefpro.model.**$$serializer { *; }
-keepclassmembers class com.chefpro.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.chefpro.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
