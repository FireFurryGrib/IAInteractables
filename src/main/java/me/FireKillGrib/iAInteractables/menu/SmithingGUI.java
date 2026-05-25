package me.FireKillGrib.iAInteractables.menu;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.data.SmithingRecipe;
import me.FireKillGrib.iAInteractables.data.SmithingTable;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;
import java.util.HashMap;
import java.util.Map;

public class SmithingGUI {
    private final SmithingTable table;
    private final VirtualInventory inventory;
    private Gui gui;
    private SmithingRecipe currentRecipe = null;
    private final Map<Character, Integer> charToSlotMap = new HashMap<>();
    private int resultGuiSlotIndex = -1;

    public SmithingGUI(SmithingTable table) {
        this.table = table;
        this.inventory = new VirtualInventory(null, 3);
        charToSlotMap.put('T', 0);
        charToSlotMap.put('B', 1);
        charToSlotMap.put('A', 2);
    }

    public void open(Player player) {
        gui = createGui();
        inventory.setPostUpdateHandler(event -> updateResult());
        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color(table.getTitle())))
                .setGui(gui)
                .addCloseHandler(() -> {
                    for (int i = 0; i < 3; i++) {
                        ItemStack is = inventory.getItem(i);
                        if (is != null && !is.getType().isAir()) {
                            player.getInventory().addItem(is).values()
                                    .forEach(remain -> player.getWorld().dropItem(player.getLocation(), remain));
                        }
                    }
                })
                .build(player)
                .open();
    }

    private Gui createGui() {
        Gui.Builder.Normal guiBuilder = Gui.normal()
                .setStructure(table.getStructure().toArray(new String[0]));
        int guiSlotCounter = 0;
        for (String row : table.getStructure()) {
            String cleanRow = row.replace(" ", "");
            for (char c : cleanRow.toCharArray()) {
                if (c == 'X') {
                    guiBuilder.addIngredient('X', table.getFiller());
                } else if (c == 'R') {
                    guiBuilder.addIngredient('R', new ResultItem());
                    resultGuiSlotIndex = guiSlotCounter;
                } else if (charToSlotMap.containsKey(c)) {
                    guiBuilder.addIngredient(c, new SlotElement.InventorySlotElement(inventory, charToSlotMap.get(c)));
                }
                guiSlotCounter++;
            }
        }
        return guiBuilder.build();
    }

    private void updateResult() {
        currentRecipe = null;
        for (SmithingRecipe recipe : table.getRecipes()) {
            if (checkRecipe(recipe)) {
                currentRecipe = recipe;
                break;
            }
        }
        if (gui != null && resultGuiSlotIndex != -1) {
            gui.setItem(resultGuiSlotIndex, new ResultItem());
        }
    }

    private boolean checkRecipe(SmithingRecipe recipe) {
        if (!checkSlot(0, recipe.getTemplate())) return false;
        if (!checkSlot(1, recipe.getBase())) return false;
        if (!checkSlot(2, recipe.getAddition())) return false;
        return true;
    }

    private boolean checkSlot(int slotIndex, ItemStack required) {
        ItemStack current = inventory.getItem(slotIndex);
        if (required == null) {
            return current == null || current.getType().isAir();
        }
        if (current == null || current.getType().isAir()) {
            return false;
        }
        return isSameItem(current, required) && current.getAmount() >= required.getAmount();
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        CustomStack c1 = CustomStack.byItemStack(item1);
        CustomStack c2 = CustomStack.byItemStack(item2);
        if (c1 != null && c2 != null) {
            return c1.getNamespacedID().equals(c2.getNamespacedID());
        }
        return c1 == null && c2 == null && item1.getType() == item2.getType();
    }

    private class ResultItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            if (currentRecipe != null) {
                return new xyz.xenondevs.invui.item.builder.ItemBuilder(currentRecipe.getResult());
            }
            return new xyz.xenondevs.invui.item.builder.ItemBuilder(Material.AIR);
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
            if (currentRecipe == null) return;
            SmithingRecipe recipeToCraft = currentRecipe;
            ItemStack resultObj = recipeToCraft.getResult().clone();

            int crafts = 1;
            if (event.isShiftClick()) {
                crafts = calculateMaxCrafts();
                int space = getAvailableSpace(player, resultObj);
                int maxFit = space / resultObj.getAmount();
                crafts = Math.min(crafts, maxFit);
                
                if (crafts <= 0) return;

                ItemStack toGive = resultObj.clone();
                toGive.setAmount(resultObj.getAmount() * crafts);
                
                consumeItem(0, recipeToCraft.getTemplate(), crafts);
                consumeItem(1, recipeToCraft.getBase(), crafts); 
                consumeItem(2, recipeToCraft.getAddition(), crafts); 

                HashMap<Integer, ItemStack> left = player.getInventory().addItem(toGive);
                for (ItemStack drop : left.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            } else {
                ItemStack cursor = player.getItemOnCursor();
                if (cursor.getType() != Material.AIR) {
                    if (!isSameItem(cursor, resultObj)) return;
                    if (cursor.getAmount() + resultObj.getAmount() > resultObj.getMaxStackSize()) return;
                }
                
                consumeItem(0, recipeToCraft.getTemplate(), 1);
                consumeItem(1, recipeToCraft.getBase(), 1); 
                consumeItem(2, recipeToCraft.getAddition(), 1); 

                if (cursor.getType() == Material.AIR) {
                    player.setItemOnCursor(resultObj);
                } else {
                    cursor.setAmount(cursor.getAmount() + resultObj.getAmount());
                }
            }

            if (table.getEffects() != null && table.getEffects().getOnCraft() != null) {
                table.getEffects().getOnCraft().play(player, player.getLocation());
            }
            updateResult();
        }

        private int calculateMaxCrafts() {
            int max = Integer.MAX_VALUE;
            max = checkMaxForSlot(0, currentRecipe.getTemplate(), max);
            max = checkMaxForSlot(1, currentRecipe.getBase(), max);
            max = checkMaxForSlot(2, currentRecipe.getAddition(), max);
            return max;
        }

        private int checkMaxForSlot(int slot, ItemStack required, int currentMax) {
            if (required == null) return currentMax;
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) {
                return Math.min(currentMax, current.getAmount() / required.getAmount());
            }
            return 0;
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

        private void consumeItem(int slot, ItemStack required, int multiplier) {
            if (required == null) return;
            ItemStack current = inventory.getItem(slot);
            if (current != null) {
                int newAmount = current.getAmount() - (required.getAmount() * multiplier);
                if (newAmount <= 0) {
                    inventory.setItem(null, slot, null);
                } else {
                    ItemStack newItem = current.clone();
                    newItem.setAmount(newAmount);
                    inventory.setItem(null, slot, newItem);
                }
            }
        }
    }
}