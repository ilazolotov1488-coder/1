package by.saskkeee.user;

import com.adl.nativeprotect.Native;
import com.adl.nativeprotect.User;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UserInfo {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static String cachedUid   = null;
    private static String cachedLogin = null;
    private static long   lastCheck   = 0;

    @Native(critical = true)
    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 10_000) return;
        lastCheck = now;

        // Приоритет 1: нативка протера (User.java) — работает с любым лоадером
        try {
            String nativeUsername = User.getInstance().profile("username");
            String nativeUid      = User.getInstance().profile("uid");
            if (nativeUsername != null && !nativeUsername.isEmpty()) cachedLogin = nativeUsername;
            if (nativeUid      != null && !nativeUid.isEmpty())      cachedUid   = nativeUid;
        } catch (Throwable ignored) {}

        // Приоритет 2: файлы нашего лоадера (C:\Space Visuals\)
        Path spaceVisuals = Path.of("C:\\Space Visuals");
        Path minecraft    = spaceVisuals.resolve(".minecraft");
        Path spaceDir     = minecraft.resolve("SpaceVisuals");

        if (cachedUid == null || cachedUid.isEmpty()) {
            for (Path base : new Path[]{ spaceVisuals, minecraft, spaceDir }) {
                try {
                    Path p = base.resolve("uid.txt");
                    if (Files.exists(p)) {
                        String v = Files.readString(p).trim();
                        if (!v.isEmpty()) { cachedUid = v; break; }
                    }
                } catch (IOException ignored) {}
            }
        }

        if (cachedLogin == null || cachedLogin.isEmpty()) {
            for (Path base : new Path[]{ spaceVisuals, minecraft, spaceDir }) {
                try {
                    Path p = base.resolve("login.txt");
                    if (Files.exists(p)) {
                        String v = Files.readString(p).trim();
                        if (!v.isEmpty()) { cachedLogin = v; break; }
                    }
                } catch (IOException ignored) {}
            }
        }

        // Приоритет 3: last_account.txt из AltManager
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

    /** Логин из нативки / лоадера / AltManager (или ник сессии как последний fallback) */
    @Native
    public static String getUsername() {
        refresh();
        if (cachedLogin != null && !cachedLogin.isEmpty()) return cachedLogin;
        return mc.getSession().getUsername();
    }

    /** UID из нативки / лоадера (или пустая строка если ещё не получен) */
    @Native
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
