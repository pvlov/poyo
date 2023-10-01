package org.pvlov;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
			} catch (RuntimeException e) {
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

    public void saveConfig() {
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

    public Optional<String> getString(String key) {
        return data.get(key) instanceof String val ? Optional.of(val) : Optional.empty();
    }

    // Maybe make this generic idk, would have to use a class parameter tho
    @SuppressWarnings("unchecked")
    public Optional<List<String>> getStringArray(String key) {
        if (data.containsKey(key) && data.get(key) instanceof List<?> list
                && !list.isEmpty() && list.get(0) instanceof String) {
            return Optional.of((List<String>) list);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<Long>> getLongArray(String key) {
        if (data.containsKey(key) && data.get(key) instanceof List<?> list
                && !list.isEmpty() && list.get(0) instanceof Long) {
            return Optional.of((List<Long>) list);
        }
        return Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        return data.get(key) instanceof Integer val ? Optional.of(val) : Optional.empty();
    }

    public void setConfig(String key, String value) {
        data.put(key, value);
    }

    private String getWorkingDir() {
        String folder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        folder = folder.replace("\\", "/");
        folder = folder.substring(0, folder.lastIndexOf("/") + 1);
        return folder;
    }

}
