package space.visuals.base.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.Getter;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class ConfigManager {
    public static File configDirectory;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ConfigManager() {
        configDirectory = new File(Zenith.getDirectory(), "configs");
        Zenith.getDirectory().mkdirs();
        configDirectory.mkdirs();

        loadConfig("current_config");

        if (!space.visuals.client.modules.impl.misc.Sounds.INSTANCE.isEnabled()) {
            space.visuals.client.modules.impl.misc.Sounds.INSTANCE.toggle();
        }

        scheduler.scheduleAtFixedRate(() -> {
            try { saveConfig("current_config"); } catch (Exception ignored) {}
        }, 5, 5, TimeUnit.MINUTES);
    }

    public boolean saveConfig(String configName) {
        try {
            configDirectory.mkdirs();
            File file = new File(configDirectory, configName + ".space");

            Config config = new Config(configName);
            JsonObject data = config.save();
            if (data == null) {
                System.err.println("[ConfigManager] save() returned null for: " + configName);
                return false;
            }

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
            Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadConfig(String configName) {
        try {
            File file = new File(configDirectory, configName + ".space");
            if (!file.exists()) return false;

            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            new Config(configName).load(object);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        File file = new File(configDirectory, configName + ".space");
        return file.exists() ? new Config(configName) : null;
    }

    public List<String> configNames() {
        List<String> names = new ArrayList<>();
        File[] files = configDirectory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".space"))
                    names.add(f.getName().replace(".space", ""));
            }
        }
        return names;
    }

    public boolean deleteConfig(String configName) {
        File file = new File(configDirectory, configName + ".space");
        return file.exists() && file.delete();
    }

    public void save() {
        scheduler.shutdown();
        saveConfig("current_config");
    }
}
