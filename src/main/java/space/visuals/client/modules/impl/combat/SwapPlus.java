package space.visuals.client.modules.impl.combat;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
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
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.input.EventMouse;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventHudRender;
import space.visuals.Zenith;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.utility.render.display.base.CustomDrawContext;
import org.lwjgl.glfw.GLFW;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.Comparator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ModuleAnnotation(name = "Swap+", category = Category.COMBAT, description = "Умный свап между двумя предметами по клавише")
public final class SwapPlus extends Module {
    public static final SwapPlus INSTANCE = new SwapPlus();

    private static final float PANEL_DISTANCE = 70f;
    private static final float PANEL_WIDTH = 52f;
    private static final float PANEL_HEIGHT = 52f;
    private static final float PANEL_RADIUS = 10f;
    
    private long wheelOpenTime = 0L;
    private float wheelAnimProgress = 0f;

    private final ModeSetting mode = new ModeSetting("Режим", "Предметы", "Колесо");
    private final KeySetting bind = new KeySetting("Кнопка свапа");
    private final KeySetting wheelBind = new KeySetting("Бинд колеса");

    private final ModeSetting firstItem = new ModeSetting("Первый", "Тотем", "Шар", "Гепл", "Щит");
    private final ModeSetting secondItem = new ModeSetting("Второй", "Тотем", "Шар", "Гепл", "Щит");

    private final NumberSetting wheelSlots = new NumberSetting("Ячейки", 3f, 3f, 8f, 1f);
    private final NumberSetting swapDelay = new NumberSetting("Делей свапа", 1f, 0f, 5f, 1f);
    private final NumberSetting closeDelay = new NumberSetting("Делей закрытия", 1f, 0f, 5f, 1f);

    // Состояние свапа (режим Предметы и Колесо)
    boolean startSwap = false;
    int swapTick = 0;
    private Slot pendingSwapSlot = null;

