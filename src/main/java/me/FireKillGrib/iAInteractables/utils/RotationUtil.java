package me.FireKillGrib.iAInteractables.utils;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class RotationUtil {
    
    public static Vector rotateVector(Vector v, int degrees) {
        int d = (degrees % 360 + 360) % 360;
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        
        if (d == 90) return new Vector(-z, y, x);
        if (d == 180) return new Vector(-x, y, -z);
        if (d == 270) return new Vector(z, y, -x);
        return new Vector(x, y, z);
    }

    public static BlockFace rotateFace(BlockFace face, int degrees) {
        int d = (degrees % 360 + 360) % 360;
        if (d == 0 || face == BlockFace.UP || face == BlockFace.DOWN) return face;
        
        int steps = d / 90;
        BlockFace[] order = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        int idx = -1;
        for (int i = 0; i < 4; i++) {
            if (order[i] == face) idx = i;
        }
        if (idx == -1) return face; 
        
        return order[(idx + steps) % 4];
    }

    // Обратное вращение (полезно для получения "оригинальной" стороны из шаблона)
    public static BlockFace unrotateFace(BlockFace face, int degrees) {
        return rotateFace(face, -degrees);
    }
}