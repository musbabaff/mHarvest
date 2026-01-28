package com.blockstock.mharvest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManager {
    private final MHarvestPlugin plugin;
    public MenuManager(MHarvestPlugin plugin) { this.plugin = plugin; }

    public void openMarket(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8mHarvest Market");
        fillGui(inv);

        inv.setItem(11, createItem(Material.BARREL, "§a§lMakine Satın Al", "§7Fiyat: §e" + plugin.getConfig().getInt("settings.machine-cost") + " TL", "", "§eTıkla ve Satın Al!"));

        String infoName = plugin.getConfig().getString("market-info.name", "&bBilgi").replace("&", "§");
        List<String> infoLoreRaw = plugin.getConfig().getStringList("market-info.lore");
        List<String> infoLoreColored = new ArrayList<>();
        for(String line : infoLoreRaw) infoLoreColored.add(line.replace("&", "§"));

        inv.setItem(15, createItem(Material.BOOK, infoName, infoLoreColored.toArray(new String[0])));
        p.openInventory(inv);
    }

    public void openDashboard(Player p, MachineManager.MachineData data) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Makine Yönetimi");
        fillGui(inv);

        inv.setItem(10, createItem(Material.CHEST, "§6§lDepo", "§7Ürünleri almak için tıkla."));

        // --- GÜNCELLENDİ: Yakıt Ekle Butonu ---
        inv.setItem(12, createItem(Material.CHARCOAL, "§c§lYakıt Ekle",
                "§7Mevcut Yakıt: §e" + data.fuel,
                "§7Kabul Edilenler: §fKömür, Lav, Blaze Çubuğu",
                "", "§eTıkla ve envanterdekileri yükle!"));

        inv.setItem(14, createItem(Material.NETHER_STAR, "§b§lMakine Durumu", "§7Seviye: §f" + data.level, "§7Sahibi: §f" + Bukkit.getOfflinePlayer(data.owner).getName()));

        // --- OTO SATIŞ BUTONU ---
        Material sellMat = data.autoSell ? Material.GOLD_INGOT : Material.IRON_INGOT;
        String sellStatus = data.autoSell ? "§a§lAÇIK" : "§c§lKAPALI";
        inv.setItem(22, createItem(sellMat, "§e§lOtomatik Satış", "§7Durum: " + sellStatus, "", "§eTıklayarak değiştirebilirsin."));

        int nextLvl = data.level + 1;
        if (plugin.getConfig().contains("levels." + nextLvl)) {
            double cost = plugin.getConfig().getDouble("levels." + nextLvl + ".cost");
            inv.setItem(16, createItem(Material.EMERALD, "§a§lSeviye Yükselt", "§7Maliyet: §e" + cost + " TL", "", "§eTıkla ve Geliştir!"));
        } else {
            inv.setItem(16, createItem(Material.BARRIER, "§c§lMAKSİMUM SEVİYE", "§7Makine en yüksek seviyede."));
        }

        // --- YENİ: MAKİNEYİ KALDIR BUTONU (Slot 26) ---
        // Sadece bu kısım eklendi, diğerleri korundu.
        inv.setItem(26, createItem(Material.BARRIER, "§c§lMakineyi Kaldır",
                "§7Makineyi söker ve sana geri verir.",
                "§7İçindeki ürünler yere dökülür!",
                "", "§eSökmek için tıkla!"));

        p.openInventory(inv);
    }

    public void openStorage(Player p, MachineManager.MachineData data) {
        int size = plugin.getConfig().getInt("levels." + data.level + ".storage", 27);
        Inventory inv = Bukkit.createInventory(null, size, "§8Depo: " + data.id);
        for(ItemStack i : data.storage) if(i != null) inv.addItem(i);
        p.openInventory(inv);
    }

    private void fillGui(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private ItemStack createItem(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            i.setItemMeta(meta);
        }
        return i;
    }
}