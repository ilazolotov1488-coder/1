package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.input.EventMouse;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventHudRender;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.CustomDrawContext;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ModuleAnnotation(name = "Swap+", category = Category.COMBAT, description = "Умный свап между двумя предметами по клавише")
public final class SwapPlus extends Module {
    public static final SwapPlus INSTANCE = new SwapPlus();

    private static final float INNER_R = 54f;
    private static final float OUTER_R = 92f;

    private final ModeSetting mode = new ModeSetting("Режим", "Предметы", "Колесо");
    private final KeySetting bind = new KeySetting("Кнопка свапа");
    private final KeySetting wheelBind = new KeySetting("Бинд колеса");

    private final ModeSetting firstItem = new ModeSetting("Первый", "Тотем", "Шар", "Гепл", "Щит");
    private final ModeSetting secondItem = new ModeSetting("Второй", "Тотем", "Шар", "Гепл", "Щит");

    private final NumberSetting wheelSlots = new NumberSetting("Ячейки", 3f, 3f, 8f, 1f);
    private final NumberSetting swapDelay = new NumberSetting("Делей свапа", 1f, 0f, 5f, 1f);
    private final NumberSetting closeDelay = new NumberSetting("Делей закрытия", 1f, 0f, 5f, 1f);

    // Состояние свапа
    private int step = 0;
    private int aka = 0;
    private int lk = -1;
    private boolean auj = false;
    private int cooldownTicks = 0;

    // Колесо
    private boolean wheelOpen = false;
    private int pendingPickSlot = -1;
    private long removeFlashUntilMs = 0L;
    private int removeFlashIndex = -1;
    private boolean cursorUnlocked = false;
    private final ItemStack[] wheelStacks = new ItemStack[8];

    private SwapPlus() {
        Arrays.fill(wheelStacks, ItemStack.EMPTY);
    }

