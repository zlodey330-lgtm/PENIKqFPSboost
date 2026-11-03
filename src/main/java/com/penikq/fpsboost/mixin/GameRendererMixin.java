package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает тряску камеры, когда тебя бьют. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noHurtShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		if (MobConfig.hideHurtCam) {
			ci.cancel();
		}
	}
}
