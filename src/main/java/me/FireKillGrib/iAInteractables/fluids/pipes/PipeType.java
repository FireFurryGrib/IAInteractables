package me.FireKillGrib.iAInteractables.fluids.pipes;

import java.util.List;

public class PipeType {
    private final String id;
    private final String placeItem;
    private final String centerModel;
    private final String armModel;
    private final String armUpModel;
    private final String armDownModel;
    
    private final int carryingCapacity;
    private final int storageCapacity;
    
    private final double durability;
    private final double pressureLimit;
    private final double damageMultiplier;
    private final double healRate;
    private final List<String> allowedGroups;

    public PipeType(String id, String placeItem, String centerModel, String armModel, String armUpModel, String armDownModel, 
                    int carryingCapacity, int storageCapacity, double durability, double pressureLimit, 
                    double damageMultiplier, double healRate, List<String> allowedGroups) {
        this.id = id;
        this.placeItem = placeItem;
        this.centerModel = centerModel;
        this.armModel = armModel;
        this.armUpModel = armUpModel;
        this.armDownModel = armDownModel;
        this.carryingCapacity = carryingCapacity;
        this.storageCapacity = storageCapacity;
        this.durability = durability;
        this.pressureLimit = pressureLimit;
        this.damageMultiplier = damageMultiplier;
        this.healRate = healRate;
        this.allowedGroups = allowedGroups;
    }

    public String getId() { return id; }
    public String getPlaceItem() { return placeItem; }
    public String getCenterModel() { return centerModel; }
    public String getArmModel() { return armModel; }
    public String getArmUpModel() { return armUpModel; }
    public String getArmDownModel() { return armDownModel; }
    public int getCarryingCapacity() { return carryingCapacity; }
    public int getStorageCapacity() { return storageCapacity; }
    
    public double getDurability() { return durability; }
    public double getPressureLimit() { return pressureLimit; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getHealRate() { return healRate; }
    public List<String> getAllowedGroups() { return allowedGroups; }

    public boolean canTransport(me.FireKillGrib.iAInteractables.fluids.FluidType fluid) {
        if (fluid == null) return false;
        for (String group : allowedGroups) {
            if (fluid.isInGroup(group)) return true;
        }
        return false;
    }
}