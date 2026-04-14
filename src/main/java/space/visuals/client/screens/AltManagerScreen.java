package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import net.minecraft.client.session.Session;

public class AltManagerScreen extends Screen {

    private static final Identifier BG_TEX = Zenith.id("icons/menu_bg.png");

    private final Screen parent;
    private final List<String> accounts = new ArrayList<>();
    private Path savePath;
    private boolean initialized = false;

    private boolean isTyping = false;
    private final StringBuilder inputText = new StringBuilder();
    private float scrollOffset = 0, targetScrollOffset = 0;
    private int selectedIndex = -1;

    private final Animation fadeIn  = new Animation(500, 0, Easing.QUAD_IN_OUT);
    private final Animation panelIn = new Animation(450, 0, Easing.BAKEK_SIZE);
    private final List<Animation> itemA     = new ArrayList<>();
    private final List<Animation> itemSlide = new ArrayList<>(); // появление слева
    private final List<Animation> itemDel   = new ArrayList<>(); // анимация удаления
    private final Animation addA  = new Animation(140, 0, Easing.QUAD_IN_OUT);
    private final Animation genA  = new Animation(140, 0, Easing.QUAD_IN_OUT);
    private final Animation backA = new Animation(140, 0, Easing.QUAD_IN_OUT);
    private final Animation inpA  = new Animation(140, 0, Easing.QUAD_IN_OUT);
    private final Animation row1A = new Animation(400, 0, Easing.QUAD_IN_OUT); // строка кнопок
    private final Animation row2A = new Animation(500, 0, Easing.QUAD_IN_OUT);

    // Компактные цвета
    private static final ColorRGBA BG_PANEL = new ColorRGBA(12, 13, 20, 210);
    private static final ColorRGBA IT       = new ColorRGBA(255,255,255, 7);
    private static final ColorRGBA IT_H     = new ColorRGBA(255,255,255,14);
    private static final ColorRGBA IT_S     = new ColorRGBA(100, 92,200,50);
    private static final ColorRGBA BR       = new ColorRGBA(255,255,255,18);
    private static final ColorRGBA BR_H     = new ColorRGBA(255,255,255,36);
    private static final ColorRGBA ACC      = new ColorRGBA(108, 99,210,220);
    private static final ColorRGBA ACC_H    = new ColorRGBA(122,113,228,255);
    private static final ColorRGBA TXT      = new ColorRGBA(240,240,252,255);
    private static final ColorRGBA SUB      = new ColorRGBA(105,105,130,190);
    private static final ColorRGBA DIM      = new ColorRGBA(70, 70, 90,130);
    private static final ColorRGBA GRN      = new ColorRGBA(65,190,105,220);
    private static final ColorRGBA GLOW     = new ColorRGBA(68, 62,175,255);

    // Компактные размеры
    private static final int PW   = 280;
    private static final int IH   = 36;  // было 44 — компактнее
    private static final int IG   = 3;   // было 4
    private static final int BH   = 30;
    private static final int ROWS = 4;

