# Gson reads these three offline yao-text DTOs reflectively. Keep this tiny
# serialization boundary while allowing R8 to optimize the rest of the app.
-keep class com.boompala.engine.data.JsonLineTextRepository$Dataset { *; }
-keep class com.boompala.engine.data.JsonLineTextRepository$HexagramEntry { *; }
-keep class com.boompala.engine.data.JsonLineTextRepository$LineEntry { *; }

# Gson reads the validated offline general-reference dataset reflectively.
-keep class com.boompala.engine.data.JsonHexagramInterpretationRepository$Dataset { *; }
-keep class com.boompala.engine.data.JsonHexagramInterpretationRepository$Source { *; }
-keep class com.boompala.engine.data.JsonHexagramInterpretationRepository$TrigramEntry { *; }
-keep class com.boompala.engine.data.JsonHexagramInterpretationRepository$HexagramEntry { *; }
