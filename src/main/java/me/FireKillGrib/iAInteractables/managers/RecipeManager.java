package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.*;
import me.FireKillGrib.iAInteractables.utils.ItemBuilder;
import me.FireKillGrib.iAInteractables.utils.ItemsBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.ItemProvider;
import java.io.File;
import java.util.*;

public class RecipeManager {
    private final List<Furnace> furnaces = new ArrayList<>();
    private final List<Workbench> workbenches = new ArrayList<>();
    private final List<SmithingTable> smithingTables = new ArrayList<>();
    public void loadFurnaces() {
        furnaces.clear();
        File folder = new File(Plugin.getInstance().getDataFolder(), "furnaces");
        if (!folder.exists()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            String title = cfg.getString("title", "&cName is not defined");
            String iaFurniture = cfg.getString("itemsadder-furniture"); 
            List<String> structure = cfg.getStringList("structure");
            List<String> specialFunctions = cfg.getStringList("special_functions");
            if (specialFunctions == null) specialFunctions = new ArrayList<>();

            ConfigurationSection fillerSec = cfg.getConfigurationSection("filler");
            ItemProvider filler;
            if (fillerSec != null) {
                String materialStr = fillerSec.getString("material", "BLACK_STAINED_GLASS_PANE");
                String displayName = fillerSec.getString("name", "");
                List<String> lore = fillerSec.getStringList("lore");
                ItemStack fillerItem;
                if (materialStr.startsWith("ia-") || materialStr.contains(":")) {
                    fillerItem = new ItemsBuilder(materialStr).build();
                } else {
                    try { fillerItem = new ItemStack(Material.valueOf(materialStr.toUpperCase())); } 
                    catch (Exception e) { fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE); }
                }
                ItemBuilder builder = new ItemBuilder(fillerItem);
                if (displayName != null && !displayName.isEmpty()) builder.setDisplayName(displayName);
                if (lore != null && !lore.isEmpty()) builder.setLore(lore);
                filler = builder.getItemProvider();
            } else {
                filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("").getItemProvider();
            }

            Set<FurnaceRecipe> recipes = new HashSet<>();
            ConfigurationSection recipesSec = cfg.getConfigurationSection("recipes");
            if (recipesSec != null) {
                for (String key : recipesSec.getKeys(false)) {
                    ConfigurationSection s = recipesSec.getConfigurationSection(key);
                    if (s == null) continue;
                    int cookTime = s.getInt("cook-time", 100);
                    ConfigurationSection resultSec = s.getConfigurationSection("result");
                    ItemStack result = null;
                    if (resultSec != null) {
                        String material = resultSec.getString("material");
                        int amount = resultSec.getInt("amount", 1);
                        if (material != null) {
                            try {
                                if (material.startsWith("ia-") || material.contains(":")) {
                                    result = new ItemsBuilder(material).build();
                                    result.setAmount(amount);
                                } else {
                                    result = new ItemStack(Material.valueOf(material.toUpperCase()), amount);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (result == null) continue;
                    
                    Map<Character, Set<ItemStack>> raws = new HashMap<>();
                    ConfigurationSection rawsSec = s.getConfigurationSection("raws");
                    if (rawsSec != null) {
                        for (String slotKey : rawsSec.getKeys(false)) {
                            char ch = slotKey.charAt(0);
                            Set<ItemStack> set = new HashSet<>();
                            List<String> materials = rawsSec.isList(slotKey) ? rawsSec.getStringList(slotKey) : Collections.singletonList(rawsSec.getString(slotKey));
                            for (String mat : materials) {
                                if (mat == null || mat.isEmpty()) continue;
                                try {
                                    if (mat.startsWith("ia-") || mat.contains(":")) set.add(new ItemsBuilder(mat).build());
                                    else set.add(new ItemStack(Material.valueOf(mat.toUpperCase())));
                                } catch (Exception ignored) {}
                            }
                            if (!set.isEmpty()) raws.put(ch, set);
                        }
                    }

                    Map<Character, Set<ItemStack>> fuels = new HashMap<>();
                    ConfigurationSection fuelsSec = s.getConfigurationSection("fuels");
                    if (fuelsSec != null) {
                        for (String slotKey : fuelsSec.getKeys(false)) {
                            char ch = slotKey.charAt(0);
                            Set<ItemStack> set = new HashSet<>();
                            List<String> materials = fuelsSec.isList(slotKey) ? fuelsSec.getStringList(slotKey) : Collections.singletonList(fuelsSec.getString(slotKey));
                            for (String mat : materials) {
                                if (mat == null || mat.isEmpty()) continue;
                                try {
                                    if (mat.startsWith("ia-") || mat.contains(":")) set.add(new ItemsBuilder(mat).build());
                                    else set.add(new ItemStack(Material.valueOf(mat.toUpperCase())));
                                } catch (Exception ignored) {}
                            }
                            if (!set.isEmpty()) fuels.put(ch, set);
                        }
                    }
                    Map<String, Integer> fluidRaws = new HashMap<>();
                    ConfigurationSection fluidSec = s.getConfigurationSection("fluid_raws");
                    if (fluidSec != null) {
                        for (String fId : fluidSec.getKeys(false)) {
                            fluidRaws.put(fId, fluidSec.getInt(fId));
                        }
                    }

                    // Парсинг охлаждения (COL)
                    FurnaceRecipe.CoolingRequirement cooling = null;
                    ConfigurationSection tagsSec = s.getConfigurationSection("special_tags");
                    if (tagsSec != null && tagsSec.contains("COL")) {
                        ConfigurationSection colSec = tagsSec.getConfigurationSection("COL");
                        cooling = new FurnaceRecipe.CoolingRequirement(
                            colSec.getInt("amount_per_recipe", 0),
                            colSec.getInt("explode_timer", 200),
                            colSec.getStringList("coolants")
                        );
                    }
                    
                    recipes.add(new FurnaceRecipe(result, cookTime, raws, fuels, fluidRaws, cooling));
                }
            }
            FurnaceEffects effects = FurnaceEffects.fromConfig(cfg.getConfigurationSection("effects"));
            ProgressBarConfig progressBar = ProgressBarConfig.fromConfig(cfg.getConfigurationSection("progress-bar"));

            Furnace.PumpSettings pumpSettings = null;
            ConfigurationSection tagsSec = cfg.getConfigurationSection("special_tags");
            if (tagsSec != null && tagsSec.contains("PUM")) {
                ConfigurationSection pSec = tagsSec.getConfigurationSection("PUM");
                pumpSettings = new Furnace.PumpSettings(
                    pSec.getString("target_fluid", "minecraft:water"),
                    pSec.getString("target_block", "WATER"),
                    pSec.getInt("required_blocks", 100),
                    pSec.getInt("amount_per_cycle", 81),
                    pSec.getInt("cycle_ticks", 100),
                    pSec.getString("fuel_slot", "U").charAt(0),
                    pSec.getInt("fuel_amount", 1)
                );
            }

            furnaces.add(new Furnace(name, title, iaFurniture, structure, filler, recipes, effects, progressBar, specialFunctions, pumpSettings));
        }
    }
    public void loadWorkbenches() {
        workbenches.clear();
        File folder = new File(Plugin.getInstance().getDataFolder(), "workbenches");
        if (!folder.exists()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            String title = cfg.getString("title", "&cName is not defined");
            String iaFurniture = cfg.getString("itemsadder-furniture");
            List<String> structure = cfg.getStringList("structure");
            List<String> specialFunctions = cfg.getStringList("special_functions");
            if (specialFunctions == null) specialFunctions = new ArrayList<>();

            ConfigurationSection fillerSec = cfg.getConfigurationSection("filler");
            ItemProvider filler;
            if (fillerSec != null) {
                String materialStr = fillerSec.getString("material", "BLACK_STAINED_GLASS_PANE");
                String displayName = fillerSec.getString("name", "");
                List<String> lore = fillerSec.getStringList("lore");
                ItemStack fillerItem;
                if (materialStr.startsWith("ia-") || materialStr.contains(":")) {
                    fillerItem = new ItemsBuilder(materialStr).build();
                } else {
                    try { fillerItem = new ItemStack(Material.valueOf(materialStr.toUpperCase())); } 
                    catch (Exception e) { fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE); }
                }
                ItemBuilder builder = new ItemBuilder(fillerItem);
                if (displayName != null && !displayName.isEmpty()) builder.setDisplayName(displayName);
                if (lore != null && !lore.isEmpty()) builder.setLore(lore);
                filler = builder.getItemProvider();
            } else {
                filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("").getItemProvider();
            }

            Set<WorkbenchRecipe> recipes = new HashSet<>();
            ConfigurationSection recipesSec = cfg.getConfigurationSection("recipes");
            if (recipesSec != null) {
                for (String key : recipesSec.getKeys(false)) {
                    ConfigurationSection s = recipesSec.getConfigurationSection(key);
                    if (s == null) continue;
                    ConfigurationSection resultSec = s.getConfigurationSection("result");
                    ItemStack result = null;
                    if (resultSec != null) {
                        String material = resultSec.getString("material");
                        int amount = resultSec.getInt("amount", 1);
                        if (material != null) {
                            try {
                                if (material.startsWith("ia-") || material.contains(":")) {
                                    result = new ItemsBuilder(material).build();
                                    result.setAmount(amount);
                                } else {
                                    result = new ItemStack(Material.valueOf(material.toUpperCase()), amount);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    if (result == null) continue;
                    
                    Map<Character, ItemStack> ingredients = new HashMap<>();
                    for (String slotKey : s.getKeys(false)) {
                        if (slotKey.equals("result")) continue;
                        char charKey = slotKey.charAt(0);
                        ConfigurationSection ingredientSec = s.getConfigurationSection(slotKey);
                        if (ingredientSec != null) {
                            String material = ingredientSec.getString("material");
                            int amount = ingredientSec.getInt("amount", 1);
                            if (material != null) {
                                try {
                                    ItemStack item;
                                    if (material.startsWith("ia-") || material.contains(":")) item = new ItemsBuilder(material).build();
                                    else item = new ItemStack(Material.valueOf(material.toUpperCase()));
                                    item.setAmount(amount);
                                    ingredients.put(charKey, item);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    recipes.add(new WorkbenchRecipe(result, ingredients));
                }
            }
            WorkbenchEffects effects = WorkbenchEffects.fromConfig(cfg.getConfigurationSection("effects"));
            workbenches.add(new Workbench(name, title, iaFurniture, structure, filler, recipes, effects, specialFunctions));
        }
    }

    public void loadSmithingTables() {
        smithingTables.clear();
        File folder = new File(Plugin.getInstance().getDataFolder(), "smithing_tables");
        if (!folder.exists()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            String title = cfg.getString("title", "&8Smithing Table");
            String iaFurniture = cfg.getString("itemsadder-furniture");
            List<String> structure = cfg.getStringList("structure");
            List<String> specialFunctions = cfg.getStringList("special_functions");
            if (specialFunctions == null) specialFunctions = new ArrayList<>();

            ItemProvider filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").getItemProvider();
            ConfigurationSection fillerSec = cfg.getConfigurationSection("filler");
            if (fillerSec != null) {
                String mat = fillerSec.getString("material", "BLACK_STAINED_GLASS_PANE");
                String nameStr = fillerSec.getString("name", "");
                List<String> lore = fillerSec.getStringList("lore");
                ItemStack item;
                try {
                    if (mat.toLowerCase().startsWith("ia-") || mat.contains(":")) item = new ItemsBuilder(mat).build();
                    else item = new ItemStack(Material.valueOf(mat.toUpperCase()));
                } catch (Exception e) {
                    item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                }
                ItemBuilder builder = new ItemBuilder(item);
                if (nameStr != null) builder.setDisplayName(nameStr);
                if (lore != null) builder.setLore(lore);
                filler = builder.getItemProvider();
            }

            Set<SmithingRecipe> recipes = new HashSet<>();
            ConfigurationSection recipesSec = cfg.getConfigurationSection("recipes");
            if (recipesSec != null) {
                for (String key : recipesSec.getKeys(false)) {
                    ConfigurationSection s = recipesSec.getConfigurationSection(key);
                    if (s == null) continue;
                    ItemStack result = parseItem(s.getConfigurationSection("result"));
                    ItemStack template = parseItem(s.getConfigurationSection("template"));
                    ItemStack base = parseItem(s.getConfigurationSection("base"));
                    ItemStack addition = parseItem(s.getConfigurationSection("addition"));
                    if (result != null) {
                        recipes.add(new SmithingRecipe(result, template, base, addition));
                    }
                }
            }
            WorkbenchEffects effects = WorkbenchEffects.fromConfig(cfg.getConfigurationSection("effects"));
            smithingTables.add(new SmithingTable(name, title, iaFurniture, structure, filler, recipes, effects, specialFunctions));
        }
    }
    private ItemStack parseItem(ConfigurationSection section) {
        if (section == null) return null;
        String material = section.getString("material");
        int amount = section.getInt("amount", 1);
        if (material == null) return null;
        try {
            ItemStack item;
            if (material.startsWith("ia-") || material.contains(":")) {
                item = new ItemsBuilder(material).build();
            } else {
                item = new ItemStack(Material.valueOf(material.toUpperCase()));
            }
            item.setAmount(amount);
            return item;
        } catch (Exception e) {
            return null;
        }
    }
    public List<SmithingTable> getSmithingTables() { return smithingTables; }
    public SmithingTable getSmithingTable(String name) { return smithingTables.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null); }
    public List<Furnace> getFurnaces() { return furnaces; }
    public List<Workbench> getWorkbenches() { return workbenches; }
    public Furnace getFurnace(String name) { return furnaces.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null); }
    public Workbench getWorkbench(String name) { return workbenches.stream().filter(w -> w.getName().equals(name)).findFirst().orElse(null); }

    public void clearAll() {
        furnaces.clear();
        workbenches.clear();
        smithingTables.clear();
    }

    public void reload() {
        clearAll();
        loadFurnaces();
        loadWorkbenches();
        loadSmithingTables();
    }
}