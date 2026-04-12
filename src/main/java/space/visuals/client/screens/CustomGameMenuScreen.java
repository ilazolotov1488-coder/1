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

    private static final Identifier BG_TEX = Zenith.id("icons/menu_bg.png");

    private final Animation fadeIn = new Animation(400, 0, Easing.QUAD_IN_OUT);
    private final List<PBtn> btns = new ArrayList<>();

    private static final ColorRGBA IT    = new ColorRGBA(255,255,255,22);
    private static final ColorRGBA IT_H  = new ColorRGBA(255,255,255,38);
    private static final ColorRGBA BR    = new ColorRGBA(255,255,255,50);
    private static final ColorRGBA BR_H  = new ColorRGBA(255,255,255,80);
    private static final ColorRGBA ACC   = new ColorRGBA(108, 99,210,230);
    private static final ColorRGBA ACC_H = new ColorRGBA(122,113,228,255);
    private static final ColorRGBA TXT   = new ColorRGBA(240,240,252,255);
    private static final ColorRGBA DIM   = new ColorRGBA(80, 80,100,130);
    private static final ColorRGBA GLOW  = new ColorRGBA(68, 62,175,255);
    private static final ColorRGBA RED   = new ColorRGBA(200, 60, 60,220);
    private static final ColorRGBA RED_H = new ColorRGBA(220, 80, 80,255);

    private static final float BW = 240, BH = 28, GAP = 6;

    public CustomGameMenuScreen() {
        super(Text.of("Меню"));
    }

    @Override
    protected void init() {
        btns.clear();
        float cx = width / 2f, bx = cx - BW / 2f;
        float by = height / 2f - (BH * 4 + GAP * 3) / 2f;

        btns.add(new PBtn("Вернуться к игре", bx, by,              BW, BH, false, false, 0, () -> client.setScreen(null)));
        btns.add(new PBtn("Настройки",        bx, by+(BH+GAP),     BW, BH, false, false, 1, () -> client.setScreen(new CustomOptionsScreen(this))));

        float hw = (BW - GAP) / 2f;
        btns.add(new PBtn("Достижения",  bx,          by+(BH+GAP)*2, hw, BH, false, false, 2, () -> client.setScreen(new net.minecraft.client.gui.screen.StatsScreen(this, client.player.getStatHandler()))));
        btns.add(new PBtn("Статистика",  bx+hw+GAP,   by+(BH+GAP)*2, hw, BH, false, false, 2, () -> client.setScreen(new net.minecraft.client.gui.screen.StatsScreen(this, client.player.getStatHandler()))));

        btns.add(new PBtn("Сохранить и выйти", bx, by+(BH+GAP)*3, BW, BH, false, true, 3, () -> {
            client.world.disconnect();
            client.disconnect(new net.minecraft.client.gui.screen.TitleScreen());
        }));

        fadeIn.animateTo(1);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a = (float) fadeIn.update();

        // Полупрозрачный фон поверх игры
        ctx.fill(0, 0, width, height, new ColorRGBA(0,0,0,(int)(120*a)).getRGB());

        // Свечение
        DrawUtil.drawShadow(ctx.getMatrices(), width/2f-80, height/2f-60, 160,120, 100f, BorderRadius.all(60), GLOW.mulAlpha(0.10f*a));

        // Заголовок
        Font tf = Fonts.BOLD.getFont(13);
        String title = "Меню";
        drawT(ctx, tf, title, width/2f - tf.width(title)/2f, height/2f - (BH*4+GAP*3)/2f - 22, TXT.mulAlpha(a));

        for (PBtn b : btns) b.render(ctx, mx, my, a);

        Font vf = Fonts.MEDIUM.getFont(5f);
        String v = "Space Visuals 2.0";
        drawT(ctx, vf, v, width/2f - vf.width(v)/2f, height - 13, DIM.mulAlpha(a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (PBtn b : btns) if (b.hit((float)mx,(float)my)) { b.run.run(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean keyPressed(int k,int s,int m){
        if(k==GLFW.GLFW_KEY_ESCAPE){ client.setScreen(null); return true; }
        return super.keyPressed(k,s,m);
    }
    @Override public boolean shouldCloseOnEsc(){ return false; }

    void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }

    private class PBtn {
        final String label; final float x,y,w,h; final boolean accent, danger; final Runnable run;
        final Animation hover = new Animation(150,0,Easing.QUAD_IN_OUT);
        final Animation appear;
        PBtn(String l,float x,float y,float w,float h,boolean ac,boolean dn,int d,Runnable r){
            label=l;this.x=x;this.y=y;this.w=w;this.h=h;accent=ac;danger=dn;run=r;
            appear = new Animation(300+d*60, 0, Easing.QUAD_IN_OUT);
            appear.animateTo(1);
        }
        boolean hit(float mx,float my){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
        void render(DrawContext ctx,float mx,float my,float alpha){
            float av=(float)appear.update();
            float slideY=(1f-av)*10f;
            float fa=alpha*av;
            boolean hov=hit(mx,my); hover.animateTo(hov?1:0); float hv=(float)hover.update();
            float ry=y+slideY;
            ColorRGBA bg, border;
            if (danger) {
                bg = RED.mix(RED_H, hv).mulAlpha(fa);
                border = new ColorRGBA(255,100,100,(int)(50*fa));
            } else {
                bg = IT.mix(IT_H, hv).mulAlpha(fa);
                border = BR.mix(BR_H, hv).mulAlpha(fa);
            }
            DrawUtil.drawRoundedRect(ctx.getMatrices(),x,ry,w,h,BorderRadius.all(9),bg);
            DrawUtil.drawRoundedBorder(ctx.getMatrices(),x,ry,w,h,-0.1f,BorderRadius.all(9),border);
            Font f=Fonts.MEDIUM.getFont(6.5f);
            drawT(ctx,f,label,x+(w-f.width(label))/2f,ry+(h-f.height())/2f,TXT.mulAlpha(0.8f+0.2f*hv).mulAlpha(fa));
        }
    }
}
