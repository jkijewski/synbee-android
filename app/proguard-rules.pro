# kotlinx.serialization keeps generated serializers through its own rules; nothing extra needed yet.

# Google's code scanner (ML Kit) finds its parts by reflection; shrinking them away crashed "Scan the QR code"
# in the first release build (NullPointerException in GmsBarcodeScanning.getClient).
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
