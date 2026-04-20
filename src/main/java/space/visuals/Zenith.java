package space.visuals;

import by.saskkeee.annotations.CompileToNative;
import by.saskkeee.annotations.vmprotect.CompileType;
import by.saskkeee.annotations.vmprotect.VMProtect;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import space.visuals.base.autobuy.AutoBuyManager;
import space.visuals.base.discord.DiscordManager;


import space.visuals.base.comand.CommandManager;
import space.visuals.base.config.ConfigManager;
import space.visuals.base.filemanager.impl.FriendManager;
import space.visuals.base.filemanager.impl.StaffManager;
import space.visuals.base.modules.ModuleManager;
import space.visuals.base.request.ScriptManager;
import space.visuals.base.rotation.RotationManager;
import space.visuals.base.rotation.deeplearnig.DeepLearningManager;
import space.visuals.base.theme.ThemeManager;
import space.visuals.client.screens.menu.MenuScreen;
import space.visuals.utility.game.server.LastServerManager;
import space.visuals.utility.game.server.ServerHandler;
import space.visuals.base.notify.NotifyManager;
import space.visuals.base.repository.RCTRepository;
import space.visuals.utility.render.display.shader.DrawUtil;
import space.visuals.utility.render.display.shader.GlProgram;

import java.io.File;

/*
    эта паста рвет во всю убивает нищету убивает и деееельта юзераааа бож че ты несешь какая дельта ты че совссем ебанулся???
    эта хуйня не вывезет даже мой пениииис йоу йоу йоу йоу
 */

@Getter
public enum Zenith {
    INSTANCE;

    public static final String NAME = "space", VER = "2.0", TYPE = "DEV";
    private static final String MOD_ID = NAME.toLowerCase();
    public static File getDirectory() {
        return new File(MinecraftClient.getInstance().runDirectory, "SpaceVisuals");
    }

    // Флаг для переключения между кастомным и ванильным меню
    public static boolean useVanillaMenu = false;

    private ModuleManager moduleManager;

    private ThemeManager themeManager;
    private MenuScreen menuScreen;
    private ScriptManager scriptManager;
    private ServerHandler serverHandler;
    private LastServerManager lastServerManager;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private DeepLearningManager deepLearningManager;
    private RotationManager rotationManager;
    private AutoBuyManager autoBuyManager;

    private NotifyManager notifyManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RCTRepository rctRepository;
    private DiscordManager discordManager;

    @CompileToNative
    @VMProtect(type = CompileType.ULTRA)
    public void init() {
        System.out.println("[ZENITH] init() started");
        try {
            // Создаём папку space и configs при первом запуске
            getDirectory().mkdirs();
            new java.io.File(getDirectory(), "configs").mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> Zenith.getInstance().shutdown()));

            // HWID-проверка: мод должен запускаться только с авторизованного устройства
            checkHwid();

            System.out.println("[ZENITH] step 1: FriendManager");
            friendManager = new FriendManager();
            System.out.println("[ZENITH] step 2: StaffManager");
            staffManager = new StaffManager();
            System.out.println("[ZENITH] step 3: NotifyManager");
            notifyManager = new NotifyManager();
            System.out.println("[ZENITH] step 3.5: DiscordManager");
            discordManager = new DiscordManager();
            discordManager.connect();
            System.out.println("[ZENITH] step 4: ServerHandler");
            serverHandler = new ServerHandler();
            System.out.println("[ZENITH] step 4.5: LastServerManager");
            lastServerManager = new LastServerManager();
            System.out.println("[ZENITH] step 5: RCTRepository");
            rctRepository = new RCTRepository();
            System.out.println("[ZENITH] step 6: ThemeManager");
            themeManager = new ThemeManager();
            System.out.println("[ZENITH] step 7: ModuleManager");
            moduleManager = new ModuleManager();
            System.out.println("[ZENITH] step 8: DeepLearningManager");
            deepLearningManager = new DeepLearningManager();
            System.out.println("[ZENITH] step 9: RotationManager");
            rotationManager = new RotationManager();
            System.out.println("[ZENITH] step 10: AutoBuyManager");
            autoBuyManager = new AutoBuyManager();
            System.out.println("[ZENITH] step 11: CommandManager");
            commandManager = new CommandManager();
            System.out.println("[ZENITH] step 12: ScriptManager");
            scriptManager = new ScriptManager();
            System.out.println("[ZENITH] step 13: MenuScreen");
            menuScreen = new MenuScreen();
            System.out.println("[ZENITH] step 14: ConfigManager");
            configManager = new ConfigManager();
            System.out.println("[ZENITH] step 15: menuScreen.initialize()");
            menuScreen.initialize();
            System.out.println("[ZENITH] step 16: ResourceManagerHelper");
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Zenith.id("after_shader_load");
                }
                @Override
                public void reload(ResourceManager manager) {
                    GlProgram.loadAndSetupPrograms();
                }
            });
            System.out.println("[ZENITH] step 17: DrawUtil.initializeShaders()");
            DrawUtil.initializeShaders();
            System.out.println("[ZENITH] init() COMPLETE!");
        } catch (Throwable t) {
            System.err.println("[ZENITH] CRASH in init(): " + t);
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public void shutdown() {
        friendManager.save();
        staffManager.save();
        configManager.save();
        if (lastServerManager != null) lastServerManager.save();
        if (discordManager != null) discordManager.shutdown();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static Zenith getInstance() {
        return INSTANCE;
    }

    public RCTRepository getRCTRepository() {
        return rctRepository;
    }

    /** Проверяет что мод запущен с авторизованного устройства */
    private static void checkHwid() {
        try {
            java.nio.file.Path hwidFile = java.nio.file.Path.of("C:\\Space Visuals\\hwid.txt");
            if (!java.nio.file.Files.exists(hwidFile)) return; // файла нет — пропускаем

            String savedHwid = java.nio.file.Files.readString(hwidFile).trim();
            if (savedHwid.isEmpty()) return;

            // Лоадер передаёт HWID через системное свойство -Dspace.hwid=...
            String jvmHwid = System.getProperty("space.hwid", "").trim();
            if (jvmHwid.isEmpty()) return; // свойство не передано — старый лоадер, пропускаем

            if (!jvmHwid.equalsIgnoreCase(savedHwid)) {
                System.err.println("[ZENITH] HWID mismatch. Unauthorized copy detected.");
                Runtime.getRuntime().halt(1);
            }
        } catch (Exception ignored) {}
    }

}
