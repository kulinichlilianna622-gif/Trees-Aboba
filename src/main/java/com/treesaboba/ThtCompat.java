package com.treesaboba;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tannyjung.tanshugetrees_core.outside.OutsideUtils;
import tannyjung.tanshugetrees_handcode.systems.Caches;

/**
 * Tree Physics only fells trees that stand on rooted dirt (it places that block itself
 * under every vanilla tree). Tan's Huge Trees uses its own generator, so its trees never
 * get one and are never felled.
 *
 * After Tan's Huge Trees has placed a tree into a chunk, this puts a rooted dirt block
 * under the tree's underground wood - but only for trees whose trunk is thin enough.
 * Huge trees get no rooted dirt, so Tree Physics leaves them standing.
 */
public final class ThtCompat {
    private static final TagKey<Block> TP_LOGS = tag("treephysics", "logs");
    private static final TagKey<Block> TP_LEAVES = tag("treephysics", "leaves");
    private static final TagKey<Block> TP_ROOTS = tag("treephysics", "roots");
    private static final TagKey<Block> TP_CAN_BE_ROOTS = tag("treephysics", "can_be_roots");

    /** Shape part ids used by Tan's Huge Trees (block type / 10). */
    private static final int TAPROOT = 110;
    private static final int FINE_ROOT = 113;
    private static final int TRUNK = 114;

    private static final Map<String, ShapeInfo> SHAPES = new ConcurrentHashMap<>();

    private ThtCompat() {
    }

    private static TagKey<Block> tag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * @param candidates x, y, z triples (shape-local) of trunk and root blocks, lowest first
     */
    private record ShapeInfo(boolean small, int[] candidates) {
        static final ShapeInfo NONE = new ShapeInfo(false, new int[0]);
    }

    public static void afterTreePlaced(
            LevelAccessor level,
            ChunkPos chunk,
            String location,
            String pathSettings,
            BlockPos center,
            int[] rotationMirrored,
            int deadTreeLevel,
            int fallenDirection
    ) {
        try {
            if (!TreesAbobaConfig.enabled()) {
                return;
            }
            // Fallen logs and woody debris are not standing trees.
            if (fallenDirection != 0 || deadTreeLevel >= 200) {
                return;
            }

            int maxWidth = TreesAbobaConfig.maxTrunkWidth();
            ShapeInfo info = SHAPES.computeIfAbsent(location + "#" + maxWidth, key -> analyze(location, maxWidth));
            if (!info.small()) {
                return;
            }

            int surfaceY = center.getY() - startHeight(pathSettings);
            markRoot(level, chunk, center, rotationMirrored, info, surfaceY);
        } catch (Throwable t) {
            TreesAboba.LOGGER.warn("Could not mark Tan's Huge Trees tree {} for Tree Physics", location, t);
        }
    }

    private static int startHeight(String pathSettings) {
        try {
            Map<String, String> settings = Caches.TreeSettings.getNormal(pathSettings);
            String value = settings == null ? null : settings.get("start_height");
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static ShapeInfo analyze(String location, int maxWidth) {
        short[] data = Caches.TreeShape.getTreeShapeData(location);
        if (data == null || data.length < 4) {
            return ShapeInfo.NONE;
        }

        Int2IntOpenHashMap trunkPerLayer = new Int2IntOpenHashMap();
        List<int[]> candidates = new ArrayList<>();

        for (int i = 0; i + 3 < data.length; i += 4) {
            int part = data[i] / 10;
            if (part < TAPROOT || part > TRUNK) {
                continue;
            }
            int x = data[i + 1];
            int y = data[i + 2];
            int z = data[i + 3];
            if (part == TRUNK) {
                trunkPerLayer.addTo(y, 1);
            }
            if (part <= FINE_ROOT || part == TRUNK) {
                candidates.add(new int[]{x, y, z});
            }
        }

        if (trunkPerLayer.isEmpty()) {
            return ShapeInfo.NONE;
        }

        // A thin trunk can still have two blocks in a layer where it bends, so look at the
        // typical layer (the median) instead of the widest one.
        int[] counts = trunkPerLayer.values().toIntArray();
        Arrays.sort(counts);
        int median = counts[(counts.length - 1) / 2];
        boolean small = median <= maxWidth * maxWidth;
        if (!small) {
            return ShapeInfo.NONE;
        }

        candidates.sort((a, b) -> Integer.compare(a[1], b[1]));
        int[] flat = new int[candidates.size() * 3];
        for (int i = 0; i < candidates.size(); i++) {
            int[] c = candidates.get(i);
            flat[i * 3] = c[0];
            flat[i * 3 + 1] = c[1];
            flat[i * 3 + 2] = c[2];
        }
        return new ShapeInfo(true, flat);
    }

    private static void markRoot(LevelAccessor level, ChunkPos chunk, BlockPos center, int[] rotationMirrored, ShapeInfo info, int surfaceY) {
        int[] c = info.candidates();
        // Pass 0: replace soil like Tree Physics does. Pass 1: any other solid block underground.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < c.length; i += 3) {
                BlockPos pos = OutsideUtils.convertPosRotationMirrored(new BlockPos(c[i], c[i + 1], c[i + 2]), rotationMirrored)
                        .offset(center.getX(), center.getY(), center.getZ());
                if (pos.getX() >> 4 != chunk.x || pos.getZ() >> 4 != chunk.z) {
                    continue;
                }

                BlockPos below = pos.below();
                // Stay below the top soil layer so no rooted dirt shows on the surface.
                if (below.getY() >= surfaceY - 1) {
                    continue;
                }

                BlockState wood = level.getBlockState(pos);
                if (!wood.is(TP_LOGS)) {
                    continue;
                }

                BlockState under = level.getBlockState(below);
                if (under.is(TP_ROOTS)) {
                    return;
                }

                boolean replace = pass == 0 ? under.is(TP_CAN_BE_ROOTS) : canReplaceUnderground(level, below, under);
                if (replace) {
                    level.setBlock(below, Blocks.ROOTED_DIRT.defaultBlockState(), 2);
                    return;
                }
            }
        }
    }

    private static boolean canReplaceUnderground(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) {
            return false;
        }
        if (state.is(TP_LOGS) || state.is(TP_LEAVES) || state.is(BlockTags.LEAVES) || state.is(Blocks.BEDROCK)) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0F && state.isSolidRender(level, pos);
    }
}
