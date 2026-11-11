-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager 2.7.0 / Room 2.2.5 are bundled transitively by the ads stack.
# Under AGP 9 / R8 full mode, WorkManager reflects the generated WorkDatabase
# implementation. Preserve RoomDatabase subclass constructors so the generated
# WorkDatabase_Impl can be instantiated during AndroidX Startup.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
