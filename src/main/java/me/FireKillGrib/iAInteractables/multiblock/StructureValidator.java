package me.FireKillGrib.iAInteractables.multiblock;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.Map;

public class StructureValidator {

    public static boolean validate(MultiblockTemplate template, Location realCore, int rotationDegrees) {
        // Проверяем блоки
        for (Map.Entry<Vector, MultiblockTemplate.BlockData> entry : template.getBlocks().entrySet()) {
            Vector rotatedOffset = rotateVector(entry.getKey(), rotationDegrees);
            Location targetLoc = realCore.clone().add(rotatedOffset);
            Block actualBlock = targetLoc.getBlock();
            MultiblockTemplate.BlockData required = entry.getValue();

            if (required.isStructureVoid()) {
                if (actualBlock.getType() != Material.AIR) {
                    CustomBlock cb = CustomBlock.byAlreadyPlaced(actualBlock);
                    String id = cb != null ? "ia-" + cb.getNamespacedID() : actualBlock.getType().name();
                    if (!template.isValidVoidException(id)) return false;
                }
                continue;
            }

            if (actualBlock.getType() != required.getMaterial()) return false;

            if (required.getCustomId() != null) {
                CustomBlock cb = CustomBlock.byAlreadyPlaced(actualBlock);
                if (cb == null || !cb.getNamespacedID().equals(required.getCustomId())) return false;
            }
        }

        // Проверяем фурнитуру
        for (Map.Entry<Vector, MultiblockTemplate.FurnitureData> entry : template.getFurniture().entrySet()) {
            Vector rotatedOffset = rotateVector(entry.getKey(), rotationDegrees);
            Location targetLoc = realCore.clone().add(rotatedOffset);

            if (!hasFurniture(targetLoc, entry.getValue().getFurnitureId())) {
                return false;
            }
        }

        return true;
    }

    public static void snapFurniture(MultiblockTemplate template, Location realCore, int rotationDegrees) {
        for (Map.Entry<Vector, MultiblockTemplate.FurnitureData> entry : template.getFurniture().entrySet()) {
            Vector rotatedOffset = rotateVector(entry.getKey(), rotationDegrees);
            Location targetLoc = realCore.clone().add(rotatedOffset);
            
            float targetYaw = (entry.getValue().getYaw() + rotationDegrees) % 360;

            for (Entity e : targetLoc.getWorld().getNearbyEntities(targetLoc.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
                if (e instanceof ArmorStand || e instanceof ItemDisplay) {
                    CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                    if (cf != null && cf.getNamespacedID().equals(entry.getValue().getFurnitureId())) {
                        e.setRotation(targetYaw, e.getLocation().getPitch());
                    }
                }
            }
        }
    }

    public static Vector rotateVector(Vector v, int degrees) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();

        if (degrees == 90) return new Vector(-z, y, x);
        if (degrees == 180) return new Vector(-x, y, -z);
        if (degrees == 270) return new Vector(z, y, -x);
        
        return new Vector(x, y, z);
    }

    private static boolean hasFurniture(Location loc, String requiredId) {
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof ArmorStand || e instanceof ItemDisplay) {
                CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                if (cf != null && cf.getNamespacedID().equals(requiredId)) return true;
            }
        }
        return false;
    }
}