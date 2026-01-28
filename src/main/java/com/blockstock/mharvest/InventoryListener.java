package com.blockstock.mharvest;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InventoryListener implements Listener {
    private final MHarvestPlugin plugin;
    public InventoryListener(MHarvestPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals("§8mHarvest Market") && !title.equals("§8Makine Yönetimi") && !title.startsWith("§8Depo:")) return;

        if (title.startsWith("§8Depo:")) return;

        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();

        if (title.equals("§8mHarvest Market")) {
            if (e.getRawSlot() == 11) {
                double cost = plugin.getConfig().getDouble("settings.machine-cost");
                if (plugin.getEconomy().getBalance(p) >= cost) {
                    plugin.getEconomy().withdrawPlayer(p, cost);
                    p.getInventory().addItem(plugin.getMachineManager().getMachineItem());
                    p.sendMessage(plugin.getConfigManager().getMsg("bought"));
                }
            }
            return;
        }

        Block b = p.getTargetBlockExact(5);
        if (b == null) return;
        MachineManager.MachineData data = plugin.getMachineManager().getMachine(b.getLocation());
        if (data == null) return;

        switch (e.getRawSlot()) {
            case 10: plugin.getMenuManager().openStorage(p, data); break;
            case 12:
                int totalFuelToAdd = 0;
                for (ItemStack i : p.getInventory().getContents()) {
                    if (i == null || i.getType() == Material.AIR) continue;

                    Material type = i.getType();
                    int amount = i.getAmount();

                    if (type == Material.COAL) {
                        totalFuelToAdd += amount * 10;
                        i.setAmount(0);
                    } else if (type == Material.CHARCOAL) {
                        totalFuelToAdd += amount * 8;
                        i.setAmount(0);
                    } else if (type == Material.BLAZE_ROD) {
                        totalFuelToAdd += amount * 50;
                        i.setAmount(0);
                    } else if (type == Material.LAVA_BUCKET) {
                        totalFuelToAdd += amount * 100;
                        i.setType(Material.BUCKET);
                    }
                }

                if (totalFuelToAdd > 0) {
                    data.fuel += totalFuelToAdd;
                    p.sendMessage(plugin.getConfigManager().getMsg("fuel-added").replace("%fuel%", String.valueOf(data.fuel)));
                    plugin.getMenuManager().openDashboard(p, data);
                    plugin.getMachineManager().updateHologram(data);
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
                }
                break;
            case 16:
                int nextLvl = data.level + 1;
                if (plugin.getConfig().contains("levels." + nextLvl)) {
                    double cost = plugin.getConfig().getDouble("levels." + nextLvl + ".cost");
                    if (plugin.getEconomy().getBalance(p) >= cost) {
                        plugin.getEconomy().withdrawPlayer(p, cost);
                        data.level = nextLvl;
                        p.sendMessage(plugin.getConfigManager().getMsg("upgrade-success").replace("%level%", String.valueOf(nextLvl)));
                        plugin.getMachineManager().updateHologram(data);
                        plugin.getMenuManager().openDashboard(p, data);
                    }
                }
                break;
            case 22:
                data.autoSell = !data.autoSell;
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openDashboard(p, data);
                break;
            // --- YENİ: MAKİNEYİ KALDIR (BOZMADAN EKLENDİ) ---
            case 26:
                if (!data.owner.equals(p.getUniqueId()) && !p.hasPermission("mharvest.admin")) {
                    p.sendMessage(plugin.getConfigManager().getMsg("not-owner"));
                    return;
                }
                p.closeInventory();
                // Bloğu dünyadan silecek ve içindekileri yere atacaktır (MachineManager'daki removeMachine sayesinde)
                plugin.getMachineManager().removeMachine(b);
                // Makineyi eşya olarak oyuncuya geri verir
                p.getInventory().addItem(plugin.getMachineManager().getMachineItem());
                p.sendMessage("§a[✓] Makine söküldü ve envanterine verildi.");
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                break;
        }
    }

    @EventHandler
    public void onStorageClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().startsWith("§8Depo: ")) {
            String id = e.getView().getTitle().replace("§8Depo: ", "");
            Player p = (Player) e.getPlayer();
            Block b = p.getTargetBlockExact(6);
            if (b != null) {
                MachineManager.MachineData data = plugin.getMachineManager().getMachine(b.getLocation());
                if (data != null && data.id.equals(id)) {
                    data.storage = Arrays.stream(e.getInventory().getContents()).filter(i -> i != null && i.getType() != Material.AIR).collect(Collectors.toList());
                    plugin.getMachineManager().saveAll();
                }
            }
        }
    }
}