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

    private final Animation fadeIn    = new Animation(900, 0, Easing.QUAD_IN_OUT);
    private final Animation logoAnim  = new Animation(1100, 0, Easing.BAKEK_SIZE);
    private final List<Btn> btns = new ArrayList<>();

    private static final ColorRGBA B_IDLE = new ColorRGBA(255,255,255,16);
    private static final ColorRGBA B_HOV  = new ColorRGBA(255,255,255,26);
    private static final ColorRGBA BR_I   = new ColorRGBA(255,255,255,35);
    private static final ColorRGBA BR_H   = new ColorRGBA(255,255,255,60);
    private static final ColorRGBA ACC    = new ColorRGBA(108, 99,210,230);
    private static final ColorRGBA ACC_H  = new ColorRGBA(122,113,228,255);
    private static final ColorRGBA TXT    = new ColorRGBA(245,245,255,255);
    private static final ColorRGBA DIM    = new ColorRGBA(80, 80,100,130);
    private static final ColorRGBA GLOW   = new ColorRGBA(90, 80,210,255);

    private static final float BW = 240, BH = 28, GAP = 6;
    private static final float LOGO_SZ = 150; // максимально большое

    public CustomTitleScreen() { super(Text.empty()); }

    @Override
    protected void init() {
        btns.clear();
        float cx = width/2f, bx = cx - BW/2f;
        float by = height * 0.56f; // выше, небольшое расстояние от лого

        btns.add(new Btn("Одиночная игра", bx, by,              BW, BH, false, 0, true,  () -> client.setScreen(new SelectWorldScreen(this))));
        btns.add(new Btn("Сетевая игра",   bx, by+(BH+GAP),     BW, BH, false, 1, false, () -> client.setScreen(new MultiplayerScreen(this))));
        btns.add(new Btn("Аккаунты",       bx, by+(BH+GAP)*2,   BW, BH, true,  2, false, () -> client.setScreen(new AltManagerScreen(this))));

        float hw = (BW-GAP)/2f;
        btns.add(new Btn("Настройки", bx,          by+(BH+GAP)*3, hw, BH, false, 3, false, () -> client.setScreen(new OptionsScreen(this, client.options))));
        btns.add(new Btn("Выйти",     bx+hw+GAP,   by+(BH+GAP)*3, hw, BH, false, 3, false, () -> client.scheduleStop()));

        fadeIn.animateTo(1);
        logoAnim.animateTo(1);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a  = (float) fadeIn.update();
        float la = (float) logoAnim.update();

        // Фон
        DrawUtil.drawTexture(ctx.getMatrices(), BG_TEX, 0, 0, width, height, ColorRGBA.WHITE.mulAlpha(a));
        ctx.fill(0, 0, width, height, new ColorRGBA(0,0,0,(int)(160*a)).getRGB());

        float cx = width/2f;
        float by = height * 0.56f; // синхронизировано с init()
        float logoX = cx - LOGO_SZ/2f;
        float logoY = by - LOGO_SZ - 28;

        // === СВЕЧЕНИЕ РИСУЕМ ПОСЛЕ ЗАТЕМНЕНИЯ — поверх тёмного фона ===
        float pulse  = (float)(Math.sin(System.currentTimeMillis() * 0.0018) * 0.5 + 0.5);
        float pulse2 = (float)(Math.sin(System.currentTimeMillis() * 0.0009 + 1.2) * 0.5 + 0.5);
        float pulse3 = (float)(Math.sin(System.currentTimeMillis() * 0.0030 + 2.5) * 0.5 + 0.5);
        float pulse4 = (float)(Math.sin(System.currentTimeMillis() * 0.0012 + 0.7) * 0.5 + 0.5);

        // Светлый круг прямо под лого — имитирует отражение света
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 80, logoY + LOGO_SZ - 20, 160, 40,
            40f, BorderRadius.all(20),
            new ColorRGBA(200, 190, 255, 255).mulAlpha((0.20f + 0.12f * pulse) * a * la));

        // Огромный внешний ореол
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 220, logoY - 80, 440, LOGO_SZ + 160,
            200f, BorderRadius.all(120),
            GLOW.mulAlpha((0.18f + 0.08f * pulse4) * a * la));

        // Большой средний
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 150, logoY - 40, 300, LOGO_SZ + 80,
            130f, BorderRadius.all(80),
            GLOW.mulAlpha((0.28f + 0.12f * pulse2) * a * la));

        // Основное яркое свечение
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 90, logoY - 10, 180, LOGO_SZ + 20,
            80f, BorderRadius.all(55),
            GLOW.mulAlpha((0.38f + 0.18f * pulse) * a * la));

        // Яркий центр
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 55, logoY + 15, 110, LOGO_SZ - 30,
            45f, BorderRadius.all(30),
            new ColorRGBA(130, 115, 255, 255).mulAlpha((0.45f + 0.20f * pulse3) * a * la));

        // Точечный блик
        DrawUtil.drawShadow(ctx.getMatrices(),
            cx - 30, logoY + 5, 60, 45,
            25f, BorderRadius.all(22),
            new ColorRGBA(200, 185, 255, 255).mulAlpha((0.35f + 0.20f * pulse) * a * la));

        // === ЛОГО — рисуем ярче ===
        float scaledSz = LOGO_SZ * (0.6f + 0.4f * la);
        float off = (LOGO_SZ - scaledSz) / 2f;
        // Основной слой
        DrawUtil.drawRoundedTexture(ctx.getMatrices(),
            LOGO_TEX,
            logoX + off, logoY + off,
            scaledSz, scaledSz,
            BorderRadius.all(14),
            ColorRGBA.WHITE.mulAlpha(a * la));
        // Второй слой поверх — осветляет
        DrawUtil.drawRoundedTexture(ctx.getMatrices(),
            LOGO_TEX,
            logoX + off, logoY + off,
            scaledSz, scaledSz,
            BorderRadius.all(14),
            new ColorRGBA(200, 210, 255, 180).mulAlpha(a * la));

        // Кнопки
        for (Btn b : btns) b.render(ctx, mx, my, a);

        // Версия
        Font vf = Fonts.MEDIUM.getFont(5f);
        String v = "Space Visuals 2.0";
        drawT(ctx, vf, v, cx - vf.width(v)/2f, height - 13, DIM.mulAlpha(a));

        // Кнопка переключения на ванильное меню — правый нижний угол
        float btnSz = 28;
        float btnX = width - btnSz - 10;
        float btnY = height - btnSz - 10;
        boolean switchHov = mx >= btnX && mx <= btnX+btnSz && my >= btnY && my <= btnY+btnSz;
        ColorRGBA switchBg = new ColorRGBA(255,255,255, switchHov ? 20 : 10);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, BorderRadius.all(7), switchBg.mulAlpha(a));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, -0.1f, BorderRadius.all(7),
            new ColorRGBA(255,255,255, switchHov ? 50 : 25).mulAlpha(a));
        // Иконка Minecraft — на весь размер кнопки с минимальным отступом
        DrawUtil.drawRoundedTexture(ctx.getMatrices(), MC_ICON,
            btnX + 2, btnY + 2, btnSz - 4, btnSz - 4,
            BorderRadius.all(5), ColorRGBA.WHITE.mulAlpha(a * (switchHov ? 1f : 0.9f)));
        if (switchHov) {
            // подсказка убрана
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Кнопка переключения на ванильное меню
        float btnSz = 28;
        float btnX = width - btnSz - 10;
        float btnY = height - btnSz - 10;
        if (btn == 0 && mx >= btnX && mx <= btnX+btnSz && my >= btnY && my <= btnY+btnSz) {
            // Устанавливаем флаг и открываем ванильный TitleScreen
            Zenith.useVanillaMenu = true;
            client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            return true;
        }
        for (Btn b : btns) if (b.hit((float)mx,(float)my)) { b.run.run(); return true; }
        return super.mouseClicked(mx, my, btn);
    }
    @Override public boolean keyPressed(int k,int s,int m){ if(k==GLFW.GLFW_KEY_ESCAPE)return false; return super.keyPressed(k,s,m); }
    @Override public boolean shouldCloseOnEsc(){ return false; }

    void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }

    private class Btn {
        final String label; final float x,y,w,h; final boolean acc; final boolean highlight; final Runnable run;
        final Animation hover  = new Animation(160,0,Easing.QUAD_IN_OUT);
        final Animation appear;
        Btn(String l,float x,float y,float w,float h,boolean ac,int d,boolean hl,Runnable r){
            label=l;this.x=x;this.y=y;this.w=w;this.h=h;acc=ac;highlight=hl;run=r;            appear = new Animation(500+d*70, 0, Easing.QUAD_IN_OUT);
            appear.animateTo(1);
        }
        boolean hit(float mx,float my){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
        void render(DrawContext ctx,float mx,float my,float alpha){
            float av=(float)appear.update();
            float slideY=(1f-av)*12f;
            float fa=alpha*av;
            boolean hov=hit(mx,my); hover.animateTo(hov?1:0); float hv=(float)hover.update();
            float ry=y+slideY;
            if(acc){
                DrawUtil.drawRoundedRect(ctx.getMatrices(),x,ry,w,h,BorderRadius.all(9),ACC.mix(ACC_H,hv).mulAlpha(fa));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(),x,ry,w,h,-0.1f,BorderRadius.all(9),new ColorRGBA(155,145,255,(int)(50*fa)));
            } else {
                if (highlight) {
                    // Одиночная игра — заметно ярче
                    DrawUtil.drawRoundedRect(ctx.getMatrices(),x,ry,w,h,BorderRadius.all(9),
                        new ColorRGBA(255,255,255,35).mix(new ColorRGBA(255,255,255,50),hv).mulAlpha(fa));
                    DrawUtil.drawRoundedBorder(ctx.getMatrices(),x,ry,w,h,-0.1f,BorderRadius.all(9),
                        new ColorRGBA(255,255,255,70).mix(new ColorRGBA(255,255,255,100),hv).mulAlpha(fa));
                } else {
                    DrawUtil.drawRoundedRect(ctx.getMatrices(),x,ry,w,h,BorderRadius.all(9),B_IDLE.mix(B_HOV,hv).mulAlpha(fa));
                    DrawUtil.drawRoundedBorder(ctx.getMatrices(),x,ry,w,h,-0.1f,BorderRadius.all(9),BR_I.mix(BR_H,hv).mulAlpha(fa));
                }
            }
            Font f=Fonts.MEDIUM.getFont(6.5f);
            ColorRGBA tc = acc ? ColorRGBA.WHITE : TXT.mulAlpha(highlight ? 1f : 0.85f + 0.15f*hv);
            drawT(ctx,f,label,x+(w-f.width(label))/2f,ry+(h-f.height())/2f, tc.mulAlpha(fa));
        }
    }
}
