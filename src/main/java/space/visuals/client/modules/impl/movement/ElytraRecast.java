package space.visuals.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.PlayerInput;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventMoveInput;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.base.rotation.RotationTarget;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.utility.game.player.MovingUtil;
import space.visuals.utility.game.player.PlayerIntersectionUtil;
import space.visuals.utility.game.player.SimulatedPlayer;
import space.visuals.utility.game.player.rotation.Rotation;

@ModuleAnnotation(name = "ElytraRecast", description = "Позволяет выше прыгать на элитрах", category = Category.MOVEMENT)
public final class ElytraRecast extends Module {
    public static final ElytraRecast INSTANCE = new ElytraRecast();

    private ElytraRecast() {

    }




    private int groundTick = 0;
    private boolean changed = false;
    @EventTarget
    public void update(EventMoveInput eventUpdate) {

        if(mc.player.isUsingItem()){
            if (Zenith.getInstance().getServerHandler().isServerSprint()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.player.setSprinting(false);
            }

            groundTick =5;
        }else if(groundTick>0){
            groundTick--;
            return;
        }

        if (!mc.player.isUsingItem()&& !mc.player.isTouchingWater()&&mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && MovingUtil.hasPlayerMovement()) {
            if (mc.player.isOnGround() && mc.player.isWalking()) {
               if(mc.player.canSprint()	&& mc.player.isWalking() &&	 !mc.player.isBlind() && !mc.player.isUsingItem()&& (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater())) {
                    if (!mc.player.isSprinting() && Zenith.getInstance().getServerHandler().isServerSprint()) {
                        mc.player.setSprinting(true);
                    }
                    if (!Zenith.getInstance().getServerHandler().isServerSprint()) {
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                        mc.player.setSprinting(true);
                        changed = true;
                    }
                }else {
                   if (Zenith.getInstance().getServerHandler().isServerSprint()) {
                       mc.player.lastSprinting =true;
                       mc.player.setSprinting(false);
                   }
                   mc.player.setSprinting(false);
               }


                    mc.player.jump();



            } else if (!mc.player.isGliding()) {
                PlayerIntersectionUtil.startFallFlying();


            }

        } else {

            if (changed&&Zenith.getInstance().getServerHandler().isServerSprint()) {
                mc.player.lastSprinting =true;
                mc.player.setSprinting(false);
                changed = false;
            }

        }
        if (groundTick > 0) {

            if (false) {
                rotationManager.setRotation(new RotationTarget(new Rotation(rotationManager.getCurrentRotation().getYaw(), -50), () -> aimManager.rotate(aimManager.getInstantSetup(), new Rotation(rotationManager.getCurrentRotation().getYaw(), -50)), aimManager.getAiSetup()), 2, this);
            }

            groundTick--;
        }

    }

    @Override
    public void onDisable() {
        if (Zenith.getInstance().getServerHandler().isServerSprint() &&changed) {
           // mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.lastSprinting =true;
            mc.player.setSprinting(false);
        }

        super.onDisable();
    }
}
