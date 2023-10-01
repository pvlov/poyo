package org.pvlov;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Config {

    public static Config INSTANCE = new Config();

    private HashMap<String, Object> data;

    private Config() {
        loadConfig();
    }

    public void loadConfig() {
        try {
            File file = new File(getWorkingDir() + "/config.yml");
            if (!file.exists())
                file.createNewFile();

            var inputStream = new FileInputStream(file);
            data = new Yaml().load(inputStream);
            inputStream.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveConfig() {
        try {
            File file = new File(getWorkingDir() + "/config.yml");
            if (!file.exists())
                file.createNewFile();

            var writer = new PrintWriter(file);
            new Yaml().dump(data, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> getString(String key) {
        if (!data.containsKey(key))
            return Optional.empty();
        return Optional.of((String) data.get(key));
    }

    public Optional<List<String>> getStringArray(String key) {
        if (!data.containsKey(key)) {
            return Optional.empty();
        }

        return Optional.of((List<String>) data.get(key));
    }

    public Optional<List<Long>> getLongArray(String key) {
        if (!data.containsKey(key))
            return Optional.empty();
        return Optional.of((List<Long>) data.get(key));
    }

    public Optional<Integer> getInt(String key) {
        if (!data.containsKey(key))
            return Optional.empty();
        return Optional.of((int) data.get(key));
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
