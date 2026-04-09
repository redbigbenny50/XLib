package com.whatxe.xlib.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ProgressionLayoutPlannerTest {
    private static final ResourceLocation ROOT_ID = id("root");
    private static final ResourceLocation LEFT_ID = id("left");
    private static final ResourceLocation RIGHT_ID = id("right");
    private static final ResourceLocation FINISH_ID = id("finish");

    @Test
    void listLayoutKeepsNodesInSingleColumnOrder() {
        ProgressionLayoutPlanner.LayoutPlan plan = ProgressionLayoutPlanner.plan(
                ProgressionNodeLayoutMode.LIST,
                testNodes(),
                List.of(ROOT_ID)
        );

        assertEquals(4, plan.placements().size());
        assertEquals(0, plan.edges().size());
        assertEquals(0, placement(plan, ROOT_ID).column());
        assertEquals(0, placement(plan, LEFT_ID).column());
        assertEquals(0, placement(plan, RIGHT_ID).column());
        assertEquals(0, placement(plan, FINISH_ID).column());
        assertEquals(0, placement(plan, ROOT_ID).row());
        assertEquals(1, placement(plan, LEFT_ID).row());
        assertTrue(placement(plan, LEFT_ID).row() < placement(plan, FINISH_ID).row());
        assertTrue(placement(plan, ROOT_ID).row() < placement(plan, RIGHT_ID).row());
    }

    @Test
    void treeLayoutKeepsRootFirstAndCarriesDepths() {
        ProgressionLayoutPlanner.LayoutPlan plan = ProgressionLayoutPlanner.plan(
                ProgressionNodeLayoutMode.TREE,
                testNodes(),
                List.of(ROOT_ID)
        );

        assertEquals(ROOT_ID, plan.placements().getFirst().node().id());
        assertEquals(0, placement(plan, ROOT_ID).depth());
        assertEquals(1, placement(plan, LEFT_ID).depth());
        assertEquals(1, placement(plan, RIGHT_ID).depth());
        assertEquals(2, placement(plan, FINISH_ID).depth());
        assertEquals(1, placement(plan, ROOT_ID).row());
        assertEquals(0, placement(plan, LEFT_ID).row());
        assertEquals(2, placement(plan, RIGHT_ID).row());
        assertTrue(placement(plan, LEFT_ID).row() < placement(plan, ROOT_ID).row());
        assertTrue(placement(plan, ROOT_ID).row() < placement(plan, RIGHT_ID).row());
    }

    private static ProgressionLayoutPlanner.NodePlacement placement(
            ProgressionLayoutPlanner.LayoutPlan plan,
            ResourceLocation nodeId
    ) {
        return plan.placements().stream()
                .filter(entry -> entry.node().id().equals(nodeId))
                .findFirst()
                .orElseThrow();
    }

    private static List<UpgradeNodeDefinition> testNodes() {
        return List.of(
                UpgradeNodeDefinition.builder(ROOT_ID).build(),
                UpgradeNodeDefinition.builder(LEFT_ID).requiredNode(ROOT_ID).build(),
                UpgradeNodeDefinition.builder(RIGHT_ID).requiredNode(ROOT_ID).build(),
                UpgradeNodeDefinition.builder(FINISH_ID).requiredNodes(List.of(LEFT_ID, RIGHT_ID)).build()
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
