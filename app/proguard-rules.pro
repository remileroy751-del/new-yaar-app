# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Keep data models used by Room
-keep class com.yaarapp.app.data.** { *; }
