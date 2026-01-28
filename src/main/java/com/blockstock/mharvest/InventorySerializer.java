package com.blockstock.mharvest;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

public class InventorySerializer {
    public static String serialize(List<ItemStack> items) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BukkitObjectOutputStream dos = new BukkitObjectOutputStream(os);
            dos.writeInt(items.size());
            for (ItemStack i : items) dos.writeObject(i);
            dos.close();
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) { return ""; }
    }

    public static ItemStack[] deserialize(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dis = new BukkitObjectInputStream(is);
            ItemStack[] items = new ItemStack[dis.readInt()];
            for (int i = 0; i < items.length; i++) items[i] = (ItemStack) dis.readObject();
            dis.close();
            return items;
        } catch (Exception e) { return new ItemStack[0]; }
    }
}