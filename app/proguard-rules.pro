# Release shrinker baseline for Pixel Scanner.
#
# The app does not currently rely on reflection-heavy code paths, so the release
# strategy is to keep the default R8 optimization behavior and preserve only the
# metadata that helps with crash diagnostics and Kotlin/Compose readability.

-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod,Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
