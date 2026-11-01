package com.treesaboba;

import com.farcr.treephysics.api.flood_fill.TreeResult;
import com.farcr.treephysics.api.util.TreeUtil;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Tree Physics follows leaves outward by their vanilla "distance" value. Tan's Huge Trees
 * places persistent leaves that all keep distance 7, so only leaves touching the wood would
 * fall with the tree and the rest of the crown would float. For persistent leaves this counts
 * the leaf-to-leaf steps from the wood during the flood fill instead.
 */
public final class LeafReach {
    private static final Map<TreeResult, Long2IntOpenHashMap> STEPS = Collections.synchronizedMap(new WeakHashMap<>());

    private LeafReach() {
    }

    /**
     * @return the rule result to use, or {@code null} to let Tree Physics decide as usual
     */
    @Nullable
    public static Boolean leafRule(BlockPos fromPos, BlockPos toPos, BlockState fromState, BlockState toState, TreeResult result) {
        if (!TreeUtil.isLeaf(toState) || !isPersistent(toState)) {
            return null;
        }

        if (TreeUtil.isLog(fromState)) {
            // Tree Physics always takes leaves that touch the wood; remember them as step 1.
            steps(result).put(toPos.asLong(), 1);
            return null;
        }

        if (!TreeUtil.isLeaf(fromState) || !TreesAbobaConfig.enabled()) {
            return null;
        }

        int fromSteps = steps(result).get(fromPos.asLong());
        if (fromSteps <= 0) {
            if (isPersistent(fromState)) {
                return null;
            }
            fromSteps = TreeUtil.getLeafDistanceRaw(fromState);
            if (fromSteps <= 0) {
                return null;
            }
        }

        if (!TreeUtil.isSameLeafType(fromState, toState)) {
            return false;
        }

        int toSteps = fromSteps + 1;
        if (toSteps > TreesAbobaConfig.leafReach()) {
            return false;
        }

        steps(result).put(toPos.asLong(), toSteps);
        return true;
    }

    private static Long2IntOpenHashMap steps(TreeResult result) {
        return STEPS.computeIfAbsent(result, r -> new Long2IntOpenHashMap());
    }

    private static boolean isPersistent(BlockState state) {
        return state.hasProperty(BlockStateProperties.PERSISTENT) && state.getValue(BlockStateProperties.PERSISTENT);
    }
}
