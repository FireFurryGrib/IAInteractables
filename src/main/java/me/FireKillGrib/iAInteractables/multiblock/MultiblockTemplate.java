package me.FireKillGrib.iAInteractables.multiblock;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockTemplate {
    private final String name;
    private final Map<Vector, BlockData> blocks;
    private final Map<Vector, FurnitureData> furniture;
    private final List<String> voidExceptions;

    public MultiblockTemplate(String name) {
        this.name = name;
        this.blocks = new HashMap<>();
        this.furniture = new HashMap<>();
        this.voidExceptions = new ArrayList<>();
    }

    public void addBlock(Vector offset, Material material, String customId) {
        blocks.put(offset, new BlockData(material, customId));
    }

    public void addFurniture(Vector offset, String furnitureId, float yaw) {
        furniture.put(offset, new FurnitureData(furnitureId, yaw));
    }

    public void setVoidExceptions(List<String> exceptions) {
        this.voidExceptions.addAll(exceptions);
    }

    public boolean isValidVoidException(String materialOrId) {
        return voidExceptions.contains(materialOrId);
    }

    public String getName() { return name; }
    public Map<Vector, BlockData> getBlocks() { return blocks; }
    public Map<Vector, FurnitureData> getFurniture() { return furniture; }
    public List<String> getVoidExceptions() { return voidExceptions; }

    public static class BlockData {
        private final Material material;
        private final String customId;
        private final boolean isStructureVoid;

        public BlockData(Material material, String customId) {
            this.material = material;
            this.customId = customId;
            this.isStructureVoid = (material == Material.STRUCTURE_VOID);
        }

        public Material getMaterial() { return material; }
        public String getCustomId() { return customId; }
        public boolean isStructureVoid() { return isStructureVoid; }
    }

    public static class FurnitureData {
        private final String furnitureId;
        private final float yaw;

        public FurnitureData(String furnitureId, float yaw) {
            this.furnitureId = furnitureId;
            this.yaw = yaw;
        }

        public String getFurnitureId() { return furnitureId; }
        public float getYaw() { return yaw; }
    }
}