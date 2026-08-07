package me.FireKillGrib.iAInteractables.fluids.pipes;

import me.FireKillGrib.iAInteractables.fluids.FluidTank;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PipeNode implements NetworkNode {
    private final Location location;
    private PipeType pipeType; 
    private final List<UUID> modelEntities = new ArrayList<>();
    private double health;

    public PipeNode(Location location, PipeType pipeType, double health) {
        this.location = location;
        this.pipeType = pipeType;
        this.health = health;
    }

    public PipeType getPipeType() { return pipeType; }
    public void setPipeType(PipeType pipeType) { this.pipeType = pipeType; } 
    public List<UUID> getModelEntities() { return modelEntities; }
    
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
    public void damage(double amount) { this.health -= amount; }
    public void heal(double amount) { this.health = Math.min(pipeType.getDurability(), this.health + amount); }

    @Override public Location getLocation() { return location; }
    @Override public NodeType getNodeType() { return NodeType.PIPE; }
    @Override public String getMachineId() { return "pipe:" + pipeType.getId(); }
    
    @Override public FluidTank getTank() { return null; }
    @Override public IOState getSideState(BlockFace worldFace) { return IOState.INPUT; }
    @Override public void setSideState(BlockFace worldFace, IOState state) {}
    @Override public IOState getAdminLock(BlockFace worldFace) { return null; }
    @Override public void setAdminLock(BlockFace worldFace, IOState state) {}
    
    @Override public void markDirty() {} // Трубе не нужно сохранять состояние жидкости
}