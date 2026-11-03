package com.penikq.fpsboost.mixin;

import com.penikq.fpsboost.MobConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Правит готовое состояние отрисовки сущности (после того как игра его собрала):
 * огонь, свечение, тени, вращение предметов и упрощение далёких мобов.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "extractEntity", at = @At("RETURN"))
	private void fpsboost$adjustState(Entity entity, float partialTick,
									  CallbackInfoReturnable<EntityRenderState> cir) {
		EntityRenderState state = cir.getReturnValue();
		if (state == null) {
			return;
		}

		// Огонь: на себе (в третьем лице) и на остальных
		boolean self = entity == Minecraft.getInstance().player;
		if (self ? MobConfig.hideFireSelf : MobConfig.hideFireOthers) {
			state.displayFireAnimation = false;
		}

		// Свечение (эффект "Свечение", стрелы-прожекторы)
		if (MobConfig.hideGlow) {
			state.outlineColor = EntityRenderState.NO_OUTLINE;
		}

		// Тени под сущностями
		if (MobConfig.hideShadows) {
			state.shadowRadius = 0.0F;
			state.shadowPieces.clear();
		}

		// Предметы на земле без вращения и покачивания
		if (MobConfig.hideItemSpin && entity instanceof ItemEntity) {
			state.ageInTicks = 0.0F;
		}

		// Упрощение далёких мобов (вкладка 2)
		if (MobConfig.simplifyFar && entity instanceof LivingEntity && !(entity instanceof Player)) {
			double limit = MobConfig.simplifyDistance;
			if (state.distanceToCameraSq >= limit * limit) {
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
	}
}
