package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.FluidStack;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class FurnaceDataManager {
    private final File storageFolder;

    public FurnaceDataManager(File dataFolder) {
        this.storageFolder = new File(dataFolder, "data/furnaces");
        if (!this.storageFolder.exists()) this.storageFolder.mkdirs();
    }

    public void saveAsync(Location location, VirtualInventory inventory, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots, FluidStack fluid, Map<BlockFace, IOState> sideConfig) {
        ItemStack[] items = new ItemStack[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) items[i] = item.clone();
        }
        Set<Integer> blocks = new HashSet<>(blockedSlots);
        FluidStack fsCopy = fluid != null ? fluid.clone() : null;
        Map<BlockFace, IOState> sidesCopy = new HashMap<>(sideConfig);

        Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
            saveSyncInternal(location, items, cookingProgress, isAutomated, blocks, fsCopy, sidesCopy);
        });
    }

    public void saveSync(Location location, VirtualInventory inventory, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots, FluidStack fluid, Map<BlockFace, IOState> sideConfig) {
        ItemStack[] items = new ItemStack[inventory.getSize()];
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) items[i] = item.clone();
        }
        saveSyncInternal(location, items, cookingProgress, isAutomated, blockedSlots, fluid, sideConfig);
    }

    private void saveSyncInternal(Location location, ItemStack[] items, int cookingProgress, boolean isAutomated, Set<Integer> blockedSlots, FluidStack fluid, Map<BlockFace, IOState> sideConfig) {
        File file = new File(storageFolder, locationToString(location) + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) config.set("items." + i, items[i]);
        }
        config.set("cooking-progress", cookingProgress);
        config.set("is-automated", isAutomated);
        config.set("blocked-slots", new ArrayList<>(blockedSlots));

        if (fluid != null) {
            config.set("fluid.id", fluid.getFluidId());
            config.set("fluid.amount", fluid.getAmountLn());
        }

        for (Map.Entry<BlockFace, IOState> entry : sideConfig.entrySet()) {
            config.set("sides." + entry.getKey().name(), entry.getValue().name());
        }
        
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Map<String, Object> load(Location location) {
        File file = new File(storageFolder, locationToString(location) + ".yml");
        if (!file.exists()) return null;
        
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

        if (config.contains("fluid.id")) {
            data.put("fluid", new FluidStack(config.getString("fluid.id"), config.getInt("fluid.amount")));
        }

        Map<BlockFace, IOState> sides = new HashMap<>();
        if (config.contains("sides")) {
            for (String key : config.getConfigurationSection("sides").getKeys(false)) {
                sides.put(BlockFace.valueOf(key), IOState.valueOf(config.getString("sides." + key)));
            }
        }
        data.put("sides", sides);
        
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