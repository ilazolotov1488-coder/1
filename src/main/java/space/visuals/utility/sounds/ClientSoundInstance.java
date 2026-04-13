package space.visuals.utility.sounds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class ClientSoundInstance extends PositionedSoundInstance {

    private static final String NS = "zenithdlc";
    private final String fileName;

    public ClientSoundInstance(String fileName, float volume) {
        super(
            Identifier.of(NS, fileName),
            SoundCategory.MASTER, // MASTER всегда возвращает 1.0 — не зависит от ползунков
            volume,
            1.0F,
            SoundInstance.createRandom(),
            false,
            0,
            SoundInstance.AttenuationType.NONE,
            0.0, 0.0, 0.0,
            true
        );
        this.fileName = fileName;
    }

    public ClientSoundInstance(String fileName, float volume, float pitch) {
        super(
            Identifier.of(NS, fileName),
            SoundCategory.MASTER,
            volume,
            pitch,
            SoundInstance.createRandom(),
            false,
            0,
            SoundInstance.AttenuationType.NONE,
            0.0, 0.0, 0.0,
            true
        );
        this.fileName = fileName;
    }

    public void play(float volume) {
        MinecraftClient.getInstance().getSoundManager().play(new ClientSoundInstance(this.fileName, volume));
    }

    public void play(float volume, float pitch) {
        MinecraftClient.getInstance().getSoundManager().play(new ClientSoundInstance(this.fileName, volume, pitch));
    }

    public String getFileName() {
        return fileName;
    }
}
