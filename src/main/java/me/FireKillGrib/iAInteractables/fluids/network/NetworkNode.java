package me.FireKillGrib.iAInteractables.fluids.network;

import me.FireKillGrib.iAInteractables.fluids.FluidTank;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface NetworkNode {
    Location getLocation();
    NodeType getNodeType();
    String getMachineId(); 
    
    FluidTank getTank();
    
    IOState getSideState(BlockFace worldFace);
    void setSideState(BlockFace worldFace, IOState state);
    
    IOState getAdminLock(BlockFace worldFace);
    void setAdminLock(BlockFace worldFace, IOState state);

    // НОВЫЙ МЕТОД: Уведомляет механизм о том, что сеть забрала или влила в него жидкость
    void markDirty();

    enum NodeType {
        PIPE, MACHINE
    }
}