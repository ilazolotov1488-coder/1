package space.visuals.client.hud.elements.component;

import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.Collection;

public class DynamicIslandComponent extends DraggableHudElement {

    private static final float H        = 36f;   // высота острова
    private static final float AVATAR   = 24f;   // размер аватара
    private static final float PAD      = 8f;
    private static final float GAP      = 6f;
    private static final float SEP_W    = 0.5f;
    private static final float ACCENT_H = 1.5f;
    private static final float FS_NAME  = 7.5f;
    private static final float FS_SUB   = 5.5f;
    private static final float Y_POS    = 7f;
    private static final float RADIUS   = 8f;

    // Анимация ширины острова при смене режима
    private final Animation widthAnim = new Animation(400, 0, Easing.QUAD_IN_OUT);
    private final Animation alphaAnim = new Animation(350, 0, Easing.QUAD_IN_OUT);

    // Последний PVP текст для анимации
    private String lastPvpText = "";

    public DynamicIslandComponent(String name, float initialX, float initialY,
                                  float windowWidth, float windowHeight,
                                  float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        width  = 200f;
        height = H;
    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;

        Zenith.getInstance().getDiscordManager().uploadPendingAvatar();

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font nameFont = Fonts.MEDIUM.getFont(FS_NAME);
        Font subFont  = Fonts.MEDIUM.getFont(FS_SUB);

        alphaAnim.animateTo(1f);
        float anim = (float) alphaAnim.update();

        // Данные
        String discordName = Zenith.getInstance().getDiscordManager().getDiscordUsername();
        String playerName  = (discordName != null && !discordName.isEmpty())
                ? discordName : mc.player.getName().getString();
        String serverText  = getServerText();
        String pingText    = getPingText();

        // PVP боссбар
        ClientBossBar pvpBar = getPvpBossBar();
        boolean hasPvp = pvpBar != null;

        // Ширины секций
        float nameW   = nameFont.width(playerName);
        float serverW = subFont.width(serverText);
        float pingW   = subFont.width(pingText);

        // PVP: пилюля с иконкой + "PVP" + таймер (как в rockstar)
        float pvpIconSize  = FS_SUB + 3f;
        Font pvpFont       = Fonts.MEDIUM.getFont(FS_SUB);
        Font pvpLabelFont  = Fonts.MEDIUM.getFont(FS_SUB);
        String pvpLabel    = "PVP";
        String pvpTimer    = hasPvp ? " " + formatPvpTime(pvpBar.getName().getString()) : "";
        String pvpFull     = pvpLabel + pvpTimer; // для расчёта ширины
        float pvpLabelW    = pvpLabelFont.width(pvpLabel);
        float pvpTimerW    = pvpFont.width(pvpTimer);
        float pvpPillW     = pvpIconSize + GAP + pvpLabelW + pvpTimerW + GAP;
        float pvpRightW    = hasPvp ? pvpPillW + GAP : 0f;

        float normalRightW = Math.max(serverW, pingW);
        float rightW = hasPvp ? pvpRightW : normalRightW;

        float targetW = PAD + AVATAR + GAP + nameW + GAP * 2 + SEP_W + GAP * 2 + rightW + PAD;
        widthAnim.animateTo(targetW);
        float islandW = (float) widthAnim.update();
        if (islandW < 60f) islandW = targetW; // первый кадр

        float screenW = ctx.getScaledWindowWidth();
        float ix = screenW / 2f - islandW / 2f;
        float iy = Y_POS;

        // ── Фон ──
        DrawUtil.drawBlurHud(ctx.getMatrices(), ix, iy, islandW, H, 20, BorderRadius.all(RADIUS), ColorRGBA.WHITE);
        ctx.drawRoundedRect(ix, iy, islandW, H, BorderRadius.all(RADIUS), theme.getForegroundColor());
        ctx.drawRoundedBorder(ix, iy, islandW, H, 0.5f, BorderRadius.all(RADIUS), theme.getForegroundStroke());

        float midY = iy + H / 2f;

        // ── Аватар (с scissor чтобы не выходил за границы) ──
        float avatarX = ix + PAD;
        float avatarY = midY - AVATAR / 2f;

        // Scissor по границам острова
        ctx.enableScissor((int)(ix), (int)(iy), (int)(ix + islandW), (int)(iy + H));

        Identifier discordAvatar = Zenith.getInstance().getDiscordManager().getAvatarTextureId();
        if (discordAvatar != null) {
            DrawUtil.drawRoundedTexture(ctx.getMatrices(), discordAvatar,
                    avatarX, avatarY, AVATAR, AVATAR,
                    BorderRadius.all(4f), ColorRGBA.WHITE.mulAlpha(anim));
        } else if (mc.player instanceof AbstractClientPlayerEntity player) {
            DrawUtil.drawPlayerHeadWithRoundedShader(
                    ctx.getMatrices(),
                    player.getSkinTextures().texture(),
                    avatarX, avatarY, AVATAR,
                    BorderRadius.all(4f), ColorRGBA.WHITE.mulAlpha(anim)
            );
        }

        ctx.disableScissor();

        // ── Имя ──
        float nameX = avatarX + AVATAR + GAP;
        float nameY = midY - nameFont.height() / 2f;
        ctx.drawText(nameFont, playerName, nameX, nameY, theme.getWhite().mulAlpha(anim));

        // ── Разделитель ──
        float sepX = nameX + nameW + GAP * 2;
        float sepH = H * 0.4f;
        ctx.drawRoundedRect(sepX, midY - sepH / 2f, SEP_W, sepH,
                BorderRadius.all(SEP_W / 2f), theme.getGrayLight().mulAlpha(anim));

        // ── Правая секция ──
        float rightX = sepX + SEP_W + GAP * 2;
        float subLineH = subFont.height();

        if (hasPvp) {
            ColorRGBA pvpColor = new ColorRGBA(210, 50, 50, 255);
            float subY = midY - pvpFont.height() / 2f;

            // Красная пилюля-фон — 55%
            float pillH = H * 0.45f;
            float pillW = pvpPillW + GAP * 2;
            float pillY = midY - pillH / 2f;
            ctx.drawRoundedRect(rightX - GAP, pillY, pillW, pillH,
                    BorderRadius.all(pillH / 2f), pvpColor.mulAlpha(0.18f * anim));

            // Иконка combat
            float iconY = midY - pvpIconSize / 2f + 0.5f;
            DrawUtil.drawTexture(ctx.getMatrices(),
                    Zenith.id("icons/pvp_icon.png"),
                    rightX, iconY, pvpIconSize, pvpIconSize,
                    pvpColor.mulAlpha(anim));

            // "PVP" — основной текст
            float labelX = rightX + pvpIconSize + GAP;
            ctx.drawText(pvpLabelFont, pvpLabel, labelX, subY, pvpColor.mulAlpha(anim));

            // Таймер — чуть прозрачнее
            ctx.drawText(pvpFont, pvpTimer, labelX + pvpLabelW, subY,
                    pvpColor.mulAlpha(0.7f * anim));
        } else {
            float totalSubH = subLineH * 2 + 2f;
            float subStartY = midY - totalSubH / 2f;
            ctx.drawText(subFont, serverText, rightX, subStartY, theme.getWhiteGray().mulAlpha(anim));
            ctx.drawText(subFont, pingText,   rightX, subStartY + subLineH + 2f, theme.getGrayLight().mulAlpha(anim));
        }

        // ── Уголки темы ──
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), ix, iy, islandW, H, 0.5f, 14f, theme.getColor(), BorderRadius.all(RADIUS));

        width  = islandW;
        height = H;
    }

    /** Вытаскивает таймер/цифры из PVP строки */
    private String extractPvpTimer(String raw) {
        StringBuilder digits = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            else if (digits.length() > 0 && c == ' ') break;
        }
        if (digits.length() > 0) return digits + "s";
        return "";
    }

    /** Форматирует секунды из строки боссбара в "M:SS" */
    private String formatPvpTime(String raw) {
        int seconds = 0;
        StringBuilder digits = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            else if (digits.length() > 0 && !Character.isDigit(c)) break;
        }
        if (digits.length() > 0) {
            try { seconds = Integer.parseInt(digits.toString()); } catch (NumberFormatException ignored) {}
        }
        if (seconds <= 0) return "";
        int m = seconds / 60;
        int s = seconds % 60;
        return m + ":" + String.format("%02d", s);
    }

    private void renderBossBarsBelow(CustomDrawContext ctx, float ix, float startY, float islandW,
                                     Theme theme, Font font, float anim) {
    }

    private ClientBossBar getPvpBossBar() {
        if (mc.inGameHud == null) return null;
        for (ClientBossBar bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String name = bar.getName().getString().toLowerCase();
            if (name.contains("pvp") || name.contains("пвп")) return bar;
        }
        return null;
    }

    private ColorRGBA getBossBarColor(ClientBossBar bar) {
        return switch (bar.getColor()) {
            case RED    -> new ColorRGBA(220, 60, 60, 220);
            case GREEN  -> new ColorRGBA(80, 200, 100, 220);
            case BLUE   -> new ColorRGBA(80, 130, 220, 220);
            case YELLOW -> new ColorRGBA(230, 190, 50, 220);
            case PURPLE -> new ColorRGBA(160, 80, 220, 220);
            case WHITE  -> new ColorRGBA(220, 220, 220, 220);
            default     -> new ColorRGBA(180, 180, 180, 220);
        };
    }

    private String getServerText() {
        if (mc.isInSingleplayer()) return "Singleplayer";
        if (mc.getCurrentServerEntry() != null) {
            String addr = mc.getCurrentServerEntry().address;
            if (addr.contains(":")) addr = addr.substring(0, addr.indexOf(":"));
            if (addr.length() > 20) addr = addr.substring(0, 20);
            return addr;
        }
        return "Online";
    }

    private String getPingText() {
        if (mc.player == null || mc.player.networkHandler == null) return "0 ms";
        var entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
        if (entry == null) return "0 ms";
        return entry.getLatency() + " ms";
    }

    @Override
    protected void renderXLine(CustomDrawContext ctx, SheetCode nearest) {}
}
