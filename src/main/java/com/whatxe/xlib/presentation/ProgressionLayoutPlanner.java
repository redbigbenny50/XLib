package com.whatxe.xlib.presentation;

import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class ProgressionLayoutPlanner {
    private ProgressionLayoutPlanner() {}

    public static LayoutPlan plan(
            ProgressionNodeLayoutMode mode,
            Collection<UpgradeNodeDefinition> nodes,
            Collection<ResourceLocation> preferredRootNodeIds
    ) {
        Map<ResourceLocation, UpgradeNodeDefinition> nodesById = new LinkedHashMap<>();
        for (UpgradeNodeDefinition node : nodes) {
            nodesById.put(node.id(), node);
        }
        if (nodesById.isEmpty()) {
            return LayoutPlan.empty();
        }

        Map<ResourceLocation, Set<ResourceLocation>> parentsByNode = new LinkedHashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> childrenByNode = new LinkedHashMap<>();
        for (UpgradeNodeDefinition node : nodesById.values()) {
            Set<ResourceLocation> visibleParents = new LinkedHashSet<>();
            for (ResourceLocation requiredNodeId : node.requiredNodes()) {
                if (!nodesById.containsKey(requiredNodeId)) {
                    continue;
                }
                visibleParents.add(requiredNodeId);
                childrenByNode.computeIfAbsent(requiredNodeId, ignored -> new ArrayList<>()).add(node.id());
            }
            parentsByNode.put(node.id(), Set.copyOf(visibleParents));
        }
        for (List<ResourceLocation> children : childrenByNode.values()) {
            children.sort(Comparator.comparing(ResourceLocation::toString));
        }

        List<ResourceLocation> rootIds = collectRootIds(nodesById.keySet(), parentsByNode, preferredRootNodeIds);
        List<ResourceLocation> topologicalOrder = topologicalOrder(nodesById.keySet(), parentsByNode, childrenByNode, rootIds);
        Map<ResourceLocation, Integer> depthById = computeDepths(topologicalOrder, parentsByNode);
        List<ResourceLocation> treeOrder = treeOrder(rootIds, nodesById.keySet(), childrenByNode);
        List<Edge> edges = collectEdges(parentsByNode);
        Map<ResourceLocation, List<ResourceLocation>> primaryTreeChildrenByNode =
                primaryTreeChildren(topologicalOrder, nodesById.keySet(), parentsByNode);
        List<ResourceLocation> orderedRoots =
                orderedRoots(rootIds, topologicalOrder, nodesById.keySet(), primaryTreeChildrenByNode);
        Map<ResourceLocation, Integer> seededRows =
                computeSeedRows(orderedRoots, topologicalOrder, primaryTreeChildrenByNode);

        return switch (mode) {
            case LIST -> listPlan(treeOrder, nodesById);
            case TREE -> treePlan(treeOrder, nodesById, depthById, seededRows, edges);
        };
    }

    private static LayoutPlan listPlan(List<ResourceLocation> order, Map<ResourceLocation, UpgradeNodeDefinition> nodesById) {
        List<NodePlacement> placements = new ArrayList<>();
        for (int row = 0; row < order.size(); row++) {
            ResourceLocation nodeId = order.get(row);
            placements.add(new NodePlacement(nodesById.get(nodeId), 0, 0, row));
        }
        return new LayoutPlan(List.copyOf(placements), List.of(), 1, placements.size());
    }

    private static LayoutPlan treePlan(
            List<ResourceLocation> order,
            Map<ResourceLocation, UpgradeNodeDefinition> nodesById,
            Map<ResourceLocation, Integer> depthById,
            Map<ResourceLocation, Integer> rowById,
            List<Edge> edges
    ) {
        List<NodePlacement> placements = new ArrayList<>();
        for (int row = 0; row < order.size(); row++) {
            ResourceLocation nodeId = order.get(row);
            int depth = depthById.getOrDefault(nodeId, 0);
            placements.add(new NodePlacement(nodesById.get(nodeId), depth, depth, rowById.getOrDefault(nodeId, row)));
        }
        int columns = placements.stream().mapToInt(NodePlacement::column).max().orElse(0) + 1;
        int rows = placements.stream().mapToInt(NodePlacement::row).max().orElse(-1) + 1;
        return new LayoutPlan(List.copyOf(placements), List.copyOf(edges), columns, rows);
    }

    private static List<ResourceLocation> collectRootIds(
            Set<ResourceLocation> nodeIds,
            Map<ResourceLocation, Set<ResourceLocation>> parentsByNode,
            Collection<ResourceLocation> preferredRootNodeIds
    ) {
        List<ResourceLocation> rootIds = new ArrayList<>();
        for (ResourceLocation rootNodeId : preferredRootNodeIds) {
            if (nodeIds.contains(rootNodeId) && !rootIds.contains(rootNodeId)) {
                rootIds.add(rootNodeId);
            }
        }
        for (ResourceLocation nodeId : nodeIds) {
            if (parentsByNode.getOrDefault(nodeId, Set.of()).isEmpty() && !rootIds.contains(nodeId)) {
                rootIds.add(nodeId);
            }
        }
        if (rootIds.isEmpty()) {
            rootIds.addAll(nodeIds);
        }
        return List.copyOf(rootIds);
    }

    private static List<ResourceLocation> topologicalOrder(
            Set<ResourceLocation> nodeIds,
            Map<ResourceLocation, Set<ResourceLocation>> parentsByNode,
            Map<ResourceLocation, List<ResourceLocation>> childrenByNode,
            List<ResourceLocation> rootIds
    ) {
        Map<ResourceLocation, Integer> remainingParents = new LinkedHashMap<>();
        for (ResourceLocation nodeId : nodeIds) {
            remainingParents.put(nodeId, parentsByNode.getOrDefault(nodeId, Set.of()).size());
        }
        ArrayDeque<ResourceLocation> queue = new ArrayDeque<>();
        Set<ResourceLocation> queued = new LinkedHashSet<>();
        for (ResourceLocation rootId : rootIds) {
            if (remainingParents.getOrDefault(rootId, 0) == 0 && queued.add(rootId)) {
                queue.addLast(rootId);
            }
        }
        for (ResourceLocation nodeId : nodeIds) {
            if (remainingParents.getOrDefault(nodeId, 0) == 0 && queued.add(nodeId)) {
                queue.addLast(nodeId);
            }
        }

        List<ResourceLocation> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ResourceLocation nodeId = queue.removeFirst();
            ordered.add(nodeId);
            for (ResourceLocation childId : childrenByNode.getOrDefault(nodeId, List.of())) {
                int count = remainingParents.computeIfPresent(childId, (ignored, value) -> Math.max(0, value - 1));
                if (count == 0 && queued.add(childId)) {
                    queue.addLast(childId);
                }
            }
        }

        for (ResourceLocation nodeId : nodeIds) {
            if (!ordered.contains(nodeId)) {
                ordered.add(nodeId);
            }
        }
        return List.copyOf(ordered);
    }

    private static Map<ResourceLocation, Integer> computeDepths(
            List<ResourceLocation> topologicalOrder,
            Map<ResourceLocation, Set<ResourceLocation>> parentsByNode
    ) {
        Map<ResourceLocation, Integer> depthById = new LinkedHashMap<>();
        for (ResourceLocation nodeId : topologicalOrder) {
            int depth = 0;
            for (ResourceLocation parentId : parentsByNode.getOrDefault(nodeId, Set.of())) {
                depth = Math.max(depth, depthById.getOrDefault(parentId, 0) + 1);
            }
            depthById.put(nodeId, depth);
        }
        return Map.copyOf(depthById);
    }

    private static List<ResourceLocation> treeOrder(
            List<ResourceLocation> rootIds,
            Set<ResourceLocation> nodeIds,
            Map<ResourceLocation, List<ResourceLocation>> childrenByNode
    ) {
        List<ResourceLocation> ordered = new ArrayList<>();
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        for (ResourceLocation rootId : rootIds) {
            appendTree(rootId, childrenByNode, visited, ordered);
        }
        for (ResourceLocation nodeId : nodeIds) {
            if (visited.add(nodeId)) {
                ordered.add(nodeId);
            }
        }
        return List.copyOf(ordered);
    }

    private static void appendTree(
            ResourceLocation nodeId,
            Map<ResourceLocation, List<ResourceLocation>> childrenByNode,
            Set<ResourceLocation> visited,
            List<ResourceLocation> output
    ) {
        if (!visited.add(nodeId)) {
            return;
        }
        output.add(nodeId);
        for (ResourceLocation childId : childrenByNode.getOrDefault(nodeId, List.of())) {
            appendTree(childId, childrenByNode, visited, output);
        }
    }

    private static List<Edge> collectEdges(Map<ResourceLocation, Set<ResourceLocation>> parentsByNode) {
        List<Edge> edges = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : parentsByNode.entrySet()) {
            for (ResourceLocation parentId : entry.getValue()) {
                edges.add(new Edge(parentId, entry.getKey()));
            }
        }
        return List.copyOf(edges);
    }

    private static Map<ResourceLocation, List<ResourceLocation>> primaryTreeChildren(
            List<ResourceLocation> topologicalOrder,
            Set<ResourceLocation> nodeIds,
            Map<ResourceLocation, Set<ResourceLocation>> parentsByNode
    ) {
        Map<ResourceLocation, Integer> orderIndex = new LinkedHashMap<>();
        for (int index = 0; index < topologicalOrder.size(); index++) {
            orderIndex.put(topologicalOrder.get(index), index);
        }

        Map<ResourceLocation, List<ResourceLocation>> primaryChildrenByNode = new LinkedHashMap<>();
        for (ResourceLocation nodeId : nodeIds) {
            primaryChildrenByNode.put(nodeId, new ArrayList<>());
        }

        for (ResourceLocation nodeId : topologicalOrder) {
            ResourceLocation primaryParentId = choosePrimaryParent(parentsByNode.getOrDefault(nodeId, Set.of()), orderIndex);
            if (primaryParentId != null) {
                primaryChildrenByNode.computeIfAbsent(primaryParentId, ignored -> new ArrayList<>()).add(nodeId);
            }
        }

        for (List<ResourceLocation> children : primaryChildrenByNode.values()) {
            children.sort(Comparator.comparingInt((ResourceLocation childId) -> orderIndex.getOrDefault(childId, Integer.MAX_VALUE))
                    .thenComparing(ResourceLocation::toString));
        }
        return primaryChildrenByNode;
    }

    private static ResourceLocation choosePrimaryParent(
            Set<ResourceLocation> parentIds,
            Map<ResourceLocation, Integer> orderIndex
    ) {
        ResourceLocation chosenParentId = null;
        int chosenOrderIndex = Integer.MIN_VALUE;
        for (ResourceLocation parentId : parentIds) {
            int parentOrderIndex = orderIndex.getOrDefault(parentId, Integer.MIN_VALUE);
            if (chosenParentId == null
                    || parentOrderIndex > chosenOrderIndex
                    || (parentOrderIndex == chosenOrderIndex
                    && parentId.toString().compareTo(chosenParentId.toString()) < 0)) {
                chosenParentId = parentId;
                chosenOrderIndex = parentOrderIndex;
            }
        }
        return chosenParentId;
    }

    private static List<ResourceLocation> orderedRoots(
            List<ResourceLocation> preferredRoots,
            List<ResourceLocation> topologicalOrder,
            Set<ResourceLocation> nodeIds,
            Map<ResourceLocation, List<ResourceLocation>> primaryTreeChildrenByNode
    ) {
        Set<ResourceLocation> childIds = new LinkedHashSet<>();
        for (List<ResourceLocation> children : primaryTreeChildrenByNode.values()) {
            childIds.addAll(children);
        }

        List<ResourceLocation> orderedRoots = new ArrayList<>();
        for (ResourceLocation rootId : preferredRoots) {
            if (nodeIds.contains(rootId) && !orderedRoots.contains(rootId)) {
                orderedRoots.add(rootId);
            }
        }
        for (ResourceLocation nodeId : topologicalOrder) {
            if (!childIds.contains(nodeId) && !orderedRoots.contains(nodeId)) {
                orderedRoots.add(nodeId);
            }
        }
        return List.copyOf(orderedRoots);
    }

    private static Map<ResourceLocation, Integer> computeSeedRows(
            List<ResourceLocation> orderedRoots,
            List<ResourceLocation> topologicalOrder,
            Map<ResourceLocation, List<ResourceLocation>> primaryTreeChildrenByNode
    ) {
        Map<ResourceLocation, Integer> rowById = new LinkedHashMap<>();
        int nextLeafRow = 0;
        for (ResourceLocation rootId : orderedRoots) {
            if (rowById.containsKey(rootId)) {
                continue;
            }
            nextLeafRow = assignSeedRows(rootId, primaryTreeChildrenByNode, rowById, nextLeafRow);
            nextLeafRow += 2;
        }
        for (ResourceLocation nodeId : topologicalOrder) {
            if (rowById.containsKey(nodeId)) {
                continue;
            }
            rowById.put(nodeId, nextLeafRow);
            nextLeafRow += 2;
        }
        return Map.copyOf(rowById);
    }

    private static int assignSeedRows(
            ResourceLocation nodeId,
            Map<ResourceLocation, List<ResourceLocation>> primaryTreeChildrenByNode,
            Map<ResourceLocation, Integer> rowById,
            int nextLeafRow
    ) {
        if (rowById.containsKey(nodeId)) {
            return nextLeafRow;
        }

        List<ResourceLocation> children = primaryTreeChildrenByNode.getOrDefault(nodeId, List.of());
        if (children.isEmpty()) {
            rowById.put(nodeId, nextLeafRow);
            return nextLeafRow + 2;
        }

        int currentLeafRow = nextLeafRow;
        for (ResourceLocation childId : children) {
            currentLeafRow = assignSeedRows(childId, primaryTreeChildrenByNode, rowById, currentLeafRow);
        }

        int firstChildRow = rowById.getOrDefault(children.getFirst(), nextLeafRow);
        int lastChildRow = rowById.getOrDefault(children.getLast(), firstChildRow);
        rowById.put(nodeId, (firstChildRow + lastChildRow) / 2);
        return currentLeafRow;
    }

    private static int nearestAvailableRow(Set<Integer> occupiedRows, int preferredRow) {
        if (!occupiedRows.contains(preferredRow)) {
            return preferredRow;
        }
        for (int distance = 1; distance < 512; distance++) {
            int upwardRow = preferredRow - distance;
            if (upwardRow >= 0 && !occupiedRows.contains(upwardRow)) {
                return upwardRow;
            }
            int downwardRow = preferredRow + distance;
            if (!occupiedRows.contains(downwardRow)) {
                return downwardRow;
            }
        }
        return preferredRow + occupiedRows.size() + 1;
    }

    public record LayoutPlan(
            List<NodePlacement> placements,
            List<Edge> edges,
            int columnCount,
            int rowCount
    ) {
        public static LayoutPlan empty() {
            return new LayoutPlan(List.of(), List.of(), 0, 0);
        }
    }

    public record NodePlacement(
            UpgradeNodeDefinition node,
            int depth,
            int column,
            int row
    ) {}

    public record Edge(
            ResourceLocation fromNodeId,
            ResourceLocation toNodeId
    ) {}
}
