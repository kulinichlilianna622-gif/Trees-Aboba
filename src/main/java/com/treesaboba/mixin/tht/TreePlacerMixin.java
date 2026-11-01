package com.treesaboba.mixin.tht;

import com.treesaboba.ThtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after Tan's Huge Trees has written one chunk's part of a tree during world generation. */
@Pseudo
@Mixin(targets = "tannyjung.tanshugetrees_handcode.systems.world_gen.TreePlacer", remap = false)
public abstract class TreePlacerMixin {
    @Inject(
            method = "place(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/core/BlockPos;[III)V",
            at = @At("TAIL")
    )
    private static void trees_aboba$markSmallTree(
            LevelAccessor level,
            ServerLevel server,
            ChunkPos chunk,
            String id,
            String location,
            String pathSettings,
            BlockPos center,
            int[] rotationMirrored,
            int deadTreeLevel,
            int fallenDirection,
            CallbackInfo ci
    ) {
        ThtCompat.afterTreePlaced(level, chunk, location, pathSettings, center, rotationMirrored, deadTreeLevel, fallenDirection);
    }
}
