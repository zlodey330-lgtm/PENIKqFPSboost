package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Не рисует текст на табличках дальше заданного расстояния. */
@Mixin(AbstractSignRenderer.class)
public abstract class SignTextMixin {

	@Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
	private void fpsboost$signDistance(SignRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, boolean front, CallbackInfo ci) {
		int limit = MobConfig.signDistance;
		if (limit >= MobConfig.SIGN_MAX) {
			return;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		double dx = state.blockPos.getX() + 0.5;
		double dy = state.blockPos.getY() + 0.5;
		double dz = state.blockPos.getZ() + 0.5;
		if (player.distanceToSqr(dx, dy, dz) > (double) limit * limit) {
			ci.cancel();
		}
	}
}
