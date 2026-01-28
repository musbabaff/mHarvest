package com.blockstock.mharvest;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MHarvestPlugin extends JavaPlugin {
    private static MHarvestPlugin instance;
    private Economy economy;
    private DatabaseManager databaseManager;
    private MachineManager machineManager;
    private ConfigManager configManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Configleri Yükle (config.yml ve prices.yml)
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // 2. Gelişmiş Konsol Logosu & Başlangıç Mesajı
        printConsoleLogo();

        // 3. Vault & Ekonomi Kontrolü
        if (!setupEconomy()) {
            getLogger().severe("§c[!] Vault veya Ekonomi eklentisi bulunamadı! Eklenti devre dışı bırakılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Sistemleri Başlat
        try {
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.initialize();

            this.machineManager = new MachineManager(this);
            this.menuManager = new MenuManager(this);

            // 5. Komut ve Event Kayıtları
            getCommand("mharvest").setExecutor(new CommandHandler(this));
            getServer().getPluginManager().registerEvents(new MachineListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

            // 6. Görevleri Başlat (Hasat & Satış)
            this.machineManager.startTasks();

            getLogger().info("§a[✓] Tüm sistemler başarıyla optimize edildi ve başlatıldı.");
        } catch (Exception e) {
            getLogger().severe("§c[!] Eklenti başlatılırken bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Eklenti dosyalarını ve ayarlarını canlı olarak yeniler.
     */
    public void reloadPlugin() {
        try {
            // Config dosyasını diskten tekrar oku
            reloadConfig();

            // ConfigManager'ı tazeleyerek prices.yml dosyasındaki yeni fiyatları yükle
            this.configManager = new ConfigManager(this);

            // Mevcut makinelerin verilerini kaydet ve hologramları yenile
            if (this.machineManager != null) {
                this.machineManager.saveAll();
                // Yeni config ayarlarına göre tüm hologramları güncelle
                this.machineManager.cache.values().forEach(data -> this.machineManager.updateHologram(data));
            }

            getLogger().info("§e[!] Yapılandırma ve Fiyat Listesi başarıyla yenilendi.");
        } catch (Exception e) {
            getLogger().severe("§c[!] Reload sırasında hata: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        // Kapatırken verilerin kaybolmaması için kritik işlemler
        if (machineManager != null) {
            machineManager.saveAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("§6[M-Harvest] §7Veriler kaydedildi ve sistem güvenli şekilde kapatıldı.");
    }

    private void printConsoleLogo() {
        Bukkit.getConsoleSender().sendMessage("§6 ");
        Bukkit.getConsoleSender().sendMessage("§6  __  __   _    _                             _   ");
        Bukkit.getConsoleSender().sendMessage("§6 |  \\/  | | |  | |                           | |  ");
        Bukkit.getConsoleSender().sendMessage("§6 | \\  / | | |__| |  __ _  _ __ __   __ ___  ___ | |_ ");
        Bukkit.getConsoleSender().sendMessage("§e | |\\/| | |  __  | / _` || '__|\\ \\ / // _ \\/ __|| __|");
        Bukkit.getConsoleSender().sendMessage("§e | |  | | | |  | || (_| || |    \\ V /|  __/\\__ \\| |_ ");
        Bukkit.getConsoleSender().sendMessage("§6 |_|  |_| |_|  |_| \\__,_||_|     \\_/  \\___||___/ \\__|");
        Bukkit.getConsoleSender().sendMessage("§e            Advanced Auto-Harvest System §7v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage("§a             Status: Loading modules... ");
        Bukkit.getConsoleSender().sendMessage("§6 ");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // Getters
    public static MHarvestPlugin getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MachineManager getMachineManager() { return machineManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public MenuManager getMenuManager() { return menuManager; }
}