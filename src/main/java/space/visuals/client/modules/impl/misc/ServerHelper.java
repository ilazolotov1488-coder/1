package space.visuals.client.modules.impl.misc;


import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.render.EventRender2D;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.impl.render.Predictions;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.request.ScriptManager;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerIntersectionUtil;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.utility.math.MathUtil;
import space.visuals.utility.math.ProjectionUtil;
import space.visuals.utility.other.BooleanSettable;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.base.color.ColorUtil;
import space.visuals.utility.render.display.shader.DrawUtil;
import space.visuals.utility.render.level.Render3DUtil;


import com.adl.nativeprotect.Native;
import java.util.*;
import java.util.List;

@ModuleAnnotation(name = "ServerHelper", category = Category.MISC, description = "")
public final class ServerHelper extends Module {

    private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    private final List<Structure> structures = new ArrayList<>();
    private final List<KeyBind> keyBindings = new ArrayList<>();
    // Временные вейпоинты ивентов: имя → время удаления (ms)
    private final Map<String, Long> tempEventWaypoints = new HashMap<>();
    

    private final Map<BlockPos, Boolean> trapCache = new HashMap<>();
    private final Map<BlockPos, Boolean> bigTrapCache = new HashMap<>();
    private static final long CACHE_DURATION = 1000;
    private final Map<BlockPos, Long> trapCacheTime = new HashMap<>();
    private final Map<BlockPos, Long> bigTrapCacheTime = new HashMap<>();

    public static final ServerHelper INSTANCE = new ServerHelper();

    private ServerHelper() {
        initialize();
    }

    private final BooleanSetting consumablesSetting = new BooleanSetting("Таймер расходников", true);
    private final BooleanSetting autoPointSetting = new BooleanSetting("Авто точка", true);
    private final ModeSetting serverMode = new ModeSetting("Сервер", "HolyWorld", "FunTime");

