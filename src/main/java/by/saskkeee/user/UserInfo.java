package by.saskkeee.user;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserInfo {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static String cachedUid   = null;
    private static String cachedLogin = null;
    private static long   lastCheck   = 0;

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 10_000) return;
        lastCheck = now;

        // Базовые пути для поиска
        Path spaceVisuals = Path.of("C:\\Space Visuals");
        Path minecraft    = spaceVisuals.resolve(".minecraft");
        Path spaceDir     = minecraft.resolve("SpaceVisuals");

        // uid.txt — создаётся лоадером после авторизации
        for (Path base : new Path[]{ spaceVisuals, minecraft, spaceDir }) {
            try {
                Path p = base.resolve("uid.txt");
                if (Files.exists(p)) {
                    String v = Files.readString(p).trim();
                    if (!v.isEmpty()) { cachedUid = v; break; }
                }
            } catch (IOException ignored) {}
        }

        // login.txt — создаётся лоадером после авторизации
        // Fallback: last_account.txt из AltManager
        for (Path base : new Path[]{ spaceVisuals, minecraft, spaceDir }) {
            try {
                Path p = base.resolve("login.txt");
                if (Files.exists(p)) {
                    String v = Files.readString(p).trim();
                    if (!v.isEmpty()) { cachedLogin = v; break; }
                }
            } catch (IOException ignored) {}
        }

        // Fallback на last_account.txt если login.txt нет
        if (cachedLogin == null || cachedLogin.isEmpty()) {
            try {
                Path p = spaceDir.resolve("last_account.txt");
                if (Files.exists(p)) {
                    String v = Files.readString(p).trim();
                    if (!v.isEmpty()) cachedLogin = v;
                }
            } catch (IOException ignored) {}
        }
    }

    /** Логин из лоадера / AltManager (или ник сессии как последний fallback) */
    public static String getUsername() {
        refresh();
        if (cachedLogin != null && !cachedLogin.isEmpty()) return cachedLogin;
        return mc.getSession().getUsername();
    }

    /** UID из лоадера (или пустая строка если ещё не получен) */
    public static String getUID() {
        refresh();
        return cachedUid != null ? cachedUid : "";
    }

    public static String getRole() {
        return "Разработчик";
    }

    /** Формат: login(uid) или просто login если uid нет */
    public static String getLoginWithUid() {
        String login = getUsername();
        String uid   = getUID();
        if (uid.isEmpty()) return login;
        return login + "(" + uid + ")";
    }
}
