package space.visuals.client.modules.impl.misc;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;


import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoAuth", category = Category.MISC, description = "Авто регистрация")
public final class AutoAuth extends Module {
    public static final AutoAuth INSTANCE = new AutoAuth();
    private AutoAuth() {
    }
    @EventTarget
    public void onReceive(EventPacket event) {
        if(!event.isReceive())return;
        if (event.getPacket() instanceof GameMessageS2CPacket chatMessagePacket) {
            if (mc.getNetworkHandler() == null) return;

            String password = "123123qq";
            String content = chatMessagePacket.content().getString().toLowerCase();

            if (content.contains("зарегистрируйтесь") || content.contains("/register")) {
                mc.getNetworkHandler().sendChatCommand("register %s %s".formatted(password, password));
            }
        }
    }
}
