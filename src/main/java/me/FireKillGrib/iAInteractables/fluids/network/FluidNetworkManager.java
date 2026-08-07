package me.FireKillGrib.iAInteractables.fluids.network;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.pipes.PipeNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.*;

public class FluidNetworkManager {
    private final Map<Location, NetworkNode> allNodes = new HashMap<>();
    private final Set<FluidNetwork> activeNetworks = new HashSet<>();
    private final Map<NetworkNode, FluidNetwork> nodeToNetwork = new HashMap<>();

    public FluidNetworkManager() {
        Bukkit.getScheduler().runTaskTimer(Plugin.getInstance(), this::tickNetworks, 0L, 20L);
    }

    public void addNode(NetworkNode node) {
        allNodes.put(node.getLocation(), node);
        recalculateNetworks(); 
    }

    public void removeNode(Location loc) {
        if (allNodes.remove(loc) != null) recalculateNetworks();
    }

    public NetworkNode getNodeAt(Location loc) { return allNodes.get(loc); }
    public FluidNetwork getNetworkFor(NetworkNode node) { return nodeToNetwork.get(node); }

    public void recalculateNetworks() {
        activeNetworks.clear();
        nodeToNetwork.clear();
        Set<Location> visitedPipes = new HashSet<>();
        BlockFace[] directions = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

        for (NetworkNode startNode : allNodes.values()) {
            // Строим сети СТРОГО начиная от труб! Механизмы не могут соединять сети.
            if (startNode.getNodeType() != NetworkNode.NodeType.PIPE) continue;
            if (visitedPipes.contains(startNode.getLocation())) continue;

            FluidNetwork network = new FluidNetwork();
            Queue<PipeNode> queue = new LinkedList<>();
            
            PipeNode startPipe = (PipeNode) startNode;
            queue.add(startPipe);
            visitedPipes.add(startPipe.getLocation());

            while (!queue.isEmpty()) {
                PipeNode current = queue.poll();
                network.addPipe(current);
                nodeToNetwork.put(current, network);

                for (BlockFace face : directions) {
                    Location neighborLoc = current.getLocation().getBlock().getRelative(face).getLocation();
                    NetworkNode neighbor = allNodes.get(neighborLoc);
                    
                    if (neighbor != null) {
                        if (neighbor.getNodeType() == NetworkNode.NodeType.PIPE) {
                            if (!visitedPipes.contains(neighborLoc)) {
                                visitedPipes.add(neighborLoc);
                                queue.add((PipeNode) neighbor);
                            }
                        } else if (neighbor.getNodeType() == NetworkNode.NodeType.MACHINE) {
                            // Механизм! Узнаем, к какой его стороне подключилась эта труба
                            BlockFace machineFace = face.getOppositeFace();
                            
                            // Если порт не NONE, добавляем привязку Механизм <-> Конкретный Порт в сеть
                            if (neighbor.getSideState(machineFace) != me.FireKillGrib.iAInteractables.fluids.IOState.NONE) {
                                network.addConnection(neighbor, machineFace);
                            }
                        }
                    }
                }
            }
            network.updateStats(); 
            activeNetworks.add(network);
        }
    }

    private void tickNetworks() {
        for (FluidNetwork net : activeNetworks) net.tickNetwork();
    }
}