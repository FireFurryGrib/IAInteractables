package me.FireKillGrib.iAInteractables.fluids.pipes;

import org.bukkit.block.BlockFace;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PipeModelMath {

    public static class PipePart {
        public final String iaModelId;
        public final float yaw;
        public final float pitch;

        public PipePart(String iaModelId, float yaw, float pitch) {
            this.iaModelId = iaModelId;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static List<PipePart> calculatePipeParts(Set<BlockFace> connections, PipeType type) {
        List<PipePart> parts = new ArrayList<>();

        parts.add(new PipePart(type.getCenterModel(), 0f, 0f));

        for (BlockFace face : connections) {
            switch (face) {
                case NORTH: parts.add(new PipePart(type.getArmModel(), 180f, 0f)); break;
                case SOUTH: parts.add(new PipePart(type.getArmModel(), 0f, 0f)); break;
                case WEST: parts.add(new PipePart(type.getArmModel(), 90f, 0f)); break;
                case EAST: parts.add(new PipePart(type.getArmModel(), -90f, 0f)); break;
                case UP: 
                    // Если вертикальная модель совпадает с горизонтальной, делаем наклон (старый кривой метод)
                    // Если указана другая (вертикальная) модель - спавним её прямо!
                    if (type.getArmUpModel().equals(type.getArmModel())) {
                        parts.add(new PipePart(type.getArmModel(), 0f, -90f));
                    } else {
                        parts.add(new PipePart(type.getArmUpModel(), 0f, 0f));
                    }
                    break;
                case DOWN: 
                    if (type.getArmDownModel().equals(type.getArmModel())) {
                        parts.add(new PipePart(type.getArmModel(), 0f, 90f));
                    } else {
                        parts.add(new PipePart(type.getArmDownModel(), 0f, 0f));
                    }
                    break;
                default: break;
            }
        }
        return parts;
    }
}