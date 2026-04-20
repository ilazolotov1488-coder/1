package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
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

public class CustomTitleScreen extends Screen {

    private static final Identifier BG_TEX   = Zenith.id("icons/menu_bg.png");
    private static final Identifier LOGO_TEX = Zenith.id("icons/logo_s.png");
    private static final Identifier MC_ICON  = Zenith.id("icons/mc_icon.png");

    private final Animation fadeIn   = new Animation(800, 0, Easing.QUAD_IN_OUT);
    private final Animation logoAnim = new Animation(1000, 0, Easing.BAKEK_SIZE);
    private final List<Btn> btns = new ArrayList<>();
    private final SpaceEffects space = new SpaceEffects();

    // Цвета
    private static final ColorRGBA B_IDLE = new ColorRGBA(255, 255, 255, 14);
    private static final ColorRGBA B_HOV  = new ColorRGBA(255, 255, 255, 28);
    private static final ColorRGBA BR_I   = new ColorRGBA(255, 255, 255, 30);
    private static final ColorRGBA BR_H   = new ColorRGBA(255, 255, 255, 70);
    private static final ColorRGBA ACC    = new ColorRGBA(108,  99, 210, 220);
    private static final ColorRGBA ACC_H  = new ColorRGBA(130, 120, 240, 255);
    private static final ColorRGBA TXT    = new ColorRGBA(240, 240, 255, 255);
    private static final ColorRGBA DIM    = new ColorRGBA(80,  80,  110, 120);
    private static final ColorRGBA GLOW   = new ColorRGBA(90,  80,  210, 255);

    // Размеры кнопок — компактнее
    private static final float BW = 200, BH = 24, GAP = 5;
    private static final float LOGO_SZ = 120;

    // Мышь для parallax
    private float mouseX, mouseY;

    public CustomTitleScreen() { super(Text.empty()); }

