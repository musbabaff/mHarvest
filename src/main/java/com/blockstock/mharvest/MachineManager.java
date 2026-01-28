package com.blockstock.mharvest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import java.sql.ResultSet;
import java.util.*;

public class MachineManager {
    private final MHarvestPlugin plugin;
    public final Map<Location, MachineData> cache = new HashMap<>();
    public final NamespacedKey machineKey;

    public MachineManager(MHarvestPlugin plugin) {
        this.plugin = plugin;
        this.machineKey = new NamespacedKey(plugin, "m_machine");
        loadAllMachines();
    }

    private void loadAllMachines() {
        try (java.sql.Statement s = plugin.getDatabaseManager().getConnection().createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM machines");
            while (rs.next()) {
                Location loc = new Location(Bukkit.getWorld(rs.getString("world")), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                boolean autoSell = rs.getBoolean("auto_sell");
                MachineData data = new MachineData(rs.getString("id"), UUID.fromString(rs.getString("owner")), loc, rs.getInt("level"), rs.getInt("fuel"), rs.getString("friends"), rs.getString("storage"), autoSell);
                cache.put(loc, data);
                updateHologram(data);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void startTasks() {
        new BukkitRunnable() {
            public void run() {
                for (MachineData data : cache.values()) {
                    if (data.fuel > 0) harvest(data);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);

        new BukkitRunnable() {
            public void run() {
                for (MachineData data : cache.values()) {
                    if (!data.storage.isEmpty()) sell(data);
                }
            }
        }.runTaskTimer(plugin, 120L, 120L);
    }

    private void harvest(MachineData data) {
        int range = plugin.getConfig().getInt("levels." + data.level + ".range", 2);
        Block center = data.loc.getBlock();
        boolean harvestedAny = false;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -2; y <= 1; y++) {
                    Block target = center.getRelative(x, y, z);
                    if (isRipe(target)) {
                        Material cropType = target.getType();
                        Collection<ItemStack> drops = target.getDrops();
                        for (ItemStack drop : drops) data.addItem(drop);
                        replant(target, cropType, data);
                        harvestedAny = true;
                        data.fuel--;
                        if (data.fuel <= 0) {
                            updateHologram(data);
                            return;
                        }
                    }
                }
            }
        }
        if (harvestedAny) {
            data.loc.getWorld().playSound(data.loc, Sound.BLOCK_CROP_BREAK, 0.5f, 1f);
            updateHologram(data);
        }
    }

    private void sell(MachineData data) {
        if (!data.autoSell) return;

        int speed = plugin.getConfig().getInt("levels." + data.level + ".speed", 5);
        double totalMoney = 0;
        int processedCount = 0;

        Iterator<ItemStack> it = data.storage.iterator();
        while (it.hasNext() && processedCount < speed) {
            ItemStack item = it.next();
            if (item == null || item.getType() == Material.AIR) continue;
            if (isSeed(item.getType())) {
                processedCount++;
                continue;
            }

            double unitPrice = plugin.getConfigManager().getPrice(item.getType());
            if (unitPrice > 0) {
                totalMoney += (unitPrice * item.getAmount());
                it.remove();
            }
            processedCount++;
        }

        if (totalMoney > 0) {
            double taxPercent = plugin.getConfig().getDouble("settings.tax-percent", 5.0);
            double finalMoney = totalMoney * ((100 - taxPercent) / 100);
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(data.owner), finalMoney);
        }
    }

    private boolean isSeed(Material m) {
        String n = m.name();
        return n.contains("SEEDS") || m == Material.CARROT || m == Material.POTATO ||
                m == Material.NETHER_WART || m == Material.BEETROOT_SEEDS;
    }

    private void replant(Block block, Material oldType, MachineData data) {
        Material seed = getSeed(oldType);
        block.setType(Material.AIR);
        if (seed != null && data.hasItem(seed)) {
            block.setType(getPlantBlock(seed));
            data.removeItem(seed, 1);
            BlockData bd = block.getBlockData();
            if (bd instanceof Ageable) {
                Ageable age = (Ageable) bd;
                age.setAge(0);
                block.setBlockData(age);
            }
        }
    }

    private Material getPlantBlock(Material seed) {
        switch (seed) {
            case WHEAT_SEEDS: return Material.WHEAT;
            case CARROT: return Material.CARROTS;
            case POTATO: return Material.POTATOES;
            case BEETROOT_SEEDS: return Material.BEETROOTS;
            case NETHER_WART: return Material.NETHER_WART;
            default: return seed;
        }
    }

    private Material getSeed(Material crop) {
        String name = crop.name();
        if (name.equals("WHEAT")) return Material.WHEAT_SEEDS;
        if (name.equals("CARROTS")) return Material.CARROT;
        if (name.equals("POTATOES")) return Material.POTATO;
        if (name.equals("BEETROOTS")) return Material.BEETROOT_SEEDS;
        if (name.equals("NETHER_WART")) return Material.NETHER_WART;
        return null;
    }

    private boolean isRipe(Block b) {
        BlockData bd = b.getBlockData();
        if (bd instanceof Ageable) {
            Ageable age = (Ageable) bd;
            return age.getAge() == age.getMaximumAge();
        }
        return b.getType() == Material.MELON || b.getType() == Material.PUMPKIN;
    }

    // --- GÜNCELLENDİ: Makine Kaldırma Mantığı ---
    public void removeMachine(Block b) {
        MachineData data = cache.remove(b.getLocation());
        if (data != null) {
            // Yakıtı kömür olarak yere at
            if (data.fuel > 0) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.COAL, Math.max(1, data.fuel / 10)));
            }
            // Depodaki eşyaları yere saç
            for (ItemStack item : data.storage) {
                if (item != null && item.getType() != Material.AIR) {
                    b.getWorld().dropItemNaturally(b.getLocation(), item);
                }
            }
            // Hologram ve DB silme
            plugin.getDatabaseManager().removeMachine(data.id);
            removeHologram(b.getLocation());

            // Makine bloğunu havaya çevir (Bloğu dünyadan siler)
            b.setType(Material.AIR);
        }
    }

    public void updateHologram(MachineData data) {
        removeHologram(data.loc);
        Location l = data.loc.clone().add(0.5, 1.8, 0.5);
        spawnArmorStand(l, "§6§lM-HARVEST");
        spawnArmorStand(l.subtract(0, 0.25, 0), "§7Seviye: §b" + data.level + " §8| §7Yakıt: §e" + data.fuel);
    }

    private void spawnArmorStand(Location l, String name) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
    }

    public void removeHologram(Location loc) {
        loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.2, 0.5), 0.8, 1.5, 0.8).forEach(e -> {
            if (e instanceof ArmorStand) e.remove();
        });
    }

    public void saveAll() {
        for (MachineData data : cache.values()) {
            plugin.getDatabaseManager().saveMachine(data.id, data.owner.toString(), data.loc, data.level, data.fuel, data.getFriendsStr(), InventorySerializer.serialize(data.storage), data.autoSell);
        }
    }

    public void createMachine(Block b, Player p) {
        String id = UUID.randomUUID().toString();
        MachineData data = new MachineData(id, p.getUniqueId(), b.getLocation(), 1, 0, "", "", true);
        cache.put(b.getLocation(), data);
        updateHologram(data);
        saveAll();
    }

    public MachineData getMachine(Location loc) { return cache.get(loc); }

    public ItemStack getMachineItem() {
        ItemStack i = new ItemStack(Material.BARREL);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName("§6§lM-Harvest Makinesi");
            m.setLore(Arrays.asList("§7Tarlaya koy ve otomatik kazanç sağla."));
            m.getPersistentDataContainer().set(machineKey, PersistentDataType.STRING, "valid");
            i.setItemMeta(m);
        }
        return i;
    }

    public static class MachineData {
        public String id; public UUID owner; public Location loc; public int level; public int fuel;
        public boolean autoSell;
        public List<String> friends = new ArrayList<>();
        public List<ItemStack> storage = new ArrayList<>();

        public MachineData(String id, UUID owner, Location loc, int lvl, int fuel, String friendStr, String storeStr, boolean autoSell) {
            this.id = id; this.owner = owner; this.loc = loc; this.level = lvl; this.fuel = fuel;
            this.autoSell = autoSell;
            if (friendStr != null && !friendStr.isEmpty()) Collections.addAll(friends, friendStr.split(","));
            if (storeStr != null && !storeStr.isEmpty()) {
                ItemStack[] items = InventorySerializer.deserialize(storeStr);
                if (items != null) storage.addAll(Arrays.asList(items));
            }
        }

        public void addItem(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) return;
            for(ItemStack i : storage) { if(i != null && i.isSimilar(item)) { i.setAmount(i.getAmount() + item.getAmount()); return; } }
            storage.add(item.clone());
        }

        public boolean hasItem(Material m) {
            for(ItemStack i : storage) { if(i != null && i.getType() == m && i.getAmount() > 0) return true; }
            return false;
        }

        public void removeItem(Material m, int amount) {
            Iterator<ItemStack> it = storage.iterator();
            while(it.hasNext()){
                ItemStack i = it.next();
                if(i != null && i.getType() == m){
                    if(i.getAmount() > amount) { i.setAmount(i.getAmount() - amount); return; }
                    else { it.remove(); return; }
                }
            }
        }
        public String getFriendsStr() { return String.join(",", friends); }
    }
}