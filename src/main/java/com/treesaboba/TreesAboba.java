package com.treesaboba;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TreesAboba.MOD_ID)
public class TreesAboba {
    public static final String MOD_ID = "trees_aboba";
    public static final Logger LOGGER = LoggerFactory.getLogger("Trees Aboba");

    public TreesAboba(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TreesAbobaConfig.SPEC);
    }
}
