package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Убирает туман под водой и в лаве: после ванильной настройки отодвигаем его очень далеко. */
@Mixin({WaterFogEnvironment.class, LavaFogEnvironment.class})
public abstract class FogEnvironmentMixin {

	@Inject(method = "setupFog", at = @At("TAIL"))
	private void fpsboost$noFog(FogData data, Camera camera, ClientLevel level, float renderDistance,
			DeltaTracker tracker, CallbackInfo ci) {
		if (MobConfig.hideFog) {
			data.environmentalStart = 100000.0F;
			data.environmentalEnd = 100001.0F;
		}
	}
}
