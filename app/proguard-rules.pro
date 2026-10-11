# Room generates direct references, but preserving database metadata makes migrations safer.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn org.conscrypt.**

# SQLCipher JNI entry points.
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
