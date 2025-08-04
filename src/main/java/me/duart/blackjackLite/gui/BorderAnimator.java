package me.duart.blackjackLite.gui;

import me.duart.blackjackLite.BlackjackLite;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BorderAnimator {

    private BorderAnimator() {}

    public static void animateBlackjack(BlackjackLite plugin,@NotNull Inventory inv,long durationTicks) {

        List<Integer> border = new ArrayList<>();
        for (int i = 0; i < 9; i++) border.add(i);
        for (int i = 17; i < 45; i += 9) border.add(i);
        for (int i = 44; i >= 36; i--) border.add(i);
        for (int i = 27; i > 0; i -= 9) border.add(i);

        final int length = border.size();
        final int period = 3;
        final int totalFrames = (int) (durationTicks / period);

        for (int frame = 0; frame < totalFrames; frame++) {
            final int offset = frame % length;
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        for (int idx = 0; idx < length; idx++) {
                            int slot = border.get((idx + offset) % length);
                            Material mat = idx % 2 == 0
                                    ? Material.RED_STAINED_GLASS_PANE
                                    : Material.BLACK_STAINED_GLASS_PANE;

                            // Only overwrite glass panes (never cards or buttons)
                            ItemStack current = inv.getItem(slot);
                            if (current == null || isGlassPane(current.getType())) {
                                inv.setItem(slot, createItem(mat));
                            }
                        }
                        inv.getViewers().forEach(viewer -> {
                            if (viewer instanceof Player player) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2f);
                            }
                        });
                    },
                    (long) frame * period
            );;
        }

        // Final static lime border once animation finishes
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> fillBorder(inv),
                durationTicks
        );
    }

    /* ----------------------------------------------------------- */

    private static boolean isGlassPane(Material material) {
        return material == Material.RED_STAINED_GLASS_PANE
                || material == Material.BLACK_STAINED_GLASS_PANE
                || material == Material.LIME_STAINED_GLASS_PANE
                || material == Material.ORANGE_STAINED_GLASS_PANE;
    }

    private static void fillBorder(@NotNull Inventory inv) {
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                ItemStack cur = inv.getItem(i);
                if (cur == null || isGlassPane(cur.getType())) {
                    inv.setItem(i, createItem(Material.LIME_STAINED_GLASS_PANE));
                }
            }
        }
    }

    private static @NotNull ItemStack createItem(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.empty()));
        return item;
    }
}
