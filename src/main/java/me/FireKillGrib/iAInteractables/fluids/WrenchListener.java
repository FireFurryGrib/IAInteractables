package me.FireKillGrib.iAInteractables.fluids;

import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.Furnace;
import me.FireKillGrib.iAInteractables.fluids.gui.MachineSideGUI;
import me.FireKillGrib.iAInteractables.managers.FurnaceController;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WrenchListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWrenchFurniture(FurnitureInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!WrenchUtil.isAnyWrench(item)) return;
        
        Location blockLoc = event.getBukkitEntity().getLocation().getBlock().getLocation();
        boolean isAdmin = WrenchUtil.isAdminWrench(item);

        // Сначала проверяем многоблок
        MultiblockInstance mb = Plugin.getInstance().getMultiblockManager().getStructureAt(blockLoc);
        if (mb != null) {
            event.setCancelled(true);
            new MachineSideGUI(mb.getPortNode(blockLoc), isAdmin).open(player);
            return;
        }

        // ВАЖНОЕ ИСПРАВЛЕНИЕ: Мы инициализируем печку (getOrCreate), если она еще не загружена в память!
        String name = event.getNamespacedID().split(":")[1];
        Furnace furnace = Plugin.getInstance().getRecipeManager().getFurnace(name);
        if (furnace != null) {
            FurnaceController fc = Plugin.getInstance().getFurnaceManager().getOrCreate(furnace, blockLoc);
            event.setCancelled(true);
            new MachineSideGUI(fc, isAdmin).open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWrenchBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!WrenchUtil.isAnyWrench(item)) return;
        
        Location loc = event.getClickedBlock().getLocation();
        boolean isAdmin = WrenchUtil.isAdminWrench(item);
        
        MultiblockInstance mb = Plugin.getInstance().getMultiblockManager().getStructureAt(loc);
        if (mb != null) {
            event.setCancelled(true);
            new MachineSideGUI(mb.getPortNode(loc), isAdmin).open(event.getPlayer());
            return;
        }

        FurnaceController fc = Plugin.getInstance().getFurnaceManager().get(loc);
        if (fc != null) {
            event.setCancelled(true);
            new MachineSideGUI(fc, isAdmin).open(event.getPlayer());
        }
    }
}