package space.visuals.client.modules.impl.render;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import space.visuals.Zenith;
import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.render.EventHudRender;
import space.visuals.base.events.impl.render.EventRender2D;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.base.theme.ThemeManager;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.MultiBooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.client.modules.impl.misc.NameProtect;
import space.visuals.client.modules.impl.render.FriendMarkers;
import space.visuals.utility.game.other.MessageUtil;
import space.visuals.utility.game.other.TextUtil;
import space.visuals.utility.game.player.PlayerIntersectionUtil;
import space.visuals.utility.math.MathUtil;
import space.visuals.utility.math.ProjectionUtil;
import space.visuals.utility.mixin.client.render.LivingEntityRendererMixin;
import space.visuals.utility.render.display.Render2DUtil;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;
import space.visuals.utility.render.level.Render3DUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import static java.util.Arrays.asList;

@ModuleAnnotation(name = "EntityESP", category = Category.RENDER, description = "ESP")
public final class EntityESP extends Module {
    public static final EntityESP INSTANCE = new EntityESP();

    private final MultiBooleanSetting elements = MultiBooleanSetting.create("Элементы",
            asList("Ники", "Предметы", "Броню","Треугольники","Боксы","Руки"));

    private final ModeSetting textMode = new ModeSetting("Шрифт");
    private final ModeSetting.Value mojo = new ModeSetting.Value(textMode, "Minecraft");
    private final ModeSetting.Value zenith = new ModeSetting.Value(textMode, "space").select();

    private final NumberSetting scale = new NumberSetting("Размер", 1.0f, 0.5f, 2.0f, 0.1f);
    private final BooleanSetting blur = new BooleanSetting("Блюр", "Требует хорошего пк", false, Interface.INSTANCE::isBlur);
    private final BooleanSetting glow = new BooleanSetting("Свечение", "Нужен мощный пк", false, Interface.INSTANCE::isGlow);

    // Кэш видимости entity — обновляем раз в 3 кадра (~50ms), не делаем 5 raycast каждый кадр
    private final java.util.HashMap<Integer, Boolean> visibilityCache = new java.util.HashMap<>();
    private int visibilityCacheFrame = 0;
    private static final int VISIBILITY_CACHE_INTERVAL = 3;

    private EntityESP() {}

    public boolean isRenderName() {
        return this.isEnabled() && this.elements.isEnable(0);
    }

