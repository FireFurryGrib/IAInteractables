package me.FireKillGrib.iAInteractables.fluids;

import java.util.HashMap;
import java.util.Map;

public class FluidTank {
    private final Map<String, Integer> fluids = new HashMap<>(); // Поддержка солянок
    private final int capacityLn;
    private String lockedFluidId; 

    public FluidTank(int capacityLn) {
        this.capacityLn = capacityLn;
    }

    public void setLockedFluid(String fluidId) { this.lockedFluidId = fluidId; }
    
    public int getTotalAmount() {
        int total = 0;
        for (Integer amount : fluids.values()) {
            if (amount != null) total += amount;
        }
        return total;
    }

    public int getFreeSpaceLn() {
        return capacityLn - getTotalAmount();
    }

    public boolean canAccept(String incomingFluidId) {
        if (lockedFluidId != null && !lockedFluidId.equals(incomingFluidId)) return false;
        return getFreeSpaceLn() > 0;
    }

    public int fill(FluidStack incoming) {
        if (!canAccept(incoming.getFluidId())) return 0;
        int fillAmount = Math.min(getFreeSpaceLn(), incoming.getAmountLn());
        if (fillAmount > 0) {
            fluids.put(incoming.getFluidId(), fluids.getOrDefault(incoming.getFluidId(), 0) + fillAmount);
        }
        return fillAmount;
    }

    public FluidStack drain(String fluidId, int maxExtractLn) {
        int current = fluids.getOrDefault(fluidId, 0);
        if (current <= 0) return null;
        
        int extractAmount = Math.min(current, maxExtractLn);
        fluids.put(fluidId, current - extractAmount);
        if (fluids.get(fluidId) <= 0) fluids.remove(fluidId);
        
        return new FluidStack(fluidId, extractAmount);
    }

    public FluidStack drainAny(int maxExtractLn) {
        if (fluids.isEmpty()) return null;
        String id = fluids.keySet().iterator().next();
        return drain(id, maxExtractLn);
    }

    public Map<String, Integer> getContents() { return new HashMap<>(fluids); }

    // ИСПРАВЛЕНИЕ ОШИБКИ: Теперь метод просто "смотрит" жидкость, а не пытается выкачать 0
    public FluidStack getFluid() {
        if (fluids.isEmpty()) return null;
        String id = fluids.keySet().iterator().next();
        return new FluidStack(id, fluids.get(id));
    }
}