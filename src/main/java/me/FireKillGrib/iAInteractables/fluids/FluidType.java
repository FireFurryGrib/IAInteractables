package me.FireKillGrib.iAInteractables.fluids;

import org.bukkit.Material;
import java.util.List;

public class FluidType {
    private final String id;
    private final String displayName;
    private final boolean isGas;
    private final double density; 
    private final double compressionLimit; 
    private final List<String> groups;
    
    // Визуальные параметры для GUI
    private final String chatColor;
    private final Material glassMaterial;

    public FluidType(String id, String displayName, boolean isGas, double density, double compressionLimit, List<String> groups, String chatColor, Material glassMaterial) {
        this.id = id;
        this.displayName = displayName;
        this.isGas = isGas;
        this.density = density;
        this.compressionLimit = compressionLimit;
        this.groups = groups;
        this.chatColor = chatColor;
        this.glassMaterial = glassMaterial;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isGas() { return isGas; }
    public double getDensity() { return density; }
    public double getCompressionLimit() { return compressionLimit; }
    public List<String> getGroups() { return groups; }
    public String getChatColor() { return chatColor; }
    public Material getGlassMaterial() { return glassMaterial; }

    public boolean isInGroup(String group) {
        return groups.contains(group);
    }
}