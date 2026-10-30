package com.penikq.fpsboost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранит дальность отрисовки для каждого типа моба.
 * Моб, которого нет в таблице, рисуется как обычно (без лимита).
 */
public final class MobConfig {
	/** Максимум ползунка. Значение MAX означает "без лимита". */
	public static final int MAX = 128;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<EntityType<?>, Integer> LIMITS = new HashMap<>();

	private MobConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fpsboost.json");
	}

	/** Есть ли вообще заданные лимиты (быстрая проверка для рендера). */
	public static boolean hasLimits() {
		return !LIMITS.isEmpty();
	}

	/** Лимит в блоках или null, если для моба лимита нет. */
	public static Integer limit(EntityType<?> type) {
		return LIMITS.get(type);
	}

	/** Значение для ползунка: MAX, если лимита нет. */
	public static int get(EntityType<?> type) {
		return LIMITS.getOrDefault(type, MAX);
	}

	public static void set(EntityType<?> type, int distance) {
		if (distance >= MAX) {
			LIMITS.remove(type);
		} else {
			LIMITS.put(type, Math.max(0, distance));
		}
	}

	public static void resetAll() {
		LIMITS.clear();
	}

	public static void load() {
		LIMITS.clear();
		Path file = file();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (!root.has("mobDistances")) {
				return;
			}
			JsonObject distances = root.getAsJsonObject("mobDistances");
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
				if (distances.has(id)) {
					set(type, distances.get(id).getAsInt());
				}
			}
		} catch (Exception e) {
			FpsBoostClient.LOGGER.warn("Не удалось прочитать fpsboost.json, использую значения по умолчанию", e);
			LIMITS.clear();
		}
	}

	public static void save() {
		JsonObject distances = new JsonObject();
		for (Map.Entry<EntityType<?>, Integer> entry : LIMITS.entrySet()) {
			distances.addProperty(BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString(), entry.getValue());
		}
		JsonObject root = new JsonObject();
		root.add("mobDistances", distances);
		try (Writer writer = Files.newBufferedWriter(file())) {
			GSON.toJson(root, writer);
		} catch (IOException e) {
			FpsBoostClient.LOGGER.warn("Не удалось сохранить fpsboost.json", e);
		}
	}
}
