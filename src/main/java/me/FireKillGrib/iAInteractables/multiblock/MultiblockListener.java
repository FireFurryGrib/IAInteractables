package me.FireKillGrib.iAInteractables.multiblock;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.utils.RotationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MultiblockListener implements Listener {
    private final MultiblockManager multiblockManager;

    public MultiblockListener(MultiblockManager manager) {
        this.multiblockManager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            Location core = block.getLocation();

            // 1. Если структура УЖЕ АКТИВНА
            MultiblockInstance instance = multiblockManager.getStructureAt(core);
            if (instance != null) {
                // ЗАЩИТА: Если игрок держит ключ, не открываем меню структуры, уступаем WrenchListener!
                if (me.FireKillGrib.iAInteractables.fluids.WrenchUtil.isAnyWrench(event.getPlayer().getInventory().getItemInMainHand())) {
                    return;
                }
                
                event.setCancelled(true);
                MultiblockGUI.openGUI(event.getPlayer(), instance);
                return;
            }

            // 2. Если НЕ АКТИВНА, пытаемся активировать
            ItemStack item = event.getItem();
            if (item != null && isActivator(item)) {
                int[] rotations = {0, 90, 180, 270};
                for (MultiblockTemplate template : multiblockManager.getAllTemplates()) {
                    for (int rot : rotations) {
                        if (StructureValidator.validate(template, core, rot)) {
                            MultiblockInstance newInstance = new MultiblockInstance(template.getName(), core, rot);
                            
                            for (org.bukkit.util.Vector v : template.getBlocks().keySet()) {
                                newInstance.addBlock(core.clone().add(RotationUtil.rotateVector(v, rot)));
                            }
                            for (org.bukkit.util.Vector v : template.getFurniture().keySet()) {
                                newInstance.addFurnitureLocation(core.clone().add(RotationUtil.rotateVector(v, rot)));
                            }

                            StructureValidator.snapFurniture(template, core, rot);
                            multiblockManager.registerStructure(newInstance);
                            
                            event.getPlayer().sendMessage("§aМногоблочная структура '" + template.getName() + "' успешно активирована!");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (multiblockManager.isPartOfActiveStructure(event.getBlock().getLocation())) {
            MultiblockInstance instance = multiblockManager.getStructureAt(event.getBlock().getLocation());
            multiblockManager.unregisterStructure(instance.getCoreLocation());
            event.getPlayer().sendMessage("§cМногоблочная структура расформирована, так как её блок был разрушен!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        // ЗАЩИТА: Если игрок держит ключ, уступаем WrenchListener
        if (me.FireKillGrib.iAInteractables.fluids.WrenchUtil.isAnyWrench(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }

        Location blockLoc = event.getBukkitEntity().getLocation().getBlock().getLocation();
        MultiblockInstance instance = multiblockManager.getStructureAt(blockLoc);
        if (instance != null) {
            event.setCancelled(true);
            MultiblockGUI.openGUI(event.getPlayer(), instance);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        Location blockLoc = event.getBukkitEntity().getLocation().getBlock().getLocation();
        MultiblockInstance instance = multiblockManager.getStructureAt(blockLoc);
        if (instance != null) {
            multiblockManager.unregisterStructure(instance.getCoreLocation());
            event.getPlayer().sendMessage("§cМногоблочная структура расформирована, так как её фурнитура была разрушена!");
        }
    }

    private boolean isActivator(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        String cfg = Plugin.getInstance().getConfig().getString("multiblock.activator-item", "GOLDEN_HOE");
        if (cfg.startsWith("ia-") || cfg.contains(":")) {
            CustomStack cs = CustomStack.byItemStack(item);
            if (cs == null) return false;
            String targetId = cfg.startsWith("ia-") ? cfg.substring(3) : cfg;
            return cs.getNamespacedID().equals(targetId) || cs.getId().equals(targetId);
        }
        return item.getType().name().equalsIgnoreCase(cfg);
    }
}