    private static final String[] NAMES = {"Player","Shadow","Storm","Dark","Wolf","Fox","Eagle","Tiger","Dragon","Frost","Nova","Apex"};

    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        savePath = client.runDirectory.toPath().resolve("SpaceVisuals/accounts.txt");
        if (!initialized) { loadAccounts(); initialized = true; }
        fadeIn.animateTo(1);
        panelIn.animateTo(1);
        row1A.animateTo(1);
        row2A.animateTo(1);
    }

    private int px()    { return width/2 - PW/2; }
    private int listH() { return ROWS*(IH+IG) + 6; }
    private int totalH(){ return 30 + listH() + 8 + BH + 6 + BH; }
    private int py()    { return height/2 - totalH()/2; }
    private int listY() { return py() + 30; }
    private int row1Y() { return listY() + listH() + 8; }
    private int row2Y() { return row1Y() + BH + 6; }

    private void loadAccounts() {
        try {
            if (Files.exists(savePath)) {
                accounts.clear();
                accounts.addAll(Files.readAllLines(savePath).stream().filter(l -> !l.isBlank()).toList());
            }
        } catch (Exception ignored) {}
        syncA();
        applyLast();
    }

    private void applyLast() {
        try {
            Path lp = savePath.getParent().resolve("last_account.txt");
            if (Files.exists(lp)) {
                String l = Files.readString(lp).trim();
                if (!l.isEmpty()) { loginAs(l, false); selectedIndex = accounts.indexOf(l); }
            }
        } catch (Exception ignored) {}
    }

    private void saveAccounts() {
        try { Files.createDirectories(savePath.getParent()); Files.write(savePath, accounts); } catch (Exception ignored) {}
    }

    private void saveLast(String n) {
        try { Files.createDirectories(savePath.getParent()); Files.writeString(savePath.getParent().resolve("last_account.txt"), n); } catch (Exception ignored) {}
    }

    private void syncA() {
        while (itemA.size() < accounts.size()) {
            itemA.add(new Animation(130, 0, Easing.QUAD_IN_OUT));
            Animation sl = new Animation(350 + itemSlide.size()*50, 0, Easing.QUAD_IN_OUT);
            sl.animateTo(1);
            itemSlide.add(sl);
            Animation del = new Animation(200, 1, Easing.QUAD_IN_OUT); // начинается с 1
            itemDel.add(del);
        }
        while (itemA.size() > accounts.size()) {
            itemA.remove(itemA.size()-1);
            itemSlide.remove(itemSlide.size()-1);
            itemDel.remove(itemDel.size()-1);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float a  = (float) fadeIn.update();
        float ps = (float) panelIn.update();
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.15f;

        // Фон
        DrawUtil.drawTexture(ctx.getMatrices(), BG_TEX, 0, 0, width, height, ColorRGBA.WHITE.mulAlpha(a));
        ctx.fill(0, 0, width, height, new ColorRGBA(0,0,0,(int)(170*a)).getRGB());

        // Свечение
        float gcx = width/2f, gcy = height/2f;
        DrawUtil.drawShadow(ctx.getMatrices(), gcx-80, gcy-60, 160,120, 100f, BorderRadius.all(60), GLOW.mulAlpha(0.12f*a));

        int px = px(), py = py(), lx = px, ly = listY(), lh = listH();

        // Панель
        float so = (1f-ps)*6f;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx-so, py-so, PW+so*2, totalH()+so*2, BorderRadius.all(14), BG_PANEL.mulAlpha(a*ps));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx-so, py-so, PW+so*2, totalH()+so*2, -0.1f, BorderRadius.all(14), BR.mulAlpha(a*ps*0.5f));

        // Заголовок
        Font tf = Fonts.MEDIUM.getFont(9);
        String title = "Аккаунты";
        drawT(ctx, tf, title, px+PW/2f-tf.width(title)/2f, py+9, TXT.mulAlpha(a*ps));
        ctx.fill(px+20, py+22, px+PW-20, py+23, new ColorRGBA(255,255,255,(int)(10*a*ps)).getRGB());

        // Список
        ctx.enableScissor(lx+1, ly+1, lx+PW-1, ly+lh-1);
        syncA();

        if (accounts.isEmpty()) {
            Font ef = Fonts.MEDIUM.getFont(6f);
            String em = "Нет аккаунтов";
            drawT(ctx, ef, em, lx+PW/2f-ef.width(em)/2f, ly+lh/2f-ef.height()/2f, DIM.mulAlpha(a*ps));
        } else {
            for (int i = 0; i < accounts.size(); i++) {
                float iy = ly+4+i*(IH+IG)-scrollOffset;
                if (iy+IH < ly || iy > ly+lh) continue;
                boolean hov = mx>=lx+4 && mx<=lx+PW-4 && my>=iy && my<=iy+IH;
                itemA.get(i).animateTo(hov?1:0);
                float hv = (float)itemA.get(i).update();
                // Анимация появления слева
                float sl = i < itemSlide.size() ? (float)itemSlide.get(i).update() : 1f;
                float delV = i < itemDel.size() ? (float)itemDel.get(i).update() : 1f;
                float slideX = (1f - sl) * 22f + (1f - delV) * 30f; // появление слева + удаление вправо
                float fa2 = a * ps * sl * delV;
                boolean sel = i == selectedIndex;

                DrawUtil.drawRoundedRect(ctx.getMatrices(), lx+4+slideX, iy, PW-8, IH, BorderRadius.all(7),
                    (sel ? IT_S : IT.mix(IT_H,hv)).mulAlpha(fa2));
                if (hov||sel) DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx+4+slideX, iy, PW-8, IH, -0.1f, BorderRadius.all(7),
                    (sel ? new ColorRGBA(100,92,200,60) : BR_H).mulAlpha(fa2));

                // Голова
                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:"+accounts.get(i)).getBytes());
                Identifier skin = DefaultSkinHelper.getSkinTextures(uuid).texture();
                float hs = 22; // чуть меньше
                DrawUtil.drawRoundedTextureWithUV(ctx.getMatrices(), skin, lx+9+slideX, iy+(IH-hs)/2f, hs, hs,
                    BorderRadius.all(4), ColorRGBA.WHITE.mulAlpha(fa2), 8f/64f, 8f/64f, 16f/64f, 16f/64f);

                Font nf = Fonts.MEDIUM.getFont(7f);
                drawT(ctx, nf, accounts.get(i), lx+38+slideX, iy+6, TXT.mulAlpha(fa2));
                Font sf = Fonts.MEDIUM.getFont(5f);
                boolean active = client.getSession().getUsername().equals(accounts.get(i));
                drawT(ctx, sf, active?"● активен":"○ не выбран", lx+38+slideX, iy+18, (active?GRN:SUB).mulAlpha(fa2));

                // Удалить
                Font df = Fonts.MEDIUM.getFont(5.5f);
                drawT(ctx, df, "✕", lx+PW-16+slideX, iy+(IH-df.height())/2f, DIM.mulAlpha(fa2*(0.25f+0.75f*hv)));
            }
        }
        ctx.disableScissor();

        // Строка 1: инпут + Добавить — с анимацией появления снизу
        float r1av = (float)row1A.update();
        float r1slide = (1f - r1av) * 10f;
        float r1fa = a * ps * r1av;
        int r1y = row1Y();
        int addW = 80, inpW = PW-addW-5;

        boolean inpHov = mx>=lx && mx<=lx+inpW && my>=r1y && my<=r1y+BH;
        inpA.animateTo(isTyping||inpHov?1:0);
        float ihv = (float)inpA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx, r1y+r1slide, inpW, BH, BorderRadius.all(8), IT.mix(IT_H,ihv).mulAlpha(r1fa));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx, r1y+r1slide, inpW, BH, -0.1f, BorderRadius.all(8),
            (isTyping?new ColorRGBA(100,92,200,80):BR).mulAlpha(r1fa));
        Font inf = Fonts.MEDIUM.getFont(6f);
        String disp = isTyping ? inputText+(System.currentTimeMillis()/500%2==0?"|":" ") : (inputText.length()>0?inputText.toString():"Ник...");
        drawT(ctx, inf, disp, lx+8, r1y+r1slide+(BH-inf.height())/2f, (inputText.length()>0||isTyping?TXT:DIM).mulAlpha(r1fa));

        int addX = lx+inpW+5;
        boolean addHov = mx>=addX && mx<=addX+addW && my>=r1y && my<=r1y+BH;
        addA.animateTo(addHov?1:0);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), addX, r1y+r1slide, addW, BH, BorderRadius.all(8), ACC.mix(ACC_H,(float)addA.update()).mulAlpha(r1fa));
        Font bf = Fonts.MEDIUM.getFont(6f);
        drawT(ctx, bf, "Добавить", addX+(addW-bf.width("Добавить"))/2f, r1y+r1slide+(BH-bf.height())/2f, TXT.mulAlpha(r1fa));

        // Строка 2: Рандом | Назад — с анимацией появления снизу
        float r2av = (float)row2A.update();
        float r2slide = (1f - r2av) * 10f;
        float r2fa = a * ps * r2av;
        int r2y = row2Y();
        int hw = (PW-5)/2;

        boolean genHov = mx>=lx && mx<=lx+hw && my>=r2y && my<=r2y+BH;
        genA.animateTo(genHov?1:0); float ghv=(float)genA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx, r2y+r2slide, hw, BH, BorderRadius.all(8), IT.mix(IT_H,ghv).mulAlpha(r2fa));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx, r2y+r2slide, hw, BH, -0.1f, BorderRadius.all(8), BR.mix(BR_H,ghv).mulAlpha(r2fa));
        drawT(ctx, bf, "Рандом", lx+(hw-bf.width("Рандом"))/2f, r2y+r2slide+(BH-bf.height())/2f, TXT.mulAlpha(r2fa));

        int bkX = lx+hw+5;
        boolean bkHov = mx>=bkX && mx<=bkX+hw && my>=r2y && my<=r2y+BH;
        backA.animateTo(bkHov?1:0); float bkHv=(float)backA.update();
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bkX, r2y+r2slide, hw, BH, BorderRadius.all(8), IT.mix(IT_H,bkHv).mulAlpha(r2fa));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), bkX, r2y+r2slide, hw, BH, -0.1f, BorderRadius.all(8), BR.mix(BR_H,bkHv).mulAlpha(r2fa));
        drawT(ctx, bf, "Назад", bkX+(hw-bf.width("Назад"))/2f, r2y+r2slide+(BH-bf.height())/2f, SUB.mix(TXT,bkHv).mulAlpha(r2fa));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int lx = px(), ly = listY(), lh = listH();
        int r1y = row1Y(), r2y = row2Y();
        int addW = 80, inpW = PW-addW-5, addX = lx+inpW+5;
        int hw = (PW-5)/2, bkX = lx+hw+5;

        if (mx>=lx && mx<=lx+PW && my>=ly && my<=ly+lh && !accounts.isEmpty()) {
            for (int i = 0; i < accounts.size(); i++) {
                float iy = ly+3+i*(IH+IG)-scrollOffset;
                if (my>=iy && my<=iy+IH) {
                    if (mx>=lx+PW-22 && btn==0) { removeAcc(i); return true; }                    if (btn==0) { loginAs(accounts.get(i), true); return true; }
                    if (btn==1) { removeAcc(i); return true; }
                }
            }
        }

        if (btn==0 && mx>=lx && mx<=lx+inpW && my>=r1y && my<=r1y+BH) { isTyping=true; return true; }
        if (btn==0 && mx>=addX && mx<=addX+addW && my>=r1y && my<=r1y+BH) { addAcc(inputText.toString().trim()); return true; }
        if (btn==0 && mx>=lx && mx<=lx+hw && my>=r2y && my<=r2y+BH) { String n=genName(); addAcc(n); loginAs(n,true); return true; }
        if (btn==0 && mx>=bkX && mx<=bkX+hw && my>=r2y && my<=r2y+BH) { close(); return true; }
        if (btn==0 && isTyping) { isTyping=false; return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int ly = listY(), lh = listH();
        if (my>=ly && my<=ly+lh && !accounts.isEmpty()) {
            targetScrollOffset -= sy*35;
            int max = Math.max(0, accounts.size()*(IH+IG)-lh+6);
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, max));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (isTyping) {
            boolean ctrl = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS;
            if (ctrl && k==GLFW.GLFW_KEY_V) {
                String cl = GLFW.glfwGetClipboardString(client.getWindow().getHandle());
                if (cl!=null) { String f=cl.replaceAll("[^\\w]",""); int max=16-inputText.length(); if(max>0) inputText.append(f,0,Math.min(f.length(),max)); }
                return true;
            }
            if (k==GLFW.GLFW_KEY_ENTER) { addAcc(inputText.toString().trim()); return true; }
            if (k==GLFW.GLFW_KEY_BACKSPACE && inputText.length()>0) { inputText.deleteCharAt(inputText.length()-1); return true; }
            if (k==GLFW.GLFW_KEY_ESCAPE) { isTyping=false; return true; }
            return true;
        }
        if (k==GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean charTyped(char c, int m) {
        if (isTyping && inputText.length()<16 && (Character.isLetterOrDigit(c)||c=='_')) { inputText.append(c); return true; }
        return super.charTyped(c, m);
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    private void addAcc(String n) {
        if (n.isEmpty()) return;
        if (accounts.stream().noneMatch(a->a.equalsIgnoreCase(n))) { accounts.add(n); syncA(); saveAccounts(); }
        inputText.setLength(0); isTyping=false;
    }

    private void removeAcc(int i) {
        // Убираем анимацию удаления через Thread - она вызывает баги с индексами
        // Просто удаляем сразу
        accounts.remove(i);
        if (selectedIndex >= accounts.size()) selectedIndex = -1;
        else if (selectedIndex == i) selectedIndex = -1;
        syncA();
        saveAccounts();
        // Сбрасываем скролл чтобы не было пустого места сверху
        int maxOff = Math.max(0, accounts.size()*(IH+IG)-listH()+6);
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxOff));
        scrollOffset = Math.min(scrollOffset, targetScrollOffset);
    }

    private void loginAs(String name, boolean save) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes());
            Session session = new Session(name, uuid, "0", Optional.empty(), Optional.empty(), Session.AccountType.LEGACY);
            Class<?> clazz = client.getClass();
            Field sf = null;
            while (clazz != null && sf == null) {
                for (Field f : clazz.getDeclaredFields()) if (f.getType()==Session.class) { sf=f; break; }
                clazz = clazz.getSuperclass();
            }
            if (sf != null) { sf.setAccessible(true); sf.set(client, session); selectedIndex=accounts.indexOf(name); if(save) saveLast(name); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String genName() {
        Random r = new Random();
        String[][] combos = {
            // прилагательное + существительное
            {"Dark","Shadow","Void","Neon","Frost","Storm","Blaze","Phantom","Silent","Crimson","Azure","Lunar"},
            {"Wolf","Fox","Eagle","Raven","Viper","Hawk","Ghost","Blade","Reaper","Knight","Hunter","Specter"}
        };
        // Варианты генерации
        int style = r.nextInt(4);
        switch (style) {
            case 0: {
                // Прилагательное + Существительное
                String adj = combos[0][r.nextInt(combos[0].length)];
                String noun = combos[1][r.nextInt(combos[1].length)];
                return adj + noun;
            }
            case 1: {
                // Существительное + число
                String noun = combos[1][r.nextInt(combos[1].length)];
                return noun + (r.nextInt(9000) + 1000);
            }
            case 2: {
                // x + Прилагательное + Существительное
                String adj = combos[0][r.nextInt(combos[0].length)];
                String noun = combos[1][r.nextInt(combos[1].length)];
                return "x" + adj + noun;
            }
            default: {
                // Прилагательное + Существительное + число
                String adj = combos[0][r.nextInt(combos[0].length)];
                String noun = combos[1][r.nextInt(combos[1].length)];
                return adj + noun + (r.nextInt(99) + 1);
            }
        }
    }

    private void drawT(DrawContext ctx, Font f, String t, float x, float y, ColorRGBA c) {
        CustomDrawContext cdc = new CustomDrawContext(client.getBufferBuilders().getEntityVertexConsumers());
        cdc.getMatrices().push();
        cdc.getMatrices().multiplyPositionMatrix(ctx.getMatrices().peek().getPositionMatrix());
        cdc.drawText(f, t, x, y, c);
        cdc.draw();
        cdc.getMatrices().pop();
    }
}
