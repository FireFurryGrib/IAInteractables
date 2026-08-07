package me.FireKillGrib.iAInteractables.managers;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.*;
import me.FireKillGrib.iAInteractables.fluids.FluidMath;
import me.FireKillGrib.iAInteractables.fluids.FluidStack;
import me.FireKillGrib.iAInteractables.fluids.FluidTank;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import me.FireKillGrib.iAInteractables.utils.RotationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import java.util.*;

public class FurnaceController implements NetworkNode {
    private final Furnace furnace;
    private final Location location;
    private final VirtualInventory inventory;
    private final Map<Character, Integer> structure;
    private final List<Integer> inputSlots = new ArrayList<>();
    
    private BukkitTask cookingTask;
    private BukkitTask idleTask; 
    
    private boolean isCooking = false;
    private FurnaceRecipe currentRecipe;
    private int cookingProgress = 0;
    private boolean isLoading = true; 
    
    private boolean isAutomated = false;
    private final Set<Integer> blockedSlots = new HashSet<>();
    private boolean wasPowered = false;
    private int hopperTick = 0;

    private final FluidTank tank;
    private final Map<BlockFace, IOState> sideConfig = new HashMap<>();

    private int resultSlot = -1;
    private int bucketSlot = -1;

    private ItemStack lastBucketItem = null;

    private int pumpTick = 0;
    private int uncooledTicks = 0;

    public FurnaceController(Furnace furnace, Location location) {
        this.furnace = furnace;
        this.location = location;
        this.inventory = new VirtualInventory(null, 54);
        this.structure = new HashMap<>();
        this.tank = new FluidTank(FluidMath.LN_PER_BUCKET * 10); 
        
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            sideConfig.put(face, IOState.NONE);
        }
        
        parseStructure();
        loadData();
        isLoading = false; 
        
        inventory.setPostUpdateHandler(event -> {
            if (isLoading) return; 
            if (!isAutomated && !isCooking) checkAndStartCooking();
            saveAsync();
        });

        idleTask = Bukkit.getScheduler().runTaskTimer(Plugin.getInstance(), this::tick, 0L, 1L);
        
        if (Plugin.getInstance().getFluidNetworkManager() != null) {
            Plugin.getInstance().getFluidNetworkManager().addNode(this);
        }
        
