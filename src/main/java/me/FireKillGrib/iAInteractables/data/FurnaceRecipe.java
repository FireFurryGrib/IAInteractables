package me.FireKillGrib.iAInteractables.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Getter
public class FurnaceRecipe {
    private final ItemStack result;
    private final int cookTimeTicks;
    private final Map<Character, Set<ItemStack>> raws;
    private final Map<Character, Set<ItemStack>> fuels;
    private final Map<String, Integer> fluidRaws;
    private final CoolingRequirement cooling;

    @AllArgsConstructor
    @Getter
    public static class CoolingRequirement {
        private final int totalAmountNeededLn; // Сколько всего нужно
        private final int explodeTimerTicks;   // Таймер до взрыва без охлаждения
        private final List<String> coolantGroups; // Разрешенные группы (e.g. water_based)
    }

    public int getCookTimeTicks() {
        return cookTimeTicks;
    }
    public boolean check(Map<Character, ItemStack> current) {
        for (Map.Entry<Character, Set<ItemStack>> entry : raws.entrySet()) {
            char slot = entry.getKey();
            Set<ItemStack> acceptedItems = entry.getValue();
            ItemStack currentItem = current.get(slot);
            if (currentItem == null || currentItem.getAmount() == 0) {
                return false;
            }
            boolean matches = false;
            for (ItemStack accepted : acceptedItems) {
                if (currentItem.isSimilar(accepted)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) return false;
        }
        for (Map.Entry<Character, Set<ItemStack>> entry : fuels.entrySet()) {
            char slot = entry.getKey();
            Set<ItemStack> acceptedItems = entry.getValue();
            ItemStack currentItem = current.get(slot);
            if (currentItem == null || currentItem.getAmount() == 0) {
                return false;
            }
            boolean matches = false;
            for (ItemStack accepted : acceptedItems) {
                if (currentItem.isSimilar(accepted)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) return false;
        }
        return true;
    }
}
