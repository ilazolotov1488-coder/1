package space.visuals.client.modules.impl.misc;

import com.adl.nativeprotect.Native;
import space.visuals.Zenith;
import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

import java.util.List;

// ООО<<МИНЦЕТ ПАСТИНГ INC>>ООО
@Native(critical = true)
@ModuleAnnotation(name = "NameProtect", category = Category.MISC, description = "Защищает имена игроков")
public final class NameProtect extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    
    private NameProtect() {
    }

    private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public static String getCustomName() {
        Module module = NameProtect.INSTANCE;
        return module != null && module.isEnabled() ? "space" : mc.player.getNameForScoreboard();
    }

    public static String getCustomName(String originalName) {
        Module module = NameProtect.INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "space");
        }

        if (module instanceof NameProtect nameProtect && nameProtect.hideFriends.isEnabled()) {
            var friends = Zenith.getInstance().getFriendManager().getItems();
            for (String friend : friends) {
                if (originalName.contains(friend)) {
                    return originalName.replace(friend, "space");
                }
            }
        }

        return originalName;
    }
}
