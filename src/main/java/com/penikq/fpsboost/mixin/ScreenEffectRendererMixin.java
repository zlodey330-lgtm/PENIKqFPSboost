package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Эффекты на весь экран: огонь перед глазами и анимация тотема. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

	@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
	private static void fpsboost$hideFire(PoseStack poseStack, MultiBufferSource buffers,
										  TextureAtlasSprite sprite, CallbackInfo ci) {
		if (MobConfig.hideFireSelf) {
			ci.cancel();
		}
	}

	@Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
	private void fpsboost$hideTotem(ItemStack stack, RandomSource random, CallbackInfo ci) {
		if (MobConfig.hideTotemAnim) {
			ci.cancel();
		}
	}
}
