package me.FireKillGrib.iAInteractables.fluids;

public class FluidStack {
    private final String fluidId;
    private int amountLn;

    public FluidStack(String fluidId, int amountLn) {
        this.fluidId = fluidId;
        this.amountLn = amountLn;
    }

    public String getFluidId() { return fluidId; }
    public int getAmountLn() { return amountLn; }
    public void setAmountLn(int amountLn) { this.amountLn = amountLn; }
    public void add(int amount) { this.amountLn += amount; }
    
    public FluidStack clone() { return new FluidStack(fluidId, amountLn); }
}