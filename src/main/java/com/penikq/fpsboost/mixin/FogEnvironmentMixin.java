package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Убирает густой туман под водой и в лаве: окружение просто не применяется. */
@Mixin({WaterFogEnvironment.class, LavaFogEnvironment.class})
public abstract class FogEnvironmentMixin {

	@Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
	private void fpsboost$noFog(FogType type, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (MobConfig.hideFog) {
			cir.setReturnValue(false);
		}
	}
}
