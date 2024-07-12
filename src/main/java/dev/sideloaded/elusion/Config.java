package dev.sideloaded.elusion;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Config {
    private static Config instance;

    @SerializedName("token")
    private String token;

    @SerializedName("guildId")
    private long guildId;

    @SerializedName("mongoConnectionString")
    private String mongoConnectionString;

    @SerializedName("authorizedUsers")
    private List<String> authorizedUsers;

    @SerializedName("allowedChannels")
    private List<Long> allowedChannels;

    private Config() {
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    private static Config loadConfig() {
        Gson gson = new Gson();
        Config config = null;

        // Try to load from file system
        Path configPath = Paths.get("config.json");
        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                config = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                System.err.println("Error reading config file: " + e.getMessage());
            }
        }

        // If not found, try to load from resources
        if (config == null) {
            try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("config.json")) {
                if (inputStream != null) {
                    try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                        config = gson.fromJson(reader, Config.class);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading config from resources: " + e.getMessage());
            }
        }

        if (config == null) {
            throw new RuntimeException("Failed to load config.json");
        }

        return config;
    }

    public String getToken() {
        return token;
    }

    public long getGuildId() {
        return guildId;
    }

    public String getMongoConnectionString() {
        return mongoConnectionString;
    }

    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public List<Long> getAllowedChannels() {
        return allowedChannels;
    }
}