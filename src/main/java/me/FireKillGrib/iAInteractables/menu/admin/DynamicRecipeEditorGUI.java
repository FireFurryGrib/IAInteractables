package me.FireKillGrib.iAInteractables.menu.admin;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DynamicRecipeEditorGUI {
    private final String category;
    private final String stationName;
    private List<String> structure;
    private final VirtualInventory gridInventory;
    private final VirtualInventory resultInventory;
    private final Map<Character, Integer> charToSlot = new HashMap<>();
    private int cookTime = 100;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public DynamicRecipeEditorGUI(String category, String stationName, List<String> structure) {
        this.category = category;
        this.stationName = stationName;
        this.structure = structure;
        this.gridInventory = new VirtualInventory(null, 54);
        this.resultInventory = new VirtualInventory(null, 1);
        
        if (this.structure == null && category.equals("vanilla_recipes")) {
            loadVanillaStructure();
        }
    }

    private void loadVanillaStructure() {
        if (stationName.equals("workbench")) {
            structure = Arrays.asList(
                    "X X X X X X X X X",
                    "X A B C X X R X X",
                    "X D E F X X X X X",
                    "X G H I X X Z X X",
                    "X X X X X X X X X"
            );
        } else if (stationName.equals("furnace")) {
            structure = Arrays.asList(
                    "X X X X X X X X X",
                    "X X I X P X R X X",
                    "X X X X X X X X X",
                    "X X U X X X Z X X",
                    "X X X X X X X X X"
            );
        } else if (stationName.equals("smithing")) {
            structure = Arrays.asList(
                    "X X X X X X X X X",
                    "X T B A X X R X X",
                    "X X X X X X Z X X"
            );
        }
    }

    public void open(Player player) {
        Gui.Builder.Normal guiBuilder = Gui.normal()
                .setStructure(structure.toArray(new String[0]));

        int inventoryIndex = 0;
        for (String row : structure) {
            String cleanRow = row.replace(" ", "");
            for (char c : cleanRow.toCharArray()) {
                if (c == 'X') {
                    guiBuilder.addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")));
                } else if (c == 'R') {
                    guiBuilder.addIngredient('R', new SlotElement.InventorySlotElement(resultInventory, 0));
                } else if (c == 'Z') {
                    guiBuilder.addIngredient('Z', new SimpleItem(new ItemBuilder(Material.LIME_DYE)
                            .setDisplayName(serializer.serialize(ChatUtil.color("&aSave Recipe"))), click -> saveRecipe(player)));
                } else if (c == 'P') {
                    guiBuilder.addIngredient('P', new CookTimeItem());
                } else {
                    if (!charToSlot.containsKey(c)) {
                        charToSlot.put(c, inventoryIndex);
                        guiBuilder.addIngredient(c, new SlotElement.InventorySlotElement(gridInventory, inventoryIndex));
                        inventoryIndex++;
                    }
                }
            }
        }

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Recipe Editor")))
                .setGui(guiBuilder.build())
                .addCloseHandler(() -> {
                    for (int i = 0; i < gridInventory.getSize(); i++) {
                        ItemStack item = gridInventory.getItem(i);
                        if (item != null && !item.getType().isAir()) {
                            player.getInventory().addItem(item).values().forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
                        }
                    }
                    ItemStack res = resultInventory.getItem(0);
                    if (res != null && !res.getType().isAir()) player.getInventory().addItem(res);
                })
                .build(player)
                .open();
    }

    private class CookTimeItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.CLOCK)
                    .setDisplayName(serializer.serialize(ChatUtil.color("&eCook Time: &f" + cookTime + " ticks")))
                    .addLoreLines(
                            serializer.serialize(ChatUtil.color("&7Left Click: &a+10")),
                            serializer.serialize(ChatUtil.color("&7Right Click: &c-10")),
                            serializer.serialize(ChatUtil.color("&7Shift Left: &a+100")),
                            serializer.serialize(ChatUtil.color("&7Shift Right: &c-100"))
                    );
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (clickType == ClickType.LEFT) cookTime += 10;
            if (clickType == ClickType.RIGHT) cookTime = Math.max(10, cookTime - 10);
            if (clickType == ClickType.SHIFT_LEFT) cookTime += 100;
            if (clickType == ClickType.SHIFT_RIGHT) cookTime = Math.max(10, cookTime - 100);
            notifyWindows();
        }
    }

    private void saveRecipe(Player player) {
        ItemStack result = resultInventory.getItem(0);
        if (result == null || result.getType().isAir()) {
            player.sendMessage(ChatUtil.color("&cPlease set a result item!"));
            return;
        }

        File file = category.equals("vanilla_recipes") 
                ? new File(Plugin.getInstance().getDataFolder(), "vanilla_recipes.yml")
                : new File(Plugin.getInstance().getDataFolder(), category + "/" + stationName + ".yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String basePath = category.equals("vanilla_recipes") ? "recipes." + stationName + "_" : "recipes.recipe_";
        String recipeId = basePath + UUID.randomUUID().toString().substring(0, 8);
        ConfigurationSection recipeSection = config.createSection(recipeId);

        saveItemToConfig(recipeSection.createSection("result"), result);

        if (category.equals("furnaces") || (category.equals("vanilla_recipes") && stationName.equals("furnace"))) {
            recipeSection.set("cook-time", cookTime);
            ConfigurationSection raws = recipeSection.createSection("raws");
            ConfigurationSection fuels = recipeSection.createSection("fuels");
            
            for (Map.Entry<Character, Integer> entry : charToSlot.entrySet()) {
                ItemStack item = gridInventory.getItem(entry.getValue());
                if (item != null && !item.getType().isAir()) {
                    if (entry.getKey() == 'U' || entry.getKey() == 'F') {
                        fuels.set(String.valueOf(entry.getKey()), serializeItem(item));
                    } else {
                        raws.set(String.valueOf(entry.getKey()), serializeItem(item));
                    }
                }
            }
        } else if (category.equals("smithing_tables") || (category.equals("vanilla_recipes") && stationName.equals("smithing"))) {
            for (Map.Entry<Character, Integer> entry : charToSlot.entrySet()) {
                ItemStack item = gridInventory.getItem(entry.getValue());
                if (item != null && !item.getType().isAir()) {
                    String keyName = "base";
                    if (entry.getKey() == 'T') keyName = "template";
                    if (entry.getKey() == 'A') keyName = "addition";
                    saveItemToConfig(recipeSection.createSection(keyName), item);
                }
            }
        } else {
            for (Map.Entry<Character, Integer> entry : charToSlot.entrySet()) {
                ItemStack item = gridInventory.getItem(entry.getValue());
                if (item != null && !item.getType().isAir()) {
                    saveItemToConfig(recipeSection.createSection(String.valueOf(entry.getKey())), item);
                }
            }
        }

        try {
            config.save(file);
            player.sendMessage(ChatUtil.color("&aSuccessfully saved recipe! Reloading..."));
            Plugin.getInstance().reload();
            player.closeInventory();
        } catch (IOException e) {
            player.sendMessage(ChatUtil.color("&cError saving recipe!"));
        }
    }

    private void saveItemToConfig(ConfigurationSection section, ItemStack item) {
        section.set("material", serializeItem(item));
        section.set("amount", item.getAmount());
    }

    private String serializeItem(ItemStack item) {
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack != null) {
            return "ia-" + customStack.getNamespacedID();
        }
        return item.getType().name();
    }
}