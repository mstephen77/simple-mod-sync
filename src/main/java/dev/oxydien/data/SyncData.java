package dev.oxydien.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.oxydien.enums.SyncModificationType;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncData {
    public static final List<Integer> ALLOWED_SYNC_VERSIONS = List.of(1,2,3);

    private final int syncVersion;
    private final List<Content> sync;
    private final List<Modification> modify;

    public SyncData(int syncVersion, List<Content> content, List<Modification> modify) {
        this.syncVersion = syncVersion;
        this.sync = content;
        this.modify = modify;
    }

    /// Used for parsing the sync data
    public static SyncData fromJson(JsonObject jsonObject) {
        // Check if the sync version is supported
        int syncVersion = JsonHelper.getInt(jsonObject, "sync_version");
        if (!ALLOWED_SYNC_VERSIONS.contains(syncVersion)) {
            throw new IllegalArgumentException("Invalid sync version: " + syncVersion);
        }

        List<SyncData.Content> contentList = new ArrayList<>();
        List<SyncData.Modification> modifications = new ArrayList<>();

        // Parse the sync data content
        int index = 0;
        JsonElement contentElement = jsonObject.get("sync");
        if (contentElement == null) {
            contentElement = jsonObject.get("content");
        }
        if (!contentElement.isJsonArray()) {
            throw new IllegalArgumentException("'sync' or 'content' is not an array in sync data");
        }
        for (JsonElement element : contentElement.getAsJsonArray()) {
            JsonObject contentObject = element.getAsJsonObject();

            SyncData.Content content = SyncData.Content.fromJson(index, contentObject);
            contentList.add(content);
            index++;
        }

        // Parse the sync data modifications (if any)
        JsonElement modifyElement = jsonObject.get("modify");
        if (modifyElement != null && modifyElement.isJsonArray()) {
            index = 0;
            for (JsonElement element : modifyElement.getAsJsonArray()) {
                JsonObject modifyObject = element.getAsJsonObject();
                SyncData.Modification modification = SyncData.Modification.fromJson(index, modifyObject);
                modifications.add(modification);
                index++;
            }
        }

        return new SyncData(syncVersion, contentList, modifications);
    }

    public int getSyncVersion() {
        return syncVersion;
    }

    public List<Content> getContent() {
        return Collections.unmodifiableList(this.sync);
    }

    public List<Modification> getModify() {
        return Collections.unmodifiableList(this.modify);
    }

    public static class Content {
        /// Used for assigning status
        private final int index;
        /// Used for downloading the given content
        private final String url;
        /// Used to determine if the content is already downloaded, or should be updated / removed
        private final String version;
        /// File name (prefix, without file extension)
        private final String name;
        /// Used to determine the folder where the content should be downloaded
        @Nullable private final String directoryOverride;
        /// File type (mod, resourcepack, shaderpack, datapack, config)
        @Nullable private final String type;

        public Content(int index, String url, String version, String fileName, @Nullable String directory, @Nullable String type) {
            this.index = index;
            this.url = url;
            this.version = version;
            this.name = fileName;
            this.directoryOverride = directory;
            this.type = type;
        }

        /// Used for parsing the sync data
        public static Content fromJson(int index, JsonObject jsonObject) {
            String url = JsonHelper.getString(jsonObject, "url");
            String version = JsonHelper.getString(jsonObject, "version");
            String name = jsonObject.has("name") ? JsonHelper.getString(jsonObject, "name") : JsonHelper.getString(jsonObject, "mod_name");
            String directory = jsonObject.get("directory") != null ? jsonObject.get("directory").getAsString() : null;
            String type = jsonObject.get("type") != null ? jsonObject.get("type").getAsString() : null;
            return new SyncData.Content(index, url, version, name, directory, type);
        }

        /// Used for assigning status
        public int getIndex() {
            return this.index;
        }
        /// Used for downloading the given content
        public String getUrl() {
            return this.url;
        }
        /// File name (prefix, without file extension)
        public String getName() {
            return this.name;
        }
        /// Used to determine if the content is already downloaded, or should be updated / removed
        public String getVersion() {
            return this.version;
        }
        /// Used to determine the folder where the content should be downloaded
        @Nullable
        public String getDirectoryOverride() {
            return this.directoryOverride;
        }
        /// File type (mod, resourcepack, shaderpack, datapack, config)
        @Nullable
        public String getType() {
            return this.type;
        }


        /**
         * Gets the folder name for the given content type.
         * <p>This is used to determine the folder where the content should be downloaded.
         * @return The folder name (e.g. "mods", "resourcepacks", etc.)
         */
        public String getTypeFolder() {
            return switch (this.getType()) {
                case "resourcepack" -> "resourcepacks";
                case "shader" -> "shaderpacks";
                case "datapack" -> "datapacks";
                case "config" -> "config";
                case null, default -> "mods";
            };
        }

        /**
         * Gets the file extension for the given content type.
         * <p>This is used to determine the file extension when downloading the content.
         * @return The file extension (e.g. ".jar", ".zip")
         */
        public String getFileExtension() {
            return switch (this.getType()) {
                case "resourcepack", "shader", "datapack" -> ".zip";
                case null, default -> ".jar";
            };
        }
    }

    public static class Modification {
        private final int index;
        private final SyncModificationType type;
        private final String pattern;
        private final String path;
        @Nullable private final String result;

        public Modification(int index, SyncModificationType type, String path, String pattern, @Nullable String result) {
            this.index = index;
            this.type = type;
            this.pattern = pattern;
            this.path = path;
            this.result = result;
        }

        public static Modification fromJson(int index, JsonObject jsonObject) {
            String typeStr = JsonHelper.getString(jsonObject, "type");
            SyncModificationType type;
            switch (typeStr) {
                case "remove" -> type = SyncModificationType.REMOVE;
                case "rename" -> type = SyncModificationType.RENAME;
                default -> type = null;
            }
            String path = JsonHelper.getString(jsonObject, "path");
            String pattern = JsonHelper.getString(jsonObject, "pattern");
            String result = jsonObject.get("result") != null ? jsonObject.get("result").getAsString() : null;
            return new SyncData.Modification(index, type, path, pattern, result);
        }

        public int getIndex() {
            return this.index;
        }
        public SyncModificationType getType() {
            return this.type;
        }
        public String getPattern() {
            return this.pattern;
        }
        public String getPath() {
            return this.path;
        }
        @Nullable
        public String getResult() {
            return this.result;
        }
    }
}
