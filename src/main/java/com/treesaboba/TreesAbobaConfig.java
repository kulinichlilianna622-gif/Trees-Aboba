package com.treesaboba;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TreesAbobaConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue MAX_TRUNK_WIDTH;
    private static final ModConfigSpec.IntValue LEAF_REACH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ENABLED = builder
                .comment("Mark small Tan's Huge Trees trees during world generation so Tree Physics can fell them.",
                        "Only affects chunks generated after this mod was installed.")
                .define("enabled", true);

        MAX_TRUNK_WIDTH = builder
                .comment("Widest trunk (in blocks) that still counts as a small tree.",
                        "1 = only trees with a one-block trunk fall; wider trunks never fall.")
                .defineInRange("max_trunk_width", 1, 1, 16);

        LEAF_REACH = builder
                .comment("How many leaf-to-leaf steps away from the wood persistent leaves are still taken along",
                        "when a tree falls. Tan's Huge Trees places persistent leaves without a distance value,",
                        "so Tree Physics can not use the vanilla distance for them.")
                .defineInRange("persistent_leaf_reach", 8, 1, 32);

        SPEC = builder.build();
    }

    private TreesAbobaConfig() {
    }

    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (IllegalStateException notLoaded) {
            return ENABLED.getDefault();
        }
    }

    public static int maxTrunkWidth() {
        try {
            return MAX_TRUNK_WIDTH.get();
        } catch (IllegalStateException notLoaded) {
            return MAX_TRUNK_WIDTH.getDefault();
        }
    }

    public static int leafReach() {
        try {
            return LEAF_REACH.get();
        } catch (IllegalStateException notLoaded) {
            return LEAF_REACH.getDefault();
        }
    }
}
