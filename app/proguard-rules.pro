# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.field.survey.data.remote.dto.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.field.survey.**$$serializer { *; }
-keepclassmembers class com.field.survey.** {
    *** Companion;
}
-keepclasseswithmembers class com.field.survey.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
