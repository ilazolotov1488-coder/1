package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;

import com.adl.nativeprotect.Native;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@ModuleAnnotation(
        name = "KTLeave",
        category = Category.COMBAT,
        description = "Открывает/закрывает ближайший люк по бинду. Синхронизируется со 2-м клиентом для KT-лива."
)
public final class KTLeave extends Module {
    public static final KTLeave INSTANCE = new KTLeave();

    private final ModeSetting mode = new ModeSetting("Режим", "Основной", "Дополнительный");
    private final KeySetting bind = new KeySetting("Кнопка лива");

    /** Максимальная дистанция от ног игрока до центра люка (чтобы не сработать на соседский люк). */
    private static final double TRAPDOOR_MAX_DIST = 1.2;
    private static final double PEARL_RADIUS = 30.0;

    /** Общий файл-сигнал между всеми клиентами на одной машине. */
    private static final File SIGNAL_FILE =
            new File(System.getProperty("java.io.tmpdir"), "zenith_ktleave.signal");

    /** Папка и имена файлов-хартбитов по клиентам (presence). */
    private static final File HB_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final String HB_PREFIX = "zenith_ktleave_hb_";
    private static final String HB_SUFFIX = ".tmp";
    private static final long HB_INTERVAL_MS = 500L;
    private static final long HB_TIMEOUT_MS  = 3000L;

    /** Уникальный id текущего клиента — чтобы не реагировать на свой же сигнал. */
    private final String sessionId = UUID.randomUUID().toString();
    private final File ownHbFile = new File(HB_DIR, HB_PREFIX + sessionId + HB_SUFFIX);

    private long lastSeenTs = 0L;
    private long lastHbWrite = 0L;
    private final Set<String> connectedPeers = new HashSet<>();

    private KTLeave() {}

    @Native
    @Override
    public void onEnable() {
        super.onEnable();
        // Стартовая отметка — игнорировать сигналы, оставшиеся от предыдущих запусков
        lastSeenTs = SIGNAL_FILE.exists() ? SIGNAL_FILE.lastModified() : 0L;
        connectedPeers.clear();
        touchHeartbeat();
    }

    @Native
    @Override
    public void onDisable() {
        super.onDisable();
        // Убираем свой heartbeat — второй клиент увидит "Разорвано".
        try { if (ownHbFile.exists()) ownHbFile.delete(); } catch (Exception ignored) {}
        connectedPeers.clear();
    }

    @Native
    @EventTarget
    public void onKey(EventKey e) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (bind.getKeyCode() <= 0) return;
        if (!e.isKeyDown(bind.getKeyCode())) return;

        boolean isMain = mode.is("Основной");

