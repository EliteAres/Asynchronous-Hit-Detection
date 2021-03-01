package cc.patrone.dev;

import cc.patrone.dev.util.KnockbackUtil;
import cc.patrone.dev.util.v_1_7_10.KnockbackUtil_7;
import cc.patrone.dev.util.v_1_8_8.KnockbackUtil_8;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketProcessor {
    public static final String handlerName = "transactions";

    public static PacketProcessor INSTANCE;
    private final Map<String, Long> delay = new HashMap<>();
    public final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private KnockbackUtil knockbackUtil;

    public PacketProcessor() {
        INSTANCE = this;
    }

    public void load() {
        knockbackUtil = PacketEvents.get().getServerUtils().getVersion().isNewerThan(ServerVersion.v_1_7_10) ? new KnockbackUtil_8() : new KnockbackUtil_7();
    }

    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.USE_ENTITY) {
            Player player = event.getPlayer();
            WrappedPacketInUseEntity useEntity = new WrappedPacketInUseEntity(event.getNMSPacket());
            Entity entity = useEntity.getEntity();
            WrappedPacketInUseEntity.EntityUseAction action = useEntity.getAction();
            if (action == WrappedPacketInUseEntity.EntityUseAction.ATTACK
                    && entity.getType() == EntityType.PLAYER) {
                if (delay.containsKey(player.getName())) {
                    long s = ((System.currentTimeMillis() - delay.get(player.getName())) / 1000) * 20;
                    System.out.println(s);
                    if (s < 5) {
                        event.setCancelled(true);
                        return;
                    }
                }

                WrappedPacketOutEntityStatus status = new WrappedPacketOutEntityStatus(entity, (byte) 2);
                delay.put(player.getName(), System.currentTimeMillis());
                applyKnockback(player, (Player) entity, status);
                event.setCancelled(true);
            }
        }

    }

    public void damageEntity(Player player, Entity target) {
        knockbackUtil.damageEntity(player, target);
    }


    public void applyKnockback(Player player, Player damaged, WrappedPacketOutEntityStatus status) {
        executorService.execute(() -> knockbackUtil.applyKnockback(player, damaged, status));
    }

    public void g(Player player, double d0, double d1, double d2) {
        knockbackUtil.g(player, d0, d1, d2);
    }

    public void injectPlayer(Player player) {
        knockbackUtil.injectPlayer(player);
    }

    public void ejectPlayer(Player player) {
        knockbackUtil.ejectPlayer(player);
    }
}