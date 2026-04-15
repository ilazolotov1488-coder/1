package space.visuals.utility.mixin.minecraft.network;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.Zenith;

import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.utility.interfaces.IMinecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements IMinecraft {

    @Unique
    private static boolean stackOverflowFix;

    // Счётчик пакетов по типу за текущую секунду
    @Unique
    private static final Map<String, AtomicLong> packetCounts = new ConcurrentHashMap<>();
    @Unique
    private static long windowStart = System.currentTimeMillis();
    @Unique
    private static long totalPerSecond = 0;
    @Unique
    private static boolean stackTracePrinted = false;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        EventPacket event = new EventPacket(EventPacket.Action.RECEIVE, packet);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void triggerSendPacketEvent(Packet<?> packet, CallbackInfo ci) {

        if (stackOverflowFix) return;

        // Логируем пакет
        String packetName = packet.getClass().getSimpleName();
        packetCounts.computeIfAbsent(packetName, k -> new AtomicLong(0)).incrementAndGet();
        totalPerSecond++;

        // Мгновенный лог если за секунду уже >200 пакетов одного типа
        AtomicLong currentMoveCount = packetCounts.get("class_2813");
        if (currentMoveCount != null && currentMoveCount.get() == 200 && !stackTracePrinted) {
            stackTracePrinted = true;
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            StringBuilder trace = new StringBuilder("[PacketLog] INSTANT TRACE (200 move packets in <1s):\n");
            for (int i = 2; i < Math.min(stack.length, 20); i++) {
                trace.append("  ").append(stack[i]).append("\n");
            }
            System.out.println(trace);
        }

        // Каждую секунду выводим статистику если пакетов много
        long now = System.currentTimeMillis();
        if (now - windowStart >= 1000) {
            if (totalPerSecond > 20) {
                StringBuilder sb = new StringBuilder("[PacketLog] " + totalPerSecond + " pkt/s: ");
                packetCounts.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                        .limit(5)
                        .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue().get()).append(" "));
                System.out.println(sb);

                // Если class_2813 (PlayerMoveC2SPacket) спамит — печатаем стектрейс
                AtomicLong moveCount = packetCounts.get("class_2813");
                if (moveCount != null && moveCount.get() > 50 && !stackTracePrinted) {
                    stackTracePrinted = true;
                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    StringBuilder trace = new StringBuilder("[PacketLog] STACK TRACE for class_2813 spam:\n");
                    for (int i = 2; i < Math.min(stack.length, 20); i++) {
                        trace.append("  ").append(stack[i]).append("\n");
                    }
                    System.out.println(trace);
                }
            } else {
                stackTracePrinted = false;
            }
            packetCounts.clear();
            totalPerSecond = 0;
            windowStart = now;
        }

        EventPacket event = new EventPacket(EventPacket.Action.SENT, packet);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }

        Packet<?> newPacket = event.getPacket();
        if (newPacket != packet) {
            ci.cancel();

            stackOverflowFix = true;
            mc.getNetworkHandler().sendPacket(newPacket);
            stackOverflowFix = false;
        }
    }
}