    @Native
    public void initialize() {
        // ── HolyWorld ──────────────────────────────────────────────────────────
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD,
                new KeySetting("Взрывная трапка"), 5, new BooleanSettable(), "HolyWorld"));

        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT,
                new KeySetting("Обыч трапка"), 0, new BooleanSettable(), "HolyWorld"));

        keyBindings.add(new KeyBind(Items.NETHER_STAR,
                new KeySetting("Стан"), 30, new BooleanSettable(), "HolyWorld"));

        keyBindings.add(new KeyBind(Items.FIRE_CHARGE,
                new KeySetting("Взрывная штука"), 0, new BooleanSettable(), "HolyWorld"));

        keyBindings.add(new KeyBind(Items.SNOWBALL,
                new KeySetting("Снежок"), 0, new BooleanSettable(), "HolyWorld"));

        // ── FunTime ────────────────────────────────────────────────────────────
        keyBindings.add(new KeyBind(Items.SNOWBALL,
                new KeySetting("Снежок"), 0, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE,
                new KeySetting("Божья аура"), 0, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP,
                new KeySetting("Трапка"), 0, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.DRIED_KELP,
                new KeySetting("Пласт"), 0, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.SUGAR,
                new KeySetting("Явная пыль"), 10, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.FIRE_CHARGE,
                new KeySetting("Огненный смерч"), 10, new BooleanSettable(), "FunTime"));

        keyBindings.add(new KeyBind(Items.ENDER_EYE,
                new KeySetting("Дезорент"), 10, new BooleanSettable(), "FunTime"));

        // Привязываем видимость каждого бинда к выбранному серверу
        keyBindings.forEach(bind ->
            bind.setting().setVisible(() -> serverMode.is(bind.server()))
        );
    }


    @Native
    @Override
    public List<Setting> getSettings() {
        ArrayList<Setting> settings = new ArrayList<>(List.of(consumablesSetting, autoPointSetting, serverMode));
        settings.addAll(keyBindings.stream().map(KeyBind::setting).toList());
        return settings;
    }


    @Native
    @EventTarget
    public void onKey(EventKey e) {
        if (e.getAction() == GLFW.GLFW_RELEASE) {
            keyBindings.stream().filter(bind -> e.is(bind.setting.getKeyCode()) && bind.setting.getVisible().get()).forEach(bind -> {
                if (mc.currentScreen == null) swapAndUseWithReset(bind.item);
                bind.draw.setValue(false);
            });
            return;
        }
        if (mc.currentScreen != null) return;

        keyBindings.stream().filter(bind -> e.is(bind.setting.getKeyCode()) && bind.setting.getVisible().get() && PlayerInventoryUtil.getSlot(bind.item, slot -> slot.getStack().get(DataComponentTypes.CUSTOM_DATA) != null) != null).forEach(bind -> bind.draw.setValue(true));
    }

    // Свап с 2 тиками сброса движения — своп пакетом в руку, использовать, вернуть
    @Native
    private void swapAndUseWithReset(net.minecraft.item.Item item) {
        if (mc.player == null) return;
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0f);
        if (cooldown > 0) return;
        net.minecraft.screen.slot.Slot slot = PlayerInventoryUtil.getSlot(item);
        if (slot == null) return;

        // Уведомление о свапе предмета
        if (space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.isEnabled()
                && space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.serverHelper.isEnabled()) {
            Zenith.getInstance().getNotifyManager().addSwapNotification(slot.getStack());
        }

        space.visuals.utility.game.player.rotation.Rotation angle = Zenith.getInstance().getRotationManager().getCurrentRotation();
        int prevSlot = mc.player.getInventory().selectedSlot;
        boolean inHotbar = slot.id >= 36 && slot.id <= 44;

        ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(task);

        // Тик 1: сброс спринта и движения
        task.schedule(EventUpdate.class, ev -> {
            mc.options.sprintKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return true;
        });
        // Тик 2: сброс спринта и движения
        task.schedule(EventUpdate.class, ev -> {
            mc.options.sprintKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return true;
        });
        if (inHotbar) {
            task.schedule(EventUpdate.class, ev -> {
                mc.player.getInventory().selectedSlot = slot.id - 36;
                return true;
            });
            task.schedule(EventUpdate.class, ev -> true); // +1 тик
            task.schedule(EventUpdate.class, ev -> {
                PlayerIntersectionUtil.useItem(net.minecraft.util.Hand.MAIN_HAND, angle);
                return true;
            });
            task.schedule(EventUpdate.class, ev -> true); // +1 тик после
            task.schedule(EventUpdate.class, ev -> {
                mc.player.getInventory().selectedSlot = prevSlot;
                return true;
            });
        } else {
            task.schedule(EventUpdate.class, ev -> {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    slot.id, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                PlayerInventoryUtil.closeScreen(true);
                return true;
            });
            task.schedule(EventUpdate.class, ev -> true); // +1 тик
            task.schedule(EventUpdate.class, ev -> {
                PlayerIntersectionUtil.useItem(net.minecraft.util.Hand.MAIN_HAND, angle);
                return true;
            });
            task.schedule(EventUpdate.class, ev -> true); // +1 тик после
            task.schedule(EventUpdate.class, ev -> {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    slot.id, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                PlayerInventoryUtil.closeScreen(true);
                return true;
            });
        }

        // 4 тика после возврата предмета — спринт не возобновляем
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { restoreMoveKeys(); return true; });
    }

    @Native
    private void restoreMoveKeys() {
        long win = mc.getWindow().getHandle();
        mc.options.sprintKey.setPressed(GLFW.glfwGetKey(win,  GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS);
        mc.options.forwardKey.setPressed(GLFW.glfwGetKey(win, GLFW.GLFW_KEY_W)     == GLFW.GLFW_PRESS);
        mc.options.leftKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_A)     == GLFW.GLFW_PRESS);
        mc.options.rightKey.setPressed(GLFW.glfwGetKey(win,   GLFW.GLFW_KEY_D)     == GLFW.GLFW_PRESS);
        mc.options.backKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_S)     == GLFW.GLFW_PRESS);
        mc.options.jumpKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
    }


    @Native
    @EventTarget
    public void onPacket(EventPacket e) {

        if(this.consumablesSetting.isEnabled()){
            if ( Zenith.getInstance().getServerHandler().isCopyTime()&&e.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDelta) {
                chunkDelta.visitUpdates((pos, state) -> blockStateMap.put(pos.add(0, 0, 0), state));
                chunkDelta.visitUpdates((pos, state) -> {
                    Vec3d vec = pos.add(0, 0, 0).toCenterPos();
                    if (blockStateMap.size() > 50 && blockStateMap.size() < 600) {
                        if (isTrap(pos.up(2)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 15000);
                        else if (isBigTrap(pos.up(3)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 30000);
                    }
                });
            }
            if (e.getPacket() instanceof PlaySoundS2CPacket soundS2CPacket) {
                if (Zenith.getInstance().getServerHandler().isHolyWorld() && soundS2CPacket.getSound().toString().equals("Reference{ResourceKey[minecraft:sound_event / minecraft:block.beacon.deactivate]=SoundEvent[location=minecraft:block.beacon.deactivate, fixedRange=Optional.empty]}")) {
                    addStructure(Items.NETHER_STAR, new Vec3d(soundS2CPacket.getX(), soundS2CPacket.getY(), soundS2CPacket.getZ()), System.currentTimeMillis() + 15000);
                }

            }
            if (Zenith.getInstance().getServerHandler().isHolyWorld() && e.getPacket() instanceof ParticleS2CPacket particleS2CPacket) {


                String particleType = particleS2CPacket.getParameters().getType().toString();

                if (particleType.contains("ExplosionSmokeParticle")) {
                    addStructure(Items.PRISMARINE_SHARD, new Vec3d(particleS2CPacket.getX(), particleS2CPacket.getY(), particleS2CPacket.getZ()), System.currentTimeMillis() + 11000);

                }
            }
        }
        if (e.getPacket() instanceof GameMessageS2CPacket gameMessage && autoPointSetting.isEnabled() && autoPointSetting.getVisible().get()) {
            Text content = gameMessage.content();
            String contentString = content.toString();
            String message = content.getString();
            String name = StringUtils.substringBetween(message, "|||   [", "]   ");
            if (name != null) {
                String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                String lvl = StringUtils.substringBetween(message, "Уровень лута: ", "\n ║");
                String owner = StringUtils.substringBetween(message, "Призван игроком: ", "\n ║");
                if (position != null) {
                    String[] pose = position.split(" ");
                    Vec3d center = BlockPos.ofFloored(Integer.parseInt(pose[0]), Integer.parseInt(pose[1]), Integer.parseInt(pose[2])).toCenterPos();
                    switch (name) {
                        case "Мистический сундук" -> addEvent(name, lvl, owner, center, "overworld", 300, 0);
                        case "Вулкан" -> addEvent(name, lvl, owner, center, "overworld", 300, 120);
                        case "Метеоритный дождь", "Маяк убийца", "Мистический Алтарь" ->
                                addEvent(name, lvl, owner, center, "overworld", 360, 0);
                        case "Загадочный маяк" -> addEvent(name, lvl, owner, center, "overworld", 60, 180);
                    }
                } else {
                    switch (name) {
                        case "Сундук смерти" ->
                                addEvent(name, lvl, owner, BlockPos.ofFloored(-155, 64, 205).toCenterPos(), "lobby", 300, 0);
                        case "Адская резня" ->
                                addEvent(name, lvl, owner, BlockPos.ofFloored(48, 87, 73).toCenterPos(), "lobby", 180, 120);
                    }
                }
            }
        }
    }


    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        // Удаляем просроченные временные вейпоинты ивентов
        long now = System.currentTimeMillis();
        tempEventWaypoints.entrySet().removeIf(entry -> {
            if (now >= entry.getValue()) {
                // Удаляем вейпоинт по имени (с owner или без)
                Zenith.getInstance().getWaypointManager().getWaypoints()
                    .stream()
                    .filter(w -> w.name.startsWith(entry.getKey()) && w.temporary)
                    .findFirst()
                    .ifPresent(w -> Zenith.getInstance().getWaypointManager().remove(w.name));
                return true;
            }
            return false;
        });
    }

    @Native
    @EventTarget
    public void onWorldRender(EventRender3D e) {
        long currentTime = System.currentTimeMillis();
        if (currentTime % 5000 < 16) {
            cleanCache();
        }
        
        MatrixStack matrix = e.getMatrix();
        keyBindings.stream().filter(bind -> bind.draw.isValue()).forEach(bind -> {
            BlockPos playerPos = mc.player.getBlockPos();
            Vec3d smooth = MathUtil.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.prevX, mc.player.prevY, mc.player.prevZ)), Vec3d.of(playerPos)).subtract(Vec3d.of(playerPos));
            switch (bind.setting.getName()) {
                case "Трапка", "Обыч трапка" ->
                        drawItemCube(playerPos, smooth, 1.99F, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB());
                case "Дезорент", "Огненный смерч", "Явная пыль" ->
                        drawItemRadius(matrix, bind.distance, ColorUtil.LIGHT_RED);
                case "Взрывная штука" ->
                        drawItemRadius(matrix, 5, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB());
                case "Пласт" -> {
                    float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
                    if (Math.abs(mc.player.getPitch()) > 60) {
                        BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
                        Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -157.5F || yaw >= 157.5F) {
                        BlockPos blockPos = playerPos.north(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -112.5F) {
                        drawSidePlast(playerPos.east(5).south().down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), -1, true);
                    } else if (yaw <= -67.5F) {
                        BlockPos blockPos = playerPos.east(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -22.5F) {
                        drawSidePlast(playerPos.east(5).down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 1, false);
                    } else if (yaw >= -22.5 && yaw <= 22.5) {
                        BlockPos blockPos = playerPos.south(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= 67.5F) {
                        drawSidePlast(playerPos.west(4).down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 1, true);
                    } else if (yaw <= 112.5F) {
                        BlockPos blockPos = playerPos.west(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= 157.5F) {
                        drawSidePlast(playerPos.west(4).south().down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), -1, false);
                    }
                }
                case "Взрывная трапка" -> drawItemCube(playerPos, smooth, 3.99F, ColorUtil.LIGHT_RED);
                case "Стан" -> drawItemCube(playerPos, smooth, 15.01F, ColorUtil.LIGHT_RED);
                case "Снежок" ->
                        Predictions.INSTANCE.drawPredictionInHand(matrix, List.of(Items.SNOWBALL.getDefaultStack()));
            }
        });
    }

    @Native
    @EventTarget
    public void onDraw(EventRender2D e) {
        DrawContext context = e.getContext();
        MatrixStack matrix = context.getMatrices();

        structures.forEach(cons -> {
            double time = (cons.time - System.currentTimeMillis()) / 1000;
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(cons.vec);

            String text = MathUtil.round(time, 0.1F) + "с";
            Font font = Fonts.MEDIUM.getFont(10);
            float width = font.width(text);
            float posX = (float) (vec3d.x - width / 2);
            float posY = (float) vec3d.y;
            float padding = 2;

            if (ProjectionUtil.canSee(cons.vec) && cons.anarchy == Zenith.getInstance().getServerHandler().getAnarchy() && Zenith.getInstance().getServerHandler().getWorldType().equals(cons.world)) {
//                blur.render(ShapeProperties.create(matrix, posX - padding, posY - padding, width + padding * 2, 10)
//                        .round(1.5F).color(ColorUtil.HALF_BLACK).build());
                DrawUtil.drawBlurHud(e.getContext().getMatrices(),posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8,22,BorderRadius.all(4), ColorRGBA.WHITE);
                e.getContext().drawRoundedRect(posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8, BorderRadius.all(4), Zenith.getInstance().getThemeManager().getCurrentTheme().getForegroundLight());
                DrawUtil.drawRoundedCorner(e.getContext().getMatrices(),posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8,0.1f,10,Zenith.getInstance().getThemeManager().getCurrentTheme().getColor(),BorderRadius.all(4));

                e.getContext().drawText(font, text, posX + 16 * 0.8f + 4, posY + 2.5f, Zenith.getInstance().getThemeManager().getCurrentTheme().getColor());

                e.getContext().getMatrices().push();
                e.getContext().getMatrices().translate(posX, posY, 0);
                e.getContext().getMatrices().scale(0.8f, 0.8f, 1);
                e.getContext().drawItem(cons.item.getDefaultStack(), 0, 0);
                e.getContext().getMatrices().pop();
            }
        });
        structures.removeIf(cons -> cons.time - System.currentTimeMillis() <= 0);
    }

    @Native
    private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int color) {
        Box box = new Box(playerPos.up()).offset(smooth).contract(0, 0.2f, 0).expand(size);
        boolean inBox = mc.world.getPlayers().stream().anyMatch(ent -> ent != mc.player && box.intersects(ent.getBoundingBox()) && !Zenith.getInstance().getFriendManager().isFriend(ent.getGameProfile().getName()));
        Render3DUtil.drawBox(box, inBox ? Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() : color, 3, true, true, true);
    }

    @Native
    private void drawItemRadius(MatrixStack matrix, float distance, int clr) {
        float playerHalfWidth = mc.player.getWidth() / 2;
        int color = validDistance(distance) ? Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() : clr;

        Vec3d pos = MathUtil.interpolate(mc.player).add(playerHalfWidth, 0.02, playerHalfWidth);
        Vec3d vec3d = pos.subtract(mc.getEntityRenderDispatcher().camera.getPos());
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Vec3d nextCosSin = MathUtil.cosSin(i + 1, size, distance);
            Render3DUtil.vertexLine(matrix, buffer, vec3d.add(cosSin), vec3d.add(cosSin.x, cosSin.y + 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0));
            Render3DUtil.drawLine(pos.add(cosSin), pos.add(nextCosSin), color, 2, true);
        }
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Render3DUtil.vertexLine(matrix, buffer, vec3d.add(cosSin), vec3d.add(cosSin.x, cosSin.y - 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    @Native
    private void draw(MatrixStack matrix, Font font, List<String> list, Vec3d vec3d) {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            String string = list.get(i);
            float width = font.width(string);
            float posX = (float) (vec3d.x - width / 2);

            offsetY += 10;
        }
    }

    @Native
    public void drawSidePlast(BlockPos blockPos, Vec3d smooth, int color, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        float width = 2;
        int quadColor = ColorUtil.multAlpha(color, 0.15F);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawVerticalLines(vec3d, color, width, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawVerticalQuads(vec3d, quadColor, i, ff);
    }

    @Native
    private void drawHorizontalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Vec3d current = vec3d;
        
        Render3DUtil.drawLine(current, current = current.add(x, 0, 0), color, width, true);
        
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(current, current = current.add(0, 0, i), color, width, true);
            Render3DUtil.drawLine(current, current = current.add(x, 0, 0), color, width, true);
        }
        
        Render3DUtil.drawLine(current, current = current.add(0, 0, i), color, width, true);
        Render3DUtil.drawLine(current, current = current.add(x * -2, 0, 0), color, width, true);
        
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(current, current = current.add(0, 0, i * -1), color, width, true);
            Render3DUtil.drawLine(current, current = current.add(x * -1, 0, 0), color, width, true);
        }
        
        Render3DUtil.drawLine(current, current.add(0, 0, i * -2), color, width, true);
    }

    @Native
    private void drawVerticalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawLine(vec3d, vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, i), vec3d.add(0, 5, 0), color, width, true);
        }
        Render3DUtil.drawLine(vec3d = vec3d.add(0, 0, i), vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x * -2, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x * -1, 0, i * -1), vec3d.add(0, 5, 0), color, width, true);
        }
    }

    @Native
    private void drawHorizontalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        vec3d = vec3d.add(0, 1e-3, 0);
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        for (int f = 0; f < 3; f++)
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i), vec3d.add(0, 0, i), color, true);
    }

    @Native
    private void drawVerticalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x * -2, 0, 0), vec3d.add(x * -2, 5, 0), vec3d.add(0, 5, 0), color, true);
        vec3d = vec3d.add(x * -1, 0, 0);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -1), vec3d.add(0, 5, i * -1), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i * -1), vec3d.add(x * -1, 0, 0), vec3d.add(x * -1, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -2), vec3d.add(0, 5, i * -2), vec3d.add(0, 5, 0), color, true);
    }

    @Native
    private void addEvent(String name, String lvl, String owner, Vec3d vec3d, String world, int timeOpen, int timeLoot) {
        // Проверяем что вейпоинта с таким именем ещё нет
        if (tempEventWaypoints.containsKey(name)) return;
        long expireMs = System.currentTimeMillis() + (timeOpen + timeLoot + 90) * 1000L;
        tempEventWaypoints.put(name, expireMs);
        String label = owner != null ? name + " (" + owner + ")" : name;
        // Убираем дубли по имени
        Zenith.getInstance().getWaypointManager().getWaypoints()
            .stream().filter(w -> w.name.equals(label)).findFirst()
            .ifPresent(w -> Zenith.getInstance().getWaypointManager().remove(w.name));
        Zenith.getInstance().getWaypointManager().add(label, vec3d.x, vec3d.y, vec3d.z, true, null);
    }

    @Native
    private void addStructure(Item item, Vec3d vec, double time) {
        if (structures.stream().noneMatch(str -> str.vec.equals(vec))) {
            structures.add(new Structure(item, vec, Zenith.getInstance().getServerHandler().getWorldType(), Zenith.getInstance().getServerHandler().getAnarchy(), time));
        }
    }

    @Native
    private Vector4f getRound(Font font, List<String> list, int i, float width) {
        if (i == 0) {
            float next = font.width(list.get(i + 1));
            return next >= width ? new Vector4f(2, 0, 2, 0) : new Vector4f(2);
        }
        if (i == list.size() - 1) {
            float prev = font.width(list.get(i - 1));
            return prev >= width ? new Vector4f(0, 2, 0, 2) : new Vector4f(2);
        }
        float prev = font.width(list.get(i - 1));
        float next = font.width(list.get(i + 1));
        return prev >= width ? next >= width ? new Vector4f() : new Vector4f(0, 2, 0, 2) : new Vector4f(2);
    }

    @Native
    private boolean validDistance(float dist) {
        return dist == 0 || mc.world.getPlayers().stream().anyMatch(p -> p != mc.player && !Zenith.getInstance().getFriendManager().isFriend(p.getGameProfile().getName()) && mc.player.distanceTo(p) <= dist);
    }

    @Native
    private boolean isTrap(BlockPos center) {
        long currentTime = System.currentTimeMillis();
        if (trapCacheTime.containsKey(center) && currentTime - trapCacheTime.get(center) < CACHE_DURATION) {
            return trapCache.get(center);
        }

        boolean result = checkTrap(center);
        trapCache.put(center, result);
        trapCacheTime.put(center, currentTime);
        return result;
    }

    @Native
    private boolean checkTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 2)) {
            if (MathUtil.getDistance(pos.toCenterPos(), center.toCenterPos()) < 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(2).north().east()) && !pos.equals(center.up(2).north().west()) && !pos.equals(center.up(2).south().east()) && !pos.equals(center.up(2).south().west())) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    @Native
    private boolean isBigTrap(BlockPos center) {
        long currentTime = System.currentTimeMillis();
        if (bigTrapCacheTime.containsKey(center) && currentTime - bigTrapCacheTime.get(center) < CACHE_DURATION) {
            return bigTrapCache.get(center);
        }

        boolean result = checkBigTrap(center);
        bigTrapCache.put(center, result);
        bigTrapCacheTime.put(center, currentTime);
        return result;
    }

    @Native
    private boolean checkBigTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 3)) {
            if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(3))) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    @Native
    private static boolean isSolid(BlockState state) {
        return state != null && !state.isAir();
    }
    
    @Native
    private void cleanCache() {
        long currentTime = System.currentTimeMillis();
        
        trapCacheTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > CACHE_DURATION);
        trapCache.entrySet().removeIf(entry -> !trapCacheTime.containsKey(entry.getKey()));
        
        bigTrapCacheTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > CACHE_DURATION);
        bigTrapCache.entrySet().removeIf(entry -> !bigTrapCacheTime.containsKey(entry.getKey()));
        
        if (trapCache.size() > 1000) {
            trapCache.clear();
            trapCacheTime.clear();
        }
        if (bigTrapCache.size() > 1000) {
            bigTrapCache.clear();
            bigTrapCacheTime.clear();
        }
    }

    @Native
    private boolean matchesMask(BlockPos center,
                                byte[][][] mask,
                                int radius,
                                int tolerance) {
        int inconsistencies = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    byte req = mask[dy + radius][dz + radius][dx + radius];
                    if (req == -1) continue;

                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = blockStateMap.get(m);
                    boolean solid = isSolid(st);


                    if ((req == 1 && !solid) || (req == 0 && solid)) {
                        if (++inconsistencies > tolerance) return false;
                    }
                }
            }
        }
        return true;
    }

    public record KeyBind(Item item, KeySetting setting, float distance, BooleanSettable draw, String server) {
    }

    public record Structure(Item item, Vec3d vec, String world, int anarchy, double time) {
    }
}
