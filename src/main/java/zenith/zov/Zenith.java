package zenith.zov;

import by.saskkeee.annotations.CompileToNative;
import by.saskkeee.annotations.Entrypoint;
import by.saskkeee.annotations.vmprotect.CompileType;
import by.saskkeee.annotations.vmprotect.VMProtect;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import zenith.zov.base.autobuy.AutoBuyManager;


import zenith.zov.base.comand.CommandManager;
import zenith.zov.base.config.ConfigManager;
import zenith.zov.base.filemanager.impl.FriendManager;
import zenith.zov.base.filemanager.impl.StaffManager;
import zenith.zov.base.modules.ModuleManager;
import zenith.zov.base.request.ScriptManager;
import zenith.zov.base.rotation.RotationManager;
import zenith.zov.base.rotation.deeplearnig.DeepLearningManager;
import zenith.zov.base.theme.ThemeManager;
import zenith.zov.client.screens.autobuy.items.AutoInventoryBuyScreen;
import zenith.zov.client.screens.menu.MenuScreen;
import zenith.zov.utility.game.server.ServerHandler;
import zenith.zov.base.notify.NotifyManager;
import zenith.zov.base.repository.RCTRepository;
import zenith.zov.utility.render.display.shader.DrawUtil;
import zenith.zov.utility.render.display.shader.GlProgram;

import java.io.File;

/*
    эта паста рвет во всю убивает нищету убивает и деееельта юзераааа бож че ты несешь какая дельта ты че совссем ебанулся???
    эта хуйня не вывезет даже мой пениииис йоу йоу йоу йоу
 */

@Getter
@Entrypoint
public enum Zenith {
    INSTANCE;

    public static final String NAME = "Zenith", VER = "2.0", TYPE = "DEV";
    private static final String MOD_ID = NAME.toLowerCase();
    public static final File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, Zenith.NAME);

    private ModuleManager moduleManager;

    private ThemeManager themeManager;
    private MenuScreen menuScreen;
    private ScriptManager scriptManager;
    private AutoInventoryBuyScreen autoInventoryBuyScreen;
    private ServerHandler serverHandler;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private DeepLearningManager deepLearningManager;
    private RotationManager rotationManager;
    private AutoBuyManager autoBuyManager;

    private NotifyManager notifyManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RCTRepository rctRepository;

    @CompileToNative
    @VMProtect(type = CompileType.ULTRA)
    public void init() {
        System.out.println("[ZENITH] init() started");
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> Zenith.getInstance().shutdown()));

            System.out.println("[ZENITH] step 1: FriendManager");
            friendManager = new FriendManager();
            System.out.println("[ZENITH] step 2: StaffManager");
            staffManager = new StaffManager();
            System.out.println("[ZENITH] step 3: NotifyManager");
            notifyManager = new NotifyManager();
            System.out.println("[ZENITH] step 4: ServerHandler");
            serverHandler = new ServerHandler();
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

}
