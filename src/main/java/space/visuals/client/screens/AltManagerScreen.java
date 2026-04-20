package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.DefaultSkinHelper;
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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AltManagerScreen extends Screen {

    private static final Identifier BG_TEX = Zenith.id("icons/menu_bg.png");

    private final Screen parent;
    private final List<String> accounts  = new ArrayList<>();
    private final List<String> pinned    = new ArrayList<>();
    private final List<Animation> cardA  = new ArrayList<>();
    private Path savePath, pinnedPath;
    private boolean initialized = false;

    // Инпут
    private boolean typing = false;
    private final StringBuilder input = new StringBuilder();

    // Скролл сетки
    private float scrollY = 0, targetScrollY = 0;

    // Анимации UI
    private final Animation fadeIn  = new Animation(600, 0, Easing.QUAD_IN_OUT);
    private final Animation panelIn = new Animation(500, 0, Easing.BAKEK_SIZE);
    private final Animation addBtnA = new Animation(130, 0, Easing.QUAD_IN_OUT);
    private final Animation rndBtnA = new Animation(130, 0, Easing.QUAD_IN_OUT);
    private final Animation bckBtnA = new Animation(130, 0, Easing.QUAD_IN_OUT);
    private final Animation inpA    = new Animation(130, 0, Easing.QUAD_IN_OUT);
    private final SpaceEffects space = new SpaceEffects();

    // Цвета
    private static final ColorRGBA PANEL   = new ColorRGBA(10, 11, 18, 215);
    private static final ColorRGBA CARD    = new ColorRGBA(255,255,255,  8);
    private static final ColorRGBA CARD_H  = new ColorRGBA(255,255,255, 18);
    private static final ColorRGBA CARD_S  = new ColorRGBA( 90, 82,200, 55);
    private static final ColorRGBA BORDER  = new ColorRGBA(255,255,255, 20);
    private static final ColorRGBA BORDER_H= new ColorRGBA(255,255,255, 45);
    private static final ColorRGBA ACC     = new ColorRGBA(108, 99,210,220);
    private static final ColorRGBA ACC_H   = new ColorRGBA(128,118,235,255);
    private static final ColorRGBA TXT     = new ColorRGBA(240,240,255,255);
    private static final ColorRGBA SUB     = new ColorRGBA(100,100,130,180);
    private static final ColorRGBA GRN     = new ColorRGBA( 60,190,100,220);
    private static final ColorRGBA DIM     = new ColorRGBA( 70, 70, 95,130);
    private static final ColorRGBA GLOW    = new ColorRGBA( 70, 62,180,255);
    private static final ColorRGBA PIN_CLR = new ColorRGBA(255,210, 50,255);
    private static final ColorRGBA RED     = new ColorRGBA(220, 60, 60,220);
    private static final ColorRGBA ICO_DEF = new ColorRGBA(160,185,255,200);

    // Левая панель
    private static final int LP_W  = 170;
    private static final int LP_H  = 190;
    private static final int BH    = 26;
    private static final int INP_H = 26;

    // Правая сетка карточек — компактнее
    private static final int COLS      = 2;
    private static final int CARD_W    = 155;
    private static final int CARD_SZ_H = 42;
    private static final int CARD_G    = 5;
    private static final int GRID_W    = COLS * CARD_W + (COLS-1) * CARD_G;
    private static final int GRID_H    = LP_H; // выровнено с левой панелью

    // Общая ширина
    private static final int GAP   = 16;
    private static final int TOT_W = LP_W + GAP + GRID_W;

    public AltManagerScreen(Screen parent) {
        super(Text.of("Аккаунты"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        savePath   = client.runDirectory.toPath().resolve("SpaceVisuals/accounts.txt");
        pinnedPath = client.runDirectory.toPath().resolve("SpaceVisuals/pinned.txt");
        if (!initialized) { load(); initialized = true; }
        fadeIn.animateTo(1); panelIn.animateTo(1);
    }

    // ── загрузка ──────────────────────────────────────────────────────────────
    private void load() {
        try { if (Files.exists(savePath))   accounts.addAll(Files.readAllLines(savePath).stream().filter(s->!s.isBlank()).toList()); } catch (Exception ignored) {}
        try { if (Files.exists(pinnedPath)) pinned.addAll(Files.readAllLines(pinnedPath).stream().filter(s->!s.isBlank()).toList()); } catch (Exception ignored) {}
        sort(); syncCards();
        // авто-логин
        try {
            Path lp = savePath.getParent().resolve("last_account.txt");
            if (Files.exists(lp)) { String n=Files.readString(lp).trim(); if(!n.isEmpty()) loginAs(n,false); }
        } catch (Exception ignored) {}
    }

    private void sort() {
        accounts.sort((a,b) -> {
            boolean pa=pinned.contains(a), pb=pinned.contains(b);
            if (pa&&!pb) return -1; if (!pa&&pb) return 1; return 0;
        });
    }

    private void syncCards() {
        while (cardA.size() < accounts.size()) {
            Animation a = new Animation(300+cardA.size()*40, 0, Easing.QUAD_IN_OUT);
            a.animateTo(1); cardA.add(a);
        }
        while (cardA.size() > accounts.size()) cardA.remove(cardA.size()-1);
    }

    private void save()       { try { Files.createDirectories(savePath.getParent());   Files.write(savePath, accounts); } catch (Exception ignored) {} }
    private void savePinned() { try { Files.createDirectories(pinnedPath.getParent()); Files.write(pinnedPath, pinned); } catch (Exception ignored) {} }
    private void saveLast(String n) { try { Files.writeString(savePath.getParent().resolve("last_account.txt"), n); } catch (Exception ignored) {} }

    // ── координаты ────────────────────────────────────────────────────────────
    private int ox()   { return width/2 - TOT_W/2; }
    private int oy()   { return height/2 - Math.max(LP_H, GRID_H)/2; }
    private int lpX()  { return ox(); }
    private int lpY()  { return oy(); }
    private int grX()  { return ox() + LP_W + GAP; }
    private int grY()  { return oy(); }

    // ── render ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a  = (float) fadeIn.update();
        float ps = (float) panelIn.update();
        scrollY += (targetScrollY - scrollY) * 0.14f;

        DrawUtil.drawTexture(ctx.getMatrices(), BG_TEX, 0, 0, width, height, ColorRGBA.WHITE.mulAlpha(a));
        ctx.fill(0,0,width,height, new ColorRGBA(0,0,0,(int)(165*a)).getRGB());

        // Космические эффекты
        long now = System.currentTimeMillis();
        space.tick(now);
        space.render(ctx, width, height, a, now);

        // Свечение
        try { DrawUtil.drawShadow(ctx.getMatrices(), width/2f-100, height/2f-80, 200,160, 120f, BorderRadius.all(80), GLOW.mulAlpha(0.10f*a)); } catch (Exception ignored) {}

        float so = (1f-ps)*8f;
        int lx=lpX(), ly=lpY(), gx=grX(), gy=grY();

        // ── Левая панель ──────────────────────────────────────────────────────
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx-so, ly-so, LP_W+so*2, LP_H+so*2, BorderRadius.all(12), PANEL.mulAlpha(a*ps));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx-so, ly-so, LP_W+so*2, LP_H+so*2, -0.1f, BorderRadius.all(12), BORDER.mulAlpha(a*ps*0.5f));

        Font tf = Fonts.MEDIUM.getFont(8.5f);
        drawT(ctx, tf, "Cracked Login", lx+LP_W/2f-tf.width("Cracked Login")/2f, ly+10, TXT.mulAlpha(a*ps));
        Font sf2 = Fonts.MEDIUM.getFont(5f);
        String sub = "Создать аккаунт по нику";
        drawT(ctx, sf2, sub, lx+LP_W/2f-sf2.width(sub)/2f, ly+22, SUB.mulAlpha(a*ps));

        // Разделитель
        ctx.fill(lx+12, ly+34, lx+LP_W-12, ly+35, new ColorRGBA(255,255,255,(int)(12*a*ps)).getRGB());

        // Инпут
        int iy = ly+42;
        boolean inpHov = mx>=lx+8 && mx<=lx+LP_W-8 && my>=iy && my<=iy+INP_H;
        inpA.animateTo(typing||inpHov?1:0); float ihv=(float)inpA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx+8, iy, LP_W-16, INP_H, BorderRadius.all(7), CARD.mix(CARD_H,ihv).mulAlpha(a*ps));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx+8, iy, LP_W-16, INP_H, -0.1f, BorderRadius.all(7),
            (typing?new ColorRGBA(100,92,200,90):BORDER).mulAlpha(a*ps));
        Font inf = Fonts.MEDIUM.getFont(6f);
        String disp = typing ? input+(System.currentTimeMillis()/500%2==0?"|":" ") : (input.length()>0?input.toString():"Username");
        drawT(ctx, inf, disp, lx+14, iy+(INP_H-inf.height())/2f, (input.length()>0||typing?TXT:DIM).mulAlpha(a*ps));

        // Кнопки Create / Random
        int by1 = iy+INP_H+6;
        int hw = (LP_W-16-5)/2;
        boolean cHov = mx>=lx+8 && mx<=lx+8+hw && my>=by1 && my<=by1+BH;
        addBtnA.animateTo(cHov?1:0);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx+8, by1, hw, BH, BorderRadius.all(7), ACC.mix(ACC_H,(float)addBtnA.update()).mulAlpha(a*ps));
        Font bf = Fonts.MEDIUM.getFont(6.5f);
        drawT(ctx, bf, "✦ Create", lx+8+(hw-bf.width("✦ Create"))/2f, by1+(BH-bf.height())/2f, TXT.mulAlpha(a*ps));

        int rx = lx+8+hw+5;
        boolean rHov = mx>=rx && mx<=rx+hw && my>=by1 && my<=by1+BH;
        rndBtnA.animateTo(rHov?1:0); float rhv=(float)rndBtnA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), rx, by1, hw, BH, BorderRadius.all(7), CARD.mix(CARD_H,rhv).mulAlpha(a*ps));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), rx, by1, hw, BH, -0.1f, BorderRadius.all(7), BORDER.mix(BORDER_H,rhv).mulAlpha(a*ps));
        drawT(ctx, bf, "⟳ Random", rx+(hw-bf.width("⟳ Random"))/2f, by1+(BH-bf.height())/2f, TXT.mulAlpha(a*ps));

        // Текущий аккаунт
        int infoY = by1+BH+10;
        ctx.fill(lx+12, infoY, lx+LP_W-12, infoY+1, new ColorRGBA(255,255,255,(int)(10*a*ps)).getRGB());
        String cur = client.getSession().getUsername();
        UUID curUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:"+cur).getBytes());
        Identifier curSkin = DefaultSkinHelper.getSkinTextures(curUuid).texture();
        float hs = 20f;
        DrawUtil.drawRoundedTextureWithUV(ctx.getMatrices(), curSkin, lx+12, infoY+6, hs, hs,
            BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(a*ps), 8f/64f,8f/64f,16f/64f,16f/64f);
        Font nf2 = Fonts.MEDIUM.getFont(6.5f);
        drawT(ctx, nf2, cur, lx+36, infoY+7, TXT.mulAlpha(a*ps));
        Font sf3 = Fonts.MEDIUM.getFont(5f);
        drawT(ctx, sf3, "☻ Всего: "+accounts.size(), lx+36, infoY+17, SUB.mulAlpha(a*ps));

        // Кнопка Назад
        int bkY = ly+LP_H-BH-8;
        boolean bkHov = mx>=lx+8 && mx<=lx+LP_W-8 && my>=bkY && my<=bkY+BH;
        bckBtnA.animateTo(bkHov?1:0); float bkHv=(float)bckBtnA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx+8, bkY, LP_W-16, BH, BorderRadius.all(7), CARD.mix(CARD_H,bkHv).mulAlpha(a*ps));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx+8, bkY, LP_W-16, BH, -0.1f, BorderRadius.all(7), BORDER.mix(BORDER_H,bkHv).mulAlpha(a*ps));
        drawT(ctx, bf, "✦ Back", lx+8+(LP_W-16-bf.width("✦ Back"))/2f, bkY+(BH-bf.height())/2f, SUB.mix(TXT,bkHv).mulAlpha(a*ps));

        // ── Правая сетка ──────────────────────────────────────────────────────
        // Заголовок
        Font gt = Fonts.MEDIUM.getFont(5.5f);
        drawT(ctx, gt, "ACCOUNTS", gx, gy-12, SUB.mulAlpha(a*ps));

        ctx.enableScissor(gx, gy, gx+GRID_W, gy+GRID_H);
        syncCards();
        for (int i = 0; i < accounts.size(); i++) {
            int col = i % COLS, row = i / COLS;
            float cx2 = gx + col*(CARD_W+CARD_G);
            float cy2 = gy + row*(CARD_SZ_H+CARD_G) - scrollY;
            if (cy2+CARD_SZ_H < gy || cy2 > gy+GRID_H) { cardA.get(i).update(); continue; }

            float av = (float) cardA.get(i).update();
            float slideY = (1f-av)*16f;
            float fa2 = a*ps*av;
            float ry = cy2+slideY;

            boolean hov = mx>=cx2 && mx<=cx2+CARD_W && my>=ry && my<=ry+CARD_SZ_H;
            boolean sel = client.getSession().getUsername().equals(accounts.get(i));
            boolean pin = pinned.contains(accounts.get(i));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), cx2, ry, CARD_W, CARD_SZ_H, BorderRadius.all(8),
                (sel?CARD_S:CARD.mix(CARD_H,hov?1:0)).mulAlpha(fa2));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx2, ry, CARD_W, CARD_SZ_H, -0.1f, BorderRadius.all(8),
                (sel?new ColorRGBA(100,92,200,70):BORDER.mix(BORDER_H,hov?1:0)).mulAlpha(fa2));

            // Аватар
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:"+accounts.get(i)).getBytes());
            Identifier skin = DefaultSkinHelper.getSkinTextures(uuid).texture();
            float avSz = 26f;
            DrawUtil.drawRoundedTextureWithUV(ctx.getMatrices(), skin, cx2+7, ry+(CARD_SZ_H-avSz)/2f, avSz, avSz,
                BorderRadius.all(5), ColorRGBA.WHITE.mulAlpha(fa2), 8f/64f,8f/64f,16f/64f,16f/64f);

            // Ник и дата
            Font nf3 = Fonts.MEDIUM.getFont(7f);
            drawT(ctx, nf3, accounts.get(i), cx2+40, ry+9, TXT.mulAlpha(fa2));
            Font df2 = Fonts.MEDIUM.getFont(4.5f);
            drawT(ctx, df2, "01.01.2025", cx2+40, ry+20, SUB.mulAlpha(fa2));

            // Три иконки справа (всегда видны)
            float iSz = 14f, iGap = 4f;
            float i3x = cx2+CARD_W-8-iSz;
            float i2x = i3x-iSz-iGap;
            float i1x = i2x-iSz-iGap;
            float icy = ry+(CARD_SZ_H-iSz)/2f;

            // Фон иконок
            DrawUtil.drawRoundedRect(ctx.getMatrices(), i1x-3, icy-2, iSz*3+iGap*2+6, iSz+4, BorderRadius.all(5),
                new ColorRGBA(30,50,120,(int)(140*(hov?1:0.5f))).mulAlpha(fa2));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), i1x-3, icy-2, iSz*3+iGap*2+6, iSz+4, -0.1f, BorderRadius.all(5),
                new ColorRGBA(60,100,220,(int)(180*(hov?1:0.5f))).mulAlpha(fa2));

            Font icf = Fonts.MEDIUM.getFont(5.5f);
            // Порядок: закрепить | копировать | удалить
            // Закрепить — звезда (рисуем геометрически)
            boolean s1h = mx>=i1x && mx<=i1x+iSz && my>=icy && my<=icy+iSz;
            if (s1h) DrawUtil.drawRoundedRect(ctx.getMatrices(), i1x, icy, iSz, iSz, BorderRadius.all(3), new ColorRGBA(60,100,220,100).mulAlpha(fa2));
            drawStar(ctx, i1x+iSz/2f, icy+iSz/2f, 4.5f, 2f, (pin?PIN_CLR:(s1h?ColorRGBA.WHITE:ICO_DEF)).mulAlpha(fa2));

            // Копировать — два прямоугольника
            boolean s2h = mx>=i2x && mx<=i2x+iSz && my>=icy && my<=icy+iSz;
            if (s2h) DrawUtil.drawRoundedRect(ctx.getMatrices(), i2x, icy, iSz, iSz, BorderRadius.all(3), new ColorRGBA(60,100,220,100).mulAlpha(fa2));
            drawCopyIcon(ctx, i2x+iSz/2f, icy+iSz/2f, (s2h?ColorRGBA.WHITE:ICO_DEF).mulAlpha(fa2));

            // Удалить — крест
            boolean s3h = mx>=i3x && mx<=i3x+iSz && my>=icy && my<=icy+iSz;
            if (s3h) DrawUtil.drawRoundedRect(ctx.getMatrices(), i3x, icy, iSz, iSz, BorderRadius.all(3), new ColorRGBA(180,40,40,100).mulAlpha(fa2));
            drawCrossIcon(ctx, i3x+iSz/2f, icy+iSz/2f, (s3h?RED:ICO_DEF).mulAlpha(fa2));
        }
        ctx.disableScissor();
    }

    // ── mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int lx=lpX(), ly=lpY(), gx=grX(), gy=grY();
        int iy=ly+42, by1=iy+INP_H+6, hw=(LP_W-16-5)/2, rx=lx+8+hw+5;
        int bkY=ly+LP_H-BH-8;

        // Левая панель
        if (btn==0 && mx>=lx+8 && mx<=lx+LP_W-8 && my>=iy && my<=iy+INP_H) { typing=true; return true; }
        if (btn==0 && mx>=lx+8 && mx<=lx+8+hw && my>=by1 && my<=by1+BH) { addAcc(input.toString().trim()); return true; }
        if (btn==0 && mx>=rx && mx<=rx+hw && my>=by1 && my<=by1+BH) { String n=genName(); addAcc(n); loginAs(n,true); return true; }
        if (btn==0 && mx>=lx+8 && mx<=lx+LP_W-8 && my>=bkY && my<=bkY+BH) { close(); return true; }
        if (btn==0 && typing) { typing=false; return true; }

        // Сетка карточек
        for (int i = 0; i < accounts.size(); i++) {
            int col=i%COLS, row=i/COLS;
            float cx2=gx+col*(CARD_W+CARD_G), cy2=gy+row*(CARD_SZ_H+CARD_G)-scrollY;
            if (mx<cx2||mx>cx2+CARD_W||my<cy2||my>cy2+CARD_SZ_H) continue;

            float iSz=14f, iGap=4f;
            float i3x=cx2+CARD_W-8-iSz, i2x=i3x-iSz-iGap, i1x=i2x-iSz-iGap;
            float icy=cy2+(CARD_SZ_H-iSz)/2f;

            if (btn==0 && mx>=i1x && mx<=i1x+iSz && my>=icy && my<=icy+iSz) { togglePin(i); return true; }
            if (btn==0 && mx>=i2x && mx<=i2x+iSz && my>=icy && my<=icy+iSz) {
                GLFW.glfwSetClipboardString(client.getWindow().getHandle(), accounts.get(i)); return true;
            }
            if (btn==0 && mx>=i3x && mx<=i3x+iSz && my>=icy && my<=icy+iSz) { removeAcc(i); return true; }
            if (btn==0) { loginAs(accounts.get(i), true); return true; }
            if (btn==1) { removeAcc(i); return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int gx=grX(), gy=grY();
        if (mx>=gx && mx<=gx+GRID_W && my>=gy && my<=gy+GRID_H) {
            targetScrollY -= sy*30;
            int rows=(int)Math.ceil((double)accounts.size()/COLS);
            int maxScroll=Math.max(0, rows*(CARD_SZ_H+CARD_G)-GRID_H);
            targetScrollY=Math.max(0,Math.min(targetScrollY,maxScroll));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (typing) {
            boolean ctrl=GLFW.glfwGetKey(client.getWindow().getHandle(),GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS;
            if (ctrl&&k==GLFW.GLFW_KEY_V) {
                String cl=GLFW.glfwGetClipboardString(client.getWindow().getHandle());
                if (cl!=null) { String f=cl.replaceAll("[^\\w]",""); int max=16-input.length(); if(max>0) input.append(f,0,Math.min(f.length(),max)); }
                return true;
            }
            if (k==GLFW.GLFW_KEY_ENTER)     { addAcc(input.toString().trim()); return true; }
            if (k==GLFW.GLFW_KEY_BACKSPACE && input.length()>0) { input.deleteCharAt(input.length()-1); return true; }
            if (k==GLFW.GLFW_KEY_ESCAPE)    { typing=false; return true; }
            return true;
        }
        if (k==GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean charTyped(char c, int m) {
        if (typing && input.length()<16 && (Character.isLetterOrDigit(c)||c=='_')) { input.append(c); return true; }
        return super.charTyped(c, m);
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // ── логика ────────────────────────────────────────────────────────────────
    private void addAcc(String n) {
        if (n.isEmpty()) return;
        if (accounts.stream().noneMatch(a->a.equalsIgnoreCase(n))) { accounts.add(n); sort(); syncCards(); save(); }
        input.setLength(0); typing=false;
    }

    private void removeAcc(int i) {
        String n=accounts.remove(i);
        pinned.remove(n);
        syncCards(); save(); savePinned();
        int maxScroll=Math.max(0,(int)Math.ceil((double)accounts.size()/COLS)*(CARD_SZ_H+CARD_G)-GRID_H);
        targetScrollY=Math.max(0,Math.min(targetScrollY,maxScroll));
        scrollY=Math.min(scrollY,targetScrollY);
    }

    private void togglePin(int i) {
        String n=accounts.get(i);
        if (pinned.contains(n)) pinned.remove(n); else pinned.add(n);
        savePinned(); sort(); syncCards();
    }

    private void loginAs(String name, boolean sv) {
        try {
            UUID uuid=UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes());
            Session session=new Session(name,uuid,"0",Optional.empty(),Optional.empty(),Session.AccountType.LEGACY);
            Class<?> cl=client.getClass(); Field sf=null;
            while (cl!=null&&sf==null) { for (Field f:cl.getDeclaredFields()) if(f.getType()==Session.class){sf=f;break;} cl=cl.getSuperclass(); }
            if (sf!=null) { sf.setAccessible(true); sf.set(client,session); if(sv) saveLast(name); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String genName() {
        Random r=new Random();
        String[] adj={"Dark","Shadow","Void","Neon","Frost","Storm","Blaze","Phantom","Crimson","Azure"};
        String[] noun={"Wolf","Fox","Eagle","Raven","Viper","Hawk","Ghost","Blade","Reaper","Knight"};
        return switch(r.nextInt(3)){
            case 0 -> adj[r.nextInt(adj.length)]+noun[r.nextInt(noun.length)];
            case 1 -> noun[r.nextInt(noun.length)]+(r.nextInt(9000)+1000);
            default -> "x"+adj[r.nextInt(adj.length)]+noun[r.nextInt(noun.length)];
        };
    }

    // Звезда (5 лучей)
    private void drawStar(DrawContext ctx, float cx, float cy, float outer, float inner, ColorRGBA c) {
        int rgb = c.getRGB();
        for (int i = 0; i < 5; i++) {
            double a1 = Math.toRadians(-90 + i*72);
            double a2 = Math.toRadians(-90 + i*72 + 36);
            double a3 = Math.toRadians(-90 + (i+1)*72);
            float x1=(float)(cx+Math.cos(a1)*outer), y1=(float)(cy+Math.sin(a1)*outer);
            float x2=(float)(cx+Math.cos(a2)*inner), y2=(float)(cy+Math.sin(a2)*inner);
            float x3=(float)(cx+Math.cos(a3)*outer), y3=(float)(cy+Math.sin(a3)*outer);
            ctx.fill((int)x1,(int)y1,(int)x2,(int)y2, rgb);
            ctx.fill((int)x2,(int)y2,(int)x3,(int)y3, rgb);
            ctx.fill((int)cx,(int)cy,(int)x1,(int)y1, rgb);
            ctx.fill((int)cx,(int)cy,(int)x2,(int)y2, rgb);
        }
    }

    // Иконка копирования — два прямоугольника
    private void drawCopyIcon(DrawContext ctx, float cx, float cy, ColorRGBA c) {
        int rgb = c.getRGB();
        // Задний прямоугольник
        ctx.fill((int)(cx-3),(int)(cy-4),(int)(cx+2),(int)(cy+1), rgb);
        // Передний прямоугольник (смещён)
        ctx.fill((int)(cx-1),(int)(cy-2),(int)(cx+4),(int)(cy+3), new ColorRGBA(0,0,0,0).getRGB()); // очистка
        ctx.fill((int)(cx-1),(int)(cy-2),(int)(cx+4),(int)(cy+3), rgb);
        // Рамка переднего (внутри пусто)
        ctx.fill((int)(cx),(int)(cy-1),(int)(cx+3),(int)(cy+2), new ColorRGBA(10,11,18,255).getRGB());
    }

    // Крест для удаления
    private void drawCrossIcon(DrawContext ctx, float cx, float cy, ColorRGBA c) {
        int rgb = c.getRGB();
        // Диагональ 1
        for (int i = -3; i <= 3; i++) { ctx.fill((int)(cx+i)-1,(int)(cy+i)-1,(int)(cx+i)+1,(int)(cy+i)+1, rgb); }
        // Диагональ 2
        for (int i = -3; i <= 3; i++) { ctx.fill((int)(cx-i)-1,(int)(cy+i)-1,(int)(cx-i)+1,(int)(cy+i)+1, rgb); }
    }

    private void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc=new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f,t,x,y,c); cdc.draw();
        cdc.getMatrices().pop();
    }
}
