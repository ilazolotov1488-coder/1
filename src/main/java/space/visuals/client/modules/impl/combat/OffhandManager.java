package space.visuals.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.ItemSlotSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;

import java.util.List;

@ModuleAnnotation(
        name = "OffhandManager",
        category = Category.COMBAT,
        description = "Свапает предмет на урон когда HP врага упало ниже порога, иначе держит предмет на броню"
)
public final class OffhandManager extends Module {
    public static final OffhandManager INSTANCE = new OffhandManager();

    // ── Предметы ──────────────────────────────────────────────────────────────
    private final ItemSlotSetting damageItem  = new ItemSlotSetting("Предмет на урон");
    private final ItemSlotSetting defenseItem = new ItemSlotSetting("Предмет на броню");

    // ── Условия ───────────────────────────────────────────────────────────────
    private final NumberSetting hpAdvantage  = new NumberSetting("Преимущество HP",  6f, 1f, 20f, 0.5f);
    private final NumberSetting swapCooldown = new NumberSetting("Кулдаун (тики)",   20f, 5f, 60f,  1f);

    // ── Состояние свапа ───────────────────────────────────────────────────────
    private boolean swapping      = false;
    private int     swapTick      = 0;
    private int     cooldownTimer = 0;
    private Slot    pendingSlot   = null;

    /**
     * Текущий желаемый режим:
     *   true  = урон (damageItem в оффхенде)
     *   false = броня (defenseItem в оффхенде)
     * null = ещё не инициализировано, нужно форсировать первый свап
     */
    private Boolean wantDamage = null;

    private OffhandManager() {}

    @Native
    @Override
    public List<Setting> getSettings() {
        return List.of(damageItem, defenseItem, hpAdvantage, swapCooldown);
    }

    @Native
    @Override
    public void onEnable() {
        wantDamage    = null;
        swapping      = false;
        swapTick      = 0;
        cooldownTimer = 0;
        pendingSlot   = null;
        super.onEnable();
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (swapping) {
            doSwapTick();
            return;
        }

        if (cooldownTimer > 0) { cooldownTimer--; return; }

        PlayerEntity target = getNearestEnemy();
        boolean needDamage = target != null && isEnemyLow(target);

        if (wantDamage != null && wantDamage == needDamage) return;

        wantDamage = needDamage;

        if (needDamage) {
            if (damageItem.isEmpty()) return;
            if (!mc.player.getOffHandStack().isEmpty()
                    && mc.player.getOffHandStack().getItem() == damageItem.getStack().getItem()) return;
            Slot slot = findSlot(damageItem.getStack());
            if (slot == null) return;
            startSwap(slot);
        } else {
            if (defenseItem.isEmpty()) return;
            if (!mc.player.getOffHandStack().isEmpty()
                    && mc.player.getOffHandStack().getItem() == defenseItem.getStack().getItem()) return;
            Slot slot = findSlot(defenseItem.getStack());
            if (slot == null) return;
            startSwap(slot);
        }
    }

    @Native
    private void startSwap(Slot slot) {
        pendingSlot = slot;
        swapping    = true;
        swapTick    = 0;
    }

    @Native
    private void doSwapTick() {
        if (pendingSlot == null) { swapping = false; return; }

        PlayerInventoryComponent.addTask(() -> {
            if (isWPressed())    mc.options.forwardKey.setPressed(true);
            if (isAPressed())    mc.options.leftKey.setPressed(true);
            if (isDPressed())    mc.options.rightKey.setPressed(true);
            if (isSPressed())    mc.options.backKey.setPressed(true);
            if (isJumpPressed()) mc.options.jumpKey.setPressed(true);

            if (swapTick >= 2) {
                ItemStack incoming = pendingSlot.getStack().copy();
                PlayerInventoryUtil.swapHand(pendingSlot, Hand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);

                if (space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.isEnabled()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(incoming);
                }

                swapping      = false;
                swapTick      = 0;
                pendingSlot   = null;
                cooldownTimer = (int) swapCooldown.getCurrent();
            } else {
                swapTick++;
                mc.options.jumpKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
            }
        });
    }

    @Native
    private boolean isEnemyLow(PlayerEntity target) {
        float myHp    = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float enemyHp = target.getHealth()    + target.getAbsorptionAmount();
        return enemyHp <= (myHp - hpAdvantage.getCurrent());
    }

    @Native
    private PlayerEntity getNearestEnemy() {
        if (mc.world == null || mc.player == null) return null;
        Box box = mc.player.getBoundingBox().expand(20);
        PlayerEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getEntitiesByClass(PlayerEntity.class, box,
                e -> e != mc.player && e.isAlive())) {
            if (Zenith.getInstance().getFriendManager().isFriend(p.getGameProfile().getName())) continue;
            double dist = mc.player.distanceTo(p);
            if (dist < minDist) { minDist = dist; nearest = p; }
        }
        return nearest;
    }

    @Native
    private Slot findSlot(ItemStack target) {
        if (target == null || target.isEmpty()) return null;
        return PlayerInventoryUtil.getSlot(target.getItem(),
                java.util.Comparator.comparing(s -> s.getStack().hasEnchantments()),
                s -> s.id != 45 && s.id != 46);
    }

    @Native
    @Override
    public void onDisable() {
        swapping      = false;
        swapTick      = 0;
        pendingSlot   = null;
        cooldownTimer = 0;
        wantDamage    = null;
        super.onDisable();
    }

    // ── Клавиши движения ──────────────────────────────────────────────────────
    @Native private boolean isWPressed()    { return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_W)     == org.lwjgl.glfw.GLFW.GLFW_PRESS; }
    @Native private boolean isAPressed()    { return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_A)     == org.lwjgl.glfw.GLFW.GLFW_PRESS; }
    @Native private boolean isDPressed()    { return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_D)     == org.lwjgl.glfw.GLFW.GLFW_PRESS; }
    @Native private boolean isSPressed()    { return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_S)     == org.lwjgl.glfw.GLFW.GLFW_PRESS; }
    @Native private boolean isJumpPressed() { return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS; }
}
