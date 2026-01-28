package com.blockstock.mharvest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final MHarvestPlugin plugin;

    public CommandHandler(MHarvestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyuncular içindir.");
            return true;
        }

        Player p = (Player) sender;

        // Argüman girilmemişse veya help istenmişse
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(p);
            return true;
        }

        // Market Komutu
        if (args[0].equalsIgnoreCase("market")) {
            plugin.getMenuManager().openMarket(p);
            return true;
        }

        // Reload Komutu
        if (args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("mharvest.admin")) {
                p.sendMessage("§cBu komut için yetkiniz yok!");
                return true;
            }
            plugin.reloadPlugin();
            p.sendMessage("§6§lM-Harvest §8» §aEklenti dosyaları ve ayarlar başarıyla yenilendi.");
            return true;
        }

        // Bilinmeyen bir alt komut girilirse help mesajını göster
        sendHelpMessage(p);
        return true;
    }

    private void sendHelpMessage(Player p) {
        p.sendMessage(" ");
        p.sendMessage("§6§lM-HARVEST §8- §fYardım Menüsü");
        p.sendMessage("§e/mh market §8» §7Makine satın alma menüsünü açar.");
        p.sendMessage("§e/mh help §8» §7Bu yardım mesajını gösterir.");

        if (p.hasPermission("mharvest.admin")) {
            p.sendMessage("§c/mh reload §8» §fAyarları ve dosyaları yeniler.");
        }
        p.sendMessage(" ");
    }
}