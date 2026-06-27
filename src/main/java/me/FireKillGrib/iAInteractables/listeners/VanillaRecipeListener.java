package me.FireKillGrib.iAInteractables.listeners;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.FurnaceRecipe;
import me.FireKillGrib.iAInteractables.data.SmithingRecipe;
import me.FireKillGrib.iAInteractables.data.WorkbenchRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VanillaRecipeListener implements Listener {

    // HIGHEST приоритет заставит наш плагин выставить результат самым последним, блокируя другие плагины!
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        Recipe bukkitRecipe = inv.getRecipe();
        
        boolean isOurDummy = false;
        if (bukkitRecipe instanceof Keyed) {
            NamespacedKey key = ((Keyed) bukkitRecipe).getKey();
            if (key.getNamespace().equals(Plugin.getInstance().getName().toLowerCase()) && key.getKey().startsWith("v_wb_")) {
                isOurDummy = true;
            }
        }
        
        ItemStack[] matrix = inv.getMatrix();
        ItemStack[][] matGrid = getMatrixGrid(matrix);
        
        if (matGrid != null) {
            for (WorkbenchRecipe recipe : Plugin.getInstance().getVanillaRecipeManager().getWorkbenchRecipes()) {
                ItemStack[][] reqGrid = getGrid(recipe.getIngredients());
                if (reqGrid != null) {
                    int[] offset = getMatchOffset(matGrid, reqGrid);
                    if (offset != null) {
                        inv.setResult(recipe.getResult().clone());
                        return;
                    }
                }
            }
        }
        
        // Если фейковый рецепт сработал, но наша строгая проверка на кол-во/NBT провалилась - обнуляем
        if (isOurDummy) {
            inv.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        Player player = (Player) event.getWhoClicked();

        ItemStack[][] matGrid = getMatrixGrid(matrix);
        if (matGrid == null) return;

        for (WorkbenchRecipe recipe : Plugin.getInstance().getVanillaRecipeManager().getWorkbenchRecipes()) {
            ItemStack[][] reqGrid = getGrid(recipe.getIngredients());
            if (reqGrid != null) {
                int[] offset = getMatchOffset(matGrid, reqGrid);
                if (offset != null) {
                    event.setCancelled(true); 
                    
                    ItemStack result = recipe.getResult().clone();
                    int crafts = 1;

                    if (event.isShiftClick()) {
                        crafts = calculateMaxCrafts(matGrid, reqGrid, offset[0], offset[1]);
                        int space = getAvailableSpace(player, result);
                        int maxFit = space / result.getAmount();
                        crafts = Math.min(crafts, maxFit);

                        if (crafts <= 0) return;

                        consumeIngredients(inv, matrix, reqGrid, offset[0], offset[1], crafts);
                        
                        ItemStack toGive = result.clone();
                        toGive.setAmount(result.getAmount() * crafts);
                        HashMap<Integer, ItemStack> left = player.getInventory().addItem(toGive);
                        for (ItemStack drop : left.values()) {
                            player.getWorld().dropItem(player.getLocation(), drop);
                        }
                    } else {
                        ItemStack cursor = event.getCursor();
                        if (cursor != null && cursor.getType() != Material.AIR) {
                            if (!isSameItem(cursor, result)) return;
                            if (cursor.getAmount() + result.getAmount() > result.getMaxStackSize()) return;
                        }

                        consumeIngredients(inv, matrix, reqGrid, offset[0], offset[1], 1);

                        if (cursor == null || cursor.getType() == Material.AIR) {
                            event.getView().setCursor(result);
                        } else {
                            cursor.setAmount(cursor.getAmount() + result.getAmount());
                            event.getView().setCursor(cursor);
                        }
                    }
                    Bukkit.getScheduler().runTask(Plugin.getInstance(), player::updateInventory);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        for (FurnaceRecipe recipe : Plugin.getInstance().getVanillaRecipeManager().getFurnaceRecipes()) {
            for (Set<ItemStack> raws : recipe.getRaws().values()) {
                for (ItemStack raw : raws) {
                    if (isSameItem(source, raw)) {
                        event.setResult(recipe.getResult().clone());
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inv = event.getInventory();
        Recipe recipe = inv.getRecipe();

        boolean isOurDummy = false;
        if (recipe instanceof Keyed) {
            NamespacedKey key = ((Keyed) recipe).getKey();
            if (key.getNamespace().equals(Plugin.getInstance().getName().toLowerCase()) && key.getKey().startsWith("v_smithing_")) {
                isOurDummy = true;
            }
        }

        ItemStack template = inv.getItem(0);
        ItemStack base = inv.getItem(1);
        ItemStack addition = inv.getItem(2);

        for (SmithingRecipe customRecipe : Plugin.getInstance().getVanillaRecipeManager().getSmithingRecipes()) {
            if (isSameItem(template, customRecipe.getTemplate()) &&
                isSameItem(base, customRecipe.getBase()) &&
                isSameItem(addition, customRecipe.getAddition()) && 
                template != null && template.getAmount() >= customRecipe.getTemplate().getAmount() &&
                base != null && base.getAmount() >= customRecipe.getBase().getAmount() &&
                addition != null && addition.getAmount() >= customRecipe.getAddition().getAmount()) {
                
                event.setResult(customRecipe.getResult().clone());
                return;
            }
        }
        
        if (isOurDummy) {
            event.setResult(null);
        }
    }

    private ItemStack[][] getGrid(Map<Character, ItemStack> ingredients) {
        ItemStack[][] grid = new ItemStack[3][3];
        boolean empty = true;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                char key = (char) ('A' + (r * 3 + c));
                ItemStack req = ingredients.get(key);
                if (req == null) req = ingredients.get((char) ('0' + (r * 3 + c)));
                if (req != null && !req.getType().isAir()) {
                    grid[r][c] = req.clone();
                    empty = false;
                }
            }
        }
        return empty ? null : grid;
    }

    private ItemStack[][] getMatrixGrid(ItemStack[] matrix) {
        ItemStack[][] grid = new ItemStack[3][3];
        boolean empty = true;
        if (matrix.length == 9) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack item = matrix[r * 3 + c];
                    if (item != null && !item.getType().isAir()) {
                        grid[r][c] = item.clone();
                        empty = false;
                    }
                }
            }
        } else if (matrix.length == 4) {
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    ItemStack item = matrix[r * 2 + c];
                    if (item != null && !item.getType().isAir()) {
                        grid[r][c] = item.clone();
                        empty = false;
                    }
                }
            }
        } else {
            return null;
        }
        return empty ? null : grid;
    }

    private int[] getMatchOffset(ItemStack[][] matGrid, ItemStack[][] reqGrid) {
        for (int rowOffset = -2; rowOffset <= 2; rowOffset++) {
            for (int colOffset = -2; colOffset <= 2; colOffset++) {
                if (matchesWithOffset(matGrid, reqGrid, rowOffset, colOffset)) {
                    return new int[]{rowOffset, colOffset};
                }
            }
        }
        return null;
    }

    private boolean matchesWithOffset(ItemStack[][] matGrid, ItemStack[][] reqGrid, int rowOffset, int colOffset) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack req = reqGrid[r][c];
                int matR = r + rowOffset;
                int matC = c + colOffset;

                ItemStack matItem = null;
                if (matR >= 0 && matR < 3 && matC >= 0 && matC < 3) {
                    matItem = matGrid[matR][matC];
                }

                if (req != null) {
                    if (matItem == null) return false;
                    if (!isSameItem(matItem, req)) return false;
                    if (matItem.getAmount() < req.getAmount()) return false;
                } else {
                    if (matItem != null) return false;
                }
            }
        }
        return true;
    }

    private int calculateMaxCrafts(ItemStack[][] matGrid, ItemStack[][] reqGrid, int rowOffset, int colOffset) {
        int max = Integer.MAX_VALUE;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack req = reqGrid[r][c];
                if (req != null) {
                    ItemStack matItem = matGrid[r + rowOffset][c + colOffset];
                    max = Math.min(max, matItem.getAmount() / req.getAmount());
                }
            }
        }
        return max;
    }

    private void consumeIngredients(CraftingInventory inv, ItemStack[] matrix, ItemStack[][] reqGrid, int rowOffset, int colOffset, int multiplier) {
        boolean is3x3 = matrix.length == 9;
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack req = reqGrid[r][c];
                if (req != null) {
                    int matR = r + rowOffset;
                    int matC = c + colOffset;
                    
                    int flatIndex = -1;
                    if (is3x3) {
                        flatIndex = matR * 3 + matC;
                    } else if (matR < 2 && matC < 2) {
                        flatIndex = matR * 2 + matC;
                    }
                    
                    if (flatIndex != -1 && flatIndex < matrix.length) {
                        ItemStack current = matrix[flatIndex];
                        if (current != null) {
                            int newAmt = current.getAmount() - (req.getAmount() * multiplier);
                            if (newAmt <= 0) {
                                matrix[flatIndex] = null;
                            } else {
                                current.setAmount(newAmt);
                                matrix[flatIndex] = current;
                            }
                        }
                    }
                }
            }
        }
        inv.setMatrix(matrix);
    }

    private int getAvailableSpace(Player player, ItemStack item) {
        int space = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (isSameItem(invItem, item)) {
                space += Math.max(0, item.getMaxStackSize() - invItem.getAmount());
            }
        }
        return space;
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) return true;
        if (item1 == null || item2 == null) return false;
        CustomStack c1 = CustomStack.byItemStack(item1);
        CustomStack c2 = CustomStack.byItemStack(item2);
        if (c1 != null && c2 != null) {
            return c1.getNamespacedID().equals(c2.getNamespacedID());
        }
        return c1 == null && c2 == null && item1.isSimilar(item2);
    }
}