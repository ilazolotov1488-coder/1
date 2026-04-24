package space.visuals.base.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import space.visuals.Zenith;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;

public class DiscordManager {

    private static final long APP_ID = 1483903628636061859L;
    private static final long UPDATE_INTERVAL_MS = 15_000L;

    private IPCClient client;
    private final long startTime = System.currentTimeMillis() / 1000L;
    private volatile String discordUsername = "";
    private volatile boolean connected = false;
    private volatile BufferedImage pendingAvatarImage = null;
    private NativeImageBackedTexture avatarTexture = null;
    private Identifier avatarTextureId = null;

    public String getDiscordUsername() {
        return discordUsername;
    }

    /** Вызывать только с render-треда. Загружает pending аватар в текстуру MC. */
    public void uploadPendingAvatar() {
        BufferedImage img = pendingAvatarImage;
        if (img == null) return;
        pendingAvatarImage = null;
        try {
            NativeImage ni = new NativeImage(NativeImage.Format.RGBA, img.getWidth(), img.getHeight(), false);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >>  8) & 0xFF;
                    int b =  argb        & 0xFF;
                    ni.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            if (avatarTexture != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(avatarTextureId);
                avatarTexture.close();
            }
            avatarTexture = new NativeImageBackedTexture(ni);
            avatarTextureId = Zenith.id("discord_avatar");
            MinecraftClient.getInstance().getTextureManager().registerTexture(avatarTextureId, avatarTexture);
            avatarTexture.upload();
        } catch (Exception e) {
            System.err.println("[Discord] Avatar upload failed: " + e.getMessage());
        }
    }

    public Identifier getAvatarTextureId() {
        return avatarTextureId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect() {
        Thread t = new Thread(() -> {
            while (!connected) {
                try {
                    client = new IPCClient(APP_ID);
                    client.setListener(new IPCListener() {
                        @Override
                        public void onReady(IPCClient ipcClient) {
                            User user = ipcClient.getCurrentUser();
                            if (user != null) {
                                discordUsername = user.getName();
                                String url = user.getEffectiveAvatarUrl();
                                if (url != null && !url.isEmpty()) {
                                    loadAvatarAsync(url);
                                }
                            }
                            connected = true;
                            System.out.println("[Discord] Ready, user=" + discordUsername);
                            sendPresence();
                            startUpdateLoop();
                        }

                        @Override
                        public void onClose(IPCClient ipcClient, JsonObject json) {
                            connected = false;
                        }

                        @Override
                        public void onDisconnect(IPCClient ipcClient, Throwable t) {
                            connected = false;
                        }

                        @Override public void onPacketSent(IPCClient c, Packet p) {}
                        @Override public void onPacketReceived(IPCClient c, Packet p) {}
                        @Override public void onActivityJoin(IPCClient c, String s) {}
                        @Override public void onActivitySpectate(IPCClient c, String s) {}
                        @Override public void onActivityJoinRequest(IPCClient c, String s, User u) {}
                    });
                    client.connect();
                } catch (Exception e) {
                    System.err.println("[Discord] Connect failed: " + e.getMessage() + ", retrying in 30s");
                    try { Thread.sleep(30_000L); } catch (InterruptedException ignored) { return; }
                }
            }
        }, "Discord-IPC");
        t.setDaemon(true);
        t.start();
    }

    private void startUpdateLoop() {
        Thread t = new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(UPDATE_INTERVAL_MS);
                    if (connected) sendPresence();
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }, "Discord-Update");
        t.setDaemon(true);
        t.start();
    }

    private void sendPresence() {
        try {
            com.google.gson.JsonArray buttons = new com.google.gson.JsonArray();
            com.google.gson.JsonObject btn1 = new com.google.gson.JsonObject();
            btn1.addProperty("label", "Telegram");
            btn1.addProperty("url", "https://t.me/spacevisuals");
            buttons.add(btn1);

            RichPresence presence = new RichPresence.Builder()
                    .setActivityType(ActivityType.Playing)
                    .setDetails("Playing Minecraft")
                    .setLargeImage("https://raw.githubusercontent.com/ilazolotov1488-coder/1/master/animlogo.gif", "Space Visuals")
                    .setStartTimestamp(startTime)
                    .setButtons(buttons)
                    .build();
            client.sendRichPresence(presence);
        } catch (Exception e) {
            System.err.println("[Discord] sendPresence failed: " + e.getMessage());
            connected = false;
        }
    }

    private void loadAvatarAsync(String url) {
        Thread t = new Thread(() -> {
            try {
                String base = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
                if (base.matches(".*\\.(webp|gif|png|jpg)$")) {
                    base = base.substring(0, base.lastIndexOf("."));
                }
                String pngUrl = base + ".png?size=64";

                File tmp = File.createTempFile("discord_avatar_", ".png");
                tmp.deleteOnExit();

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "Invoke-WebRequest -Uri '" + pngUrl + "' -OutFile '" + tmp.getAbsolutePath() + "' -TimeoutSec 15"
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.getInputStream().transferTo(OutputStream.nullOutputStream());
                proc.waitFor();

                if (tmp.length() > 0) {
                    BufferedImage img = ImageIO.read(tmp);
                    if (img != null) {
                        pendingAvatarImage = img;
                        System.out.println("[Discord] Avatar loaded " + img.getWidth() + "x" + img.getHeight());
                    }
                }
                tmp.delete();
            } catch (Exception e) {
                System.err.println("[Discord] Avatar load failed: " + e.getMessage());
            }
        }, "Discord-Avatar");
        t.setDaemon(true);
        t.start();
    }

    public void shutdown() {
        connected = false;
        try {
            if (client != null) client.close();
        } catch (Exception ignored) {}
    }
}