    @Override
    protected void init() {
        btns.clear();
        float cx = width / 2f, bx = cx - BW / 2f;
        float by = height * 0.57f;

        btns.add(new Btn("Одиночная игра", bx, by,                BW, BH, false, 0, true,  () -> client.setScreen(new SelectWorldScreen(this))));
        btns.add(new Btn("Сетевая игра",   bx, by + (BH+GAP),     BW, BH, false, 1, false, () -> client.setScreen(new MultiplayerScreen(this))));
        btns.add(new Btn("Аккаунты",       bx, by + (BH+GAP)*2,   BW, BH, true,  2, false, () -> client.setScreen(new AltManagerScreen(this))));

        float hw = (BW - GAP) / 2f;
        btns.add(new Btn("Настройки", bx,          by + (BH+GAP)*3, hw, BH, false, 3, false, () -> client.setScreen(new OptionsScreen(this, client.options))));
        btns.add(new Btn("Выйти",     bx + hw+GAP, by + (BH+GAP)*3, hw, BH, false, 3, false, () -> client.scheduleStop()));

        fadeIn.animateTo(1);
        logoAnim.animateTo(1);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseX = (float) mx;
        mouseY = (float) my;
        super.mouseMoved(mx, my);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a  = (float) fadeIn.update();
        float la = (float) logoAnim.update();

        // Parallax смещение лого от мыши
        float px = (mouseX / Math.max(width,  1) - 0.5f) * 10f;
        float py = (mouseY / Math.max(height, 1) - 0.5f) * 8f;

        // Фон
        DrawUtil.drawTexture(ctx.getMatrices(), BG_TEX, 0, 0, width, height, ColorRGBA.WHITE.mulAlpha(a));
        ctx.fill(0, 0, width, height, new ColorRGBA(0, 0, 0, (int)(150 * a)).getRGB());

        // Космические эффекты (только если шейдеры загружены)
        long now = System.currentTimeMillis();
        space.tick(now);
        space.render(ctx, width, height, a, now);

        float cx = width / 2f;
        float by = height * 0.57f;
        float logoX = cx - LOGO_SZ / 2f + px;
        float logoY = by - LOGO_SZ - 22 + py;

        // Пульсация свечения
        float pulse  = (float)(Math.sin(System.currentTimeMillis() * 0.0018) * 0.5 + 0.5);
        float pulse2 = (float)(Math.sin(System.currentTimeMillis() * 0.0009 + 1.2) * 0.5 + 0.5);
        float pulse3 = (float)(Math.sin(System.currentTimeMillis() * 0.0030 + 2.5) * 0.5 + 0.5);

        // Свечение под лого
        try { DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 180 + px, logoY - 60, 360, LOGO_SZ + 120,
            160f, BorderRadius.all(100),
            GLOW.mulAlpha((0.15f + 0.07f * pulse2) * a * la)); } catch (Exception ignored) {}
        try { DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 100 + px, logoY - 20, 200, LOGO_SZ + 40,
            100f, BorderRadius.all(70),
            GLOW.mulAlpha((0.28f + 0.12f * pulse) * a * la)); } catch (Exception ignored) {}
        try { DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 55 + px, logoY + 5, 110, LOGO_SZ - 10,
            55f, BorderRadius.all(40),
            new ColorRGBA(130, 115, 255, 255).mulAlpha((0.40f + 0.18f * pulse3) * a * la)); } catch (Exception ignored) {}

        // Лого с parallax + scale анимация
        float scaledSz = LOGO_SZ * (0.65f + 0.35f * la);
        float off = (LOGO_SZ - scaledSz) / 2f;
        DrawUtil.drawRoundedTexture(ctx.getMatrices(), LOGO_TEX,
            logoX + off, logoY + off, scaledSz, scaledSz,
            BorderRadius.all(12), ColorRGBA.WHITE.mulAlpha(a * la));
        // Осветляющий слой
        DrawUtil.drawRoundedTexture(ctx.getMatrices(), LOGO_TEX,
            logoX + off, logoY + off, scaledSz, scaledSz,
            BorderRadius.all(12), new ColorRGBA(210, 200, 255, 160).mulAlpha(a * la));

        // Кнопки
        for (Btn b : btns) b.render(ctx, mx, my, a);

        // Версия
        Font vf = Fonts.MEDIUM.getFont(5.5f);
        String v = "Space Visuals 2.0";
        drawT(ctx, vf, v, cx - vf.width(v) / 2f, height - 12, DIM.mulAlpha(a));

        // Кнопка ванильного меню
        float btnSz = 24;
        float btnX = width - btnSz - 8;
        float btnY = height - btnSz - 8;
        boolean switchHov = mx >= btnX && mx <= btnX+btnSz && my >= btnY && my <= btnY+btnSz;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, BorderRadius.all(6),
            new ColorRGBA(255, 255, 255, switchHov ? 22 : 10).mulAlpha(a));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, -0.1f, BorderRadius.all(6),
            new ColorRGBA(255, 255, 255, switchHov ? 55 : 25).mulAlpha(a));
        DrawUtil.drawRoundedTexture(ctx.getMatrices(), MC_ICON,
            btnX + 2, btnY + 2, btnSz - 4, btnSz - 4,
            BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(a * (switchHov ? 1f : 0.85f)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float btnSz = 24, btnX = width - btnSz - 8, btnY = height - btnSz - 8;
        if (btn == 0 && mx >= btnX && mx <= btnX+btnSz && my >= btnY && my <= btnY+btnSz) {
            Zenith.useVanillaMenu = true;
            client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            return true;
        }
        for (Btn b : btns) if (b.hit((float)mx, (float)my)) { b.onClick(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean keyPressed(int k, int s, int m) { if (k == GLFW.GLFW_KEY_ESCAPE) return false; return super.keyPressed(k, s, m); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }

    private class Btn {
        final String label;
        final float x, y, w, h;
        final boolean acc, highlight;
        final Runnable run;
        final Animation hover  = new Animation(150, 0, Easing.QUAD_IN_OUT);
        final Animation appear;
        final Animation click  = new Animation(120, 0, Easing.QUAD_IN_OUT);
        boolean clicking = false;

        Btn(String l, float x, float y, float w, float h, boolean ac, int d, boolean hl, Runnable r) {
            label = l; this.x = x; this.y = y; this.w = w; this.h = h;
            acc = ac; highlight = hl; run = r;
            appear = new Animation(450 + d * 60, 0, Easing.QUAD_IN_OUT);
            appear.animateTo(1);
        }

        boolean hit(float mx, float my) { return mx >= x && mx <= x+w && my >= y && my <= y+h; }

        void onClick() {
            click.reset();
            click.animateTo(1);
            clicking = true;
            run.run();
        }

        void render(DrawContext ctx, float mx, float my, float alpha) {
            float av = (float) appear.update();
            float slideY = (1f - av) * 14f;
            float fa = alpha * av;
            boolean hov = hit(mx, my);
            hover.animateTo(hov ? 1 : 0);
            float hv = (float) hover.update();

            // Анимация нажатия — лёгкое сжатие
            float cv = clicking ? (float) click.update() : 0f;
            if (clicking && click.isDone()) clicking = false;
            float pressScale = 1f - cv * 0.04f;

            float ry = y + slideY;
            float cx2 = x + w / 2f;
            float cy2 = ry + h / 2f;
            float rw = w * pressScale;
            float rh = h * pressScale;
            float rx = cx2 - rw / 2f;
            float ryy = cy2 - rh / 2f;

            // Hover свечение снизу кнопки
            if (hv > 0.01f) {
                try { DrawUtil.drawShadow(ctx.getMatrices(),
                    rx + rw * 0.1f, ryy + rh * 0.5f, rw * 0.8f, rh,
                    18f, BorderRadius.all(9),
                    (acc ? ACC_H : new ColorRGBA(255, 255, 255, 255)).mulAlpha(0.12f * hv * fa)); } catch (Exception ignored) {}
            }

            if (acc) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), rx, ryy, rw, rh, BorderRadius.all(8),
                    ACC.mix(ACC_H, hv).mulAlpha(fa));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), rx, ryy, rw, rh, -0.1f, BorderRadius.all(8),
                    new ColorRGBA(160, 150, 255, (int)(60 + 40 * hv)).mulAlpha(fa));
            } else if (highlight) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), rx, ryy, rw, rh, BorderRadius.all(8),
                    new ColorRGBA(255, 255, 255, (int)(30 + 20 * hv)).mulAlpha(fa));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), rx, ryy, rw, rh, -0.1f, BorderRadius.all(8),
                    new ColorRGBA(255, 255, 255, (int)(65 + 35 * hv)).mulAlpha(fa));
            } else {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), rx, ryy, rw, rh, BorderRadius.all(8),
                    B_IDLE.mix(B_HOV, hv).mulAlpha(fa));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), rx, ryy, rw, rh, -0.1f, BorderRadius.all(8),
                    BR_I.mix(BR_H, hv).mulAlpha(fa));
            }

            // Текст — крупнее
            Font f = Fonts.MEDIUM.getFont(7.5f);
            ColorRGBA tc = acc ? ColorRGBA.WHITE : TXT.mulAlpha(0.80f + 0.20f * hv);
            drawT(ctx, f, label, rx + (rw - f.width(label)) / 2f, ryy + (rh - f.height()) / 2f, tc.mulAlpha(fa));
        }
    }
}
