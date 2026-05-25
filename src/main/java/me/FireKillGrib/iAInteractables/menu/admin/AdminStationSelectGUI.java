package me.FireKillGrib.iAInteractables.menu.admin;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.Furnace;
import me.FireKillGrib.iAInteractables.data.SmithingTable;
import me.FireKillGrib.iAInteractables.data.Workbench;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class AdminStationSelectGUI {
    private final String category;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public AdminStationSelectGUI(String category) {
        this.category = category;
    }

    public void open(Player player) {
        List<Item> items = new ArrayList<>();

        if (category.equals("workbenches")) {
            for (Workbench wb : Plugin.getInstance().getRecipeManager().getWorkbenches()) {
                ItemStack icon = getIcon(wb.getNamespacedID(), Material.CRAFTING_TABLE);
                items.add(new SimpleItem(new ItemBuilder(icon).setDisplayName(serializer.serialize(ChatUtil.color("&e" + wb.getTitle()))), 
                        click -> new AdminStationMenuGUI(category, wb.getName(), wb.getStructure()).open(player)));
            }
        } else if (category.equals("furnaces")) {
            for (Furnace fn : Plugin.getInstance().getRecipeManager().getFurnaces()) {
                ItemStack icon = getIcon(fn.getNamespacedID(), Material.FURNACE);
                items.add(new SimpleItem(new ItemBuilder(icon).setDisplayName(serializer.serialize(ChatUtil.color("&c" + fn.getTitle()))), 
                        click -> new AdminStationMenuGUI(category, fn.getName(), fn.getStructure()).open(player)));
            }
        } else if (category.equals("smithing_tables")) {
            for (SmithingTable st : Plugin.getInstance().getRecipeManager().getSmithingTables()) {
                ItemStack icon = getIcon(st.getNamespacedID(), Material.SMITHING_TABLE);
                items.add(new SimpleItem(new ItemBuilder(icon).setDisplayName(serializer.serialize(ChatUtil.color("&8" + st.getTitle()))), 
                        click -> new AdminStationMenuGUI(category, st.getName(), st.getStructure()).open(player)));
            }
        } else if (category.equals("vanilla")) {
            items.add(new SimpleItem(new ItemBuilder(Material.CRAFTING_TABLE).setDisplayName(serializer.serialize(ChatUtil.color("&eVanilla Workbench"))), 
                    click -> new DynamicRecipeEditorGUI("vanilla_recipes", "workbench", null).open(player)));
            items.add(new SimpleItem(new ItemBuilder(Material.FURNACE).setDisplayName(serializer.serialize(ChatUtil.color("&cVanilla Furnace"))), 
                    click -> new DynamicRecipeEditorGUI("vanilla_recipes", "furnace", null).open(player)));
            items.add(new SimpleItem(new ItemBuilder(Material.SMITHING_TABLE).setDisplayName(serializer.serialize(ChatUtil.color("&8Vanilla Smithing Table"))), 
                    click -> new DynamicRecipeEditorGUI("vanilla_recipes", "smithing", null).open(player)));
        }

        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # < # B # > # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('B', new SimpleItem(new ItemBuilder(Material.BARRIER).setDisplayName(serializer.serialize(ChatUtil.color("&cBack"))), click -> new AdminMainMenuGUI().open(player)))
                .addIngredient('<', new SimpleItem(new ItemBuilder(Material.ARROW).setDisplayName(serializer.serialize(ChatUtil.color("&eBack")))))
                .addIngredient('>', new SimpleItem(new ItemBuilder(Material.ARROW).setDisplayName(serializer.serialize(ChatUtil.color("&eForward")))))
                .setContent(items);

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Select Station")))
                .setGui(builder.build())
                .build(player)
                .open();
    }

    private ItemStack getIcon(String namespacedId, Material fallback) {
        if (namespacedId != null && CustomStack.getInstance(namespacedId) != null) {
            return CustomStack.getInstance(namespacedId).getItemStack();
        }
        return new ItemStack(fallback);
    }
}