package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Упрощение далёких мобов: у мобов дальше заданной дистанции убираем ник, броню и голову-предмет.
 * Правим уже готовое состояние отрисовки, поэтому слои (броня и т.д.) просто ничего не рисуют.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "extractEntity", at = @At("RETURN"), require = 0)
	private void fpsboost$simplifyFar(Entity entity, float partialTick,
									  CallbackInfoReturnable<EntityRenderState> cir) {
		if (!MobConfig.simplifyFar) {
			return;
		}
		if (!(entity instanceof LivingEntity) || entity instanceof Player) {
			return;
		}
		EntityRenderState state = cir.getReturnValue();
		if (state == null) {
			return;
		}
		double limit = MobConfig.simplifyDistance;
		if (state.distanceToCameraSq < limit * limit) {
			return;
		}

		state.nameTag = null;

		if (state instanceof LivingEntityRenderState living) {
			living.headItem.clear();
		}
		if (state instanceof HumanoidRenderState humanoid) {
			humanoid.headEquipment = ItemStack.EMPTY;
			humanoid.chestEquipment = ItemStack.EMPTY;
			humanoid.legsEquipment = ItemStack.EMPTY;
			humanoid.feetEquipment = ItemStack.EMPTY;
			humanoid.rightHandItemState.clear();
			humanoid.leftHandItemState.clear();
		}
	}
}
