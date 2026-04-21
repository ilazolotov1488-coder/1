package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.*;
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

public class CustomOptionsScreen extends Screen {

    private static final Identifier BG_TEX = Zenith.id("icons/menu_bg.png");

    private final Screen parent;
    private final Animation fadeIn = new Animation(500, 0, Easing.QUAD_IN_OUT);
    private final List<OBtn> btns = new ArrayList<>();
    private final SpaceEffects space = new SpaceEffects();
    private final MouseTrailEffect mouseTrail = new MouseTrailEffect();

    // FOV слайдер
    private boolean draggingFov = false;
    private float fovSliderX, fovSliderY, fovSliderW;

    private static final ColorRGBA IT    = new ColorRGBA(255,255,255,18);
    private static final ColorRGBA IT_H  = new ColorRGBA(255,255,255,30);
    private static final ColorRGBA BR    = new ColorRGBA(255,255,255,40);
    private static final ColorRGBA BR_H  = new ColorRGBA(255,255,255,65);
    private static final ColorRGBA ACC   = new ColorRGBA(108, 99,210,220);
    private static final ColorRGBA TXT   = new ColorRGBA(240,240,252,255);
    private static final ColorRGBA DIM   = new ColorRGBA(80, 80,100,130);
    private static final ColorRGBA GLOW  = new ColorRGBA(68, 62,175,255);

    // Размеры сетки
    private static final int BW  = 130; // ширина кнопки
    private static final int BH  = 26;  // высота кнопки
    private static final int GAP = 6;   // отступ

    public CustomOptionsScreen(Screen parent) {
        super(Text.of("Настройки"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        btns.clear();
        float cx = width / 2f;
        // Две колонки, центрированные
        float totalW = BW * 2 + GAP;
        float lx = cx - totalW / 2f; // левая колонка X
        float rx = lx + BW + GAP;    // правая колонка X

        // FOV слайдер вверху на всю ширину
        float sliderY = height / 2f - 110;
        fovSliderX = lx;
        fovSliderY = sliderY;
        fovSliderW = totalW;

        float startY = sliderY + BH + GAP * 2;

        // Левая колонка
        btns.add(new OBtn("Внешний вид",       lx, startY,              BW, BH, 0, false, () -> openSkinOptions()));
        btns.add(new OBtn("Настройки графики",  lx, startY+(BH+GAP),    BW, BH, 2, false, () -> openVideoOptions()));
        btns.add(new OBtn("Язык",               lx, startY+(BH+GAP)*2,  BW, BH, 4, false, () -> openLanguage()));
        btns.add(new OBtn("Наборы ресурсов",    lx, startY+(BH+GAP)*3,  BW, BH, 6, false, () -> openResourcePacks()));
        btns.add(new OBtn("Собираемые данные",  lx, startY+(BH+GAP)*4,  BW, BH, 8, true,  () -> {})); // disabled        // Правая колонка
        btns.add(new OBtn("Онлайн",             rx, startY,              BW, BH, 1, false, () -> client.setScreen(new OnlineOptionsScreen(this, client.options))));
        btns.add(new OBtn("Музыка и звуки",     rx, startY+(BH+GAP),    BW, BH, 3, false, () -> client.setScreen(new SoundOptionsScreen(this, client.options))));
        btns.add(new OBtn("Управление",         rx, startY+(BH+GAP)*2,  BW, BH, 5, false, () -> client.setScreen(new KeybindsScreen(this, client.options))));
        btns.add(new OBtn("Специальные",        rx, startY+(BH+GAP)*3,  BW, BH, 7, false, () -> client.setScreen(new AccessibilityOptionsScreen(this, client.options))));
        btns.add(new OBtn("Титры",              rx, startY+(BH+GAP)*4,  BW, BH, 9, false, () -> client.setScreen(new CreditsAndAttributionScreen(this))));

        // Кнопка "Готово" на всю ширину
        btns.add(new OBtn("Готово", lx, startY+(BH+GAP)*5+GAP, totalW, BH, 10, false, () -> close()));

        fadeIn.animateTo(1);
    }

    private void openSkinOptions() {
        try { client.setScreen(new SkinOptionsScreen(this, client.options)); }
        catch (Exception e) { client.setScreen(new OptionsScreen(this, client.options)); }
    }

    private void openVideoOptions() {
        client.setScreen(new VideoOptionsScreen(this, client, client.options));
    }

    private void openLanguage() {
        try {
            client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
        } catch (Exception e) { client.setScreen(new OptionsScreen(this, client.options)); }
    }

    private void openResourcePacks() {
        try {
            client.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(
                client.getResourcePackManager(),
                manager -> {
                    // После закрытия возвращаемся на наш экран
                    client.reloadResources();
                    client.setScreen(this);
                },
                client.getResourcePackDir(),
                net.minecraft.text.Text.of("Наборы ресурсов")));
        } catch (Exception e) { client.setScreen(new OptionsScreen(this, client.options)); }
    }

    private void openScreen(String name) {
        Zenith.useVanillaMenu = true;
        client.setScreen(new OptionsScreen(this, client.options));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a = (float) fadeIn.update();
        
        // Обновляем трейл
        mouseTrail.update((float) mx, (float) my);

        DrawUtil.drawTexture(ctx.getMatrices(), BG_TEX, 0, 0, width, height, ColorRGBA.WHITE.mulAlpha(a));
        ctx.fill(0, 0, width, height, new ColorRGBA(0,0,0,(int)(165*a)).getRGB());

        long now = System.currentTimeMillis();
        space.tick(now);
        space.render(ctx, width, height, a, now);
        
        // Рендерим трейл частиц
        mouseTrail.render(ctx, a * 0.7f);

        try { DrawUtil.drawShadow(ctx.getMatrices(), width/2f-80, height/2f-60, 160,120, 100f, BorderRadius.all(60), GLOW.mulAlpha(0.12f*a)); } catch (Exception ignored) {}

        // Заголовок
        Font tf = Fonts.BOLD.getFont(13);
        String title = "Настройки";
        drawT(ctx, tf, title, width/2f - tf.width(title)/2f, fovSliderY - 22, TXT.mulAlpha(a));

        // FOV слайдер
        renderFovSlider(ctx, mx, my, a);

        // Кнопки
        for (OBtn b : btns) b.render(ctx, mx, my, a);

        Font vf = Fonts.MEDIUM.getFont(5f);
        String v = "Space Visuals 2.0";
        drawT(ctx, vf, v, width/2f - vf.width(v)/2f, height - 13, DIM.mulAlpha(a));
    }

    private void renderFovSlider(DrawContext ctx, int mx, int my, float a) {
        float fov = client.options.getFov().getValue();
        // FOV range: 30..110
        float t = (fov - 30f) / 80f;

        float sx = fovSliderX, sy = fovSliderY, sw = fovSliderW, sh = BH;

        // Фон слайдера
        DrawUtil.drawRoundedRect(ctx.getMatrices(), sx, sy, sw, sh, BorderRadius.all(8), IT.mulAlpha(a));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), sx, sy, sw, sh, -0.1f, BorderRadius.all(8), BR.mulAlpha(a));

