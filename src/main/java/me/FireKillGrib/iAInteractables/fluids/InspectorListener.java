package me.FireKillGrib.iAInteractables.fluids;

import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.network.FluidNetwork;
import me.FireKillGrib.iAInteractables.fluids.network.NetworkNode;
import me.FireKillGrib.iAInteractables.fluids.pipes.PipeNode;
import me.FireKillGrib.iAInteractables.managers.FurnaceController;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockInstance;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InspectorListener implements Listener {
    private final Map<UUID, Long> interactCooldowns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInspectFurniture(FurnitureInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!WrenchUtil.isInspector(item)) return;
        
        long currentTime = System.currentTimeMillis();
        if (interactCooldowns.containsKey(player.getUniqueId()) && (currentTime - interactCooldowns.get(player.getUniqueId()) < 100)) return;
        interactCooldowns.put(player.getUniqueId(), currentTime);

        Location loc = event.getBukkitEntity().getLocation().getBlock().getLocation();
        inspectLocation(player, loc, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInspectBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!WrenchUtil.isInspector(item)) return;
        
        long currentTime = System.currentTimeMillis();
        if (interactCooldowns.containsKey(player.getUniqueId()) && (currentTime - interactCooldowns.get(player.getUniqueId()) < 100)) return;
        interactCooldowns.put(player.getUniqueId(), currentTime);

        Location loc = event.getClickedBlock().getLocation();
        inspectLocation(player, loc, event);
    }

    private void inspectLocation(Player player, Location loc, org.bukkit.event.Cancellable event) {
        NetworkNode pipeNode = Plugin.getInstance().getPipeManager().getPipeAt(loc);
        if (pipeNode != null && pipeNode instanceof PipeNode) {
            event.setCancelled(true);
            PipeNode p = (PipeNode) pipeNode;
            FluidNetwork net = Plugin.getInstance().getFluidNetworkManager().getNetworkFor(p);
            if (net == null) return;

            player.sendMessage(ChatUtil.color("&8&m-----------------------------------"));
            player.sendMessage(ChatUtil.color("&b&lАнализ Трубы &7(Сеть: " + net.getNetworkId().toString().substring(0, 6) + ")"));
            player.sendMessage(ChatUtil.color("&7Тип: &f" + p.getPipeType().getId()));
            
            double healthPercent = (p.getHealth() / p.getPipeType().getDurability()) * 100.0;
            String healthColor = healthPercent > 50 ? "&a" : (healthPercent > 20 ? "&e" : "&c");
            player.sendMessage(ChatUtil.color("&7Прочность: " + healthColor + String.format("%.1f", p.getHealth()) + " &8/ " + p.getPipeType().getDurability() + " HP"));
            
            String fId = net.getLastFlowedFluidId();
            if (fId != null) {
                FluidType fType = Plugin.getInstance().getFluidRegistry().getFluid(fId);
                String fName = fType != null ? fType.getDisplayName() : fId;
                int mB = net.getFluid() != null ? FluidMath.lnToMb(net.getFluid().getAmountLn()) : 0;
                int capMB = FluidMath.lnToMb(net.getCapacity());
                
                player.sendMessage(ChatUtil.color("&7Содержимое: &f" + fName + " &8[" + mB + " / " + capMB + " mB]"));
                player.sendMessage(ChatUtil.color(String.format("&7Давление: &e%.2f atm &8(Лимит: %.2f atm)", net.getPressure(), p.getPipeType().getPressureLimit())));
                player.sendMessage(ChatUtil.color("&7Поток (Труба/Тик): &f" + FluidMath.lnToMb(net.getThroughput()) + " mB/t"));
            } else {
                player.sendMessage(ChatUtil.color("&7Содержимое: &8Пусто &8[0 / " + FluidMath.lnToMb(net.getCapacity()) + " mB]"));
                player.sendMessage(ChatUtil.color("&7Давление: &a0.0 atm"));
            }
            player.sendMessage(ChatUtil.color("&8&m-----------------------------------"));
            return;
        }

        NetworkNode machineNode = null;
        MultiblockInstance mb = Plugin.getInstance().getMultiblockManager().getStructureAt(loc);
        if (mb != null) machineNode = mb.getPortNode(loc);
        else {
            FurnaceController fc = Plugin.getInstance().getFurnaceManager().get(loc);
            if (fc != null) machineNode = fc;
        }

        if (machineNode != null) {
            event.setCancelled(true);
            me.FireKillGrib.iAInteractables.fluids.FluidTank tank = machineNode.getTank();
            
            player.sendMessage(ChatUtil.color("&8&m-----------------------------------"));
            player.sendMessage(ChatUtil.color("&6&lАнализ Механизма"));
            
            if (tank != null && tank.getFluid() != null) {
                FluidType fType = Plugin.getInstance().getFluidRegistry().getFluid(tank.getFluid().getFluidId());
                String fName = fType != null ? fType.getDisplayName() : tank.getFluid().getFluidId();
                int mB = FluidMath.lnToMb(tank.getFluid().getAmountLn());
                int capMB = FluidMath.lnToMb(tank.getFreeSpaceLn() + tank.getFluid().getAmountLn());
                
                player.sendMessage(ChatUtil.color("&7Резервуар: &f" + fName + " &8[" + mB + " / " + capMB + " mB]"));
            } else {
                player.sendMessage(ChatUtil.color("&7Резервуар: &8Пусто"));
            }
            player.sendMessage(ChatUtil.color("&8&m-----------------------------------"));
        }
    }
}