    // Колесо
    private boolean wheelOpen = false;
    private int pendingPickSlot = -1;
    private int pendingPickDelay = 0; // задержка перед открытием инвентаря
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

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.player == null) return;
        // Задержка перед открытием инвентаря для выбора предмета в ячейку колеса
        if (pendingPickDelay > 0) {
            pendingPickDelay--;
            if (pendingPickDelay == 0 && pendingPickSlot != -1) {
                mc.setScreen(new InventoryScreen(mc.player));
            }
            return;
        }
        if (!startSwap || pendingSwapSlot == null) return;

        PlayerInventoryComponent.addTask(() -> {
            if (isWPressed())    mc.options.forwardKey.setPressed(true);
            if (isAPressed())    mc.options.leftKey.setPressed(true);
            if (isDPressed())    mc.options.rightKey.setPressed(true);
            if (isSPressed())    mc.options.backKey.setPressed(true);
            if (isJumpPressed()) mc.options.jumpKey.setPressed(true);

            if (swapTick >= 2) {
                // Сохраняем стек ДО свапа — это то что придёт в оффхенд
                ItemStack incomingStack = pendingSwapSlot != null ? pendingSwapSlot.getStack().copy() : ItemStack.EMPTY;
                PlayerInventoryUtil.swapHand(pendingSwapSlot, Hand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
                // Уведомление о свапе — показываем что пришло в руку
                if (space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.isEnabled()
                        && space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.swapPlus.isEnabled()
                        && !incomingStack.isEmpty()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(incomingStack);
                }
                startSwap = false;
                swapTick = 0;
                pendingSwapSlot = null;
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

    @Native(critical = true)
    @CompileToNative
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

    @Native(critical = true)
    @CompileToNative
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
            int hover = getHoverPanelIndex(mx, my, cx, cy, count, wheelAnimProgress);
            if (hover == -1) {
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
                    pendingPickDelay = 4; // ~4 тика задержки перед открытием инвентаря
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
        
        // Анимация появления
        long elapsed = System.currentTimeMillis() - wheelOpenTime;
        wheelAnimProgress = Math.min(1f, elapsed / 200f);
        float eased = easeOutCubic(wheelAnimProgress);
        
        int count = getWheelSlotCount();
        float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
        float mx = (float)(mc.mouse.getX() * ctx.getScaledWindowWidth() / mc.getWindow().getWidth());
        float my = (float)(mc.mouse.getY() * ctx.getScaledWindowHeight() / mc.getWindow().getHeight());
        int hover = getHoverPanelIndex(mx, my, cx, cy, count, eased);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        Tessellator tess = Tessellator.getInstance();
        
        // Проход 1: только панели (без иконок, чтобы drawItem не сбрасывал шейдер)
        float[] panelXs = new float[count];
        float[] panelYs = new float[count];
        for (int i = 0; i < count; i++) {
            boolean isHover = i == hover;
            boolean isFlash = i == removeFlashIndex && System.currentTimeMillis() <= removeFlashUntilMs;
            
            float angle = (float)(2.0 * Math.PI * i / count - Math.PI / 2.0);
            panelXs[i] = cx + (float)Math.cos(angle) * PANEL_DISTANCE * eased;
            panelYs[i] = cy + (float)Math.sin(angle) * PANEL_DISTANCE * eased;
            
            int bgColor = isFlash ? 0xFF3A1A1A : (isHover ? 0xFF2E2E2E : 0xFF1A1A1A);
            int alpha = (int)(eased * (isHover ? 240 : 220));
            bgColor = (bgColor & 0xFFFFFF) | (alpha << 24);
            
            drawRoundedRect(tess, panelXs[i] - PANEL_WIDTH/2, panelYs[i] - PANEL_HEIGHT/2, 
                           PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, bgColor);
        }
        
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        
        // Проход 2: иконки поверх панелей (drawItem меняет RenderSystem, поэтому после геометрии)
        for (int i = 0; i < count; i++) {
            if (!isWheelEmpty(i)) {
                ctx.drawItem(wheelStacks[i], (int)(panelXs[i] - 8), (int)(panelYs[i] - 8));
            } else {
                // Крест для пустой ячейки — небольшой, по центру
                drawCross(tess, panelXs[i], panelYs[i], 5f, (int)(eased * 120));
            }
        }
        
        // Центральный текст через кастомный шрифт
        Font boldFont = Fonts.BOLD.getFont(7f);
        Font subFont = Fonts.MEDIUM.getFont(5.5f);
        int textAlpha = (int)(eased * 200);
        ColorRGBA mainColor = new ColorRGBA(255, 255, 255, textAlpha);
        ColorRGBA subColor = new ColorRGBA(180, 180, 180, (int)(textAlpha * 0.7f));
        
        String centerText = "Для добавления предмета";
        String centerSubtext = "нажмите ЛКМ";
        
        ctx.drawText(boldFont, centerText, cx - boldFont.width(centerText) / 2f, cy - boldFont.height() / 2f - 3, mainColor);
        ctx.drawText(subFont, centerSubtext, cx - subFont.width(centerSubtext) / 2f, cy + boldFont.height() / 2f + 1, subColor);
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
        startSwap = false;
        swapTick = 0;
        pendingSwapSlot = null;
        closeWheel();
        super.onDisable();
    }

    // --- приватные методы ---

    private void startItemSwap() {
        if (mc.player == null || mc.currentScreen != null || startSwap) return;
        Item item1 = getItemByMode(firstItem.get());
        Item item2 = getItemByMode(secondItem.get());
        Item offhand = mc.player.getOffHandStack().getItem();
        Item target = (offhand == item1) ? item2 : item1;
        Slot slot = PlayerInventoryUtil.getSlot(target,
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                s -> s.id != 46 && s.id != 45);
        if (slot == null) return;
        pendingSwapSlot = slot;
        startSwap = true;
    }

    private void startWheelSwap(int idx) {
        if (mc.player == null || startSwap) return;
        ItemStack saved = isWheelEmpty(idx) ? ItemStack.EMPTY : wheelStacks[idx];
        if (saved.isEmpty()) return;
        if (ItemStack.areItemsAndComponentsEqual(mc.player.getOffHandStack(), saved)) return;
        Slot slot = findSlotByStack(saved);
        if (slot == null) return;
        pendingSwapSlot = slot;
        startSwap = true;
    }

    private void openWheel() { 
        if (!wheelOpen) { 
            wheelOpen = true; 
            wheelOpenTime = System.currentTimeMillis();
            wheelAnimProgress = 0f;
            updateCursor(true); 
        } 
    }
    private void releaseWheel() { if (wheelOpen) { wheelOpen = false; removeFlashIndex = -1; removeFlashUntilMs = 0L; updateCursor(false); } }
    private void closeWheel() { wheelOpen = false; pendingPickSlot = -1; pendingPickDelay = 0; removeFlashIndex = -1; removeFlashUntilMs = 0L; updateCursor(false); }

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

    public boolean isWPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W)     == GLFW.GLFW_PRESS; }
    public boolean isAPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_A)     == GLFW.GLFW_PRESS; }
    public boolean isDPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_D)     == GLFW.GLFW_PRESS; }
    public boolean isSPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_S)     == GLFW.GLFW_PRESS; }
    public boolean isJumpPressed() { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS; }

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

    private Slot findSlotByStack(ItemStack target) {
        if (mc.player == null || target.isEmpty()) return null;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        for (Slot s : slots) if (s.id >= 36 && s.id <= 44 && ItemStack.areItemsAndComponentsEqual(s.getStack(), target)) return s;
        for (Slot s : slots) if (s.id >= 9  && s.id <= 35 && ItemStack.areItemsAndComponentsEqual(s.getStack(), target)) return s;
        String name = target.getName().getString();
        for (Slot s : slots) if (s.id >= 36 && s.id <= 44 && !s.getStack().isEmpty() && s.getStack().getName().getString().equals(name)) return s;
        for (Slot s : slots) if (s.id >= 9  && s.id <= 35 && !s.getStack().isEmpty() && s.getStack().getName().getString().equals(name)) return s;
        return null;
    }

    private int getHoverPanelIndex(float mx, float my, float cx, float cy, int count, float animProgress) {
        for (int i = 0; i < count; i++) {
            float angle = (float)(2.0 * Math.PI * i / count - Math.PI / 2.0);
            float panelX = cx + (float)Math.cos(angle) * PANEL_DISTANCE * animProgress;
            float panelY = cy + (float)Math.sin(angle) * PANEL_DISTANCE * animProgress;
            
            float dx = mx - panelX;
            float dy = my - panelY;
            
            if (Math.abs(dx) <= PANEL_WIDTH/2 && Math.abs(dy) <= PANEL_HEIGHT/2) {
                return i;
            }
        }
        return -1;
    }
    
    private float easeOutCubic(float t) {
        return 1f - (float)Math.pow(1f - t, 3);
    }
    
    private void drawCross(Tessellator tess, float cx, float cy, float size, int alpha) {
        float thick = 1.5f;
        int r = 120, g = 120, b = 120;
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        // Линия \ (диагональ 1)
        float dx = size * 0.707f, dy = size * 0.707f;
        float nx = -thick * 0.707f, ny = thick * 0.707f;
        buf.vertex(cx - dx + nx, cy - dy - ny, 0).color(r, g, b, alpha);
        buf.vertex(cx - dx - nx, cy - dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx + dx - nx, cy + dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx - dx + nx, cy - dy - ny, 0).color(r, g, b, alpha);
        buf.vertex(cx + dx - nx, cy + dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx + dx + nx, cy + dy - ny, 0).color(r, g, b, alpha);
        // Линия / (диагональ 2)
        buf.vertex(cx + dx + nx, cy - dy - ny, 0).color(r, g, b, alpha);
        buf.vertex(cx + dx - nx, cy - dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx - dx - nx, cy + dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx + dx + nx, cy - dy - ny, 0).color(r, g, b, alpha);
        buf.vertex(cx - dx - nx, cy + dy + ny, 0).color(r, g, b, alpha);
        buf.vertex(cx - dx + nx, cy + dy - ny, 0).color(r, g, b, alpha);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    private void drawRoundedRect(Tessellator tess, float x, float y, float w, float h, float r, int color) {
        int a = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        // Центральный прямоугольник
        buf.vertex(x + r, y, 0).color(red, g, b, a);
        buf.vertex(x + w - r, y, 0).color(red, g, b, a);
        buf.vertex(x + w - r, y + h, 0).color(red, g, b, a);
        
        buf.vertex(x + r, y, 0).color(red, g, b, a);
        buf.vertex(x + w - r, y + h, 0).color(red, g, b, a);
        buf.vertex(x + r, y + h, 0).color(red, g, b, a);
        
        buf.vertex(x, y + r, 0).color(red, g, b, a);
        buf.vertex(x + r, y + r, 0).color(red, g, b, a);
        buf.vertex(x + r, y + h - r, 0).color(red, g, b, a);
        
        buf.vertex(x, y + r, 0).color(red, g, b, a);
        buf.vertex(x + r, y + h - r, 0).color(red, g, b, a);
        buf.vertex(x, y + h - r, 0).color(red, g, b, a);
        
        buf.vertex(x + w - r, y + r, 0).color(red, g, b, a);
        buf.vertex(x + w, y + r, 0).color(red, g, b, a);
        buf.vertex(x + w, y + h - r, 0).color(red, g, b, a);
        
        buf.vertex(x + w - r, y + r, 0).color(red, g, b, a);
        buf.vertex(x + w, y + h - r, 0).color(red, g, b, a);
        buf.vertex(x + w - r, y + h - r, 0).color(red, g, b, a);
        
        // Углы
        drawCorner(buf, x + r, y + r, r, 180, 270, red, g, b, a);
        drawCorner(buf, x + w - r, y + r, r, 270, 360, red, g, b, a);
        drawCorner(buf, x + w - r, y + h - r, r, 0, 90, red, g, b, a);
        drawCorner(buf, x + r, y + h - r, r, 90, 180, red, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
    
    private void drawCorner(BufferBuilder buf, float cx, float cy, float r, float startDeg, float endDeg, int red, int g, int b, int a) {
        int steps = 8;
        float startRad = (float)Math.toRadians(startDeg);
        float endRad = (float)Math.toRadians(endDeg);
        float step = (endRad - startRad) / steps;
        
        for (int i = 0; i < steps; i++) {
            float a1 = startRad + step * i;
            float a2 = startRad + step * (i + 1);
            
            float x1 = cx + (float)Math.cos(a1) * r;
            float y1 = cy + (float)Math.sin(a1) * r;
            float x2 = cx + (float)Math.cos(a2) * r;
            float y2 = cy + (float)Math.sin(a2) * r;
            
            buf.vertex(cx, cy, 0).color(red, g, b, a);
            buf.vertex(x1, y1, 0).color(red, g, b, a);
            buf.vertex(x2, y2, 0).color(red, g, b, a);
        }
    }
}
