package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает трещины на блоке, пока его ломают. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noCracks(int id, BlockPos pos, int progress, CallbackInfo ci) {
		if (MobConfig.hideBreakAnim) {
			ci.cancel();
		}
	}
}