    /**
     * Нужно ли скрывать ванильный нейм тег для данного игрока.
     * Скрываем только если игрок ближе 30 блоков — тогда рисуем свой кастомный.
     * Дальше 30 блоков — оставляем ванильный.
     */
    public boolean shouldHideVanillaName(PlayerEntity player) {
        if (!this.isEnabled() || !this.elements.isEnable(0)) return false;
        if (mc.player == null) return false;
        return player.distanceTo(mc.player) <= 30;
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onHudRender(EventRender3D event) {
        if (!elements.isEnable(4)) return;
        if (mc.world == null || mc.player == null) return;
        for (Entity ent : mc.world.getPlayers()) {
            // Общая оптимизация: дистанция и поле зрения
            if (ent.distanceTo(mc.player) > 30) continue;
            if (!ProjectionUtil.canSee(ent.getBoundingBox())) continue;

            if (ent instanceof PlayerEntity entity) {
                if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
                // Инвизок — не рендерим
                if (entity.isInvisible()) continue;
                // За стеной — не рендерим
                if (!isEntityVisible(entity)) continue;
                Render3DUtil.drawBox((entity.getBoundingBox().offset(MathUtil.interpolate(entity).subtract(entity.getPos()))), Zenith.getInstance().getThemeManager().getClientColor(180).getRGB(), 1f);
            }
        }
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onHudRender(EventRender2D event) {
        Render2DUtil.onRender(event.getContext());
        if (mc.player == null || mc.world == null || mc.getEntityRenderDispatcher().camera == null) return;
        visibilityCacheFrame++;
        // Очищаем кэш раз в 60 кадров чтобы не накапливать мёртвые entity
        if (visibilityCacheFrame % 60 == 0) visibilityCache.clear();
        try {
            RenderSystem.depthMask(false);
            CustomDrawContext context = event.getContext();

            for (Entity ent : mc.world.getEntities()) {
                // Общая оптимизация для всех энтити: дистанция и поле зрения
                float dist = ent.distanceTo(mc.player);
                if (dist > 30) continue; // Дальше 30 блоков — не рендерим
                if (!ProjectionUtil.canSee(ent.getBoundingBox())) continue; // Не в поле зрения — не рендерим

                if (ent instanceof PlayerEntity entity) {
                    if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
                    // Инвизок — не рендерим
                    if (entity.isInvisible()) continue;

                    // За стеной — не рендерим
                    if (!isEntityVisible(entity)) continue;

                    // Ближе 30 блоков, виден и в поле зрения — рендерим всё
                    if (this.elements.isEnable(5)) {
                        Vector4d vec4d = ProjectionUtil.getVector4D(entity);
                        drawHands(event.getContext(), entity, vec4d);
                    }
                    if (this.elements.isEnable(0)) renderNameTag(context, entity, event.getTickDelta());
                    if (elements.isEnable(3)) renderEntityBox(context, entity, event.getTickDelta());
                }

                if (ent instanceof ItemEntity entity) {
                    // За стеной — не рендерим
                    if (!isEntityVisible(entity)) continue;
                    if (this.elements.isEnable(1)) renderItemESP(context, entity, event.getTickDelta());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RenderSystem.depthMask(true);
        }
    }

    /**
     * Проверяет, виден ли хотя бы 1 пиксель сущности (не за стеной).
     * Кэшируется раз в VISIBILITY_CACHE_INTERVAL кадров для экономии raycast.
     */
    private boolean isEntityVisible(Entity entity) {
        int id = entity.getId();
        // Обновляем кэш раз в N кадров
        if (visibilityCacheFrame % VISIBILITY_CACHE_INTERVAL == 0) {
            visibilityCache.remove(id); // сбрасываем для пересчёта
        }
        Boolean cached = visibilityCache.get(id);
        if (cached != null) return cached;

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Box box = entity.getBoundingBox();
        Vec3d[] checkPoints = {
            box.getCenter(),
            new Vec3d(box.getCenter().x, box.maxY - 0.1, box.getCenter().z),
            new Vec3d(box.getCenter().x, box.minY + 0.1, box.getCenter().z),
        };
        boolean visible = false;
        for (Vec3d point : checkPoints) {
            net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(
                    camPos, point,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    mc.player
            );
            if (mc.world.raycast(ctx).getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
                visible = true;
                break;
            }
        }
        visibilityCache.put(id, visible);
        return visible;
    }
    private void drawHands(CustomDrawContext context, PlayerEntity player, Vector4d vec) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) return;

        if(ProjectionUtil.canSee(vec) ) return;
        double posY = vec.w; 
        final float centerX = (float) ProjectionUtil.centerX(vec);

        for (ItemStack stack : player.getHandItems()) {
            if (stack == null || stack.isEmpty()) continue;

            MutableText text = stack.getName().copy();


            if(text.getSiblings().isEmpty()) {
                text = text.copy().setStyle(Style.EMPTY.withColor(Zenith.getInstance().getThemeManager().getCurrentTheme().getWhite().getRGB()));
            }
            if (!text.getString().isEmpty() ) {
                text = TextUtil.replaceLastChar(text, "");

            }

            if (stack.getCount() > 1) {
                text = text.copy().append(Text.of(" x" + stack.getCount()).copy().setStyle(Style.EMPTY.withColor(Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB())));
                ;
            }




                Font font = Fonts.MEDIUM.getFont(8 * scale.getCurrent());
            posY += (font.height() / 2.0f) + 12 + 2;



            float textX = centerX - (font.width(text) / 2.0f);

            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

            ColorRGBA rectColor = Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())
                    ? theme.getColor().withAlpha(theme.getForegroundLight().getAlpha())
                    : theme.getForegroundLight();

            float actualTextWidth = font.width(text) * scale.getCurrent();
            float rectWidth = actualTextWidth + 16 * scale.getCurrent();
            float rectHeight = 12 * scale.getCurrent();
            float rectX = textX - 8 * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, (float) posY, rectWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX, (float) posY, rectWidth, rectHeight, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, (float) posY, rectWidth, rectHeight, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            context.pushMatrix();
            context.getMatrices().translate(textX, (float) posY + 3 * scale.getCurrent(), 0);
            context.drawText(font, text, 0, 0);
            context.popMatrix();
        }
    }
    private void renderNameTag(CustomDrawContext context, PlayerEntity player, float tickDelta) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) return;

