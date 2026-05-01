package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import com.adl.nativeprotect.Native;
import space.visuals.utility.sounds.ClientSounds;

@ModuleAnnotation(name = "Sounds", category = Category.MISC, description = "Добавляет звуки клиента")
public final class Sounds extends Module {

    public static final Sounds INSTANCE = new Sounds();

    public final NumberSetting volumeEnable   = new NumberSetting("Громкость включения",   1.0f, 0.0f, 2.0f, 0.05f);
    public final NumberSetting volumeDisable  = new NumberSetting("Громкость выключения",  1.0f, 0.0f, 2.0f, 0.05f);
    public final NumberSetting volumeGuiOpen  = new NumberSetting("Громкость открытия гуи", 1.0f, 0.0f, 2.0f, 0.05f);
    public final NumberSetting volumeGuiClose = new NumberSetting("Громкость закрытия гуи", 1.0f, 0.0f, 2.0f, 0.05f);

    private Sounds() {}

    @Native
    @Override public void onEnable()  { super.onEnable(); }
    @Native
    @Override public void onDisable() { super.onDisable(); }

    @Native
    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString();
            if (msg.contains("Вы успешно купили") || msg.contains("отправлено игроку")) {
                ClientSounds.APPLEPAY.play(volumeEnable.getCurrent());
            }
        }
    }
}
