package me.FireKillGrib.iAInteractables.multiblock;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.utils.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MultiblockManager {
    private static final String TEMPLATES_FOLDER = "multiblocks/templates";
    private static final String ACTIVE_FOLDER = "multiblocks/active";
    
    private final Map<Location, MultiblockInstance> activeStructures = new HashMap<>();
    private final Map<String, MultiblockTemplate> loadedTemplates = new HashMap<>();

    public MultiblockManager() {
        loadAllTemplates();
        loadAllActiveStructures();
    }

    public Collection<MultiblockTemplate> getAllTemplates() {
        return loadedTemplates.values();
    }

    public void loadAllTemplates() {
        loadedTemplates.clear();
        File folder = new File(Plugin.getInstance().getDataFolder(), TEMPLATES_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            MultiblockTemplate template = new MultiblockTemplate(config.getString("name"));
            
            if (config.contains("blocks")) {
                for (String key : config.getConfigurationSection("blocks").getKeys(false)) {
                    Vector vec = stringToVector(key);
                    Material mat = Material.matchMaterial(config.getString("blocks." + key + ".material"));
                    String customId = config.getString("blocks." + key + ".custom-id");
                    template.addBlock(vec, mat, customId);
                }
            }
            if (config.contains("furniture")) {
                for (String key : config.getConfigurationSection("furniture").getKeys(false)) {
                    Vector vec = stringToVector(key);
                    String id = config.getString("furniture." + key + ".id");
                    float yaw = (float) config.getDouble("furniture." + key + ".yaw");
                    template.addFurniture(vec, id, yaw);
                }
            }
            if (config.contains("void-exceptions")) {
                template.setVoidExceptions(config.getStringList("void-exceptions"));
            }
            loadedTemplates.put(template.getName(), template);
        }
    }

    public void saveTemplate(MultiblockTemplate template) {
        File folder = new File(Plugin.getInstance().getDataFolder(), TEMPLATES_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, template.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("name", template.getName());
        
        for (Map.Entry<Vector, MultiblockTemplate.BlockData> e : template.getBlocks().entrySet()) {
            String key = vectorToString(e.getKey());
            config.set("blocks." + key + ".material", e.getValue().getMaterial().name());
            if (e.getValue().getCustomId() != null) config.set("blocks." + key + ".custom-id", e.getValue().getCustomId());
        }
        
        for (Map.Entry<Vector, MultiblockTemplate.FurnitureData> e : template.getFurniture().entrySet()) {
            String key = vectorToString(e.getKey());
            config.set("furniture." + key + ".id", e.getValue().getFurnitureId());
            config.set("furniture." + key + ".yaw", e.getValue().getYaw());
        }
        
        config.set("void-exceptions", template.getVoidExceptions());
        
        try { config.save(file); loadedTemplates.put(template.getName(), template); } 
        catch (IOException ignored) {}
    }

    public void loadAllActiveStructures() {
        activeStructures.clear();
        File folder = new File(Plugin.getInstance().getDataFolder(), ACTIVE_FOLDER);
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String templateName = config.getString("template");
            Location core = stringToLocation(config.getString("core"));
            int rot = config.getInt("rotation");

            MultiblockTemplate template = loadedTemplates.get(templateName);
            if (template != null && core != null) {
                // Восстанавливаем кэш
                MultiblockInstance instance = new MultiblockInstance(templateName, core, rot);
                for (Vector v : template.getBlocks().keySet()) {
                    instance.addBlock(core.clone().add(RotationUtil.rotateVector(v, rot)));
                }
                for (Vector v : template.getFurniture().keySet()) {
                    instance.addFurnitureLocation(core.clone().add(RotationUtil.rotateVector(v, rot)));
                }
                // Регистрируем без повторного сохранения
                for (Location loc : instance.getAllElements()) {
                    activeStructures.put(normalize(loc), instance);
                }
            } else {
                // Если шаблон удалили, удаляем и осиротевшую структуру
                file.delete();
            }
        }
    }

    public void saveActiveStructure(MultiblockInstance instance) {
        File folder = new File(Plugin.getInstance().getDataFolder(), ACTIVE_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        
        File file = new File(folder, locationToString(instance.getCoreLocation()) + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("template", instance.getTemplateName());
        config.set("core", locationToString(instance.getCoreLocation()));
        config.set("rotation", instance.getRotation());
        
        try { config.save(file); } catch (IOException ignored) {}
    }

    public void registerStructure(MultiblockInstance instance) {
        for (Location loc : instance.getAllElements()) {
            activeStructures.put(normalize(loc), instance);
        }
        for (Location loc : instance.getAllElements()) {
            Plugin.getInstance().getPipeManager().updateAdjacentPipes(loc);
        }
        saveActiveStructure(instance);
    }

    public void unregisterStructure(Location coreLocation) {
        MultiblockInstance instance = activeStructures.get(normalize(coreLocation));
        if (instance != null) {
            for (Location loc : instance.getAllElements()) {
                activeStructures.remove(normalize(loc));
            }
            File folder = new File(Plugin.getInstance().getDataFolder(), ACTIVE_FOLDER);
            File file = new File(folder, locationToString(instance.getCoreLocation()) + ".yml");
            if (file.exists()) file.delete();
        }
    }

    public MultiblockInstance getStructureAt(Location location) {
        if (location == null) return null;
        return activeStructures.get(normalize(location));
    }

    public boolean isPartOfActiveStructure(Location location) {
        if (location == null) return false;
        return activeStructures.containsKey(normalize(location));
    }

    // Нормализация координат для идеального сравнения (срезаем доли и градусы)
    private Location normalize(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    private Location stringToLocation(String s) {
        if (s == null) return null;
        String[] p = s.split("_");
        if (p.length < 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    private String vectorToString(Vector v) { return v.getBlockX() + "," + v.getBlockY() + "," + v.getBlockZ(); }
    
    private Vector stringToVector(String s) {
        String[] p = s.split(",");
        return new Vector(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }
}