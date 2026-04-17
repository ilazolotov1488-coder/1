# ─── ProGuard конфиг для Space Visuals (Fabric мод) ───────────────────────────

# Не предупреждать о Minecraft/Fabric классах которых нет в classpath
-dontwarn **
-ignorewarnings

# Не прерываться при неполной иерархии классов
-dontpreverify

# Отключаем оптимизацию — она требует полной иерархии классов
# Обфускация (переименование) всё равно работает
-dontoptimize

# Не сжимать (shrink) — оставляем все классы
-dontshrink

# ─── Сохраняем точки входа Fabric ─────────────────────────────────────────────

# Главный инициализатор мода
-keep class space.visuals.Zenith {
    public *;
}

# Все Mixin классы — нельзя переименовывать (Mixin ищет по имени)
-keep @org.spongepowered.asm.mixin.Mixin class * {
    *;
}

# Все классы с @Inject, @Redirect, @Overwrite и другими Mixin аннотациями
-keepclassmembers class * {
    @org.spongepowered.asm.mixin.injection.Inject *;
    @org.spongepowered.asm.mixin.injection.Redirect *;
    @org.spongepowered.asm.mixin.injection.Overwrite *;
    @org.spongepowered.asm.mixin.injection.ModifyVariable *;
    @org.spongepowered.asm.mixin.injection.ModifyArg *;
    @org.spongepowered.asm.mixin.injection.At *;
}

# EventTarget методы (EventAPI ищет по аннотации через reflection)
-keepclassmembers class * {
    @com.darkmagician6.eventapi.EventTarget *;
}

# Все event классы — передаются в EventTarget методы
-keep class space.visuals.base.events.** { *; }

# Все модули — сохраняем класс и INSTANCE поле (используется через reflection)
-keep class space.visuals.client.modules.** {
    public static final ** INSTANCE;
    public <init>();
}

# Настройки модулей — имена отображаются в UI
-keepclassmembers class * extends space.visuals.client.modules.api.setting.impl.* {
    public <init>(...);
}

# ModuleAnnotation — читается через reflection
-keepattributes *Annotation*
-keepclassmembers class * {
    @space.visuals.client.modules.api.ModuleAnnotation *;
}

# Fabric mod entrypoints
-keep class * implements net.fabricmc.api.ModInitializer { *; }
-keep class * implements net.fabricmc.api.ClientModInitializer { *; }

# Lombok генерирует геттеры/сеттеры — сохраняем публичные методы классов с @Data/@Getter
-keepclassmembers @lombok.Data class * { *; }
-keepclassmembers @lombok.Getter class * { *; }

# Kotlin — сохраняем companion objects и object классы
-keep class **$Companion { *; }
-keepclassmembers class * {
    ** INSTANCE;
}

# Сохраняем имена enum (используются в switch/when)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# Serialization (Gson, JSON)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Атрибуты которые нужно сохранить ─────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# ─── Не обфусцируем Minecraft/Fabric классы (они не в нашем JAR) ──────────────
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep class org.spongepowered.** { *; }
-keep class com.darkmagician6.** { *; }
-keep class ai.djl.** { *; }
-keep class org.jetbrains.** { *; }
-keep class kotlin.** { *; }
