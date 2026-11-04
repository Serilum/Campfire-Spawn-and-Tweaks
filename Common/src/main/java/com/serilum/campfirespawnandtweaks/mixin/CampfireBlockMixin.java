package com.serilum.campfirespawnandtweaks.mixin;

import com.serilum.campfirespawnandtweaks.events.CampfireEvent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CampfireBlock.class, priority = 1001)
public class CampfireBlockMixin {
	@Inject(method = "getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "RETURN"), cancellable = true)
	public void getStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		cir.setReturnValue(CampfireEvent.onCampfirePlacement(cir.getReturnValue(), context.getPlayer()));
	}
}
