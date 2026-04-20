package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomGameMenuScreen extends Screen {

    private final Animation fadeIn = new Animation(350, 0, Easing.QUAD_IN_OUT);
    private final List<PBtn> btns  = new ArrayList<>();
    private final SpaceEffects space = new SpaceEffects();

    // Цвета
    private static final ColorRGBA OVERLAY = new ColorRGBA(0,   0,   0,  110);
    private static final ColorRGBA PANEL   = new ColorRGBA(8,   9,  16,  200);
    private static final ColorRGBA PANEL_B = new ColorRGBA(255,255,255,   22);
    private static final ColorRGBA IT      = new ColorRGBA(255,255,255,   16);
    private static final ColorRGBA IT_H    = new ColorRGBA(255,255,255,   30);
    private static final ColorRGBA BR      = new ColorRGBA(255,255,255,   35);
    private static final ColorRGBA BR_H    = new ColorRGBA(255,255,255,   70);
    private static final ColorRGBA ACC     = new ColorRGBA(108, 99, 210, 225);
    private static final ColorRGBA ACC_H   = new ColorRGBA(128,118, 240, 255);
    private static final ColorRGBA TXT     = new ColorRGBA(240,240, 255, 255);
    private static final ColorRGBA DIM     = new ColorRGBA( 80, 80, 110, 120);
    private static final ColorRGBA GLOW    = new ColorRGBA( 70, 62, 185, 255);
    private static final ColorRGBA RED     = new ColorRGBA(190, 55,  55, 210);
    private static final ColorRGBA RED_H   = new ColorRGBA(215, 75,  75, 255);

    private static final float BW = 210, BH = 26, GAP = 5;

    public CustomGameMenuScreen() { super(Text.of("Меню")); }

    @Override
    protected void init() {
        btns.clear();
        float cx = width / 2f, bx = cx - BW / 2f;
        float by = height / 2f - (BH * 4 + GAP * 3) / 2f + 8;

        btns.add(new PBtn("Вернуться к игре", bx, by,              BW, BH, true,  false, 0, () -> client.setScreen(null)));
        btns.add(new PBtn("Настройки",        bx, by+(BH+GAP),     BW, BH, false, false, 1, () -> client.setScreen(new CustomOptionsScreen(this))));

        float hw = (BW - GAP) / 2f;
        btns.add(new PBtn("Достижения", bx,        by+(BH+GAP)*2, hw, BH, false, false, 2,
            () -> client.setScreen(new net.minecraft.client.gui.screen.StatsScreen(this, client.player.getStatHandler()))));
        btns.add(new PBtn("Статистика", bx+hw+GAP, by+(BH+GAP)*2, hw, BH, false, false, 2,
            () -> client.setScreen(new net.minecraft.client.gui.screen.StatsScreen(this, client.player.getStatHandler()))));

        btns.add(new PBtn("Сохранить и выйти", bx, by+(BH+GAP)*3, BW, BH, false, true, 3, () -> {
            client.world.disconnect();
            client.disconnect(new net.minecraft.client.gui.screen.TitleScreen());
        }));

        fadeIn.animateTo(1);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a  = (float) fadeIn.update();
        long now = System.currentTimeMillis();

        // Фоновая текстура на весь экран поверх игры
        DrawUtil.drawTexture(ctx.getMatrices(), Zenith.id("icons/menu_bg.png"),
            0, 0, width, height, new ColorRGBA(255, 255, 255, (int)(180 * a)));

        // Затемнение
        ctx.fill(0, 0, width, height, new ColorRGBA(0, 0, 0, (int)(100 * a)).getRGB());

        // Космические эффекты
        space.tick(now);
        space.render(ctx, width, height, a * 0.7f, now);

        // Свечение за кнопками
        float cx = width / 2f, cy = height / 2f;
        try {
            DrawUtil.drawShadow(ctx.getMatrices(), cx - 120, cy - 90, 240, 180,
                140f, BorderRadius.all(80), GLOW.mulAlpha(0.14f * a));
        } catch (Exception ignored) {}

        // Заголовок прямо на фоне
        Font tf = Fonts.BOLD.getFont(13f);
        String title = "Меню";
        float by0 = cy - (BH * 4 + GAP * 3) / 2f + 8;
        drawT(ctx, tf, title, cx - tf.width(title) / 2f, by0 - 24, TXT.mulAlpha(a));

        // Кнопки
        for (PBtn b : btns) b.render(ctx, mx, my, a);

        // Версия
        Font vf = Fonts.MEDIUM.getFont(5f);
        String v = "Space Visuals 2.0";
        drawT(ctx, vf, v, cx - vf.width(v) / 2f, height - 12, DIM.mulAlpha(a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (PBtn b : btns) if (b.hit((float)mx, (float)my)) { b.run.run(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean keyPressed(int k, int s, int m) {
        if (k == GLFW.GLFW_KEY_ESCAPE) { client.setScreen(null); return true; }
        return super.keyPressed(k, s, m);
    }
    @Override public boolean shouldCloseOnEsc() { return false; }

    void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }

    private class PBtn {
        final String label; final float x, y, w, h;
        final boolean accent, danger; final Runnable run;
        final Animation hover  = new Animation(140, 0, Easing.QUAD_IN_OUT);
        final Animation appear;
        final Animation click  = new Animation(100, 0, Easing.QUAD_IN_OUT);

        PBtn(String l, float x, float y, float w, float h, boolean ac, boolean dn, int d, Runnable r) {
            label=l; this.x=x; this.y=y; this.w=w; this.h=h; accent=ac; danger=dn; run=r;
            appear = new Animation(280 + d * 55, 0, Easing.QUAD_IN_OUT);
            appear.animateTo(1);
        }

        boolean hit(float mx, float my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }

        void render(DrawContext ctx, float mx, float my, float alpha) {
            float av = (float) appear.update();
            float fa = alpha * av;
            float slideY = (1f - av) * 12f;
            boolean hov = hit(mx, my);
            hover.animateTo(hov ? 1 : 0);
            float hv = (float) hover.update();
            float ry = y + slideY;

            // Hover свечение
            if (hv > 0.01f && !danger) {
                DrawUtil.drawShadow(ctx.getMatrices(), x + w*0.1f, ry + h*0.3f, w*0.8f, h,
                    16f, BorderRadius.all(8), (accent ? ACC_H : new ColorRGBA(255,255,255,255)).mulAlpha(0.10f * hv * fa));
            }

            ColorRGBA bg, border;
            if (danger) {
                bg = RED.mix(RED_H, hv).mulAlpha(fa);
                border = new ColorRGBA(255, 100, 100, (int)(55 * fa));
            } else if (accent) {
                bg = ACC.mix(ACC_H, hv).mulAlpha(fa);
                border = new ColorRGBA(155, 145, 255, (int)(60 * fa));
            } else {
                bg = IT.mix(IT_H, hv).mulAlpha(fa);
                border = BR.mix(BR_H, hv).mulAlpha(fa);
            }

            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, ry, w, h, BorderRadius.all(8), bg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, ry, w, h, -0.1f, BorderRadius.all(8), border);

            Font f = Fonts.MEDIUM.getFont(7f);
            drawT(ctx, f, label, x + (w - f.width(label)) / 2f, ry + (h - f.height()) / 2f,
                TXT.mulAlpha(0.82f + 0.18f * hv).mulAlpha(fa));
        }
    }
}
