package me.FireKillGrib.iAInteractables.menu.admin;

import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

public class AdminStationMenuGUI {
    private final String category;
    private final String stationName;
    private final List<String> structure;

    public AdminStationMenuGUI(String category, String stationName, List<String> structure) {
        this.category = category;
        this.stationName = stationName;
        this.structure = structure;
    }

    public void open(Player player) {
        Gui gui = Gui.normal()
                .setStructure(
                        "X X X X X X X X X",
                        "X X R X X X E X X",
                        "X X X X X X X X X"
                )
                .addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
                .addIngredient('R', new SimpleItem(new ItemBuilder(Material.KNOWLEDGE_BOOK)
                        .setDisplayName("&aEdit Recipes"), click -> new DynamicRecipeEditorGUI(category, stationName, structure).open(player)))
                .addIngredient('E', new SimpleItem(new ItemBuilder(Material.BLAZE_POWDER)
                        .setDisplayName("&dEdit Effects & Sounds"), click -> new AdminEffectEditorGUI(category, stationName).open(player)))
                .build();

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8" + stationName + " - Menu")))
                .setGui(gui)
                .build(player)
                .open();
    }
}