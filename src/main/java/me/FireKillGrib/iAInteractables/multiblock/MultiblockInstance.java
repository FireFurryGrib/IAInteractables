package me.FireKillGrib.iAInteractables.multiblock;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.FluidMath;
import me.FireKillGrib.iAInteractables.fluids.FluidTank;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import me.FireKillGrib.iAInteractables.utils.RotationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

public class MultiblockInstance {
    private final String templateName;
    private final Location coreLocation;
    private final Set<Location> allElements; 
    private final int rotation;
    private final Date createdAt;

    private final Map<Location, Map<BlockFace, IOState>> portConfigs = new HashMap<>();
    private final FluidTank primaryTank = new FluidTank(FluidMath.LN_PER_BUCKET * 50); 

    public MultiblockInstance(String templateName, Location coreLocation, int rotation) {
        this.templateName = templateName;
        this.coreLocation = new Location(coreLocation.getWorld(), coreLocation.getBlockX(), coreLocation.getBlockY(), coreLocation.getBlockZ());
        this.rotation = rotation;
        this.allElements = new HashSet<>();
        this.createdAt = new Date();
        this.allElements.add(this.coreLocation);
    }

    public void addBlock(Location loc) { allElements.add(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())); }
    public void addFurnitureLocation(Location loc) { allElements.add(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())); }

    public String getTemplateName() { return templateName; }
    public Location getCoreLocation() { return coreLocation; }
    public int getRotation() { return rotation; }
    public Date getCreatedAt() { return createdAt; }
    public Set<Location> getAllElements() { return allElements; }

    public boolean isActive() { return true; }
    public int getElementsCount() { return allElements.size(); }

    public boolean containsLocation(Location loc) {
        Location norm = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return allElements.contains(norm);
    }

    public IOState getPortState(Location loc, BlockFace relativeFace) {
        return portConfigs.getOrDefault(loc, Collections.emptyMap()).getOrDefault(relativeFace, IOState.NONE);
    }

    public void setPortState(Location loc, BlockFace relativeFace, IOState state) {
        portConfigs.computeIfAbsent(loc, k -> new HashMap<>()).put(relativeFace, state);
    }

    public class MultiblockPortNode implements NetworkNode {
        private final Location location;
        public MultiblockPortNode(Location location) { this.location = location; }
        
        private BlockFace getRelFace(BlockFace worldFace) {
            return RotationUtil.unrotateFace(worldFace, rotation);
        }
        
        @Override public Location getLocation() { return location; }
        @Override public NodeType getNodeType() { return NodeType.MACHINE; }
        
        @Override public String getMachineId() { 
            Vector offset = new Vector(location.getBlockX() - coreLocation.getBlockX(), location.getBlockY() - coreLocation.getBlockY(), location.getBlockZ() - coreLocation.getBlockZ());
            Vector unrot = RotationUtil.rotateVector(offset, -rotation);
            return "multiblock:" + templateName + ":" + unrot.getBlockX() + "_" + unrot.getBlockY() + "_" + unrot.getBlockZ(); 
        }
        
        @Override public FluidTank getTank() { return primaryTank; }
        
        @Override public IOState getAdminLock(BlockFace worldFace) {
            return Plugin.getInstance().getAdminLockManager().getLockedSide(getMachineId(), getRelFace(worldFace));
        }
        
        @Override public void setAdminLock(BlockFace worldFace, IOState state) {
            Plugin.getInstance().getAdminLockManager().setLockedSide(getMachineId(), getRelFace(worldFace), state);
        }
        
        @Override public IOState getSideState(BlockFace worldFace) { 
            IOState lock = getAdminLock(worldFace);
            if (lock != null) return lock;
            return getPortState(location, getRelFace(worldFace)); 
        }
        
        @Override public void setSideState(BlockFace worldFace, IOState state) { 
            setPortState(location, getRelFace(worldFace), state); 
            if (Plugin.getInstance().getFluidNetworkManager() != null) Plugin.getInstance().getFluidNetworkManager().recalculateNetworks();
        }
        @Override 
        public void markDirty() { 
            Plugin.getInstance().getMultiblockManager().saveActiveStructure(MultiblockInstance.this);
        }
    }

    public NetworkNode getPortNode(Location loc) {
        return new MultiblockPortNode(loc);
    }
    
}