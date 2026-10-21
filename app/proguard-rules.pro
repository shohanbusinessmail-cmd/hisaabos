# HisaabOS ProGuard / R8 rules
# The app uses no reflection-heavy libraries beyond Room, which ships its own
# consumer rules. Keep the backup DTOs used by kotlinx.serialization explicit.

-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# kotlinx.serialization generated serializers
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.shohankhan.hisaabos.**$$serializer { *; }
-keepclassmembers class com.shohankhan.hisaabos.** {
    *** Companion;
}
-keepclasseswithmembers class com.shohankhan.hisaabos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class com.shohankhan.hisaabos.data.db.** { *; }

# Kotlin metadata is needed for coroutines / Room generated code debugging.
-keep class kotlin.Metadata { *; }