        // Заполненная часть
        float fillW = t * (sw - 8);
        if (fillW > 0) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), sx + 4, sy + 4, fillW, sh - 8, BorderRadius.all(5), ACC.mulAlpha(a));
        }

        // Ручка
        float handleX = sx + 4 + fillW - 4;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), handleX, sy + 3, 8, sh - 6, BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(a));

        // Текст
        Font f = Fonts.MEDIUM.getFont(6f);
        String label = "Поле зрения: " + (int)fov + "°";
        drawT(ctx, f, label, sx + sw/2f - f.width(label)/2f, sy + (sh - f.height())/2f, TXT.mulAlpha(a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Клик по слайдеру FOV
        if (btn == 0 && mx >= fovSliderX && mx <= fovSliderX + fovSliderW
                && my >= fovSliderY && my <= fovSliderY + BH) {
            draggingFov = true;
            updateFov((float)mx);
            return true;
        }
        for (OBtn b : btns) if (b.hit((float)mx,(float)my)) { b.run.run(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingFov) { updateFov((float)mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingFov = false;
        return super.mouseReleased(mx, my, btn);
    }

    private void updateFov(float mx) {
        float t = Math.max(0, Math.min(1, (mx - fovSliderX - 4) / (fovSliderW - 8)));
        int fov = (int)(30 + t * 80);
        client.options.getFov().setValue(fov);
        client.options.write();
    }

    @Override public boolean keyPressed(int k,int s,int m){ if(k==GLFW.GLFW_KEY_ESCAPE){close();return true;} return super.keyPressed(k,s,m); }
    @Override public void close() { client.setScreen(parent); }

    void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }

    private class OBtn {
        final String label; final float x,y,w,h; final boolean disabled; final Runnable run;
        final Animation hover  = new Animation(150,0,Easing.QUAD_IN_OUT);
        final Animation appear;
        OBtn(String l,float x,float y,float w,float h,int d,boolean dis,Runnable r){
            label=l;this.x=x;this.y=y;this.w=w;this.h=h;disabled=dis;run=r;
            appear = new Animation(300+d*35, 0, Easing.QUAD_IN_OUT);
            appear.animateTo(1);
        }
        boolean hit(float mx,float my){ return !disabled && mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }
        void render(DrawContext ctx,float mx,float my,float alpha){
            float av=(float)appear.update();
            float slideY=(1f-av)*8f;
            float fa=alpha*av;
            boolean hov=!disabled&&hit(mx,my); hover.animateTo(hov?1:0); float hv=(float)hover.update();
            float ry=y+slideY;
            float bgAlpha = disabled ? 0.4f : 1f;
            DrawUtil.drawRoundedRect(ctx.getMatrices(),x,ry,w,h,BorderRadius.all(8),IT.mix(IT_H,hv).mulAlpha(fa*bgAlpha));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(),x,ry,w,h,-0.1f,BorderRadius.all(8),BR.mix(BR_H,hv).mulAlpha(fa*bgAlpha));
            Font f=Fonts.MEDIUM.getFont(6f);
            ColorRGBA textColor = disabled ? DIM : TXT.mulAlpha(0.8f+0.2f*hv);
            drawT(ctx,f,label,x+(w-f.width(label))/2f,ry+(h-f.height())/2f,textColor.mulAlpha(fa));
            // Пометка "скоро" для disabled
            if (disabled) {
                Font sf = Fonts.MEDIUM.getFont(4.5f);
                String soon = "н/д";
                drawT(ctx, sf, soon, x+w-sf.width(soon)-4, ry+2, new ColorRGBA(150,150,170,160).mulAlpha(fa));
            }
        }
    }
}
