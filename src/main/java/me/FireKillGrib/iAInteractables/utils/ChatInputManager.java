package me.FireKillGrib.iAInteractables.utils;

import me.FireKillGrib.iAInteractables.Plugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {
    private static final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

    public static void requestInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(ChatUtil.color(prompt));
        player.sendMessage(ChatUtil.color("&7Type '&ccancel&7' to cancel."));
        pendingInputs.put(player.getUniqueId(), callback);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        if (pendingInputs.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
            Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
            
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatUtil.color("&cInput cancelled."));
                return;
            }
            
            Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> callback.accept(msg));
        }
    }
}