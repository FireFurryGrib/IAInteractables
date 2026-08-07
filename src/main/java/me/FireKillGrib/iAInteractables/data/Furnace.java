package me.FireKillGrib.iAInteractables.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
public class Furnace {
    private final String name;
    private final String title;
    private final String namespacedID; 
    private final List<String> structure;
    private final ItemProvider filler;
    private final Set<FurnaceRecipe> recipes;
    private final FurnaceEffects effects;
    private final ProgressBarConfig progressBar;
    private final List<String> specialFunctions; 
    
    // Новая настройка для помпы
    private final PumpSettings pumpSettings;

    @AllArgsConstructor
    @Getter
    public static class PumpSettings {
        private final String targetFluid;
        private final String targetBlock;
        private final int requiredBlocks;
        private final int pumpAmountLn;
        private final int pumpIntervalTicks;
        private final char fuelSlot;
        private final int fuelAmount;
    }
}