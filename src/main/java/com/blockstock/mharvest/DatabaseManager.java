package com.blockstock.mharvest;

import java.io.File;
import java.sql.*;
import org.bukkit.Location;

public class DatabaseManager {
    private final MHarvestPlugin plugin;
    private Connection connection;

    public DatabaseManager(MHarvestPlugin plugin) { this.plugin = plugin; }

    public void initialize() {
        try {
            File file = new File(plugin.getDataFolder(), "database.db");
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            Statement s = connection.createStatement();

            // Tablo oluşturma (auto_sell sütunu eklendi)
            s.execute("CREATE TABLE IF NOT EXISTS machines (id TEXT PRIMARY KEY, owner TEXT, world TEXT, x INT, y INT, z INT, level INT, fuel INT, friends TEXT, storage TEXT, auto_sell INTEGER DEFAULT 1)");

            // Mevcut veritabanı varsa ve auto_sell sütunu yoksa otomatik ekle (Hata almamak için)
            try {
                s.execute("ALTER TABLE machines ADD COLUMN auto_sell INTEGER DEFAULT 1");
            } catch (SQLException ignored) {
                // Sütun zaten varsa hata verir, görmezden geliyoruz.
            }

            s.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Connection getConnection() { return connection; }

    // saveMachine metoduna boolean autoSell parametresi eklendi
    public void saveMachine(String id, String owner, Location loc, int lvl, int fuel, String friends, String storage, boolean autoSell) {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO machines (id, owner, world, x, y, z, level, fuel, friends, storage, auto_sell) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id); ps.setString(2, owner); ps.setString(3, loc.getWorld().getName());
            ps.setInt(4, loc.getBlockX()); ps.setInt(5, loc.getBlockY()); ps.setInt(6, loc.getBlockZ());
            ps.setInt(7, lvl); ps.setInt(8, fuel); ps.setString(9, friends); ps.setString(10, storage);
            ps.setInt(11, autoSell ? 1 : 0); // SQLite boolean yerine 1/0 kullanır
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void removeMachine(String id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM machines WHERE id=?")) {
            ps.setString(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException e) {}
    }
}