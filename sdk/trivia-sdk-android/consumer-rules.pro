# Moshi uses Kotlin reflection for these internal transport DTOs.
-keep class com.dosparta.trivia.data.remote.dto.** { *; }

# Gson persists this model in the existing Room schema; preserve JSON field names.
-keep class com.dosparta.trivia.domain.model.TriviaQuestion { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.dosparta.trivia.data.mapper.GameSessionMapper$* { *; }