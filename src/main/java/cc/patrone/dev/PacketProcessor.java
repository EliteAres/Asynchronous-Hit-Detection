package cc.patrone.dev;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketProcessor {
    private static final String handlerName = "transactions";
    private final Map<String, Long> delay = new HashMap<>();
    public final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public PacketProcessor() {

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
                runAsync(player, (Player) entity, status);
                event.setCancelled(true);
            }
        }

    }

    public void damageEntity(EntityPlayer player, net.minecraft.server.v1_7_R4.Entity entity) {
        boolean disablePlayerCrits = false;
        float f = (float) player.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).getValue();
        byte b0 = 0;
        float f1 = 0.0F;

        if (entity instanceof EntityLiving) {
            f1 = EnchantmentManager.a(player.bA(), ((EntityLiving) entity).getMonsterType());
        } else {
            f1 = EnchantmentManager.a(player.bA(), EnumMonsterType.UNDEFINED);
        }

        int i = b0 + EnchantmentManager.a((EntityLiving) player);

        if (player.isSprinting()) {
            ++i;
        }

        if (f > 0.0F || f1 > 0.0F) {
            boolean flag = !disablePlayerCrits && player.fallDistance > 0.0F && !player.onGround && !player.k_() && !player.V() && !player.hasEffect(MobEffectList.BLINDNESS) && player.vehicle == null && entity instanceof EntityLiving; // PaperSpigot

            if (flag && f > 0.0F) {
                f *= 1.5F;
            }

            f += f1;
            boolean flag1 = false;
            int j = EnchantmentManager.getFireAspectEnchantmentLevel(player);

            if (entity instanceof EntityLiving && j > 0 && !entity.isBurning()) {
                // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
                EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(player.getBukkitEntity(), entity.getBukkitEntity(), 1);
                org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

                if (!combustEvent.isCancelled()) {
                    flag1 = true;
                    entity.setOnFire(combustEvent.getDuration());
                }
                // CraftBukkit end
            }

            double d0 = entity.motX;
            double d1 = entity.motY;
            double d2 = entity.motZ;
            entity.damageEntity(DamageSource.playerAttack(player), f);
        }
    }


    public void runAsync(Player player, Player damaged, WrappedPacketOutEntityStatus status) {
        Runnable runnable = new Runnable() {
            public void run() {
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    CraftPlayer cp = (CraftPlayer) p;

                    EntityPlayer ep = ((CraftPlayer) damaged).getHandle();
                    EntityPlayer epa = ((CraftPlayer) player).getHandle();

                    damageEntity(epa, ep);

                    double victimMotX = ep.motX;
                    double victimMotY = ep.motY;
                    double victimMotZ = ep.motZ;

                    ep.velocityChanged = true;

                    g(ep, -MathHelper.sin(epa.yaw * 3.1415927F / 180.0F) * 1 * 0.5F * 0.95D * 1, 0.1D * 0.7,
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
        };
        executorService.execute(runnable);
    }

    public void g(EntityPlayer e, double d0, double d1, double d2) {
        e.motX += d0;
        e.motY += d1;
        e.motZ += d2;
        e.setSprinting(true); //e.a1 = true
    }

    public void injectPlayer(Player player) {
        Channel channel = (Channel) NMSUtils.getChannel(player);
        channel.pipeline().addAfter("decoder", handlerName, new MessageToMessageDecoder() {
            public boolean processIncomingPacket(Channel channel, Object packet) {
                PacketPlayReceiveEvent event = new PacketPlayReceiveEvent(player, channel, new NMSPacket(packet));
                onPacketPlayReceive(event);
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

    public void ejectPlayer(Player player) {
        Channel channel = (Channel) NMSUtils.getChannel(player);
        if (channel.pipeline().get(handlerName) != null) {
            channel.pipeline().remove(handlerName);
        }
    }
}