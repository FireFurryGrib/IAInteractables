package me.FireKillGrib.iAInteractables.fluids;

import me.FireKillGrib.iAInteractables.Plugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FluidRegistry {
    private final Map<String, FluidType> fluids = new HashMap<>();
    private final File configFile;

    public FluidRegistry() {
        configFile = new File(Plugin.getInstance().getDataFolder(), "fluids.yml");
        generateDefaultConfig();
        loadFluids();
    }

    private void generateDefaultConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                YamlConfiguration cfg = new YamlConfiguration();
                
                cfg.set("fluids.minecraft:water.display-name", "Water");
                cfg.set("fluids.minecraft:water.is-gas", false);
                cfg.set("fluids.minecraft:water.density", 1000.0);
                cfg.set("fluids.minecraft:water.compression-limit", 1.0);
                cfg.set("fluids.minecraft:water.groups", Arrays.asList("liquids", "cold", "water_based"));
                cfg.set("fluids.minecraft:water.chat-color", "&9");
                cfg.set("fluids.minecraft:water.glass-material", "BLUE_STAINED_GLASS_PANE");

                cfg.set("fluids.minecraft:lava.display-name", "Lava");
                cfg.set("fluids.minecraft:lava.is-gas", false);
                cfg.set("fluids.minecraft:lava.density", 3100.0);
                cfg.set("fluids.minecraft:lava.compression-limit", 1.0);
                cfg.set("fluids.minecraft:lava.groups", Arrays.asList("liquids", "hot"));
                cfg.set("fluids.minecraft:lava.chat-color", "&c");
                cfg.set("fluids.minecraft:lava.glass-material", "ORANGE_STAINED_GLASS_PANE");

                cfg.set("fluids.custom:steam.display-name", "Steam");
                cfg.set("fluids.custom:steam.is-gas", true);
                cfg.set("fluids.custom:steam.density", 0.6); 
                cfg.set("fluids.custom:steam.compression-limit", 50.0); 
                cfg.set("fluids.custom:steam.groups", Arrays.asList("gases", "hot"));
                cfg.set("fluids.custom:steam.chat-color", "&f");
                cfg.set("fluids.custom:steam.glass-material", "WHITE_STAINED_GLASS_PANE");

                cfg.save(configFile);
                Plugin.getInstance().getLogger().info("Файл fluids.yml успешно сгенерирован!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadFluids() {
        fluids.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        if (!cfg.contains("fluids")) return;

        for (String key : cfg.getConfigurationSection("fluids").getKeys(false)) {
            String path = "fluids." + key;
            
            Material mat;
            try { mat = Material.valueOf(cfg.getString(path + ".glass-material", "GLASS_PANE")); }
            catch (Exception e) { mat = Material.GLASS_PANE; }

            FluidType type = new FluidType(
                key,
                cfg.getString(path + ".display-name", key),
                cfg.getBoolean(path + ".is-gas", false),
                cfg.getDouble(path + ".density", 1000.0),
                cfg.getDouble(path + ".compression-limit", 1.0),
                cfg.getStringList(path + ".groups"),
                cfg.getString(path + ".chat-color", "&7"),
                mat
            );
            fluids.put(key, type);
        }
    }

    public FluidType getFluid(String id) { return fluids.get(id); }
    public Collection<FluidType> getAllFluids() { return fluids.values(); }
}