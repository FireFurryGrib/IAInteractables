package me.FireKillGrib.iAInteractables.fluids.gui;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class MachineSideGUI {
    private final NetworkNode node; 
    private final boolean isAdmin;

    public MachineSideGUI(NetworkNode node, boolean isAdmin) {
        this.node = node;
        this.isAdmin = isAdmin;
    }

    public void open(Player player) {
        @SuppressWarnings("deprecation")
        Gui gui = Gui.normal()
            .setStructure(
                "X X U X X X X X X",
                "X W F E X N X S X",
                "X X D X X X X X X"
            )
            .addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('U', new SideToggleItem(BlockFace.UP))
            .addIngredient('D', new SideToggleItem(BlockFace.DOWN))
            .addIngredient('N', new SideToggleItem(BlockFace.NORTH))
            .addIngredient('S', new SideToggleItem(BlockFace.SOUTH))
            .addIngredient('E', new SideToggleItem(BlockFace.EAST))
            .addIngredient('W', new SideToggleItem(BlockFace.WEST))
            .addIngredient('F', new SimpleItem(new ItemBuilder(Material.FURNACE).setDisplayName(ChatColor.YELLOW + "Front / Core")))
            .build();

        Window.single()
            .setTitle(new AdventureComponentWrapper(ChatUtil.color(isAdmin ? "&c[Admin] Lock Sides" : "&eMachine Configuration")))
            .setGui(gui)
            .build(player)
            .open();
    }

    private class SideToggleItem extends AbstractItem {
        private final BlockFace worldFace;

        public SideToggleItem(BlockFace worldFace) {
            this.worldFace = worldFace;
        }

        @SuppressWarnings("deprecation")
        @Override
        public ItemProvider getItemProvider() {
            IOState adminLock = node.getAdminLock(worldFace);

            if (isAdmin) {
                if (adminLock == null) {
                    return new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aUnlocked: &7" + worldFace.name()));
                } else if (adminLock == IOState.NONE) {
                    return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cGlobal Lock: &7NONE (" + worldFace.name() + ")"));
                } else if (adminLock == IOState.INPUT) {
                    return new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cGlobal Lock: &bINPUT (" + worldFace.name() + ")"));
                } else {
                    return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cGlobal Lock: &6OUTPUT (" + worldFace.name() + ")"));
                }
            } else {
                if (adminLock != null) {
                    Material mat = adminLock == IOState.INPUT ? Material.BLUE_STAINED_GLASS_PANE : 
                                  (adminLock == IOState.OUTPUT ? Material.ORANGE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
                    return new ItemBuilder(mat)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8[Locked] &7" + worldFace.name() + ": " + adminLock.name()));
                }

                IOState state = node.getSideState(worldFace);
                if (state == IOState.INPUT) {
                    return new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bInput: &7" + worldFace.name()));
                } else if (state == IOState.OUTPUT) {
                    return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Output: &7" + worldFace.name()));
                } else {
                    return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                            .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7None: &7" + worldFace.name()));
                }
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (isAdmin) {
                IOState locked = node.getAdminLock(worldFace);
                if (locked == null) node.setAdminLock(worldFace, IOState.NONE);
                else if (locked == IOState.NONE) node.setAdminLock(worldFace, IOState.INPUT);
                else if (locked == IOState.INPUT) node.setAdminLock(worldFace, IOState.OUTPUT);
                else node.setAdminLock(worldFace, null);
            } else {
                if (node.getAdminLock(worldFace) != null) {
                    player.sendMessage(ChatColor.RED + "Эта сторона жестко заблокирована администратором!");
                    return;
                }
                node.setSideState(worldFace, node.getSideState(worldFace).next());
            }
            
            // ВАЖНО: При изменении порта обновляем визуальное соединение труб вокруг механизма!
            if (Plugin.getInstance().getPipeManager() != null) {
                Plugin.getInstance().getPipeManager().updateAdjacentPipes(node.getLocation());
            }
            
            notifyWindows();
        }
    }
}