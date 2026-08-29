# Keep stack traces readable in release builds. Without these, a crash report
# from a user is obfuscated line numbers with no source file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room, Hilt, Compose and kotlinx.serialization all ship consumer rules, so
# nothing is needed for them here. Serialization is only used for List<String>
# type converters, which uses built-in serializers rather than reflection over
# @Serializable classes.
