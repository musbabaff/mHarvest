package com.blockstock.mharvest;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MachineListener implements Listener {
    private final MHarvestPlugin plugin;
    public MachineListener(MHarvestPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.getMachineManager().machineKey, PersistentDataType.STRING)) {
            plugin.getMachineManager().createMachine(e.getBlock(), e.getPlayer());
            e.getPlayer().sendMessage(plugin.getConfigManager().getMsg("machine-place").replace("%player%", e.getPlayer().getName()));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        MachineManager.MachineData data = plugin.getMachineManager().getMachine(e.getBlock().getLocation());
        if (data != null) {
            // --- GÜNCELLENDİ: Fiziksel Kırmayı Engelleme ---
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c[!] Makineyi normal yolla kıramazsın. Lütfen menüdeki 'Makineyi Kaldır' butonunu kullan.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        MachineManager.MachineData data = plugin.getMachineManager().getMachine(b.getLocation());
        if (data != null) {
            e.setCancelled(true);
            Player p = e.getPlayer();

            // Arkadaş listesi veya admin kontrolü (Sistem bozulmadı)
            if (!data.owner.equals(p.getUniqueId()) && !data.friends.contains(p.getName()) && !p.hasPermission("mharvest.admin")) {
                p.sendMessage(plugin.getConfigManager().getMsg("not-owner"));
                return;
            }
            plugin.getMenuManager().openDashboard(p, data);
        }
    }
}