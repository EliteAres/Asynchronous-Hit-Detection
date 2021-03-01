package cc.patrone.dev;


import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketLoginSendEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.login.out.success.WrappedPacketLoginOutSuccess;
import io.github.retrooper.packetevents.packetwrappers.play.in.chat.WrappedPacketInChat;
import io.github.retrooper.packetevents.packetwrappers.play.out.chat.WrappedPacketOutChat;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class Transactions extends JavaPlugin {
    public static Transactions INSTANCE;
    private PacketProcessor processor = new PacketProcessor();

    @Override
    public void onLoad() {
        INSTANCE = this;
        /*
         * PacketEvents likes to maintain compatibility with other plugins shading the same API.
         * If some other API loaded before you, the create method will return it's PacketEvents instance
         * to maintain compatibility, otherwise you will create the instance.
         * Set the settings.
         */
        PacketEvents.create(this).getSettings().checkForUpdates(false);
        /*
         * Access the stored instance of the PacketEvents class and load PacketEvents.
         * A few settings need to be specified before loading PacketEvents
         * as PacketEvents already uses a few in the load method.
         * To be safe, just set them all before loading.
         */
        PacketEvents.get().load();
    }

    @Override
    public void onEnable() {

        //Initiate PacketEvents
        PacketEvents.get().init(this);

    }

    @Override
    public void onDisable() {
        //Terminate PacketEvents
        processor.executorService.shutdown();
        PacketEvents.get().terminate();

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        processor.injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        processor.ejectPlayer(event.getPlayer());
    }


}
