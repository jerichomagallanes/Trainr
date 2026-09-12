# Keep stack traces readable in release builds. Without these, a crash report
# from a user is obfuscated line numbers with no source file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room, Hilt, Compose and kotlinx.serialization all ship consumer rules, so
# nothing is needed for them here. kotlinx.serialization generates serializers
# at compile time, so no keep rules are needed.