        double x = interpolate(player.prevX, player.getX(), tickDelta);
        double y = interpolate(player.prevY, player.getY(), tickDelta);
        double z = interpolate(player.prevZ, player.getZ(), tickDelta);
        Vec3d projected = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y + getPlayerHeight(player), z));

        if (projected.z <= 0 || projected.z >= 1) return;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        ColorRGBA rectColor = Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())
                ? theme.getColor().withAlpha(theme.getForegroundLight().getAlpha())
                : theme.getForegroundLight();
        float posX = 0;
        float posY = 0;
        float textWidth = 0;
        if (mojo.isSelected()) {
            Text playerName = (player == mc.player ||
                    (NameProtect.getCustomName() != null && Zenith.getInstance().getFriendManager().isFriend(player.getNameForScoreboard())))
                    ? Text.of(NameProtect.getCustomName(player.getNameForScoreboard()))
                    : player.getDisplayName();
            if (playerName == null) return;

            playerName = playerName.getString().contains(player.getGameProfile().getName()) ? TextUtil.truncateAfterSubstring(playerName, player.getGameProfile().getName(), false) : TextUtil.truncateAfterSecondSpace(playerName, false);

            playerName = playerName.copy().append(Text.of("  " + PlayerIntersectionUtil.getHealthString(player)).copy().getWithStyle(Style.EMPTY.withColor(getHealthColorRGBA(PlayerIntersectionUtil.getHealth(player)).getRGB())).get(0));


            textWidth = mc.textRenderer.getWidth(playerName);
            posX = (float) projected.x - textWidth / 2;
            posY = (float) projected.y - getPlayerHeight(player);

            context.pushMatrix();
            context.getMatrices().translate(posX + textWidth / 2, posY + 6.5f, 0);
            context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent(), 1f);
            context.getMatrices().translate(-posX - textWidth / 2, -posY - 6.5f, 0);


            float actualTextWidth = mc.textRenderer.getWidth(playerName) * scale.getCurrent();
            float rectWidth = actualTextWidth + 12 * scale.getCurrent();
            float rectHeight = 12 * scale.getCurrent();
            float rectX = posX - 6 * scale.getCurrent();
            float rectY = posY - 13f * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, rectY, rectWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, rectY, rectWidth, rectHeight, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            
            context.pushMatrix();
            context.getMatrices().translate(posX, posY - 13f * scale.getCurrent() + 2 * scale.getCurrent(), 0);
            context.drawText(Fonts.MEDIUM.getFont(10 * scale.getCurrent()), Text.of(playerName), 0, 0);

            context.popMatrix();
        } else {
            Text playerName = (player == mc.player ||
                    (NameProtect.getCustomName() != null && Zenith.getInstance().getFriendManager().isFriend(player.getNameForScoreboard())))
                    ? Text.of(NameProtect.getCustomName(player.getGameProfile().getName()))
                    : player.getDisplayName();
            if (playerName == null) return;
            playerName = playerName.getString().contains(player.getGameProfile().getName()) ? TextUtil.truncateAfterSubstring(playerName, player.getGameProfile().getName(), false) : TextUtil.truncateAfterSecondSpace(playerName, false);
            Text finalText = Text.of("");
            for (Text text: playerName.getSiblings()){

                finalText =finalText.copy().append(text.copy().setStyle(text.getStyle().getColor()==null?Style.EMPTY.withColor(theme.getWhite().getRGB()):text.getStyle()));
            }
            playerName = finalText;
            float hp = PlayerIntersectionUtil.getHealth(player);
            String health = PlayerIntersectionUtil.getHealthString(hp);
            Font font = Fonts.MEDIUM.getFont(8 * scale.getCurrent());

            textWidth = font.width(playerName) + 8 + font.width(health);
            posX = (float) projected.x - textWidth / 2;
            posY = (float) projected.y - getPlayerHeight(player);

            context.pushMatrix();
            context.getMatrices().translate(posX + textWidth / 2, posY + 6.5f, 0);
            context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent(), 1f);
            context.getMatrices().translate(-posX - textWidth / 2, -posY - 6.5f, 0);


            float actualNameWidth = font.width(playerName) * scale.getCurrent();
            float actualHealthWidth = font.width(health) * scale.getCurrent();
            float actualTotalWidth = actualNameWidth + 8 * scale.getCurrent() + actualHealthWidth;
            float rectWidth2 = actualTotalWidth + 16 * scale.getCurrent();
            float rectHeight2 = 12 * scale.getCurrent();
            float rectX2 = posX - 8 * scale.getCurrent();
            float rectY2 = posY - 13f * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX2, rectY2, rectWidth2, rectHeight2, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX2, rectY2, rectWidth2, rectHeight2, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX2, rectY2, rectWidth2, rectHeight2, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            
            context.pushMatrix();
            context.getMatrices().translate(posX, posY - 13f * scale.getCurrent() + 3 * scale.getCurrent(), 0);
            context.drawText(font, playerName, 0, 0);
            context.drawText(font, health, actualNameWidth + 8 * scale.getCurrent(), 0, getHealthColorRGBA(hp));

            context.popMatrix();
        }

        


        if ( this.elements.isEnable(2)) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            stacks.add(player.getOffHandStack());
            stacks.addAll(player.getInventory().armor);
            stacks.add(player.getMainHandStack());

            int nonEmpty = (int) stacks.stream().filter(stack -> !stack.isEmpty()).count();
            if (nonEmpty > 0) {
                float totalWidth = nonEmpty * 18 * scale.getCurrent();
                float centerX = posX + textWidth / 2;
                float rectX = centerX - totalWidth / 2 - 4 * scale.getCurrent();
                float rectY = posY - 35 * scale.getCurrent();
                float rectHeight = 18 * scale.getCurrent();
                
                DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, rectY, totalWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());
                context.drawRoundedRect(rectX, rectY, totalWidth, rectHeight, BorderRadius.all(2), rectColor);

                DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, rectY, totalWidth, rectHeight, 0.1f, 10f, theme.getColor(), BorderRadius.all(2));

                float currentOffset = 0;
                for (ItemStack stack : stacks) {
                    if (!stack.isEmpty()) {
                        context.pushMatrix();
                        context.getMatrices().translate(centerX - totalWidth / 2 + currentOffset, rectY, 0);
                        context.getMatrices().scale(scale.getCurrent(), scale.getCurrent(), 1f);
                        DiffuseLighting.disableGuiDepthLighting();

                        context.drawItem(stack, 0, 1);
                        context.popMatrix();
                        currentOffset += 18 * scale.getCurrent();
                    }
                }
            }
        }



        context.popMatrix();
    }

    private void renderItemESP(CustomDrawContext context, ItemEntity ent, float tickDelta) {

        Vec3d[] corners = getPoints(ent, tickDelta);
        Vector4d screenBB = null;

        for (Vec3d v : corners) {
            Vec3d p = ProjectionUtil.worldSpaceToScreenSpace(v);
            if (p.z > 0 && p.z < 1) {
                if (screenBB == null) screenBB = new Vector4d(p.x, p.y, p.x, p.y);
                screenBB.x = Math.min(screenBB.x, p.x);
                screenBB.y = Math.min(screenBB.y, p.y);
                screenBB.z = Math.max(screenBB.z, p.x);
                screenBB.w = Math.max(screenBB.w, p.y);
            }
        }
        if (screenBB == null) return;


        Text label = ent.getStack().getName().copy();
        if(label.getSiblings().isEmpty()) {
            label = label.copy().setStyle(Style.EMPTY.withColor(Zenith.getInstance().getThemeManager().getCurrentTheme().getWhite().getRGB()));
        }
        if (!label.getString().isEmpty() && label.getString().charAt(label.getString().length() - 1) == ' ') {
            label = TextUtil.replaceLastChar(label, "");

        }

        if (ent.getStack().getCount() > 1) {
            label = label.copy().append(Text.of(" x" + ent.getStack().getCount()).copy().setStyle(Style.EMPTY.withColor(Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB())));
            ;
        }


        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();


        Font font = Fonts.MEDIUM.getFont(10 * scale.getCurrent());

        float padX = 8f * scale.getCurrent();
        float padY = 12f * scale.getCurrent();
        float textW = font.width(label) * scale.getCurrent();
        float rectW = textW + padX * 2 + 16 * 0.7f * scale.getCurrent() + padX;
        float textH = 10f * scale.getCurrent();


        float boxMinX = (float) screenBB.x;
        float boxMaxX = (float) screenBB.z;
        float boxMinY = (float) screenBB.y;

        float posX = (boxMinX + (boxMaxX - boxMinX) / 2f) - (rectW / 2f);
        float posY = boxMinY - 13f;

        float rectX = posX - padX;
        float rectY = posY;

        float rectH = padY;


        context.pushMatrix();
        context.getMatrices().translate(rectX + rectW / 2f, rectY + 6.5f, 0);
        context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent(), 1f);
        context.getMatrices().translate(-(rectX + rectW / 2f), -(rectY + 6.5f), 0);


        DrawUtil.drawBlurHudBooleanCheck(
                context.getMatrices(),
                rectX, rectY,
                rectW, rectH,
                22,
                BorderRadius.all(2),
                ColorRGBA.WHITE,
                blur.isEnabled() && blur.isVisible(),
                glow.isEnabled() && glow.isVisible()
        );

        ColorRGBA rectColor = theme.getForegroundLight();
        context.drawRoundedRect(rectX, rectY, rectW, rectH, BorderRadius.all(2), rectColor);
        DrawUtil.drawRoundedCorner(
                context.getMatrices(),
                rectX, rectY,
                rectW, rectH,
                0.1f, 9f,
                theme.getColor(),
                BorderRadius.all(2)
        );


        context.pushMatrix();
        context.getMatrices().translate(posX + 16 * 0.7f * scale.getCurrent() + 8 * scale.getCurrent(), rectY + 2f * scale.getCurrent(), 0);
        context.drawText(font, label, 0, 0);
        context.popMatrix();
        float itemX = rectX + padX;
        float itemY = rectY;
        context.pushMatrix();
        context.getMatrices().translate(itemX, itemY, 0);
        context.getMatrices().scale(0.7f * scale.getCurrent(), 0.7f * scale.getCurrent(), 1);
        DiffuseLighting.disableGuiDepthLighting();

        context.drawItem(ent.getStack(), 0, 0);

        context.popMatrix();
        context.popMatrix();
    }

    private void renderEntityBox(CustomDrawContext context, Entity entity, float tickDelta) {
        if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) return;
        Vector4d vec4d = ProjectionUtil.getVector4D(entity);
        if (ProjectionUtil.canSee(vec4d)) return;
        drawFlatBox((entity instanceof PlayerEntity player && Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())), vec4d);

    }

    private void drawFlatBox(boolean friend, Vector4d vec) {
        int client1 = !friend ? Zenith.getInstance().getThemeManager().getClientColor(0).getRGB() : Zenith.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client2 = !friend ? Zenith.getInstance().getThemeManager().getClientColor(90).getRGB() : Zenith.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client3 = !friend ? Zenith.getInstance().getThemeManager().getClientColor(180).getRGB() : Zenith.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client4 = !friend ? Zenith.getInstance().getThemeManager().getClientColor(270).getRGB() : Zenith.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float baseSize = (endPosX - posX) / 3;
        float size = baseSize * scale.getCurrent();
        float thickness = 2f * scale.getCurrent();
        
        float offsetX = (baseSize - size) / 2;
        float offsetY = (baseSize - size) / 2;

        Render2DUtil.drawQuad(posX - thickness + offsetX, posY - thickness + offsetY, size, thickness, client1);
        Render2DUtil.drawQuad(posX - thickness + offsetX, posY + offsetY, thickness, size + thickness, client1);

        Render2DUtil.drawQuad(posX - thickness + offsetX, endPosY - size - thickness + offsetY, thickness, size, client2);
        Render2DUtil.drawQuad(posX - thickness + offsetX, endPosY - thickness + offsetY, thickness, thickness, client2);

        Render2DUtil.drawQuad(endPosX - size + thickness * 2 + offsetX, posY - thickness + offsetY, size, thickness, client3);
        Render2DUtil.drawQuad(endPosX + thickness + offsetX, posY + offsetY, thickness, size + thickness, client3);

        Render2DUtil.drawQuad(endPosX + thickness + offsetX, endPosY - size - thickness + offsetY, thickness, size, client4);
        Render2DUtil.drawQuad(endPosX - size + thickness * 2 + offsetX, endPosY - thickness + offsetY, thickness, thickness, client4);
    }

    private float getPlayerHeight(PlayerEntity player) {
        return (float) player.getBoundingBox().getLengthY() + 0.2f;
    }

    private double interpolate(double prev, double current, float delta) {
        return prev + (current - prev) * delta;
    }


    private Vec3d[] getPoints(Entity entity, float tickDelta) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;

        Box box = entity.getBoundingBox();
        Box shifted = new Box(
                box.minX - entity.getX() + x - 0.05,
                box.minY - entity.getY() + y,
                box.minZ - entity.getZ() + z - 0.05,
                box.maxX - entity.getX() + x + 0.05,
                box.maxY - entity.getY() + y + 0.15,
                box.maxZ - entity.getZ() + z + 0.05
        );

        return new Vec3d[]{
                new Vec3d(shifted.minX, shifted.minY, shifted.minZ),
                new Vec3d(shifted.minX, shifted.maxY, shifted.minZ),
                new Vec3d(shifted.maxX, shifted.minY, shifted.minZ),
                new Vec3d(shifted.maxX, shifted.maxY, shifted.minZ),
                new Vec3d(shifted.minX, shifted.minY, shifted.maxZ),
                new Vec3d(shifted.minX, shifted.maxY, shifted.maxZ),
                new Vec3d(shifted.maxX, shifted.minY, shifted.maxZ),
                new Vec3d(shifted.maxX, shifted.maxY, shifted.maxZ)
        };
    }

    private ColorRGBA getHealthColorRGBA(float health) {
        if (health <= 7) return new ColorRGBA(255, 0, 0, 255);
        if (health <= 15) return new ColorRGBA(255, 255, 0, 255);
        return new ColorRGBA(0, 255, 0, 255);
    }

}

