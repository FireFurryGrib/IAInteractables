package me.FireKillGrib.iAInteractables.menu.recipebook;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.*;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;
import java.util.*;
import me.FireKillGrib.iAInteractables.integration.RecipeContainer;

public class StationListGUI {
    public void open(Player player) {
        List<String> hidden = Plugin.getInstance().getConfig().getStringList("recipe-book.hidden-stations");
        List<xyz.xenondevs.invui.item.Item> items = new ArrayList<>();
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

        // Custom Workbenches
        for (Workbench wb : Plugin.getInstance().getRecipeManager().getWorkbenches()) {
            if (hidden.contains(wb.getName())) continue;
            ItemStack iconStack = getIcon(wb.getNamespacedID(), Material.CRAFTING_TABLE);
            ItemBuilder iconBuilder = new ItemBuilder(iconStack)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&e" + wb.getTitle())))
                    .addLoreLines(
                        serializer.serialize(ChatUtil.color("&7Click to see recipies")),
                        serializer.serialize(ChatUtil.color("&7Type: workbench"))
                    );
            items.add(new SimpleItem(iconBuilder, click -> new RecipeListGUI(wb).open(player)));
        }

        // Custom Furnaces
        for (Furnace fn : Plugin.getInstance().getRecipeManager().getFurnaces()) {
            if (hidden.contains(fn.getName())) continue;
            ItemStack iconStack = getIcon(fn.getNamespacedID(), Material.FURNACE);
            ItemBuilder iconBuilder = new ItemBuilder(iconStack)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&c" + fn.getTitle())))
                    .addLoreLines(
                        serializer.serialize(ChatUtil.color("&7Click to see recipies")),
                        serializer.serialize(ChatUtil.color("&7Type: furnace"))
                    );
            items.add(new SimpleItem(iconBuilder, click -> new RecipeListGUI(fn).open(player)));
        }

        // Custom Smithing Tables
        for (SmithingTable st : Plugin.getInstance().getRecipeManager().getSmithingTables()) {
            if (hidden.contains(st.getName())) continue;
            ItemStack iconStack = getIcon(st.getNamespacedID(), Material.SMITHING_TABLE);
            ItemBuilder iconBuilder = new ItemBuilder(iconStack)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&e" + st.getTitle())))
                    .addLoreLines(
                            serializer.serialize(ChatUtil.color("&7Click to see recipies")),
                            serializer.serialize(ChatUtil.color("&7Type: smithing table"))
                    );
            items.add(new SimpleItem(iconBuilder, click -> new RecipeListGUI(st).open(player)));
        }

        // Vanilla Workbench
        Set<WorkbenchRecipe> filteredWb = new HashSet<>();
        for (WorkbenchRecipe r : Plugin.getInstance().getVanillaRecipeManager().getWorkbenchRecipes()) {
            if (CustomStack.byItemStack(r.getResult()) == null) filteredWb.add(r);
        }
        if (!filteredWb.isEmpty() && !hidden.contains("vanilla_workbench")) {
            Workbench dummyWb = new Workbench("v_wb", "&eVanilla Workbench", null, 
                Arrays.asList("X X X X X X X X X", "X A B C X X R X X", "X D E F X X X X X", "X G H I X X Z X X", "X X X X X X X X X"), 
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "), filteredWb, null, hidden);
            
            items.add(new SimpleItem(new ItemBuilder(Material.CRAFTING_TABLE)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&eVanilla Workbench")))
                    .addLoreLines(serializer.serialize(ChatUtil.color("&7Click to see recipies")), serializer.serialize(ChatUtil.color("&7Type: vanilla"))),
                    click -> new RecipeListGUI(dummyWb).open(player)));
        }

        // Vanilla Furnace
        Set<FurnaceRecipe> filteredFn = new HashSet<>();
        for (FurnaceRecipe r : Plugin.getInstance().getVanillaRecipeManager().getFurnaceRecipes()) {
            if (CustomStack.byItemStack(r.getResult()) == null) filteredFn.add(r);
        }
        if (!filteredFn.isEmpty() && !hidden.contains("vanilla_furnace")) {
            Furnace dummyFn = new Furnace("v_fn", "&cVanilla Furnace", null, 
                Arrays.asList("X X X X X X X X X", "X X I X P X R X X", "X X X X X X X X X", "X X U X X X Z X X", "X X X X X X X X X"), 
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "), filteredFn, null, ProgressBarConfig.getDefault(), hidden, null);
            
            items.add(new SimpleItem(new ItemBuilder(Material.FURNACE)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&cVanilla Furnace")))
                    .addLoreLines(serializer.serialize(ChatUtil.color("&7Click to see recipies")), serializer.serialize(ChatUtil.color("&7Type: vanilla"))),
                    click -> new RecipeListGUI(dummyFn).open(player)));
        }

        // Vanilla Smithing
        Set<SmithingRecipe> filteredSt = new HashSet<>();
        for (SmithingRecipe r : Plugin.getInstance().getVanillaRecipeManager().getSmithingRecipes()) {
            if (CustomStack.byItemStack(r.getResult()) == null) filteredSt.add(r);
        }
        if (!filteredSt.isEmpty() && !hidden.contains("vanilla_smithing")) {
            SmithingTable dummySt = new SmithingTable("v_st", "&8Vanilla Smithing", null, 
                Arrays.asList("X X X X X X X X X", "X T B A X X R X X", "X X X X X X Z X X"), 
                new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "), filteredSt, null, hidden);
            
            items.add(new SimpleItem(new ItemBuilder(Material.SMITHING_TABLE)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&8Vanilla Smithing Table")))
                    .addLoreLines(serializer.serialize(ChatUtil.color("&7Click to see recipies")), serializer.serialize(ChatUtil.color("&7Type: vanilla"))),
                    click -> new RecipeListGUI(dummySt).open(player)));
        }

        // External Integrations
        Map<String, List<RecipeContainer>> externalRecipes = Plugin.getInstance().getIntegrationManager().getExternalRecipes();
        for (Map.Entry<String, List<RecipeContainer>> entry : externalRecipes.entrySet()) {
            String namespace = entry.getKey();
            List<RecipeContainer> recipes = entry.getValue();
            if (recipes.isEmpty()) continue;
            String displayName = Plugin.getInstance().getIntegrationManager().getDisplayName(namespace);
            ItemBuilder iconBuilder = new ItemBuilder(Material.ENDER_CHEST)
                    .setDisplayName(serializer.serialize(ChatUtil.color(displayName)))
                    .addLoreLines(
                        serializer.serialize(ChatUtil.color("&7Click to see recipies")),
                        serializer.serialize(ChatUtil.color("&7Total recipies: &e" + recipes.size())),
                        serializer.serialize(ChatUtil.color("&7Plugin: &f" + namespace))
                    );
            items.add(new SimpleItem(iconBuilder, click -> new ExternalRecipeListGUI(namespace, displayName, recipes).open(player)));
        }

        Gui gui = PagedGui.items()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # < # C # > # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new SimpleItem(new ItemBuilder(Material.ARROW)
                        .setDisplayName(serializer.serialize(ChatUtil.color("&eBack")))))
                .addIngredient('>', new SimpleItem(new ItemBuilder(Material.ARROW)
                        .setDisplayName(serializer.serialize(ChatUtil.color("&eForward")))))
                .addIngredient('C', new SimpleItem(new ItemBuilder(Material.BARRIER)
                        .setDisplayName(serializer.serialize(ChatUtil.color("&cClose"))), 
                        click -> click.getPlayer().closeInventory()))
                .setContent(items)
                .build();
                
        Window.single()
                .setViewer(player)
                .setTitle(new AdventureComponentWrapper(ChatUtil.color(Plugin.getInstance().getConfig().getString("recipe-book.gui-title"))))
                .setGui(gui)
                .build()
                .open();
    }

    private ItemStack getIcon(String namespacedId, Material fallback) {
        if (namespacedId != null && CustomStack.getInstance(namespacedId) != null) {
            return CustomStack.getInstance(namespacedId).getItemStack();
        }
        return new ItemStack(fallback);
    }
}