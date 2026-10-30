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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Все настройки мода: дальность отрисовки каждого моба (вкладка 3) и переключатели вкладки 2.
 * Моб, которого нет в таблице, рисуется как обычно (без лимита).
 * Именованные конфиги хранятся в папке config/fpsboost-configs/.
 */
public final class MobConfig {
	/** Максимум ползунка. Значение MAX означает "без лимита". */
	public static final int MAX = 128;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<EntityType<?>, Integer> LIMITS = new HashMap<>();

	// Вкладка 2 "Умная отрисовка"
	public static final int SIMPLIFY_MIN = 8;
	public static final int SIMPLIFY_MAX = 64;
	/** Упрощать далёких мобов (без брони, предметов в руках и ника). */
	public static boolean simplifyFar = false;
	/** С какой дистанции (в блоках) моб считается далёким. */
	public static int simplifyDistance = 24;
	/** Не рисовать мобов, скрытых за блоками. */
	public static boolean occlusionCull = false;

	private MobConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("fpsboost.json");
	}

	private static Path profilesDir() {
		return FabricLoader.getInstance().getConfigDir().resolve("fpsboost-configs");
	}

	// ---------- дальности мобов ----------

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

	/** Ставит дальность 0 (не рисовать) всем переданным мобам. */
	public static void disableAll(Collection<EntityType<?>> types) {
		for (EntityType<?> type : types) {
			LIMITS.put(type, 0);
		}
	}

	// ---------- чтение и запись ----------

	private static JsonObject toJson() {
		JsonObject distances = new JsonObject();
		for (Map.Entry<EntityType<?>, Integer> entry : LIMITS.entrySet()) {
			distances.addProperty(BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString(), entry.getValue());
		}
		JsonObject root = new JsonObject();
		root.addProperty("simplifyFar", simplifyFar);
		root.addProperty("simplifyDistance", simplifyDistance);
		root.addProperty("occlusionCull", occlusionCull);
		root.add("mobDistances", distances);
		return root;
	}

	private static void fromJson(JsonObject root) {
		LIMITS.clear();
		if (root.has("simplifyFar")) {
			simplifyFar = root.get("simplifyFar").getAsBoolean();
		}
		if (root.has("simplifyDistance")) {
			simplifyDistance = Math.max(SIMPLIFY_MIN, Math.min(SIMPLIFY_MAX, root.get("simplifyDistance").getAsInt()));
		}
		if (root.has("occlusionCull")) {
			occlusionCull = root.get("occlusionCull").getAsBoolean();
		}
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
	}

	private static boolean readInto(Path path) {
		if (!Files.exists(path)) {
			return false;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			fromJson(JsonParser.parseReader(reader).getAsJsonObject());
			return true;
		} catch (Exception e) {
			FpsBoostClient.LOGGER.warn("Не удалось прочитать " + path.getFileName() + ", использую значения по умолчанию", e);
			LIMITS.clear();
			return false;
		}
	}

	private static boolean writeTo(Path path) {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(toJson(), writer);
			}
			return true;
		} catch (IOException e) {
			FpsBoostClient.LOGGER.warn("Не удалось сохранить " + path.getFileName(), e);
			return false;
		}
	}

	public static void load() {
		LIMITS.clear();
		readInto(file());
	}

	public static void save() {
		writeTo(file());
	}

	// ---------- именованные конфиги ----------

	/** Оставляет в названии только буквы, цифры, пробел, "_" и "-". Возвращает null, если ничего не осталось. */
	public static String sanitizeName(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.trim().replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
		if (name.length() > 24) {
			name = name.substring(0, 24).trim();
		}
		return name.isEmpty() ? null : name;
	}

	private static Path profileFile(String name) {
		return profilesDir().resolve(name + ".json");
	}

	public static List<String> listProfiles() {
		List<String> names = new ArrayList<>();
		Path dir = profilesDir();
		if (!Files.isDirectory(dir)) {
			return names;
		}
		try (Stream<Path> files = Files.list(dir)) {
			files.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".json"))
					.map(n -> n.substring(0, n.length() - 5))
					.forEach(names::add);
		} catch (IOException e) {
			FpsBoostClient.LOGGER.warn("Не удалось прочитать список конфигов", e);
		}
		names.sort(Comparator.comparing(String::toLowerCase));
		return names;
	}

	public static boolean saveProfile(String name) {
		return writeTo(profileFile(name));
	}

	/** Загружает конфиг и сразу делает его текущим (записывает в основной файл). */
	public static boolean loadProfile(String name) {
		if (!readInto(profileFile(name))) {
			return false;
		}
		save();
		return true;
	}

	public static boolean deleteProfile(String name) {
		try {
			return Files.deleteIfExists(profileFile(name));
		} catch (IOException e) {
			FpsBoostClient.LOGGER.warn("Не удалось удалить конфиг " + name, e);
			return false;
		}
	}
}
