# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.lux.field.data.remote.dto.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lux.field.**$$serializer { *; }
-keepclassmembers class com.lux.field.** {
    *** Companion;
}
-keepclasseswithmembers class com.lux.field.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
