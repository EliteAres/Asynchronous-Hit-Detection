package cc.patrone.dev.util;

import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface KnockbackUtil {

    void damageEntity(Player player, Entity target);

    void applyKnockback(Player player, Player target, WrappedPacketOutEntityStatus status);

    void g(Player player, double deltaMotX, double deltaMotY, double deltaMotZ);

    void injectPlayer(Player player);

    void ejectPlayer(Player player);
}
