package me.FireKillGrib.iAInteractables.menu;

import me.FireKillGrib.iAInteractables.data.Furnace;
import me.FireKillGrib.iAInteractables.managers.FurnaceController;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FurnaceSettingsGUI {
    private final Furnace furnace;
    private final FurnaceController controller;

    public FurnaceSettingsGUI(Furnace furnace, FurnaceController controller) {
        this.furnace = furnace;
        this.controller = controller;
    }

    public void open(Player player) {
        String[] cleanStructure = furnace.getStructure().stream()
                .map(row -> row.replace(" ", ""))
                .toArray(String[]::new);
                
        Gui.Builder.Normal guiBuilder = Gui.normal().setStructure(cleanStructure);
        
        Set<Character> processedChars = new HashSet<>();
        Map<Character, Integer> structureMap = controller.getStructure();

        for (String row : cleanStructure) {
            for (char c : row.toCharArray()) {
                if (!processedChars.contains(c)) {
                    processedChars.add(c);
                    if (c == 'X' || c == 'Z' || c == 'R') {
                        guiBuilder.addIngredient(c, new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")));
                    } else if (c == 'P') {
                        guiBuilder.addIngredient('P', new ModeToggleItem());
                    } else {
                        Integer slotIdx = structureMap.get(c);
                        if (slotIdx != null) {
                            guiBuilder.addIngredient(c, new SlotToggleItem(slotIdx));
                        } else {
                            guiBuilder.addIngredient(c, new SimpleItem(new ItemBuilder(Material.AIR)));
                        }
                    }
                }
            }
        }

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Automation Settings")))
                .setGui(guiBuilder.build())
                .build(player)
                .open();
    }

    private class SlotToggleItem extends AbstractItem {
        private final int slotIndex;

        public SlotToggleItem(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        @SuppressWarnings("deprecation")
        @Override
        public ItemProvider getItemProvider() {
            if (controller.getBlockedSlots().contains(slotIndex)) {
                return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                        .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cSlot Blocked"))
                        .addLoreLines(ChatColor.translateAlternateColorCodes('&', "&7Click to allow hoppers"));
            } else {
                return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aSlot Open"))
                        .addLoreLines(ChatColor.translateAlternateColorCodes('&', "&7Click to block hoppers"));
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            controller.toggleBlockedSlot(slotIndex);
            notifyWindows();
        }
    }

    private class ModeToggleItem extends AbstractItem {
        @SuppressWarnings("deprecation")
        @Override
        public ItemProvider getItemProvider() {
            if (controller.isAutomated()) {
                return new ItemBuilder(Material.REDSTONE_TORCH)
                        .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cMode: Automated"))
                        .addLoreLines(
                                ChatColor.translateAlternateColorCodes('&', "&7Requires redstone pulse to cook."),
                                ChatColor.translateAlternateColorCodes('&', "&eClick to switch to Manual.")
                        );
            } else {
                return new ItemBuilder(Material.COAL)
                        .setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Mode: Manual"))
                        .addLoreLines(
                                ChatColor.translateAlternateColorCodes('&', "&7Cooks automatically when items are present."),
                                ChatColor.translateAlternateColorCodes('&', "&eClick to switch to Automated.")
                        );
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            controller.setAutomated(!controller.isAutomated());
            notifyWindows();
        }
    }
}