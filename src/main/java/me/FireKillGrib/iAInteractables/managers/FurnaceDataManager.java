package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FurnaceDataManager {
    private final File storageFolder;

    public FurnaceDataManager(File dataFolder) {
        this.storageFolder = new File(dataFolder, "data/furnaces");
        if (!this.storageFolder.exists()) {
            this.storageFolder.mkdirs();
        }
    }

    public void saveAsync(Location location, VirtualInventory inventory, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots) {
        // Сбор данных строго в главном потоке (Thread-Safe)
        ItemStack[] items = new ItemStack[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                items[i] = item.clone();
            }
        }
        Set<Integer> blocks = new HashSet<>(blockedSlots);

        Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
            saveSyncInternal(location, items, cookingProgress, isAutomated, blocks);
        });
    }

    public void saveSync(Location location, VirtualInventory inventory, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots) {
        ItemStack[] items = new ItemStack[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                items[i] = item.clone();
            }
        }
        saveSyncInternal(location, items, cookingProgress, isAutomated, blockedSlots);
    }

    private void saveSyncInternal(Location location, ItemStack[] items, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots) {
        File file = new File(storageFolder, locationToString(location) + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                config.set("items." + i, items[i]);
            }
        }
        config.set("cooking-progress", cookingProgress);
        config.set("is-automated", isAutomated);
        config.set("blocked-slots", new ArrayList<>(blockedSlots));
        
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> load(Location location) {
        File file = new File(storageFolder, locationToString(location) + ".yml");
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> data = new HashMap<>();
        Map<Integer, ItemStack> items = new HashMap<>();
        
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                items.put(Integer.parseInt(key), config.getItemStack("items." + key));
            }
        }
        data.put("items", items);
        data.put("cooking-progress", config.getInt("cooking-progress", 0));
        data.put("is-automated", config.getBoolean("is-automated", false));
        data.put("blocked-slots", config.getIntegerList("blocked-slots"));
        
        return data;
    }

    public void delete(Location location) {
        File file = new File(storageFolder, locationToString(location) + ".yml");
        if (file.exists()) file.delete();
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
}