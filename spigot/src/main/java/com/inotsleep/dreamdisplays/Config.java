package com.inotsleep.dreamdisplays;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moandjiezana.toml.Toml;
import org.bukkit.Material;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Gson gson = new Gson();
    private final File configFile;
    private Toml toml;

    public LanguageSection language;
    public SettingsSection settings;
    public StorageSection storage;
    public PermissionsSection permissions;
    public final Map<String, Object> messages = new HashMap<>();

    public static class LanguageSection {
        private final Toml toml;
        public LanguageSection(Toml toml) { this.toml = toml; }
        public String getMessageLanguage() {
            String lang = toml.getString("language.message_language");
            return lang != null ? lang : "en";
        }
    }

    public static class SettingsSection {
        private final Toml toml;
        public SettingsSection(Toml toml) { this.toml = toml; }

        public String getWebhookUrl() {
            String url = toml.getString("settings.webhook_url");
            return url != null ? url : "";
        }
        public int getReportCooldown() {
            Long cooldown = toml.getLong("settings.report_cooldown");
            return cooldown != null ? Math.toIntExact(cooldown) : 15000;
        }
        public String getRepoName() {
            String name = toml.getString("settings.repo_name");
            return name != null ? name : "dreamdisplays";
        }
        public String getRepoOwner() {
            String owner = toml.getString("settings.repo_owner");
            return owner != null ? owner : "arsmotorin";
        }

        public Material getSelectionMaterial() {
            String mat = toml.getString("settings.selection_material");
            if (mat == null) return Material.DIAMOND_AXE;
            Material m = Material.matchMaterial(mat);
            return m != null ? m : Material.DIAMOND_AXE;
        }

        public Material getBaseMaterial() {
            String mat = toml.getString("settings.base_material");
            if (mat == null) return Material.BLACK_CONCRETE;
            Material m = Material.matchMaterial(mat);
            return m != null ? m : Material.BLACK_CONCRETE;
        }

        public int getCUIParticleRenderDelay() {
            Long delay = toml.getLong("settings.CUI_particle_render_delay");
            return delay != null ? Math.toIntExact(delay) : 2;
        }
        public int getCUIParticlesPerBlock() {
            Long count = toml.getLong("settings.CUI_particles_per_block");
            return count != null ? Math.toIntExact(count) : 3;
        }
        public int getCUIParticlesColor() {
            Long color = toml.getLong("settings.CUI_particles_color");
            return color != null ? Math.toIntExact(color) : 65280;
        }
        public int getMinWidth() {
            Long width = toml.getLong("settings.min_width");
            return width != null ? Math.toIntExact(width) : 1;
        }
        public int getMinHeight() {
            Long height = toml.getLong("settings.min_height");
            return height != null ? Math.toIntExact(height) : 1;
        }
        public int getMaxWidth() {
            Long width = toml.getLong("settings.max_width");
            return width != null ? Math.toIntExact(width) : 32;
        }
        public int getMaxHeight() {
            Long height = toml.getLong("settings.max_height");
            return height != null ? Math.toIntExact(height) : 24;
        }
        public double getMaxRenderDistance() {
            Double distance = toml.getDouble("settings.maxRenderDistance");
            return distance != null ? distance : 96.0;
        }

        // Cached properties
        public String webhookUrl;
        public int reportCooldown;
        public String repoName;
        public String repoOwner;
        public Material selectionMaterial;
        public Material baseMaterial;
        public int particleRenderDelay;
        public int particlesPerBlock;
        public int particlesColor;
        public int minWidth;
        public int minHeight;
        public int maxWidth;
        public int maxHeight;
        public double maxRenderDistance;

        private void cache() {
            webhookUrl = getWebhookUrl();
            reportCooldown = getReportCooldown();
            repoName = getRepoName();
            repoOwner = getRepoOwner();
            selectionMaterial = getSelectionMaterial();
            baseMaterial = getBaseMaterial();
            particleRenderDelay = getCUIParticleRenderDelay();
            particlesPerBlock = getCUIParticlesPerBlock();
            particlesColor = getCUIParticlesColor();
            minWidth = getMinWidth();
            minHeight = getMinHeight();
            maxWidth = getMaxWidth();
            maxHeight = getMaxHeight();
            maxRenderDistance = getMaxRenderDistance();
        }
    }

    public static class StorageSection extends me.inotsleep.utils.storage.StorageSettings {
        private final Toml toml;

        // Public fields that shadow parent's private fields
        public String storageType;
        public String sqliteFile;
        public String host;
        public String port;
        public String database;
        public String password;
        public String username;
        public String options;
        public String tablePrefix;

        public StorageSection(Toml toml) {
            this.toml = toml;
        }

        private void cache() {
            String type = toml.getString("storage.storageType");
            this.storageType = type != null ? type : "SQLITE";

            String file = toml.getString("storage.sqliteFile");
            this.sqliteFile = file != null ? file : "database.db";

            String hostVal = toml.getString("storage.host");
            this.host = hostVal != null ? hostVal : "localhost";

            String portVal = toml.getString("storage.port");
            this.port = portVal != null ? portVal : "3306";

            String dbVal = toml.getString("storage.database");
            this.database = dbVal != null ? dbVal : "my_database";

            String passVal = toml.getString("storage.password");
            this.password = passVal != null ? passVal : "veryStrongPassword";

            String userVal = toml.getString("storage.username");
            this.username = userVal != null ? userVal : "username";

            String optVal = toml.getString("storage.options");
            this.options = optVal != null ? optVal : "autoReconnect=true&useSSL=false;";

            String prefixVal = toml.getString("storage.tablePrefix");
            this.tablePrefix = prefixVal != null ? prefixVal : "";
        }
    }

    public static class PermissionsSection {
        private final Toml toml;
        public PermissionsSection(Toml toml) { this.toml = toml; }

        // Cached properties
        public String premium;
        public String delete;
        public String list;
        public String reload;

        private void cache() {
            String prem = toml.getString("permissions.premium");
            premium = prem != null ? prem : "group.premium";

            String del = toml.getString("permissions.delete");
            delete = del != null ? del : "dreamdisplays.delete";

            String listVal = toml.getString("permissions.list");
            list = listVal != null ? listVal : "dreamdisplays.list";

            String reloadVal = toml.getString("permissions.reload");
            reload = reloadVal != null ? reloadVal : "dreamdisplays.reload";
        }
    }

    public Config(DreamDisplaysPlugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "config.toml");

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("config.toml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create default config.toml: " + e.getMessage());
            }
        }

        extractLangFiles(plugin, true); // Force overwrite on first run
        load();

        this.language = new LanguageSection(toml);
        this.settings = new SettingsSection(toml);
        this.storage = new StorageSection(toml);
        this.permissions = new PermissionsSection(toml);

        // Cache values
        settings.cache();
        storage.cache();
        permissions.cache();

        String selectedLang = language.getMessageLanguage();
        me.inotsleep.utils.logging.LoggingManager.log("Loading messages for language: " + selectedLang);
        setMessages(selectedLang);
    }

    private void extractLangFiles(DreamDisplaysPlugin plugin, boolean overwrite) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        // Assuming you have a method to get all resource paths under a directory
        // This part is pseudo-code and needs to be adapted to how you can list resources in your environment.
        // For Bukkit, you might need to list them manually or use a library that can scan JAR resources.
        // Here's a conceptual implementation:
        List<String> langFiles = List.of("be_by.json", "cs_cz.json", "de_de.json", "en.json", "es_es.json", "fr_fr.json", "it_it.json", "ja_jp.json", "ko_kr.json", "nl_nl.json", "pl_pl.json", "pt_br.json", "ru.json", "sv_se.json", "tr_tr.json", "uk_ua.json", "zh_cn.json", "zh_tw.json");
        for (String fileName : langFiles) {
            try (InputStream in = plugin.getResource("assets/dreamdisplays/lang/" + fileName)) {
                if (in != null) {
                    File targetFile = new File(langFolder, fileName);
                    if (overwrite || !targetFile.exists()) {
                        Files.copy(in, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not extract language file " + fileName + ": " + e.getMessage());
            }
        }
    }

    private void load() {
        try {
            toml = new Toml().read(configFile);
        } catch (Exception e) {
            toml = new Toml(); // Empty toml with defaults
        }
    }

    public void reload(DreamDisplaysPlugin plugin) {
        load();
        this.language = new LanguageSection(toml);
        this.settings = new SettingsSection(toml);
        this.storage = new StorageSection(toml);
        this.permissions = new PermissionsSection(toml);

        // Cache values
        settings.cache();
        storage.cache();
        permissions.cache();
        extractLangFiles(plugin, false); // Don't overwrite user changes
        setMessages(language.getMessageLanguage());
    }

    private void setMessages(String lang) {
        File langFile = new File(configFile.getParentFile(), "lang/" + lang + ".json");
        if (!langFile.exists()) {
            me.inotsleep.utils.logging.LoggingManager.warn("Language file not found: " + langFile.getPath() + ", falling back to en.json");
            langFile = new File(configFile.getParentFile(), "lang/en.json");
        }

        if (langFile.exists()) {
            try (InputStream is = new FileInputStream(langFile)) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> msgs = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                if (msgs != null && !msgs.isEmpty()) {
                    messages.clear();
                    messages.putAll(msgs);
                } else {
                    me.inotsleep.utils.logging.LoggingManager.warn("No messages found in language file: " + lang);
                }
            } catch (IOException e) {
                me.inotsleep.utils.logging.LoggingManager.error("Error loading language file: " + lang, e);
            }
        } else {
            me.inotsleep.utils.logging.LoggingManager.error("Could not load any language file for: " + lang);
        }
    }
}
