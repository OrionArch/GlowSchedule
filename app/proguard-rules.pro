# Add project specific ProGuard rules here.
# By default, the noise of splitting/merging etc. is kept minimal.

# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase_Impl
-keep class * implements androidx.room.RoomOpenHelper
-dontwarn androidx.room.paging.**

# Keep entities, DAOs, and database structure
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class com.example.schday.data.entity.** { *; }

# Kotlin Serialization Keep Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** $serializer;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class com.example.schday.Main { *; }
-keep class com.example.schday.AddEditCourse { *; }
-keep class com.example.schday.ImportCourses { *; }
