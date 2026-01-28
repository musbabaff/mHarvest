package com.blockstock.mharvest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final MHarvestPlugin plugin;
    private FileConfiguration pricesConfig;
    private FileConfiguration messages;
    // mHarvest - Fiyatları hızlıca çekmek için hafızada tutuyoruz
    private final Map<Material, Double> priceMap = new HashMap<>();

    public ConfigManager(MHarvestPlugin plugin) {
        this.plugin = plugin;
        loadFiles();
    }

    public void loadFiles() {
        File pFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pFile.exists()) plugin.saveResource("prices.yml", false);
        pricesConfig = YamlConfiguration.loadConfiguration(pFile);

        File mFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!mFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(mFile);

        // Fiyatları yükle
        loadPriceMap();
    }

    /**
     * prices.yml içindeki "prices" bölümünü tarayıp hafızaya alır.
     */
    private void loadPriceMap() {
        priceMap.clear();
        ConfigurationSection section = pricesConfig.getConfigurationSection("prices");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                double price = section.getDouble(key);
                priceMap.put(mat, price);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Gecersiz eşya ismi (prices.yml): " + key);
            }
        }
    }

    /**
     * mHarvest - Verilen materyalin birim fiyatını döndürür.
     */
    public double getPrice(Material material) {
        return priceMap.getOrDefault(material, 0.0);
    }

    public String getMsg(String key) {
        String prefix = messages.getString("prefix", "");
        String msg = messages.getString(key);
        if (msg == null) return ChatColor.RED + "Mesaj bulunamadı: " + key;
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    public String getRawMsg(String key) {
        String msg = messages.getString(key, "&c" + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}