package space.visuals.base.waypoints;

import net.minecraft.util.math.Vec3d;
import space.visuals.utility.game.other.MessageUtil;

import java.util.*;

/**
 * Менеджер для управления метками (waypoints)
 */
public class WaypointManager {
    private final Map<String, Waypoint> waypoints = new HashMap<>();

    public void add(String name, double x, double y, double z) {
        add(name, x, y, z, false, null);
    }

    public void add(String name, double x, double y, double z, boolean temporary, UUID playerUUID) {
        if (waypoints.containsKey(name)) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Метка с таким именем уже существует!");
            return;
        }

        Vec3d pos = new Vec3d(x, y, z);
        waypoints.put(name, new Waypoint(name, pos, temporary, System.currentTimeMillis(), playerUUID));
        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Метка §f" + name + " §7добавлена: §f" +
            String.format("%.0f %.0f %.0f", x, y, z));
    }

    public void remove(String name) {
        if (waypoints.remove(name) != null) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Метка §f" + name + " §7удалена");
        } else {
            MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Метка §f" + name + " §7не найдена");
        }
    }

    public void clear() {
        waypoints.clear();
        MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Все метки удалены");
    }

    public Collection<Waypoint> getWaypoints() {
        return waypoints.values();
    }

    public boolean contains(String name) {
        return waypoints.containsKey(name);
    }

    public static class Waypoint {
        public String name;
        public Vec3d pos;
        public boolean temporary;
        public long creationTime;
        public UUID playerUUID;

        public Waypoint(String name, Vec3d pos, boolean temporary, long creationTime, UUID playerUUID) {
            this.name = name;
            this.pos = pos;
            this.temporary = temporary;
            this.creationTime = creationTime;
            this.playerUUID = playerUUID;
        }
    }
}
