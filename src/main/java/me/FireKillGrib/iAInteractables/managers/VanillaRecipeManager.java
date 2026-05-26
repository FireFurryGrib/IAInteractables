package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.FurnaceRecipe;
import me.FireKillGrib.iAInteractables.data.SmithingRecipe;
import me.FireKillGrib.iAInteractables.data.WorkbenchRecipe;
import me.FireKillGrib.iAInteractables.utils.ItemsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.io.File;
import java.util.*;

public class VanillaRecipeManager {
    private final List<WorkbenchRecipe> workbenchRecipes = new ArrayList<>();
    private final List<FurnaceRecipe> furnaceRecipes = new ArrayList<>();
    private final List<SmithingRecipe> smithingRecipes = new ArrayList<>();
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public void load() {
        workbenchRecipes.clear();
        furnaceRecipes.clear();
        smithingRecipes.clear();
        
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();

        File file = new File(Plugin.getInstance().getDataFolder(), "vanilla_recipes.yml");
        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection recipesSec = cfg.getConfigurationSection("recipes");
        if (recipesSec == null) return;

        for (String key : recipesSec.getKeys(false)) {
            ConfigurationSection s = recipesSec.getConfigurationSection(key);
            if (s == null) continue;

            ItemStack result = parseItem(s.getConfigurationSection("result"));
            if (result == null) continue;

            if (key.startsWith("workbench_")) {
                Map<Character, ItemStack> ingredients = new HashMap<>();
                for (String slotKey : s.getKeys(false)) {
                    if (slotKey.equals("result")) continue;
                    char charKey = slotKey.charAt(0);
                    ItemStack item = parseItem(s.getConfigurationSection(slotKey));
                    if (item != null) {
                        ingredients.put(charKey, item); // Сохраняем оригинальное количество!
                    }
                }
                workbenchRecipes.add(new WorkbenchRecipe(result, ingredients));

                // Регистрация фиктивного рецепта в Bukkit для клиента (MaterialChoice обходит баги ядра с NBT/количеством)
                try {
                    NamespacedKey wKey = new NamespacedKey(Plugin.getInstance(), "v_wb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
                    ShapedRecipe sr = new ShapedRecipe(wKey, result);
                    
                    String r1 = "", r2 = "", r3 = "";
                    for (int i = 0; i < 9; i++) {
                        char c = (char) ('A' + i);
                        ItemStack req = ingredients.get(c);
                        if (req == null) req = ingredients.get((char) ('0' + i));
                        
                        char shapeChar = (req != null && !req.getType().isAir()) ? c : ' ';
                        if (i < 3) r1 += shapeChar;
                        else if (i < 6) r2 += shapeChar;
                        else r3 += shapeChar;
                    }
                    sr.shape(r1, r2, r3);
                    boolean hasAny = false;
                    for (int i = 0; i < 9; i++) {
                        char c = (char) ('A' + i);
                        ItemStack req = ingredients.get(c);
                        if (req == null) req = ingredients.get((char) ('0' + i));
                        
                        if (req != null && !req.getType().isAir()) {
                            sr.setIngredient(c, new RecipeChoice.MaterialChoice(req.getType()));
                            hasAny = true;
                        }
                    }
                    if (hasAny) {
                        Bukkit.addRecipe(sr);
                        registeredKeys.add(wKey);
                    }
                } catch (Exception ignored) {}

            } else if (key.startsWith("furnace_")) {
                int cookTime = s.getInt("cook-time", 200);
                Map<Character, Set<ItemStack>> raws = new HashMap<>();
                Map<Character, Set<ItemStack>> fuels = new HashMap<>();
                
                ConfigurationSection rawsSec = s.getConfigurationSection("raws");
                ItemStack firstRaw = null;
                if (rawsSec != null) {
                    for (String rk : rawsSec.getKeys(false)) {
                        ItemStack item = parseItemStr(rawsSec.getString(rk));
                        if (item != null) {
                            raws.put(rk.charAt(0), Collections.singleton(item));
                            if (firstRaw == null) firstRaw = item.clone();
                        }
                    }
                }
                
                ConfigurationSection fuelsSec = s.getConfigurationSection("fuels");
                if (fuelsSec != null) {
                    for (String fk : fuelsSec.getKeys(false)) {
                        ItemStack item = parseItemStr(fuelsSec.getString(fk));
                        if (item != null) fuels.put(fk.charAt(0), Collections.singleton(item));
                    }
                }
                
                FurnaceRecipe customRecipe = new FurnaceRecipe(result, cookTime, raws, fuels);
                furnaceRecipes.add(customRecipe);
                
                if (firstRaw != null) {
                    try {
                        NamespacedKey fKey = new NamespacedKey(Plugin.getInstance(), "v_furnace_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
                        org.bukkit.inventory.FurnaceRecipe fr = new org.bukkit.inventory.FurnaceRecipe(fKey, result, new RecipeChoice.MaterialChoice(firstRaw.getType()), 0f, cookTime);
                        Bukkit.addRecipe(fr);
                        registeredKeys.add(fKey);
                    } catch (Exception ignored) {}
                }

            } else if (key.startsWith("smithing_")) {
                ItemStack template = parseItem(s.getConfigurationSection("template"));
                ItemStack base = parseItem(s.getConfigurationSection("base"));
                ItemStack addition = parseItem(s.getConfigurationSection("addition"));
                
                if (template != null && base != null && addition != null) {
                    smithingRecipes.add(new SmithingRecipe(result, template, base, addition));
                    try {
                        NamespacedKey sKey = new NamespacedKey(Plugin.getInstance(), "v_smithing_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
                        SmithingTransformRecipe str = new SmithingTransformRecipe(sKey, result,
                            new RecipeChoice.MaterialChoice(template.getType()),
                            new RecipeChoice.MaterialChoice(base.getType()),
                            new RecipeChoice.MaterialChoice(addition.getType()));
                        Bukkit.addRecipe(str);
                        registeredKeys.add(sKey);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private ItemStack parseItem(ConfigurationSection section) {
        if (section == null) return null;
        String material = section.getString("material");
        int amount = section.getInt("amount", 1);
        if (material != null) {
            ItemStack item = parseItemStr(material);
            if (item != null) item.setAmount(amount);
            return item;
        }
        return null;
    }

    private ItemStack parseItemStr(String material) {
        if (material == null) return null;
        try {
            if (material.startsWith("ia-") || material.contains(":")) {
                return new ItemsBuilder(material).build();
            }
            return new ItemStack(Material.valueOf(material.toUpperCase()));
        } catch (Exception e) {
            return null;
        }
    }

    public List<WorkbenchRecipe> getWorkbenchRecipes() { return workbenchRecipes; }
    public List<FurnaceRecipe> getFurnaceRecipes() { return furnaceRecipes; }
    public List<SmithingRecipe> getSmithingRecipes() { return smithingRecipes; }
}