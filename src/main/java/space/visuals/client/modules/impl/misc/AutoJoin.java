package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.utility.math.Timer;

@ModuleAnnotation(name = "AutoJoin", category = Category.MISC, description = "Автоматически заходит на режим")
public final class AutoJoin extends Module {

    public static final AutoJoin INSTANCE = new AutoJoin();

    private final ModeSetting mode = new ModeSetting("Режим", "Дуэли SpookyTime", "Гриф RW/FT/Spooky");
    private final Timer timer = new Timer();

    private AutoJoin() {}

    private boolean isST() { return Zenith.getInstance().getServerHandler().getServer().equals("CopyTime"); }
    private boolean isFT() { return Zenith.getInstance().getServerHandler().getServer().equals("FunTime"); }
    private boolean isRW() { return Zenith.getInstance().getServerHandler().getServer().equals("ReallyWorld"); }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (isST() && mode.get().equals("Дуэли SpookyTime")) {
            Slot compass = PlayerInventoryUtil.getSlot(Items.COMPASS);
            Slot sword   = PlayerInventoryUtil.getSlot(Items.DIAMOND_SWORD);

            if (compass != null && timer.finished(300)) {
                int hotbarSlot = compass.id - 36;
                if (hotbarSlot >= 0 && hotbarSlot <= 8) {
                    mc.player.getInventory().selectedSlot = hotbarSlot;
                }
                mc.interactionManager.sendSequencedPacket(mc.world,
                    seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, mc.player.getYaw(), mc.player.getPitch()));

                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler
                        && mc.currentScreen != null
                        && mc.currentScreen.getTitle().getString().contains("Выберите режим")) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 14, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
                timer.reset();
            }

            if (sword != null) {
                this.toggle();
            }
        }

        if ((isFT() || isRW() || isST()) && mode.get().equals("Гриф RW/FT/Spooky")) {
            mc.player.networkHandler.sendChatCommand("an306");
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (!(isFT() || isRW() || isST())) return;
        if (!mode.get().equals("Гриф RW/FT/Spooky")) return;

        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString().toLowerCase();
            if (isFT() ? msg.contains("вы уже подключены к этому серверву!") : msg.contains("вы уже подключены на этот сервер!")) {
                this.toggle();
            }
        }
    }
}
