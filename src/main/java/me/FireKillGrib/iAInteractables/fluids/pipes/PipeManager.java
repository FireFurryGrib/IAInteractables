package me.FireKillGrib.iAInteractables.fluids.pipes;

import dev.lone.itemsadder.api.CustomFurniture;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PipeManager {
    private final Map<Location, PipeNode> pipes = new HashMap<>();
    private final Map<String, PipeType> pipeTypes = new HashMap<>();
    private final File dataFile;
    private final File configFile;

    public PipeManager() {
        dataFile = new File(Plugin.getInstance().getDataFolder(), "fluids_pipes_data.yml");
        configFile = new File(Plugin.getInstance().getDataFolder(), "pipes.yml");
        
        generateDefaultConfig();
        loadPipeTypes();
        loadPipesData();
    }

    public void reload() {
        // Перечитываем конфиг
        loadPipeTypes();
        
        // Обновляем все трубы в мире
        Iterator<Map.Entry<Location, PipeNode>> it = pipes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, PipeNode> entry = it.next();
            PipeNode pipe = entry.getValue();
            PipeType newType = pipeTypes.get(pipe.getPipeType().getId());
            
            if (newType != null) {
                // Обновляем тип (новые лимиты, давление и тд применяются сразу)
                pipe.setPipeType(newType);
                // Если админ урезал прочность трубы в конфиге, подрезаем здоровье
                if (pipe.getHealth() > newType.getDurability()) {
                    pipe.setHealth(newType.getDurability());
                }
            } else {
                // Если тип трубы удален из конфига, труба взрывается/исчезает
                Plugin.getInstance().getFluidNetworkManager().removeNode(entry.getKey());
                clearLeftoverPipes(entry.getKey());
                it.remove();
            }
        }
        
        // Перерисовываем трубы (вдруг поменяли модели в конфиге)
        Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
            for (Location loc : pipes.keySet()) {
                updatePipeVisuals(loc);
            }
            Plugin.getInstance().getFluidNetworkManager().recalculateNetworks();
        });
    }

    private void generateDefaultConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                YamlConfiguration cfg = new YamlConfiguration();
                
                cfg.set("pipes.steel_pipe.place-item", "ia_interactables:steel_pipe_item");
                cfg.set("pipes.steel_pipe.center-model", "ia_interactables:pipe_center");
                cfg.set("pipes.steel_pipe.arm-model", "ia_interactables:pipe_arm");
                cfg.set("pipes.steel_pipe.arm-up-model", "ia_interactables:pipe_arm_up");
                cfg.set("pipes.steel_pipe.arm-down-model", "ia_interactables:pipe_arm_down");
                cfg.set("pipes.steel_pipe.carrying_capacity", 100);
                cfg.set("pipes.steel_pipe.storage_capacity", 1000);
                cfg.set("pipes.steel_pipe.durability", 1000.0);
                cfg.set("pipes.steel_pipe.pressure-limit", 50.0);
                cfg.set("pipes.steel_pipe.damage-multiplier", 2.0);
                cfg.set("pipes.steel_pipe.heal-rate", 0.5);
                cfg.set("pipes.steel_pipe.allowed-groups", Arrays.asList("liquids", "gases", "hot"));

                cfg.save(configFile);
                Plugin.getInstance().getLogger().info("Файл pipes.yml успешно сгенерирован!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadPipeTypes() {
        pipeTypes.clear();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        if (!cfg.contains("pipes")) return;
        
        for (String key : cfg.getConfigurationSection("pipes").getKeys(false)) {
            String path = "pipes." + key;
            String arm = cfg.getString(path + ".arm-model", "ia_interactables:pipe_arm");
            
            PipeType type = new PipeType(
                key,
                cfg.getString(path + ".place-item", "ia_interactables:pipe_item"),
                cfg.getString(path + ".center-model", "ia_interactables:pipe_center"),
                arm,
                cfg.getString(path + ".arm-up-model", arm),
                cfg.getString(path + ".arm-down-model", arm),
                cfg.getInt(path + ".carrying_capacity", 100),
                cfg.getInt(path + ".storage_capacity", 1000),
                cfg.getDouble(path + ".durability", 500.0),
                cfg.getDouble(path + ".pressure-limit", 20.0),
                cfg.getDouble(path + ".damage-multiplier", 2.0),
                cfg.getDouble(path + ".heal-rate", 0.5),
                cfg.getStringList(path + ".allowed-groups")
            );
            pipeTypes.put(key, type);
        }
    }

    public Collection<PipeType> getPipeTypes() { return pipeTypes.values(); }

    public void placePipe(Location loc, PipeType type) {
        PipeNode pipe = new PipeNode(loc, type, type.getDurability());
        pipes.put(loc, pipe);
        Plugin.getInstance().getFluidNetworkManager().addNode(pipe);
        savePipesAsync();

        updatePipeVisuals(loc);
        updateAdjacentPipes(loc);
    }

    public void removePipe(Location loc) {
        PipeNode pipe = pipes.remove(loc);
        if (pipe != null) {
            clearLeftoverPipes(loc);
            Plugin.getInstance().getFluidNetworkManager().removeNode(loc);
            savePipesAsync();
            updateAdjacentPipes(loc);
        }
    }

    public void explodePipe(PipeNode pipe) {
        Location loc = pipe.getLocation();
        loc.getWorld().createExplosion(loc, 0F, false);
        loc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
        
        pipes.remove(loc);
        clearLeftoverPipes(loc);
        Plugin.getInstance().getFluidNetworkManager().removeNode(loc);
        savePipesAsync();
        updateAdjacentPipes(loc);
    }

    public PipeNode getPipeAt(Location loc) { return pipes.get(loc); }
    public boolean isPipe(Location loc) { return pipes.containsKey(loc); }

    public void updateAdjacentPipes(Location center) {
        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Location adj = center.clone().add(face.getModX(), face.getModY(), face.getModZ());
            if (pipes.containsKey(adj)) {
                updatePipeVisuals(adj);
            }
        }
    }

    public void updatePipeVisuals(Location loc) {
        PipeNode pipe = pipes.get(loc);
        if (pipe == null) return;

        clearLeftoverPipes(loc);
        pipe.getModelEntities().clear();

        Set<BlockFace> connections = new HashSet<>();
        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : faces) {
            Location neighborLoc = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
            NetworkNode neighbor = Plugin.getInstance().getFluidNetworkManager().getNodeAt(neighborLoc);
            
            if (neighbor != null) {
                if (neighbor.getNodeType() == NetworkNode.NodeType.PIPE) {
                    connections.add(face);
                } else if (neighbor.getNodeType() == NetworkNode.NodeType.MACHINE) {
                    if (neighbor.getSideState(face.getOppositeFace()) != IOState.NONE) {
                        connections.add(face);
                    }
                }
            }
        }

        List<PipeModelMath.PipePart> parts = PipeModelMath.calculatePipeParts(connections, pipe.getPipeType());
        for (PipeModelMath.PipePart part : parts) {
            String spawnId = part.iaModelId;
            if (spawnId != null && spawnId.startsWith("ia-")) spawnId = spawnId.substring(3);
            if (spawnId != null && spawnId.contains(":")) spawnId = spawnId.split(":")[1]; 

            CustomFurniture cf = CustomFurniture.spawn(spawnId, loc.getBlock());
            if (cf != null && cf.getEntity() != null) {
                Entity e = cf.getEntity();
                if (e instanceof ArmorStand) {
                    ArmorStand as = (ArmorStand) e;
                    as.setRotation(part.yaw, 0f);
                    as.setHeadPose(new EulerAngle(Math.toRadians(part.pitch), 0, 0));
                } else {
                    e.setRotation(part.yaw, part.pitch);
                }
                pipe.getModelEntities().add(e.getUniqueId());
            }
        }
    }

    private void clearLeftoverPipes(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 0.1, 0.1, 0.1)) {
            if (!(e instanceof ArmorStand || e instanceof ItemDisplay)) continue;
            if (e.getLocation().getBlockX() != loc.getBlockX() || e.getLocation().getBlockY() != loc.getBlockY() || e.getLocation().getBlockZ() != loc.getBlockZ()) continue;

            CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
            if (cf != null) {
                String fullId = cf.getNamespacedID();
                String shortId = cf.getId();
                
                boolean isPipePart = false;
                for (PipeType type : pipeTypes.values()) {
                    if (isIAIdMatch(fullId, shortId, type.getCenterModel()) || 
                        isIAIdMatch(fullId, shortId, type.getArmModel()) ||
                        isIAIdMatch(fullId, shortId, type.getArmUpModel()) ||
                        isIAIdMatch(fullId, shortId, type.getArmDownModel())) {
                        isPipePart = true;
                        break;
                    }
                }
                if (isPipePart) cf.remove(false);
            }
        }
    }

    public boolean isIAIdMatch(String fullId, String shortId, String configString) {
        if (configString == null) return false;
        String target = configString.startsWith("ia-") ? configString.substring(3) : configString;
        if (target.contains(":")) target = target.split(":")[1];
        return target.equals(fullId) || target.equals(shortId);
    }

    private void loadPipesData() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        if (config.contains("locations")) {
            for (String s : config.getConfigurationSection("locations").getKeys(false)) {
                Location loc = stringToLocation(s);
                String data = config.getString("locations." + s);
                
                String typeId = data;
                double health = -1;
                if (data.contains(";")) {
                    String[] split = data.split(";");
                    typeId = split[0];
                    health = Double.parseDouble(split[1]);
                }

                PipeType type = pipeTypes.get(typeId);
                if (loc != null && type != null) {
                    if (health == -1) health = type.getDurability(); 
                    
                    PipeNode pipe = new PipeNode(loc, type, health);
                    pipes.put(loc, pipe);
                    Plugin.getInstance().getFluidNetworkManager().addNode(pipe);
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
            for (Location loc : pipes.keySet()) {
                clearLeftoverPipes(loc);
                updatePipeVisuals(loc);
            }
        }, 40L);
    }

    private void savePipesAsync() {
        Map<String, String> locs = new HashMap<>();
        for (Map.Entry<Location, PipeNode> entry : pipes.entrySet()) {
            locs.put(locationToString(entry.getKey()), entry.getValue().getPipeType().getId() + ";" + entry.getValue().getHealth());
        }
        Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, String> entry : locs.entrySet()) {
                config.set("locations." + entry.getKey(), entry.getValue());
            }
            try { config.save(dataFile); } catch (IOException ignored) {}
        });
    }

    private String locationToString(Location loc) { return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ(); }
    private Location stringToLocation(String s) {
        String[] p = s.split("_");
        if (p.length < 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }
}