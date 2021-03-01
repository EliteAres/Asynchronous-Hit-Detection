package cc.patrone.dev.util.v_1_8_8;

import cc.patrone.dev.PacketProcessor;
import cc.patrone.dev.util.KnockbackUtil;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import java.util.List;

public class KnockbackUtil_8 implements KnockbackUtil {
    @Override
    public void damageEntity(Player player, Entity target) {
        EntityPlayer ep = ((CraftPlayer)player).getHandle();
        net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity)target).getHandle();
        boolean disablePlayerCrits = false;
        float f = (float) ep.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).getValue();
        byte b0 = 0;
        float f1;

        if (nmsEntity instanceof EntityLiving) {
            f1 = EnchantmentManager.a(ep.bA(), ((EntityLiving) nmsEntity).getMonsterType());
        } else {
            f1 = EnchantmentManager.a(ep.bA(), EnumMonsterType.UNDEFINED);
        }

        int i = b0 + EnchantmentManager.a(ep);

        if (ep.isSprinting()) {
            ++i;
        }

        if (f > 0.0F || f1 > 0.0F) {
            boolean flag = !disablePlayerCrits && ep.fallDistance > 0.0F && !ep.onGround && !ep.k_() && !ep.V() && !ep.hasEffect(MobEffectList.BLINDNESS) && ep.vehicle == null && nmsEntity instanceof EntityLiving; // PaperSpigot

            if (flag && f > 0.0F) {
                f *= 1.5F;
            }

            f += f1;
            int j = EnchantmentManager.getFireAspectEnchantmentLevel(ep);

            if (nmsEntity instanceof EntityLiving && j > 0 && !nmsEntity.isBurning()) {
                // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
                EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(ep.getBukkitEntity(), nmsEntity.getBukkitEntity(), 1);
                org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

                if (!combustEvent.isCancelled()) {
                    nmsEntity.setOnFire(combustEvent.getDuration());
                }
                // CraftBukkit end
            }

            nmsEntity.damageEntity(DamageSource.playerAttack(ep), f);
        }
    }

    @Override
    public void applyKnockback(Player player, Player target, WrappedPacketOutEntityStatus status) {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            CraftPlayer cp = (CraftPlayer) p;

            EntityPlayer ep = ((CraftPlayer) target).getHandle();
            EntityPlayer epa = ((CraftPlayer) player).getHandle();

            damageEntity(player, target);

            double victimMotX = ep.motX;
            double victimMotY = ep.motY;
            double victimMotZ = ep.motZ;

            ep.velocityChanged = true;

            g(player, -MathHelper.sin(epa.yaw * 3.1415927F / 180.0F) * 1 * 0.5F * 0.95D * 1, 0.1D * 0.7,
                    MathHelper.cos(epa.yaw * 3.1415927F / 180.0F) * 1 * 0.5F * 0.95D * 1);
            PlayerVelocityEvent event = new PlayerVelocityEvent(ep.getBukkitEntity(), ep.getBukkitEntity().getVelocity());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                ep.getBukkitEntity().setVelocity(event.getVelocity());
            }

            ep.velocityChanged = false;
            ep.motX = victimMotX;
            ep.motY = victimMotY;
            ep.motZ = victimMotZ;

            PacketEvents.get().getPlayerUtils().sendPacket(p,
                    status);

        }
    }

    @Override
    public void g(Player player, double deltaMotX, double deltaMotY, double deltaMotZ) {
        EntityPlayer ep = ((CraftPlayer)player).getHandle();
        ep.motX += deltaMotX;
        ep.motY += deltaMotY;
        ep.motZ += deltaMotZ;
        ep.ai = true;
    }

    @Override
    public void injectPlayer(Player player) {
        Channel channel = (Channel) NMSUtils.getChannel(player);
        channel.pipeline().addAfter("decoder", PacketProcessor.handlerName, new MessageToMessageDecoder() {
            public boolean processIncomingPacket(Channel channel, Object packet) {
                PacketPlayReceiveEvent event = new PacketPlayReceiveEvent(player, channel, new NMSPacket(packet));
                PacketProcessor.INSTANCE.onPacketPlayReceive(event);
                return event.isCancelled();
            }

            @Override
            protected void decode(ChannelHandlerContext ctx, Object packet, List list) throws Exception {
                list.add(packet);

                if (!processIncomingPacket(ctx.channel(), packet)) {
                    list.remove(packet);
                    ctx.flush();
                }

            }
        });
    }

    @Override
    public void ejectPlayer(Player player) {
        Channel channel = (Channel) NMSUtils.getChannel(player);
        if (channel.pipeline().get(PacketProcessor.handlerName) != null) {
            channel.pipeline().remove(PacketProcessor.handlerName);
        }
    }
}
