-dontobfuscate
-dontoptimize
-dontwarn **
-dontnote **

# Keep all extension and source classes and their members
-keep class eu.kanade.tachiyomi.extension.** { *; }
-keep class eu.kanade.tachiyomi.source.** { *; }

# Enum handling
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlinx Serialization
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-keepclassmembers class *$$serializer {
    private ** descriptor;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public <init>(...);
}
