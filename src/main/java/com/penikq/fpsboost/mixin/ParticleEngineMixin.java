package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Отключение частиц из вкладки "Частицы и мир".
 * Обычные частицы создаются через createParticle. А осколки при ломании блока и при ударах по нему
 * игра добавляет напрямую (destroy и crack), поэтому их перехватываем отдельно.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideParticle(ParticleOptions options, double x, double y, double z,
									   double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
		if (options instanceof BlockParticleOption blockOption) {
			// Частицы блока: важные блоки (исключения) показываем всегда
			if (MobConfig.isBlockException(blockOption.getState().getBlock())) {
				return;
			}
			if (MobConfig.hideBlockParticles) {
				cir.setReturnValue(null);
				return;
			}
		}
		if (MobConfig.hasHiddenParticles() && MobConfig.isParticleHidden(options.getType())) {
			cir.setReturnValue(null);
		}
	}

	/** Осколки при разрушении блока. */
	@Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideDestroy(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (MobConfig.hideBlockParticles && !MobConfig.isBlockException(state.getBlock())) {
			ci.cancel();
		}
	}

	/** Осколки, пока бьёшь по блоку. */
	@Inject(method = "crack", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideCrack(BlockPos pos, Direction side, CallbackInfo ci) {
		if (!MobConfig.hideBlockParticles) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !MobConfig.isBlockException(mc.level.getBlockState(pos).getBlock())) {
			ci.cancel();
		}
	}
}
