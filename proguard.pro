# ─── ProGuard конфиг для Space Visuals (Fabric мод) ───────────────────────────

# Не предупреждать о Minecraft/Fabric классах которых нет в classpath
-dontwarn **
-ignorewarnings

# Не прерываться при неполной иерархии классов
# dontpreverify убирает stackmap frames — добавляем их обратно через target
-dontpreverify

# Отключаем оптимизацию — она требует полной иерархии классов
-dontoptimize

# Не сжимать (shrink) — оставляем все классы
-dontshrink

# ─── Сохраняем ВСЕ имена классов space.visuals ────────────────────────────────
# Обфусцируем только тела методов и имена полей внутри классов
# Это защищает логику но сохраняет совместимость с Fabric/Mixin
-keep class space.visuals.** {
    public protected *;
}
-keepnames class space.visuals.** { *; }

# Mixin аннотации ОБЯЗАТЕЛЬНО сохранять — Mixin ищет их через reflection
-keepattributes *Annotation*
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keep @org.spongepowered.asm.mixin.Mixin interface * { *; }

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
