package me.FireKillGrib.iAInteractables.fluids.network;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.FluidStack;
import me.FireKillGrib.iAInteractables.fluids.FluidType;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import me.FireKillGrib.iAInteractables.fluids.pipes.PipeNode;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;

import java.util.*;

public class FluidNetwork {
    private final UUID networkId = UUID.randomUUID();
    private final Set<PipeNode> pipes = new HashSet<>();
    private final List<MachineConnection> connections = new ArrayList<>();

    private FluidStack networkFluid;
    private String lastFlowedFluidId = null; 
    
    private int totalCapacityLn = 1;
    private int maxTransferRateLn = 100; 
    private double weakestPressureLimit = Double.MAX_VALUE;
    
    private double currentPressure = 0.0;
    private int throughputLastTick = 0;

    public static class MachineConnection {
        public final NetworkNode machine;
        public final BlockFace connectedFace;

        public MachineConnection(NetworkNode machine, BlockFace connectedFace) {
            this.machine = machine;
            this.connectedFace = connectedFace;
        }
    }

    public void addPipe(PipeNode pipe) { pipes.add(pipe); }
    public void addConnection(NetworkNode machine, BlockFace face) { connections.add(new MachineConnection(machine, face)); }

    public void updateStats() {
        totalCapacityLn = pipes.stream().mapToInt(p -> p.getPipeType().getStorageCapacity()).sum();
        if (totalCapacityLn == 0) totalCapacityLn = 1;
        
        weakestPressureLimit = pipes.stream().mapToDouble(p -> p.getPipeType().getPressureLimit()).min().orElse(Double.MAX_VALUE);
        maxTransferRateLn = pipes.stream().mapToInt(p -> p.getPipeType().getCarryingCapacity()).min().orElse(100);
        if (maxTransferRateLn <= 0) maxTransferRateLn = 1;
    }

    public boolean canTransport(FluidType type) {
        if (type == null) return false;
        for (PipeNode pipe : pipes) {
            if (!pipe.getPipeType().canTransport(type)) return false;
        }
        return true;
    }

    public void tickNetwork() {
        if (pipes.isEmpty() || connections.isEmpty()) return;

        int throughputThisTick = 0;

        for (MachineConnection conn : connections) {
            NetworkNode producer = conn.machine;
            if (producer.getSideState(conn.connectedFace) != IOState.OUTPUT) continue;
            if (producer.getTank() == null || producer.getTank().getFluid() == null) continue;

            FluidStack machineFluid = producer.getTank().getFluid();
            FluidType type = Plugin.getInstance().getFluidRegistry().getFluid(machineFluid.getFluidId());
            
            if (!canTransport(type)) continue;
            if (networkFluid != null && !networkFluid.getFluidId().equals(machineFluid.getFluidId())) continue; 
                
            int space = totalCapacityLn * 10 - (networkFluid == null ? 0 : networkFluid.getAmountLn());
            if (space > 0) {
                int toDrain = Math.min(maxTransferRateLn, space);
                // ИСПРАВЛЕНИЕ ОШИБКИ: Теперь передаем ID жидкости
                FluidStack drained = producer.getTank().drain(machineFluid.getFluidId(), toDrain);
                if (drained != null) {
                    if (networkFluid == null) networkFluid = drained;
                    else networkFluid.add(drained.getAmountLn());
                    throughputThisTick += drained.getAmountLn();
                    lastFlowedFluidId = drained.getFluidId();
                    
                    producer.markDirty(); 
                }
            }
        }

        if (networkFluid != null && networkFluid.getAmountLn() > 0) {
            for (MachineConnection conn : connections) {
                if (networkFluid == null || networkFluid.getAmountLn() == 0) break;
                
                NetworkNode consumer = conn.machine;
                if (consumer.getSideState(conn.connectedFace) != IOState.INPUT) continue;
                if (consumer.getTank() == null) continue;

                if (consumer.getTank().canAccept(networkFluid.getFluidId())) {
                    int toPush = Math.min(maxTransferRateLn, networkFluid.getAmountLn());
                    int accepted = consumer.getTank().fill(new FluidStack(networkFluid.getFluidId(), toPush));
                    if (accepted > 0) {
                        networkFluid.add(-accepted);
                        throughputThisTick += accepted;
                        consumer.markDirty(); 
                    }
                }
            }
        }

        if (networkFluid != null && networkFluid.getAmountLn() <= 0) networkFluid = null;
        throughputLastTick = throughputThisTick;

        processPhysicsAndDamage(throughputThisTick);
    }

    private void processPhysicsAndDamage(int throughput) {
        String activeFluidId = networkFluid != null ? networkFluid.getFluidId() : lastFlowedFluidId;
        
        if (activeFluidId == null) {
            currentPressure = 0.0;
            for (PipeNode pipe : pipes) pipe.heal(pipe.getPipeType().getHealRate());
            return;
        }

        FluidType type = Plugin.getInstance().getFluidRegistry().getFluid(activeFluidId);
        if (type == null) return;

        double basePressure = networkFluid != null ? ((double) networkFluid.getAmountLn() / totalCapacityLn) * type.getCompressionLimit() : 0.0;
        double flowPressure = ((double) throughput / totalCapacityLn) * (type.getDensity() / 1000.0);
        
        currentPressure = basePressure + flowPressure;

        List<PipeNode> brokenPipes = new ArrayList<>();

        for (PipeNode pipe : pipes) {
            if (currentPressure > pipe.getPipeType().getPressureLimit()) {
                double overload = currentPressure - pipe.getPipeType().getPressureLimit();
                pipe.damage(overload * pipe.getPipeType().getDamageMultiplier());
                if (pipe.getHealth() <= 0) brokenPipes.add(pipe);
            } else {
                pipe.heal(pipe.getPipeType().getHealRate());
            }
        }

        if (!brokenPipes.isEmpty()) {
            Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
                for (PipeNode broken : brokenPipes) {
                    Plugin.getInstance().getPipeManager().explodePipe(broken);
                }
            });
        }
    }

    public UUID getNetworkId() { return networkId; }
    public Set<PipeNode> getPipes() { return pipes; }
    public FluidStack getFluid() { return networkFluid; }
    public String getLastFlowedFluidId() { return networkFluid != null ? networkFluid.getFluidId() : lastFlowedFluidId; }
    public int getCapacity() { return totalCapacityLn; }
    public double getPressure() { return currentPressure; }
    public int getThroughput() { return throughputLastTick; }
    public double getWeakestPressureLimit() { return weakestPressureLimit; }
}