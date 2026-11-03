package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает блеск зачарований у предметов (в руке, в инвентаре, на земле). */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemFoilMixin {

	@Inject(method = "setFoilType", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noGlint(ItemStackRenderState.FoilType type, CallbackInfo ci) {
		if (MobConfig.hideEnchantGlint) {
			ci.cancel();
		}
	}
}
