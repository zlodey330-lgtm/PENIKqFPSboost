package com.penikq.fpsboost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.penikq.fpsboost.HandScale;
import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Размер предмета в руке от первого лица: отдельно тотем, меч, щит, зелье (пока пьёшь) и еда (пока ешь).
 * Предмет уменьшается вокруг своего места в руке, поэтому не улетает в сторону.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

	@Unique
	private boolean fpsboost$pushed = false;

	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void fpsboost$scaleStart(AbstractClientPlayer player, float partialTick, float pitch,
									 InteractionHand hand, float swing, ItemStack stack, float equip,
									 PoseStack poseStack, SubmitNodeCollector collector, int light,
									 CallbackInfo ci) {
		fpsboost$pushed = false;
		if (!MobConfig.handScalingActive()) {
			return;
		}
		float scale = HandScale.scaleFor(player, hand, stack);
		if (scale >= 0.999F) {
			return;
		}
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		float px = side * 0.56F;
		float py = -0.52F;
		float pz = -0.72F;
		poseStack.pushPose();
		poseStack.translate(px, py, pz);
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-px, -py, -pz);
		fpsboost$pushed = true;
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void fpsboost$scaleEnd(AbstractClientPlayer player, float partialTick, float pitch,
								   InteractionHand hand, float swing, ItemStack stack, float equip,
								   PoseStack poseStack, SubmitNodeCollector collector, int light,
								   CallbackInfo ci) {
		if (fpsboost$pushed) {
			poseStack.popPose();
			fpsboost$pushed = false;
		}
	}
}
