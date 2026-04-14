package space.visuals.utility.game.server;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.network.ServerInfo;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.utility.interfaces.IMinecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Сохраняет последний сервер/анархию при выходе,
 * и автоматически переходит на него при следующем входе.
 */
public class LastServerManager implements IMinecraft {

    private final File FILE = new File(Zenith.getDirectory(), "last_server.json");

    private String savedServer = "";
    private int savedAnarchy = -1;
    private String savedIp = "";

    // флаг — уже выполнили реконнект в этой сессии
    private boolean reconnected = false;
    private boolean wasInGame = false;

    public LastServerManager() {
        load();
        EventManager.register(this);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) {
            wasInGame = false;
            reconnected = false;
            return;
        }

        ServerHandler sh = Zenith.getInstance().getServerHandler();

        // Сохраняем текущее состояние каждый тик
        String currentServer = sh.getServer();
        int currentAnarchy   = sh.getAnarchy();
        String currentIp     = currentIp();

        if (!currentServer.equals("Vanilla")) {
            savedServer  = currentServer;
            savedAnarchy = currentAnarchy;
            savedIp      = currentIp;
            save();
        }

        // Автореконнект — один раз после входа на сервер
        if (!reconnected && !wasInGame) {
            wasInGame = true;
            tryReconnect(sh, currentIp);
        }
    }

    // ── Reconnect ─────────────────────────────────────────────────────────────

    private void tryReconnect(ServerHandler sh, String currentIp) {
        if (savedIp.isEmpty() || savedServer.isEmpty()) return;
        // Только если зашли на тот же сервер
        if (!currentIp.equalsIgnoreCase(savedIp)) return;

        reconnected = true;

        switch (savedServer) {
            case "HolyWorld" -> {
                if (savedAnarchy > 0) {
                    Zenith.getInstance().getRCTRepository().reconnect(savedAnarchy);
                }
            }
            case "FunTime", "CopyTime" -> {
                if (savedAnarchy > 0) {
                    // FunTime/CopyTime — команда /an<номер>
                    mc.player.networkHandler.sendChatCommand("an" + savedAnarchy);
                }
            }
            case "ReallyWorld" -> {
                // ReallyWorld анархия через команду
                if (savedAnarchy > 0) {
                    mc.player.networkHandler.sendChatCommand("an" + savedAnarchy);
                }
            }
        }
    }

    // ── File IO ───────────────────────────────────────────────────────────────

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("server",  savedServer);
            obj.addProperty("anarchy", savedAnarchy);
            obj.addProperty("ip",      savedIp);
            try (FileWriter w = new FileWriter(FILE, StandardCharsets.UTF_8)) {
                w.write(new Gson().toJson(obj));
            }
        } catch (Exception ignored) {}
    }

    private void load() {
        if (!FILE.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(FILE, StandardCharsets.UTF_8))) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            savedServer  = obj.has("server")  ? obj.get("server").getAsString()  : "";
            savedAnarchy = obj.has("anarchy") ? obj.get("anarchy").getAsInt()    : -1;
            savedIp      = obj.has("ip")      ? obj.get("ip").getAsString()      : "";
        } catch (Exception ignored) {}
    }

    private String currentIp() {
        if (mc.getNetworkHandler() == null) return "";
        ServerInfo info = mc.getNetworkHandler().getServerInfo();
        return info != null ? info.address.toLowerCase() : "";
    }
}
