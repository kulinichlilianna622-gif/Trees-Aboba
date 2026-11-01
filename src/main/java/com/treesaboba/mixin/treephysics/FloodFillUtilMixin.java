package com.treesaboba.mixin.treephysics;

import com.farcr.treephysics.api.flood_fill.TreeResult;
import com.treesaboba.LeafReach;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.farcr.treephysics.api.util.FloodFillUtil", remap = false)
public abstract class FloodFillUtilMixin {
    @Inject(
            method = "leafRule(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lcom/farcr/treephysics/api/flood_fill/TreeResult;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void trees_aboba$persistentLeaves(
            BlockPos fromPos,
            BlockPos toPos,
            BlockState fromState,
            BlockState toState,
            TreeResult result,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Boolean override = LeafReach.leafRule(fromPos, toPos, fromState, toState, result);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