        // На Основном люк НЕ трогаем локально (даже если рядом с люком).
        // Только Дополнительный (твинк) открывает свой люк.
        if (!isMain) {
            triggerLocal();
        }
        // Оба режима шлют сигнал второму клиенту
        writeSignal();
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        if (now - lastHbWrite >= HB_INTERVAL_MS) {
            touchHeartbeat();
        }
        scanPeers();
        pollSignal();
    }

    /** Обновляет время жизни собственного heartbeat-файла. */
    @Native
    private void touchHeartbeat() {
        try {
            if (!ownHbFile.exists()) ownHbFile.createNewFile();
            ownHbFile.setLastModified(System.currentTimeMillis());
            lastHbWrite = System.currentTimeMillis();
        } catch (Exception ignored) {}
    }

    /** Сканирует хартбиты других клиентов и сообщает о подключении/отключении. */
    @Native
    private void scanPeers() {
        long now = System.currentTimeMillis();
        FilenameFilter filter = (d, name) -> name.startsWith(HB_PREFIX) && name.endsWith(HB_SUFFIX);
        File[] files = HB_DIR.listFiles(filter);
        Set<String> alive = new HashSet<>();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                String id = name.substring(HB_PREFIX.length(), name.length() - HB_SUFFIX.length());
                if (id.equals(sessionId)) continue;
                if (now - f.lastModified() <= HB_TIMEOUT_MS) {
                    alive.add(id);
                }
            }
        }
        // Новые подключения
        for (String id : alive) {
            if (connectedPeers.add(id)) {
                sendChatConnected();
            }
        }
        // Утерянные подключения
        Iterator<String> it = connectedPeers.iterator();
        while (it.hasNext()) {
            String id = it.next();
            if (!alive.contains(id)) {
                it.remove();
                sendChatDisconnected();
            }
        }
    }

    /** Локальное действие: открыть/закрыть люк, на который наведён прицел (только если перка рядом). */
    @Native
    private void triggerLocal() {
        if (!isPearlNearby(PEARL_RADIUS)) {
            sendChatNoPearl();
            return;
        }
        BlockPos pos = findTargetedTrapdoor();
        if (pos == null) {
            sendChatNoTrapdoor();
            return;
        }
        toggleTrapdoor(pos);
    }

    /** Проверяет есть ли эндер-перла в радиусе radius блоков от игрока. */
    @Native
    private boolean isPearlNearby(double radius) {
        if (mc.world == null || mc.player == null) return false;
        double r2 = radius * radius;
        Vec3d playerPos = mc.player.getPos();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity)) continue;
            if (entity.getPos().squaredDistanceTo(playerPos) <= r2) return true;
        }
        return false;
    }

    /** Читает файл-сигнал. Если там новый ts от другого клиента — срабатываем. */
    @Native
    private void pollSignal() {
        try {
            if (!SIGNAL_FILE.exists()) return;
            String content = Files.readString(SIGNAL_FILE.toPath()).trim();
            if (content.isEmpty()) return;

            int idx = content.indexOf(':');
            if (idx <= 0 || idx == content.length() - 1) return;

            long ts;
            try {
                ts = Long.parseLong(content.substring(0, idx));
            } catch (NumberFormatException nfe) {
                return;
            }
            String origin = content.substring(idx + 1);

            if (ts > lastSeenTs && !origin.equals(sessionId)) {
                lastSeenTs = ts;
                // На Основном люк не трогаем, на Дополнительном — открываем.
                if (!mode.is("Основной")) {
                    triggerLocal();
                }
                // Подтверждаем что связь между клиентами работает
                sendChatSuccess();
            }
        } catch (Exception ignored) {}
    }

    /** Пишет в общий файл-сигнал текущий timestamp и свой sessionId. */
    @Native
    private void writeSignal() {
        try {
            long ts = System.currentTimeMillis();
            lastSeenTs = ts; // не реагировать на свой же сигнал
            Files.writeString(SIGNAL_FILE.toPath(), ts + ":" + sessionId);
        } catch (Exception ignored) {}
    }

    /** Пишет в локальный чат (только видно игроку) подтверждение синхронизации. */
    @Native
    private void sendChatSuccess() {
        try {
            if (mc.inGameHud == null) return;
            String role = mode.is("Основной") ? "Основной" : "Дополнительный";
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[" + Formatting.LIGHT_PURPLE + "KTLeave" + Formatting.GRAY + "§8] "
                            + Formatting.GREEN + "Успешно §7(" + role + ")")
            );
        } catch (Exception ignored) {}
    }

    /** Другой клиент включил модуль. */
    @Native
    private void sendChatConnected() {
        try {
            if (mc.inGameHud == null) return;
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[" + Formatting.LIGHT_PURPLE + "KTLeave" + Formatting.GRAY + "§8] "
                            + Formatting.GREEN + "Успешно подключено")
            );
        } catch (Exception ignored) {}
    }

    /** Другой клиент выключил модуль или вышел. */
    @Native
    private void sendChatDisconnected() {
        try {
            if (mc.inGameHud == null) return;
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[" + Formatting.LIGHT_PURPLE + "KTLeave" + Formatting.GRAY + "§8] "
                            + Formatting.RED + "Разорвано")
            );
        } catch (Exception ignored) {}
    }

    /** Прицел не наведён на люк (или люк слишком далеко). */
    @Native
    private void sendChatNoTrapdoor() {
        try {
            if (mc.inGameHud == null) return;
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[" + Formatting.LIGHT_PURPLE + "KTLeave" + Formatting.GRAY + "§8] "
                            + Formatting.RED + "Наведись на люк")
            );
        } catch (Exception ignored) {}
    }

    /** Сообщает в чат что перка не найдена. */
    @Native
    private void sendChatNoPearl() {
        try {
            if (mc.inGameHud == null) return;
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("§8[" + Formatting.LIGHT_PURPLE + "KTLeave" + Formatting.GRAY + "§8] "
                            + Formatting.RED + "Перка не найдена §7(в радиусе " + (int) PEARL_RADIUS + " бл.)")
            );
        } catch (Exception ignored) {}
    }

    /**
     * Берёт блок, на который наведён прицел игрока. Если это люк и он в пределах
     * {@link #TRAPDOOR_MAX_DIST} блоков от ног — возвращает его позицию. Иначе null.
     */
    @Native
    private BlockPos findTargetedTrapdoor() {
        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr)) return null;
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof TrapdoorBlock)) return null;

        double maxDistSq = TRAPDOOR_MAX_DIST * TRAPDOOR_MAX_DIST;
        if (Vec3d.ofCenter(pos).squaredDistanceTo(mc.player.getPos()) > maxDistSq) return null;

        return pos;
    }

    @Native
    private void toggleTrapdoor(BlockPos pos) {
        if (mc.interactionManager == null) return;
        Vec3d hit = Vec3d.ofCenter(pos);
        BlockHitResult result = new BlockHitResult(hit, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
