package space.visuals.client.modules.impl.render;

import net.minecraft.util.Identifier;
import space.visuals.Zenith;

import java.util.Arrays;

public enum CustomModelType {
    CRAZY_RABBIT("Безумный кролик", Zenith.id("custom_models/rabbit.png"), ModelKey.RABBIT),
    WHITE_DEMON("Белый демон", Zenith.id("custom_models/whitedemon.png"), ModelKey.DEMON),
    RED_DEMON("Красный демон", Zenith.id("custom_models/reddemon.png"), ModelKey.DEMON),
    FREDDY_BEAR("Фредди медведь", Zenith.id("custom_models/freddy.png"), ModelKey.FREDDY),
    AMOGUS("Амогус", Zenith.id("custom_models/amogus.png"), ModelKey.AMOGUS),
    RYABCHIK("Рябчик", Zenith.id("custom_models/ryabchik.png"), ModelKey.RYABCHIK);

    private final String displayName;
    private final Identifier texture;
    private final ModelKey modelKey;

    CustomModelType(String displayName, Identifier texture, ModelKey modelKey) {
        this.displayName = displayName;
        this.texture = texture;
        this.modelKey = modelKey;
    }

    public String getDisplayName() { return this.displayName; }
    public Identifier getTexture() { return this.texture; }
    public ModelKey getModelKey() { return this.modelKey; }

    public static CustomModelType fromDisplay(String name) {
        if (name == null) return null;
        return Arrays.stream(CustomModelType.values())
                .filter(t -> t.displayName.equalsIgnoreCase(name) || t.matchesLegacyName(name))
                .findFirst().orElse(null);
    }

    private boolean matchesLegacyName(String name) {
        return switch (this) {
            case CRAZY_RABBIT -> "Crazy Rabbit".equalsIgnoreCase(name);
            case WHITE_DEMON  -> "White Demon".equalsIgnoreCase(name);
            case RED_DEMON    -> "Red Demon".equalsIgnoreCase(name);
            case FREDDY_BEAR  -> "Freddy Bear".equalsIgnoreCase(name);
            case AMOGUS       -> "Amogus".equalsIgnoreCase(name);
            case RYABCHIK     -> false;
        };
    }

    public static String[] names() {
        return Arrays.stream(CustomModelType.values()).map(CustomModelType::getDisplayName).toArray(String[]::new);
    }

    public enum ModelKey { RABBIT, DEMON, FREDDY, AMOGUS, RYABCHIK }
}