package me.FireKillGrib.iAInteractables.fluids.pipes;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import me.FireKillGrib.iAInteractables.Plugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PipeListener implements Listener {

    private final Map<UUID, Long> interactCooldowns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPipePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        
        long currentTime = System.currentTimeMillis();
        if (interactCooldowns.containsKey(player.getUniqueId()) && (currentTime - interactCooldowns.get(player.getUniqueId()) < 100)) {
            return;
        }
        interactCooldowns.put(player.getUniqueId(), currentTime);

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        PipeType placedType = null;
        for (PipeType type : Plugin.getInstance().getPipeManager().getPipeTypes()) {
            if (isMatch(item, type.getPlaceItem())) {
                placedType = type;
                break;
            }
        }

        if (placedType != null) {
            Block clickedBlock = event.getClickedBlock();
            
            if (clickedBlock.getType().isInteractable() && !player.isSneaking()) {
                return;
            }

            Block target = clickedBlock.getRelative(event.getBlockFace());
            
            if (Plugin.getInstance().getPipeManager().isPipe(target.getLocation())) {
                return;
            }

            if (target.getType() == Material.AIR || target.getType() == Material.WATER || target.getType() == Material.LAVA) {
                event.setCancelled(true);

                Plugin.getInstance().getPipeManager().placePipe(target.getLocation(), placedType);

                if (player.getGameMode() != GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPipeBreak(FurnitureBreakEvent event) {
        CustomFurniture cf = event.getFurniture();
        String fullId = cf.getNamespacedID();
        String shortId = cf.getId();
        
        Location loc = event.getBukkitEntity().getLocation().getBlock().getLocation();
        PipeManager manager = Plugin.getInstance().getPipeManager();
        PipeNode pipeNode = manager.getPipeAt(loc);
        
        if (pipeNode != null) {
            PipeType type = pipeNode.getPipeType();

            if (manager.isIAIdMatch(fullId, shortId, type.getCenterModel()) || 
                manager.isIAIdMatch(fullId, shortId, type.getArmModel()) ||
                manager.isIAIdMatch(fullId, shortId, type.getArmUpModel()) ||
                manager.isIAIdMatch(fullId, shortId, type.getArmDownModel())) {
                
                event.setCancelled(true); 
                manager.removePipe(loc);
                
                ItemStack drop;
                String placeItem = type.getPlaceItem();
                if (placeItem.startsWith("ia-") || placeItem.contains(":")) {
                    String targetId = placeItem.startsWith("ia-") ? placeItem.substring(3) : placeItem;
                    if (targetId.contains(":")) targetId = targetId.split(":")[1];
                    
                    CustomStack cs = CustomStack.getInstance(targetId);
                    drop = cs != null ? cs.getItemStack() : new ItemStack(Material.GLASS);
                } else {
                    try {
                        drop = new ItemStack(Material.matchMaterial(placeItem.toUpperCase()));
                    } catch (Exception e) {
                        drop = new ItemStack(Material.GLASS);
                    }
                }
                
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), drop);
            }
        }
    }

    private boolean isMatch(ItemStack item, String configString) {
        if (configString.startsWith("ia-") || configString.contains(":")) {
            CustomStack cs = CustomStack.byItemStack(item);
            if (cs == null) return false;
            String targetId = configString.startsWith("ia-") ? configString.substring(3) : configString;
            if (targetId.contains(":")) targetId = targetId.split(":")[1];
            return cs.getNamespacedID().equals(targetId) || cs.getId().equals(targetId);
        }
        return item.getType().name().equalsIgnoreCase(configString);
    }
}