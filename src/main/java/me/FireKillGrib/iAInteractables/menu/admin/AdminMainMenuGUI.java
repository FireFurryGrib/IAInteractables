package me.FireKillGrib.iAInteractables.menu.admin;

import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class AdminMainMenuGUI {
    public void open(Player player) {
        Gui gui = Gui.normal()
                .setStructure(
                        "X X X X X X X X X",
                        "X X W X F X S X X",
                        "X X X X V X X X X",
                        "X X X X X X X X X"
                )
                .addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
                .addIngredient('W', new SimpleItem(new ItemBuilder(Material.CRAFTING_TABLE)
                        .setDisplayName("Custom Workbenches"), click -> new AdminStationSelectGUI("workbenches").open(player)))
                .addIngredient('F', new SimpleItem(new ItemBuilder(Material.FURNACE)
                        .setDisplayName("Custom Furnaces"), click -> new AdminStationSelectGUI("furnaces").open(player)))
                .addIngredient('S', new SimpleItem(new ItemBuilder(Material.SMITHING_TABLE)
                        .setDisplayName("Custom Smithing Tables"), click -> new AdminStationSelectGUI("smithing_tables").open(player)))
                .addIngredient('V', new SimpleItem(new ItemBuilder(Material.KNOWLEDGE_BOOK)
                        .setDisplayName("Vanilla Stations"), click -> new AdminStationSelectGUI("vanilla").open(player)))
                .build();

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Editor - Main Menu")))
                .setGui(gui)
                .build(player)
                .open();
    }
}