        if (!isAutomated) checkAndStartCooking();
        Plugin.getInstance().getPipeManager().updateAdjacentPipes(location);
    }

    public int getResultSlot() { return resultSlot; }
    public int getBucketSlot() { return bucketSlot; }

    // --- МАТЕМАТИКА ОРИЕНТАЦИИ ПЕЧКИ ---
    private int getRotationDegrees() {
        for (Entity e : location.getWorld().getNearbyEntities(location.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof ArmorStand || e instanceof ItemDisplay) {
                CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                if (cf != null && cf.getNamespacedID().equals(furnace.getNamespacedID())) {
                    return (int) Math.round(e.getLocation().getYaw() / 90.0) * 90 % 360;
                }
            }
        }
        return 0;
    }

    private BlockFace getRelFace(BlockFace worldFace) {
        return RotationUtil.unrotateFace(worldFace, getRotationDegrees());
    }

    // --- NETWORK NODE ---
    @Override public Location getLocation() { return location; }
    @Override public NodeType getNodeType() { return NodeType.MACHINE; }
    @Override public String getMachineId() { return "furnace:" + furnace.getName(); }
    @Override public FluidTank getTank() { return tank; }
    
    @Override public IOState getAdminLock(BlockFace worldFace) {
        return Plugin.getInstance().getAdminLockManager().getLockedSide(getMachineId(), getRelFace(worldFace));
    }
    
    @Override public void setAdminLock(BlockFace worldFace, IOState state) {
        Plugin.getInstance().getAdminLockManager().setLockedSide(getMachineId(), getRelFace(worldFace), state);
    }
    
    @Override public IOState getSideState(BlockFace worldFace) { 
        IOState locked = getAdminLock(worldFace);
        if (locked != null) return locked;
        return sideConfig.getOrDefault(getRelFace(worldFace), IOState.NONE); 
    }
    
    @Override public void setSideState(BlockFace worldFace, IOState state) { 
        sideConfig.put(getRelFace(worldFace), state);
        if (Plugin.getInstance().getFluidNetworkManager() != null) Plugin.getInstance().getFluidNetworkManager().recalculateNetworks();
        saveAsync();
    }

    public Map<BlockFace, IOState> getSideConfig() { return sideConfig; }
    public VirtualInventory getInventory() { return inventory; }
    public boolean isCooking() { return isCooking; }
    public FurnaceRecipe getCurrentRecipe() { return currentRecipe; }
    public int getCookingProgress() { return cookingProgress; }
    public boolean isAutomated() { return isAutomated; }
    public Set<Integer> getBlockedSlots() { return blockedSlots; }

    public void setAutomated(boolean automated) {
        this.isAutomated = automated;
        saveAsync();
        if (!isAutomated && !isCooking) checkAndStartCooking();
    }

    public void toggleBlockedSlot(int slot) {
        if (blockedSlots.contains(slot)) blockedSlots.remove(slot);
        else blockedSlots.add(slot);
        saveAsync();
    }

    private void tick() {
        if (isLoading) return;

        boolean isPowered = location.getBlock().isBlockIndirectlyPowered() || location.getBlock().isBlockPowered();
        if (isPowered && !wasPowered) {
            if (isAutomated && !isCooking) checkAndStartCooking();
        }
        wasPowered = isPowered;

        processBucketSlot(); 
        
        // НОВАЯ ЛОГИКА ПОМПЫ
        if (furnace.getPumpSettings() != null) {
            pumpTick++;
            if (pumpTick >= furnace.getPumpSettings().getPumpIntervalTicks()) {
                pumpTick = 0;
                checkAndPumpFluid();
            }
        }

        hopperTick++;
        if (hopperTick >= 8) {
            hopperTick = 0;
            processHoppers();
        }
    }

    private void checkAndPumpFluid() {
        Furnace.PumpSettings p = furnace.getPumpSettings();
        if (tank.getFreeSpaceLn() < p.getPumpAmountLn()) return; // Нет места
        if (!tank.canAccept(p.getTargetFluid())) return; // Заблокировано другой жидкостью

        // Проверка топлива
        if (p.getFuelAmount() > 0) {
            Integer fSlot = structure.get(p.getFuelSlot());
            if (fSlot == null) return;
            ItemStack fuel = inventory.getItem(fSlot);
            if (fuel == null || fuel.getAmount() < p.getFuelAmount()) return;
        }

        // Асинхронный поиск блоков, чтобы не лагал сервер
        Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
            Set<Location> visited = new HashSet<>();
            Queue<Location> queue = new LinkedList<>();
            queue.add(location);
            
            int count = 0;
            String targetId = p.getTargetBlock().startsWith("ia-") ? p.getTargetBlock().substring(3) : p.getTargetBlock();
            boolean isIA = p.getTargetBlock().startsWith("ia-") || p.getTargetBlock().contains(":");
            
            while (!queue.isEmpty() && count < p.getRequiredBlocks()) {
                Location curr = queue.poll();
                for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP}) {
                    Location adj = curr.getBlock().getRelative(face).getLocation();
                    if (!visited.contains(adj)) {
                        visited.add(adj);
                        
                        // Защита от прогрузки чанков (чтобы асинхронно не крашнуть сервер)
                        if (!adj.isWorldLoaded() || !adj.getWorld().isChunkLoaded(adj.getBlockX() >> 4, adj.getBlockZ() >> 4)) continue;

                        boolean match = false;
                        if (isIA) {
                            dev.lone.itemsadder.api.CustomBlock cb = dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(adj.getBlock());
                            if (cb != null && cb.getNamespacedID().equals(targetId)) match = true;
                        } else {
                            if (adj.getBlock().getType().name().equalsIgnoreCase(targetId)) match = true;
                        }

                        if (match) {
                            count++;
                            queue.add(adj);
                        }
                    }
                }
            }

            // Если нашли нужное количество блоков
            if (count >= p.getRequiredBlocks()) {
                Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
                    // Снимаем топливо в главном потоке
                    if (p.getFuelAmount() > 0) {
                        Integer fSlot = structure.get(p.getFuelSlot());
                        if (fSlot == null) return;
                        ItemStack fuel = inventory.getItem(fSlot);
                        if (fuel == null || fuel.getAmount() < p.getFuelAmount()) return;
                        
                        fuel.setAmount(fuel.getAmount() - p.getFuelAmount());
                        inventory.setItem(null, fSlot, fuel.getAmount() <= 0 ? null : fuel);
                    }
                    
                    tank.fill(new FluidStack(p.getTargetFluid(), p.getPumpAmountLn()));
                    saveAsync();
                });
            }
        });
    }

    private void processBucketSlot() {
        if (bucketSlot == -1) return;
        ItemStack currentItem = inventory.getItem(bucketSlot);
        
        boolean isSame = false;
        if (currentItem == null && lastBucketItem == null) isSame = true;
        else if (currentItem != null && lastBucketItem != null && currentItem.isSimilar(lastBucketItem) && currentItem.getAmount() == lastBucketItem.getAmount()) isSame = true;

        if (isSame) return; 
        
        lastBucketItem = currentItem != null ? currentItem.clone() : null;
        if (currentItem == null || currentItem.getType().isAir()) return;

        if (currentItem.getAmount() > 1) return; 

        boolean changed = false;

        if (currentItem.getType() == Material.WATER_BUCKET && tank.getFreeSpaceLn() >= FluidMath.LN_PER_BUCKET) {
            if (tank.canAccept("minecraft:water")) {
                tank.fill(new FluidStack("minecraft:water", FluidMath.LN_PER_BUCKET));
                inventory.setItem(null, bucketSlot, new ItemStack(Material.BUCKET));
                changed = true;
            }
        } 
        else if (currentItem.getType() == Material.LAVA_BUCKET && tank.getFreeSpaceLn() >= FluidMath.LN_PER_BUCKET) {
            if (tank.canAccept("minecraft:lava")) {
                tank.fill(new FluidStack("minecraft:lava", FluidMath.LN_PER_BUCKET));
                inventory.setItem(null, bucketSlot, new ItemStack(Material.BUCKET));
                changed = true;
            }
        } 
        else if (currentItem.getType() == Material.BUCKET && tank.getFluid() != null && tank.getFluid().getAmountLn() >= FluidMath.LN_PER_BUCKET) {
            if (tank.getFluid().getFluidId().equals("minecraft:water")) {
                // ИСПРАВЛЕНИЕ ОШИБКИ
                tank.drain("minecraft:water", FluidMath.LN_PER_BUCKET);
                inventory.setItem(null, bucketSlot, new ItemStack(Material.WATER_BUCKET));
                changed = true;
            } else if (tank.getFluid().getFluidId().equals("minecraft:lava")) {
                // ИСПРАВЛЕНИЕ ОШИБКИ
                tank.drain("minecraft:lava", FluidMath.LN_PER_BUCKET);
                inventory.setItem(null, bucketSlot, new ItemStack(Material.LAVA_BUCKET));
                changed = true;
            }
        }

        if (changed) {
            lastBucketItem = inventory.getItem(bucketSlot) != null ? inventory.getItem(bucketSlot).clone() : null;
            saveAsync();
        }
    }

    private void processHoppers() {
        if (resultSlot != -1) {
            Block blockBelow = location.clone().add(0, -1, 0).getBlock();
            if (blockBelow.getType() == Material.HOPPER) {
                org.bukkit.block.BlockState state = blockBelow.getState();
                if (state instanceof org.bukkit.block.Hopper) {
                    org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) state;
                    ItemStack resultItem = inventory.getItem(resultSlot);
                    if (resultItem != null && !resultItem.getType().isAir()) {
                        ItemStack toPush = resultItem.clone();
                        toPush.setAmount(1);
                        HashMap<Integer, ItemStack> leftover = hopper.getInventory().addItem(toPush);
                        if (leftover.isEmpty()) {
                            resultItem.setAmount(resultItem.getAmount() - 1);
                            inventory.setItem(null, resultSlot, resultItem.getAmount() <= 0 ? null : resultItem);
                            hopper.update(); 
                        }
                    }
                }
            }
        }

        BlockFace[] faces = {BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block adjacent = location.getBlock().getRelative(face);
            if (adjacent.getType() == Material.HOPPER) {
                org.bukkit.block.data.type.Hopper hopperData = (org.bukkit.block.data.type.Hopper) adjacent.getBlockData();
                if (hopperData.getFacing() == face.getOppositeFace()) {
                    org.bukkit.block.BlockState state = adjacent.getState();
                    if (state instanceof org.bukkit.block.Hopper) {
                        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) state;
                        boolean moved = false;
                        for (int i = 0; i < hopper.getInventory().getSize(); i++) {
                            ItemStack hopperItem = hopper.getInventory().getItem(i);
                            if (hopperItem != null && !hopperItem.getType().isAir()) {
                                if (tryInsertIntoFurnace(hopperItem)) {
                                    hopperItem.setAmount(hopperItem.getAmount() - 1);
                                    hopper.getInventory().setItem(i, hopperItem.getAmount() > 0 ? hopperItem : null);
                                    moved = true;
                                    break;
                                }
                            }
                        }
                        if (moved) hopper.update(); 
                    }
                }
            }
        }
    }

    private boolean tryInsertIntoFurnace(ItemStack item) {
        for (int slot : inputSlots) {
            if (blockedSlots.contains(slot)) continue;
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) {
                if (itemsMatch(current, item) && current.getAmount() < current.getMaxStackSize()) {
                    current.setAmount(current.getAmount() + 1);
                    inventory.setItem(null, slot, current);
                    return true;
                }
            }
        }
        for (int slot : inputSlots) {
            if (blockedSlots.contains(slot)) continue;
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                ItemStack clone = item.clone();
                clone.setAmount(1);
                inventory.setItem(null, slot, clone);
                return true;
            }
        }
        return false;
    }

    private void saveAsync() {
        if (isLoading) return;
        Plugin.getInstance().getFurnaceDataManager().saveAsync(location, inventory, cookingProgress, isAutomated, blockedSlots, tank.getFluid(), sideConfig);
    }

    private void parseStructure() {
        int invIndex = 0;
        int funcIndex = 0;
        List<String> funcs = furnace.getSpecialFunctions(); 

        for (String row : furnace.getStructure()) {
            for (char c : row.toCharArray()) {
                if (c == ' ') continue;
                if (c == '$') {
                    if (funcIndex < funcs.size()) {
                        String tag = funcs.get(funcIndex++);
                        if (tag.equals("RES")) resultSlot = invIndex;
                        else if (tag.equals("BUC")) bucketSlot = invIndex;
                    }
                    invIndex++;
                } else if (c != 'X') {
                    if (!structure.containsKey(c)) {
                        structure.put(c, invIndex);
                        inputSlots.add(invIndex); 
                    }
                    invIndex++;
                } else {
                    invIndex++; 
                }
            }
        }
    }

    public Map<Character, Integer> getStructure() { return new HashMap<>(structure); }

    private void loadData() {
        Map<String, Object> savedData = Plugin.getInstance().getFurnaceDataManager().load(location);
        if (savedData != null) {
            @SuppressWarnings("unchecked")
            Map<Integer, ItemStack> items = (Map<Integer, ItemStack>) savedData.get("items");
            if (items != null) {
                for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) inventory.setItem(null, entry.getKey(), entry.getValue());
            }
            cookingProgress = (int) savedData.getOrDefault("cooking-progress", 0);
            isAutomated = (boolean) savedData.getOrDefault("is-automated", false);
            
            @SuppressWarnings("unchecked")
            List<Integer> savedBlocks = (List<Integer>) savedData.get("blocked-slots");
            if (savedBlocks != null) blockedSlots.addAll(savedBlocks);

            FluidStack fs = (FluidStack) savedData.get("fluid");
            if (fs != null) tank.fill(fs);

            @SuppressWarnings("unchecked")
            Map<BlockFace, IOState> sides = (Map<BlockFace, IOState>) savedData.get("sides");
            if (sides != null) sideConfig.putAll(sides);
        }
    }

    private Location getCenterLocation() { return location.clone().add(0.5, 0.5, 0.5); }
    public int getProgressPercentage() {
        if (currentRecipe == null || currentRecipe.getCookTimeTicks() == 0) return 0;
        return (int) ((float) cookingProgress / currentRecipe.getCookTimeTicks() * 100);
    }

    private void checkAndStartCooking() {
        if (isCooking) return;
        if (resultSlot != -1) {
            ItemStack resultItem = inventory.getItem(resultSlot);
            if (resultItem != null && resultItem.getAmount() >= resultItem.getMaxStackSize()) return;
        }
        for (FurnaceRecipe recipe : furnace.getRecipes()) {
            Map<Character, Integer> matchedSlots = matchRecipe(recipe);
            if (matchedSlots != null) {
                startCooking(recipe, matchedSlots);
                return;
            }
        }
    }

    private Map<Character, Integer> matchRecipe(FurnaceRecipe recipe) {
        Map<Character, Integer> matchedSlots = new HashMap<>();
        if (!checkGroup(recipe.getRaws(), matchedSlots)) return null;
        if (!checkGroup(recipe.getFuels(), matchedSlots)) return null;
        return matchedSlots;
    }

    private boolean checkGroup(Map<Character, Set<ItemStack>> group, Map<Character, Integer> matchedSlots) {
        for (Map.Entry<Character, Set<ItemStack>> entry : group.entrySet()) {
            char slotChar = entry.getKey();
            Integer slotIndex = structure.get(slotChar);
            if (slotIndex == null) return false;
            ItemStack item = inventory.getItem(slotIndex);
            if (item == null || item.getType() == Material.AIR) return false;
            boolean matchFound = false;
            for (ItemStack allowed : entry.getValue()) {
                if (itemsMatch(item, allowed)) { matchFound = true; break; }
            }
            if (!matchFound) return false;
            matchedSlots.put(slotChar, slotIndex);
        }
        return true;
    }

    private boolean itemsMatch(ItemStack item, ItemStack allowed) {
        if (item == null || allowed == null) return false;
        CustomStack customItem = CustomStack.byItemStack(item);
        CustomStack customAllowed = CustomStack.byItemStack(allowed);
        if (customItem != null && customAllowed != null) return customItem.getNamespacedID().equals(customAllowed.getNamespacedID());
        else if (customItem == null && customAllowed == null) return item.getType() == allowed.getType();
        return false;
    }

    private boolean checkIngredientsPresent(FurnaceRecipe recipe, Map<Character, Integer> slots) {
        for (Map.Entry<Character, Integer> entry : slots.entrySet()) {
            char key = entry.getKey();
            int slotIdx = entry.getValue();
            ItemStack item = inventory.getItem(slotIdx);
            if (item == null || item.getType() == Material.AIR) return false;
            
            Set<ItemStack> allowedRaws = recipe.getRaws().get(key);
            if (allowedRaws != null) {
                if (allowedRaws.stream().noneMatch(allowed -> itemsMatch(item, allowed))) return false;
                continue;
            }
            Set<ItemStack> allowedFuels = recipe.getFuels().get(key);
            if (allowedFuels != null) {
                if (allowedFuels.stream().noneMatch(allowed -> itemsMatch(item, allowed))) return false;
            }
        }
        return true;
    }

    private void startCooking(FurnaceRecipe recipe, Map<Character, Integer> slots) {
        if (isCooking) return;
        isCooking = true;
        currentRecipe = recipe;
        uncooledTicks = 0; // Сбрасываем таймер взрыва
        if (cookingProgress >= recipe.getCookTimeTicks()) cookingProgress = 0;
        playStartEffects();

        if (recipe.getFluidRaws() != null) {
            for (Map.Entry<String, Integer> req : recipe.getFluidRaws().entrySet()) {
                int available = tank.getContents().getOrDefault(req.getKey(), 0);
                if (available < req.getValue()) return; // Не хватает жидкости
            }
        }
        
        cookingTask = Bukkit.getScheduler().runTaskTimer(Plugin.getInstance(), () -> {
            if (!checkIngredientsPresent(recipe, slots)) {
                cancelCooking();
                return;
            }
            if (recipe.getCooling() != null) {
                boolean cooledThisTick = false;
                int consumePerTick = Math.max(1, recipe.getCooling().getTotalAmountNeededLn() / recipe.getCookTimeTicks());
                
                // Ищем охладитель в танке
                for (String fluidId : tank.getContents().keySet()) {
                    me.FireKillGrib.iAInteractables.fluids.FluidType type = Plugin.getInstance().getFluidRegistry().getFluid(fluidId);
                    if (type != null && Collections.disjoint(type.getGroups(), recipe.getCooling().getCoolantGroups()) == false) {
                        FluidStack drained = tank.drain(fluidId, consumePerTick);
                        if (drained != null && drained.getAmountLn() >= consumePerTick) {
                            cooledThisTick = true;
                            break;
                        }
                    }
                }

                if (!cooledThisTick) uncooledTicks++;
                else uncooledTicks = 0;

                if (uncooledTicks >= recipe.getCooling().getExplodeTimerTicks()) {
                    explodeMachine();
                    return;
                }
            }
            cookingProgress++;
            if (furnace.getEffects() != null && cookingProgress % furnace.getEffects().getCookingInterval() == 0) playCookingEffects();
            if (cookingProgress >= recipe.getCookTimeTicks()) completeCooking(recipe, slots);
        }, 0L, 1L);
    }

    private void completeCooking(FurnaceRecipe recipe, Map<Character, Integer> slots) {
        if (cookingTask != null) { cookingTask.cancel(); cookingTask = null; }
        isCooking = false;
        ItemStack resultToAdd = recipe.getResult().clone();
        
        if (resultSlot != -1) {
            ItemStack existing = inventory.getItem(resultSlot);
            if (existing != null && existing.getType() != Material.AIR) {
                if (!itemsMatch(existing, resultToAdd) || existing.getAmount() + resultToAdd.getAmount() > existing.getMaxStackSize()) {
                    cookingProgress = recipe.getCookTimeTicks(); 
                    saveAsync();
                    return; 
                }
            }
        }
        
        for (Integer slotIdx : slots.values()) {
            ItemStack item = inventory.getItem(slotIdx);
            if (item != null) {
                item.setAmount(item.getAmount() - 1);
                inventory.setItem(null, slotIdx, item.getAmount() <= 0 ? null : item);
            }
        }
        
        if (resultSlot != -1) {
            ItemStack existing = inventory.getItem(resultSlot);
            if (existing == null || existing.getType() == Material.AIR) inventory.setItem(null, resultSlot, resultToAdd);
            else {
                existing.setAmount(existing.getAmount() + resultToAdd.getAmount());
                inventory.setItem(null, resultSlot, existing);
            }
        }
        
        playCompleteEffects();
        cookingProgress = 0;
        saveAsync();
        
        if (!isAutomated) checkAndStartCooking();
    }

    private void explodeMachine() {
        cancelCooking();
        location.getWorld().createExplosion(location, 4F, true);
        Plugin.getInstance().getFurnaceManager().remove(location);
        location.getBlock().setType(Material.AIR);
    }

    private void playStartEffects() { if (furnace.getEffects() != null && furnace.getEffects().getOnStart() != null) furnace.getEffects().getOnStart().play(getCenterLocation()); }
    private void playCookingEffects() { if (furnace.getEffects() != null && furnace.getEffects().getOnCooking() != null) furnace.getEffects().getOnCooking().play(getCenterLocation()); }
    private void playCompleteEffects() { if (furnace.getEffects() != null && furnace.getEffects().getOnComplete() != null) furnace.getEffects().getOnComplete().play(getCenterLocation()); }

    private void cancelCooking() {
        if (cookingTask != null) { cookingTask.cancel(); cookingTask = null; }
        isCooking = false;
        currentRecipe = null;
        cookingProgress = 0;
        saveAsync();
    }

    public void shutdown() {
        if (cookingTask != null) cookingTask.cancel();
        if (idleTask != null) idleTask.cancel();
        if (Plugin.getInstance().getFluidNetworkManager() != null) Plugin.getInstance().getFluidNetworkManager().removeNode(location);
        Plugin.getInstance().getFurnaceDataManager().saveSync(location, inventory, cookingProgress, isAutomated, blockedSlots, tank.getFluid(), sideConfig);
    }

    @Override
    public void markDirty() {
        saveAsync();
    }
}