package me.FireKillGrib.iAInteractables.multiblock;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import me.FireKillGrib.iAInteractables.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class TemplateScanner {

    public static MultiblockTemplate scan(String name, Block coreBlock) {
        String markerConfig = Plugin.getInstance().getConfig().getString("multiblock.marker-block", "GLOWSTONE");
        List<Location> markers = findMarkers(coreLocation(coreBlock), markerConfig);

        if (markers.size() != 8) {
            throw new IllegalStateException("Found " + markers.size() + " markers instead of 8!");
        }

        int minX = markers.get(0).getBlockX();
        int minY = markers.get(0).getBlockY();
        int minZ = markers.get(0).getBlockZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (Location loc : markers) {
            if (loc.getBlockX() < minX) minX = loc.getBlockX();
            if (loc.getBlockY() < minY) minY = loc.getBlockY();
            if (loc.getBlockZ() < minZ) minZ = loc.getBlockZ();
            if (loc.getBlockX() > maxX) maxX = loc.getBlockX();
            if (loc.getBlockY() > maxY) maxY = loc.getBlockY();
            if (loc.getBlockZ() > maxZ) maxZ = loc.getBlockZ();
        }

        MultiblockTemplate template = new MultiblockTemplate(name);
        Location coreLoc = coreBlock.getLocation();
        World world = coreLoc.getWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    
                    if (isMarker(loc.getBlock(), markerConfig)) continue;

                    Vector offset = new Vector(x - coreLoc.getBlockX(), y - coreLoc.getBlockY(), z - coreLoc.getBlockZ());

                    if (loc.getBlock().getType() == Material.AIR) {
                        continue; 
                    }

                    if (loc.getBlock().getType() == Material.STRUCTURE_VOID) {
                        template.addBlock(offset, Material.STRUCTURE_VOID, null);
                        continue;
                    }

                    CustomFurniture furniture = getFurnitureAt(loc);
                    if (furniture != null) {
                        float yaw = getFurnitureYawAt(loc);
                        template.addFurniture(offset, furniture.getNamespacedID(), yaw);
                        continue;
                    }

                    CustomBlock cb = CustomBlock.byAlreadyPlaced(loc.getBlock());
                    if (cb != null) {
                        template.addBlock(offset, loc.getBlock().getType(), cb.getNamespacedID());
                    } else {
                        template.addBlock(offset, loc.getBlock().getType(), null);
                    }
                }
            }
        }
        return template;
    }

    private static List<Location> findMarkers(Location center, String markerConfig) {
        List<Location> markers = new ArrayList<>();
        World world = center.getWorld();
        int radius = 15; 

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (isMarker(b, markerConfig)) {
                        markers.add(b.getLocation());
                    }
                }
            }
        }
        return markers;
    }

    private static boolean isMarker(Block block, String config) {
        if (config.startsWith("ia-")) {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb == null) return false;
            String targetId = config.substring(3);
            return cb.getNamespacedID().equals(targetId) || cb.getId().equals(targetId);
        }
        return block.getType().name().equalsIgnoreCase(config);
    }

    private static CustomFurniture getFurnitureAt(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof ArmorStand || e instanceof ItemDisplay) {
                CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                if (cf != null) return cf;
            }
        }
        return null;
    }

    private static float getFurnitureYawAt(Location loc) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof ArmorStand || e instanceof ItemDisplay) {
                if (CustomFurniture.byAlreadySpawned(e) != null) return e.getLocation().getYaw();
            }
        }
        return 0f;
    }

    private static Location coreLocation(Block block) {
        return block.getLocation();
    }
}