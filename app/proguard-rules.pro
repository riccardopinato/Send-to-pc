-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AGP 9 / R8 strict full-mode compatibility for transitively bundled Room 2.2.5.
# WorkManager 2.7.0 reflects WorkDatabase_Impl and requires its zero-arg constructor.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
