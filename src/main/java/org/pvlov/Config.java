package org.pvlov;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.pvlov.Bot.LOG;

public class Config {

	public static Config INSTANCE = new Config();

	private Map<String, Object> data;

	private Config() {
		loadConfig();
	}

	public void loadConfig() {
		try {
			File file = new File(getWorkingDir() + "/config.yml");
			if (!file.exists()) {
				file.createNewFile();
			}
			try (var inputStream = new FileInputStream(file)) {
				data = new Yaml().loadAs(inputStream, Map.class);
			} catch (RuntimeException ignored) {
				LOG.error("Error parsing config.yml, make sure it's valid YAML");
			}

			// config file containing no / invalid tokens
			if (data == null) {
				this.data = new HashMap<>();
				return;
			}

			// remove null values from Map
			this.data = data.entrySet().stream().filter(
					entry -> entry.getValue() != null).collect(
					Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void saveConfig() {
		try {
			File file = new File(getWorkingDir() + "/config.yml");
			if (!file.exists()) {
				file.createNewFile();
			}
			var writer = new PrintWriter(file);
			new Yaml().dump(data, writer);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Optional<T> getEntry(String key, Class<T> clazz) {
		Object entry = data.get(key);
		return clazz.isInstance(entry) ? Optional.of(clazz.cast(entry)) : Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<Collection<T>> getCollectionEntry(String key, Class<T> clazz) {
		if (data.containsKey(key)
				&& data.get(key) instanceof Collection<?> collection
				&& !collection.isEmpty()
				&& clazz.isInstance(collection.stream().findAny().orElseThrow())) {
			return Optional.of((Collection<T>) collection);
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<List<T>> getListEntry(String key, Class<T> clazz) {
		if (data.containsKey(key)
				&& data.get(key) instanceof List<?> list
				&& !list.isEmpty()
				&& clazz.isInstance(list.get(0))) {
			return Optional.of((List<T>) list);
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public <K, V> Optional<Map<K, V>> getMapEntry(String key, Class<K> kClazz, Class<V> vClazz) {
		if (data.containsKey(key)
				&& data.get(key) instanceof Map<?, ?> map
				&& !map.isEmpty()
				&& kClazz.isInstance(map.entrySet().stream().findAny().orElseThrow().getKey())
				&& vClazz.isInstance(map.entrySet().stream().findAny().orElseThrow().getValue())) {
			return Optional.of((Map<K, V>) map);
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	public <K, VK, VV> Optional<Map<K, Map<VK, VV>>> getNestedMapEntry(String key, Class<K> kClazz, Class<VK> vkClazz, Class<VV> vvClazz) {
		if (data.containsKey(key)
				&& data.get(key) instanceof Map<?, ?> map
				&& !map.isEmpty()
				&& kClazz.isInstance(map.entrySet().stream().findAny().orElseThrow().getKey())
				&& map.entrySet().stream().findAny().orElseThrow() instanceof Map<?, ?> nestMap
				&& !nestMap.isEmpty()
				&& vkClazz.isInstance(nestMap.entrySet().stream().findAny().orElseThrow().getKey())
				&& vvClazz.isInstance(nestMap.entrySet().stream().findAny().orElseThrow().getValue())) {
			return Optional.of((Map<K, Map<VK, VV>>) map);
		}
		return Optional.empty();
	}


	public synchronized void setConfig(String key, Object value) {
		data.put(key, value);
	}

	private String getWorkingDir() {
		String folder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		folder = folder.replace("\\", "/");
		folder = folder.substring(0, folder.lastIndexOf("/") + 1);
		return folder;
	}

}