    @Override
    public List<Setting> getSettings() {
        return List.of(mode, bind, wheelBind, firstItem, secondItem, wheelSlots, swapDelay, closeDelay);
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.player == null) return;
        if (cooldownTicks > 0) cooldownTicks--;
        if (step == 0) return;
        if (aka > 0) { aka--; return; }
        switch (step) {
            case 1 -> {
                if (!auj) mc.setScreen(new InventoryScreen(mc.player));
                nextStep((int) swapDelay.getCurrent());
            }
            case 2 -> {
                doSwap(lk);
                nextStep((int) closeDelay.getCurrent());
            }
            case 3 -> {
                if (!auj && mc.currentScreen instanceof InventoryScreen)
                    mc.currentScreen.close();
                reset();
            }
        }
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (mode.get().equals("Предметы")) {
            if (e.getAction() == 1 && e.is(bind.getKeyCode())) startItemSwap();
            return;
        }
        if (mode.get().equals("Колесо")) {
            if (!e.is(wheelBind.getKeyCode())) return;
            if (e.getAction() == 1) openWheel();
            else if (e.getAction() == 0) releaseWheel();
        }
    }

    @EventTarget
    public void onMouse(EventMouse event) {
        if (mc.player == null) return;
        if (mode.get().equals("Предметы")) {
            if (mc.currentScreen != null) return;
            if (event.getAction() == 1 && event.getButton() == bind.getKeyCode()) startItemSwap();
            return;
        }
        if (mode.get().equals("Колесо")) {
            if (!wheelOpen || mc.currentScreen != null || event.getAction() != 1) return;
            int count = getWheelSlotCount();
            float cx = mc.getWindow().getScaledWidth() / 2f, cy = mc.getWindow().getScaledHeight() / 2f;
            float mx = (float)(mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
            float my = (float)(mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());
            int hover = getHoverIndex(mx, my, cx, cy, INNER_R, OUTER_R + 28f, count);
            if (hover == -1) {
                if (event.getButton() == 0) {
                    int nearest = getAngleIndex(mx, my, cx, cy, count);
                    if (nearest != -1 && !isWheelEmpty(nearest)) startWheelSwap(nearest);
                }
                closeWheel();
                return;
            }
            if (event.getButton() == 1) {
                wheelStacks[hover] = ItemStack.EMPTY;
                removeFlashIndex = hover;
                removeFlashUntilMs = System.currentTimeMillis() + 250L;
                return;
            }
            if (event.getButton() == 0) {
                if (isWheelEmpty(hover)) {
                    pendingPickSlot = hover;
                    mc.setScreen(new InventoryScreen(Objects.requireNonNull(mc.player)));
                    return;
                }
                startWheelSwap(hover);
                closeWheel();
            }
        }
    }

    @EventTarget
    public void onRender(EventHudRender event) {
        renderWheel(event.getContext());
    }

    private void renderWheel(CustomDrawContext ctx) {
        if (!mode.get().equals("Колесо") || !wheelOpen || mc.currentScreen != null || mc.player == null) return;
        updateCursor(true);
        int count = getWheelSlotCount();
        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        float mx = (float)(mc.mouse.getX() * ctx.getScaledWindowWidth() / mc.getWindow().getWidth());
        float my = (float)(mc.mouse.getY() * ctx.getScaledWindowHeight() / mc.getWindow().getHeight());
        int hover = getHoverIndex(mx, my, cx, cy, INNER_R, OUTER_R + 28f, count);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            boolean isHover = i == hover;
            boolean isFlash = i == removeFlashIndex && System.currentTimeMillis() <= removeFlashUntilMs;
            int r = 205, g = 205, b = 205, a = 95;
            if (isHover) { r = 255; g = 209; b = 47; a = 140; }
            if (isFlash) { r = 255; g = 70; b = 70; a = 140; }
            float start = (float)(-Math.PI / 2d + 2d * Math.PI * (i / (double)count));
            float end = (float)(-Math.PI / 2d + 2d * Math.PI * ((i + 1d) / count));
            drawRingSegment(buf, cx, cy, INNER_R, OUTER_R, start, end, r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        for (int i = 0; i < count; i++) {
            if (!isWheelEmpty(i)) {
                float start = (float)(-Math.PI / 2d + 2d * Math.PI * (i / (double)count));
                float end = (float)(-Math.PI / 2d + 2d * Math.PI * ((i + 1d) / count));
                float mid = (start + end) / 2f, iconR = (INNER_R + OUTER_R) / 2f;
                ctx.drawItem(wheelStacks[i], (int)(cx + Math.cos(mid) * iconR - 8), (int)(cy + Math.sin(mid) * iconR - 8));
            }
        }
    }

    // Вызывается из HandledScreenMixin при клике на слот в инвентаре
    public void onClickSlot(int slotId, SlotActionType actionType) {
        if (!mode.get().equals("Колесо") || pendingPickSlot == -1
                || !(mc.currentScreen instanceof InventoryScreen)
                || actionType != SlotActionType.PICKUP) return;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        if (slotId < 0 || slotId >= slots.size()) return;
        ItemStack picked = slots.get(slotId).getStack();
        if (picked == null || picked.isEmpty() || picked.getItem() == Items.AIR) return;
        wheelStacks[pendingPickSlot] = picked.copy();
        pendingPickSlot = -1;
        mc.setScreen(null);
        mc.execute(() -> updateCursor(true));
    }

    public boolean isPendingPick() { return pendingPickSlot != -1; }

    public boolean isWheelOpen() { return wheelOpen; }

    @Override
    public void onDisable() {
        if (!auj && mc.currentScreen instanceof InventoryScreen) mc.currentScreen.close();
        reset();
        closeWheel();
        super.onDisable();
    }

    // --- приватные методы ---

    private void startItemSwap() {
        if (mc.player == null || mc.currentScreen != null || step != 0 || cooldownTicks > 0) return;
        Item item1 = getItemByMode(firstItem.get());
        Item item2 = getItemByMode(secondItem.get());
        Item offhand = mc.player.getOffHandStack().getItem();
        Item target = (offhand == item1) ? item2 : item1;
        int slotId = findSlotByItem(target);
        if (slotId == -1) return;
        startSwap(slotId);
    }

    private void startWheelSwap(int idx) {
        if (mc.player == null || step != 0 || cooldownTicks > 0) return;
        ItemStack saved = isWheelEmpty(idx) ? ItemStack.EMPTY : wheelStacks[idx];
        if (saved.isEmpty()) return;
        if (ItemStack.areItemsAndComponentsEqual(mc.player.getOffHandStack(), saved)) return;
        int slotId = findSlotByStack(saved);
        if (slotId == -1) return;
        startSwap(slotId);
    }

    private void startSwap(int slotId) {
        if (mc.player == null || step != 0 || cooldownTicks > 0) return;
        lk = slotId;
        auj = mc.currentScreen instanceof InventoryScreen;
        step = 1;
        aka = 0;
    }

    private void nextStep(int delay) { step++; aka = delay; }

    private void doSwap(int slotId) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            slotId, 40, SlotActionType.SWAP, mc.player
        );
    }

    private void reset() {
        step = 0; aka = 0; lk = -1; auj = false;
        cooldownTicks = 10;
    }

    private void openWheel() { if (!wheelOpen) { wheelOpen = true; updateCursor(true); } }
    private void releaseWheel() { if (wheelOpen) { wheelOpen = false; removeFlashIndex = -1; removeFlashUntilMs = 0L; updateCursor(false); } }
    private void closeWheel() { wheelOpen = false; pendingPickSlot = -1; removeFlashIndex = -1; removeFlashUntilMs = 0L; updateCursor(false); }

    private void updateCursor(boolean unlock) {
        if (mc == null || mc.mouse == null) return;
        if (unlock) { if (!cursorUnlocked) { mc.mouse.unlockCursor(); cursorUnlocked = true; } }
        else { if (cursorUnlocked) { if (mc.currentScreen == null) mc.mouse.lockCursor(); cursorUnlocked = false; } }
    }

    private boolean isWheelEmpty(int i) {
        if (i < 0 || i >= wheelStacks.length) return true;
        ItemStack s = wheelStacks[i];
        return s == null || s.isEmpty() || s.getItem() == Items.AIR;
    }

    private Item getItemByMode(String name) {
        return switch (name) {
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Шар"   -> Items.PLAYER_HEAD;
            case "Гепл"  -> Items.GOLDEN_APPLE;
            case "Щит"   -> Items.SHIELD;
            default      -> Items.AIR;
        };
    }

    private int getWheelSlotCount() { return MathHelper.clamp(Math.round(wheelSlots.getCurrent()), 3, 8); }

    private int findSlotByItem(Item item) {
        if (mc.player == null) return -1;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        for (Slot s : slots) if (s.id >= 36 && s.id <= 44 && s.getStack().getItem() == item) return s.id;
        for (Slot s : slots) if (s.id >= 9  && s.id <= 35 && s.getStack().getItem() == item) return s.id;
        return -1;
    }

    private int findSlotByStack(ItemStack target) {
        if (mc.player == null || target.isEmpty()) return -1;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        for (Slot s : slots) if (s.id >= 36 && s.id <= 44 && ItemStack.areItemsAndComponentsEqual(s.getStack(), target)) return s.id;
        for (Slot s : slots) if (s.id >= 9  && s.id <= 35 && ItemStack.areItemsAndComponentsEqual(s.getStack(), target)) return s.id;
        String name = target.getName().getString();
        for (Slot s : slots) if (s.id >= 36 && s.id <= 44 && !s.getStack().isEmpty() && s.getStack().getName().getString().equals(name)) return s.id;
        for (Slot s : slots) if (s.id >= 9  && s.id <= 35 && !s.getStack().isEmpty() && s.getStack().getName().getString().equals(name)) return s.id;
        return -1;
    }

    private int getHoverIndex(float mx, float my, float cx, float cy, float innerR, float outerR, int count) {
        float dx = mx - cx, dy = my - cy, dist = (float)Math.sqrt(dx * dx + dy * dy);
        if (dist < innerR || dist > outerR) return -1;
        double ang = Math.atan2(dy, dx) + Math.PI / 2d;
        if (ang < 0) ang += Math.PI * 2d;
        int idx = (int)Math.floor(ang / (Math.PI * 2d) * count);
        return (idx < 0 || idx >= count) ? -1 : idx;
    }

    private int getAngleIndex(float mx, float my, float cx, float cy, int count) {
        double ang = Math.atan2(my - cy, mx - cx) + Math.PI / 2d;
        if (ang < 0) ang += Math.PI * 2d;
        int idx = (int)Math.floor(ang / (Math.PI * 2d) * count);
        return (idx < 0 || idx >= count) ? -1 : idx;
    }

    private void drawRingSegment(BufferBuilder buf, float cx, float cy, float innerR, float outerR, float start, float end, int r, int g, int b, int a) {
        int steps = Math.max(10, (int)(48 * Math.abs(end - start) / (Math.PI * 2f)));
        float st = (end - start) / steps;
        for (int i = 0; i < steps; i++) {
            float a0 = start + st * i, a1 = start + st * (i + 1);
            float x0o = cx + (float)Math.cos(a0) * outerR, y0o = cy + (float)Math.sin(a0) * outerR;
            float x1o = cx + (float)Math.cos(a1) * outerR, y1o = cy + (float)Math.sin(a1) * outerR;
            float x0i = cx + (float)Math.cos(a0) * innerR, y0i = cy + (float)Math.sin(a0) * innerR;
            float x1i = cx + (float)Math.cos(a1) * innerR, y1i = cy + (float)Math.sin(a1) * innerR;
            buf.vertex(x0i, y0i, 0).color(r, g, b, a);
            buf.vertex(x0o, y0o, 0).color(r, g, b, a);
            buf.vertex(x1o, y1o, 0).color(r, g, b, a);
            buf.vertex(x0i, y0i, 0).color(r, g, b, a);
            buf.vertex(x1o, y1o, 0).color(r, g, b, a);
            buf.vertex(x1i, y1i, 0).color(r, g, b, a);
        }
    }
}
