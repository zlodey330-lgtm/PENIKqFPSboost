package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает рисование дождя и снега (струи и хлопья). */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideWeather(MultiBufferSource buffers, Vec3 camera, WeatherRenderState state,
									  CallbackInfo ci) {
		if (MobConfig.hideWeather) {
			ci.cancel();
		}
	}
}
