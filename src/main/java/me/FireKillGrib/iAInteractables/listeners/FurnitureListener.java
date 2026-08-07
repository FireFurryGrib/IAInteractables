package me.FireKillGrib.iAInteractables.listeners;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.Furnace;
import me.FireKillGrib.iAInteractables.data.Workbench;
import me.FireKillGrib.iAInteractables.managers.FurnaceController;
import me.FireKillGrib.iAInteractables.menu.FurnaceGUI;
import me.FireKillGrib.iAInteractables.menu.WorkbenchGUI;
import me.FireKillGrib.iAInteractables.data.SmithingTable;
import me.FireKillGrib.iAInteractables.menu.SmithingGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;

public class FurnitureListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        Player player = event.getPlayer();
        if (me.FireKillGrib.iAInteractables.fluids.WrenchUtil.isAnyWrench(player.getInventory().getItemInMainHand())) {
            return;
        }
        String name = event.getNamespacedID().split(":")[1];
        Location entityLocation = event.getBukkitEntity().getLocation();
        Location blockLocation = entityLocation.getBlock().getLocation();
        
        // ВАЖНО: Если эта фурнитура - часть многоблочной структуры, уступаем приоритет MultiblockListener!
        if (Plugin.getInstance().getMultiblockManager().isPartOfActiveStructure(blockLocation)) {
            return;
        }
        
        Furnace furnace = Plugin.getInstance().getRecipeManager().getFurnace(name);
        if (furnace != null) {
            event.setCancelled(true);
            FurnaceController controller = Plugin.getInstance().getFurnaceManager()
                .getOrCreate(furnace, blockLocation);
            new FurnaceGUI(furnace, blockLocation, controller).open(player);
            return;
        }
        
        Workbench workbench = Plugin.getInstance().getRecipeManager().getWorkbench(name);
        if (workbench != null) {
            event.setCancelled(true);
            new WorkbenchGUI(workbench).open(player);
        }
        
        SmithingTable smithingTable = Plugin.getInstance().getRecipeManager().getSmithingTable(name);
        if (smithingTable != null) {
            event.setCancelled(true);
            new SmithingGUI(smithingTable).open(player);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        String name = event.getNamespacedID().split(":")[1];
        
        Location entityLocation = event.getBukkitEntity().getLocation();
        Location blockLocation = entityLocation.getBlock().getLocation();
        
        if (Plugin.getInstance().getMultiblockManager().isPartOfActiveStructure(blockLocation)) {
            return; // Разрушением займется MultiblockListener
        }

        Furnace furnace = Plugin.getInstance().getRecipeManager().getFurnace(name);
        if (furnace != null) {
            FurnaceController controller = Plugin.getInstance()
                .getFurnaceManager()
                .get(blockLocation);
            if (controller != null) {
                VirtualInventory inventory = controller.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        blockLocation.getWorld().dropItemNaturally(
                            blockLocation.clone().add(0.5, 0.5, 0.5), 
                            item
                        );
                    }
                }
                Plugin.getInstance().getFurnaceManager().remove(blockLocation);
            }
            Plugin.getInstance().getFurnaceDataManager().delete(blockLocation);
        }
    }
}