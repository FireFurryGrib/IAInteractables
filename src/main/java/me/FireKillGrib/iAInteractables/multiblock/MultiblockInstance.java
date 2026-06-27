package me.FireKillGrib.iAInteractables.multiblock;

import org.bukkit.Location;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MultiblockInstance {
    private final String templateName;
    private final Location coreLocation;
    private final Set<Location> allElements; 
    private final int rotation;
    private final Date createdAt;

    public MultiblockInstance(String templateName, Location coreLocation, int rotation) {
        this.templateName = templateName;
        this.coreLocation = new Location(coreLocation.getWorld(), coreLocation.getBlockX(), coreLocation.getBlockY(), coreLocation.getBlockZ());
        this.rotation = rotation;
        this.allElements = new HashSet<>();
        this.createdAt = new Date();
        this.allElements.add(this.coreLocation);
    }

    public void addBlock(Location loc) { 
        allElements.add(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())); 
    }
    
    public void addFurnitureLocation(Location loc) { 
        allElements.add(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())); 
    }

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
}