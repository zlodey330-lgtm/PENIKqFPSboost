package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает небо (цвет, закат, небо Края) и отдельно светила со звёздами. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

	@Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
	private void fpsboost$skyDisc(int color, CallbackInfo ci) {
		if (MobConfig.hideSky) {
			ci.cancel();
		}
	}

	@Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
	private void fpsboost$darkDisc(CallbackInfo ci) {
		if (MobConfig.hideSky) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void fpsboost$sunrise(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
		if (MobConfig.hideSky) {
			ci.cancel();
		}
	}

	@Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
	private void fpsboost$endSky(CallbackInfo ci) {
		if (MobConfig.hideSky) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
	private void fpsboost$sunMoonStars(PoseStack poseStack, float a, float b, float c, MoonPhase phase,
									   float d, float e, CallbackInfo ci) {
		if (MobConfig.hideSunMoonStars) {
			ci.cancel();
		}
	}
}
