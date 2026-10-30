package com.penikq.fpsboost;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Список мобов для вкладки "Оптимизация мобов": мирные, враждебные, боссы. Игроков здесь нет. */
public final class MobCatalog {

	public enum Section {
		PEACEFUL("МИРНЫЕ МОБЫ", 0xFF7DDC8C),
		HOSTILE("ВРАЖДЕБНЫЕ МОБЫ", 0xFFFF6B6B),
		BOSSES("БОССЫ", 0xFFFFC857);

		public final String title;
		public final int color;

		Section(String title, int color) {
			this.title = title;
			this.color = color;
		}
	}

	public record Mob(EntityType<?> type, Section section, String name) {
	}

	/** Мирные мобы, у которых в игре категория MISC, поэтому по категории их не найти. */
	private static final Set<String> EXTRA_PEACEFUL = Set.of(
			"minecraft:villager",
			"minecraft:wandering_trader",
			"minecraft:iron_golem",
			"minecraft:snow_golem",
			"minecraft:copper_golem"
	);

	private MobCatalog() {
	}

	public static List<Mob> build() {
		List<Mob> mobs = new ArrayList<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type == EntityType.PLAYER || !type.canSummon()) {
				continue;
			}
			Section section = classify(type);
			if (section == null) {
				continue;
			}
			mobs.add(new Mob(type, section, type.getDescription().getString()));
		}
		mobs.sort(Comparator.comparingInt((Mob m) -> m.section().ordinal())
				.thenComparing(Mob::name, String.CASE_INSENSITIVE_ORDER));
		return mobs;
	}

	private static Section classify(EntityType<?> type) {
		if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
			return Section.BOSSES;
		}
		String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
		if (EXTRA_PEACEFUL.contains(id)) {
			return Section.PEACEFUL;
		}
		switch (type.getCategory()) {
			case MONSTER:
				return Section.HOSTILE;
			case CREATURE:
			case AMBIENT:
			case AXOLOTLS:
			case UNDERGROUND_WATER_CREATURE:
			case WATER_CREATURE:
			case WATER_AMBIENT:
				return Section.PEACEFUL;
			default:
				return null;
		}
	}
}
