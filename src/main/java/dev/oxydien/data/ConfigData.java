package dev.oxydien.data;

import com.google.gson.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Used to save information about config content (NOT CONFIG OF THIS MOD)
 * Theoretically, can be used as a bundle not just for config
 */
public class ConfigData {
    public static final int CURRENT_SCHEME_VERSION = 1;
    public static final List<Integer> ALLOWED_SCHEME_VERSIONS = List.of(1);

    private final int schemeVersion;
    /// List of modified files (absolute path)
    private final List<String> config;

    public ConfigData(List<String> config) {
        this.schemeVersion = CURRENT_SCHEME_VERSION;
        this.config = config;
    }

    public static ConfigData fromJson(JsonObject jsonObject) {
        int schemeVersion = jsonObject.get("scheme_version").getAsInt();
        if (!ALLOWED_SCHEME_VERSIONS.contains(schemeVersion)) {
            throw new IllegalArgumentException("Invalid scheme version: " + schemeVersion);
        }

        List<String> config = jsonObject.get("config").getAsJsonArray()
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        return new ConfigData(config);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("scheme_version", schemeVersion);
        JsonArray configArray = new JsonArray();
        for (String configItem : config) {
            configArray.add(configItem);
        }
        jsonObject.add("config", configArray);
        return jsonObject;
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }
    /// List of modified files (absolute path)
    public List<String> getConfig() {
        return config;
    }
}
