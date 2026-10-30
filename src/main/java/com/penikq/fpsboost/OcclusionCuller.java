package com.penikq.fpsboost;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Решает, что моб спрятан за блоками и его можно не рисовать.
 * Чтобы мобы не "выскакивали" при повороте головы:
 * - видимость проверяется лучами сразу в нескольких точках тела моба;
 * - после того как моб пропал из виду, он остаётся нарисованным ещё GRACE_TICKS тиков (0.5 сек);
 * - как только моб снова виден, он показывается мгновенно.
 */
public final class OcclusionCuller {
	/** Как часто (в тиках) перепроверять видимость одного моба. */
	private static final int RECHECK_TICKS = 4;
	/** Задержка перед скрытием: 10 тиков = 0.5 секунды. */
	private static final int GRACE_TICKS = 10;
	/** Мобов ближе этого расстояния (в блоках) всегда рисуем. */
	private static final double MIN_DIST = 6.0;

	/** id моба -> {последняя проверка, последний раз был виден} (в тиках мира). */
	private static final Map<Integer, long[]> CACHE = new HashMap<>();
	private static long lastCleanup = 0;

	private OcclusionCuller() {
	}

	public static boolean shouldHide(Entity entity, double camX, double camY, double camZ) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		if (level == null || mc.player == null) {
			return false;
		}
		// Только мобы. Игроков, боссов и особые случаи не трогаем.
		if (!(entity instanceof LivingEntity) || entity instanceof Player
				|| entity instanceof EnderDragon || entity instanceof WitherBoss) {
			return false;
		}
		if (entity.isCurrentlyGlowing() || entity.isPassenger() || entity.isVehicle()) {
			return false;
		}
		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		if (dx * dx + dy * dy + dz * dz < MIN_DIST * MIN_DIST) {
			return false;
		}

		long now = level.getGameTime();
		long[] entry = CACHE.get(entity.getId());
		if (entry == null) {
			// Новый моб: считаем видимым, дальше решит проверка.
			entry = new long[]{now - RECHECK_TICKS, now};
			CACHE.put(entity.getId(), entry);
		}
		if (now - entry[0] >= RECHECK_TICKS) {
			entry[0] = now;
			if (isVisible(level, entity, new Vec3(camX, camY, camZ))) {
				entry[1] = now;
			}
		}

		if (now - lastCleanup > 200) {
			lastCleanup = now;
			CACHE.values().removeIf(e -> now - e[0] > 100);
		}
		return now - entry[1] > GRACE_TICKS;
	}

	private static boolean isVisible(Level level, Entity entity, Vec3 cam) {
		AABB box = entity.getBoundingBox();
		double cx = (box.minX + box.maxX) / 2.0;
		double cz = (box.minZ + box.maxZ) / 2.0;
		double h = box.maxY - box.minY;
		double mid = box.minY + h * 0.5;
		double inset = 0.05;

		Vec3[] points = {
				new Vec3(cx, mid, cz),
				new Vec3(cx, box.maxY - inset, cz),
				new Vec3(cx, box.minY + inset, cz),
				new Vec3(box.minX + inset, mid, box.minZ + inset),
				new Vec3(box.maxX - inset, mid, box.minZ + inset),
				new Vec3(box.minX + inset, mid, box.maxZ - inset),
				new Vec3(box.maxX - inset, mid, box.maxZ - inset)
		};
		for (Vec3 p : points) {
			HitResult hit = level.clip(new ClipContext(cam, p, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
			if (hit.getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}
}
