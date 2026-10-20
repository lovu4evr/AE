# Keep EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Keep Retrofit / Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}