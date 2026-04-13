package space.visuals.utility.sounds;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SoundRegistry {

    // namespace совпадает с mod ID из fabric.mod.json
    private static final String NS = "zenithdlc";

    public static SoundEvent CLICKGUI_OPEN;
    public static SoundEvent MODULE;
    public static SoundEvent APPLEPAY;
    public static SoundEvent WELCOME;
    public static SoundEvent CRITICAL;
    public static SoundEvent TYPING;
    public static SoundEvent HITSOUND1;
    public static SoundEvent HITSOUND2;
    public static SoundEvent HITSOUND3;
    public static SoundEvent HITSOUND4;
    public static SoundEvent HITSOUND5;
    public static SoundEvent HITSOUND6;
    public static SoundEvent HITSOUND7;
    public static SoundEvent HITSOUND8;

    public static void register() {
        CLICKGUI_OPEN = reg("clickgui_open");
        MODULE        = reg("toggle");
        APPLEPAY      = reg("applepay");
        WELCOME       = reg("welcome");
        CRITICAL      = reg("critical");
        TYPING        = reg("typing");
        HITSOUND1     = reg("hitsound1");
        HITSOUND2     = reg("hitsound2");
        HITSOUND3     = reg("hitsound3");
        HITSOUND4     = reg("hitsound4");
        HITSOUND5     = reg("hitsound5");
        HITSOUND6     = reg("hitsound6");
        HITSOUND7     = reg("hitsound7");
        HITSOUND8     = reg("hitsound8");
    }

    private static SoundEvent reg(String name) {
        Identifier id = Identifier.of(NS, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private SoundRegistry() {}
}
