package me.duart.blackjackLite.gui;

import me.duart.blackjackLite.BlackjackLite;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class BlackjackInventoryHolder implements InventoryHolder {

    private final Inventory inventory;
    private final String menuTypeid;
    private int bet;

    public BlackjackInventoryHolder(@NotNull BlackjackLite plugin, Component title, int size, String menuTypeid, int bet) {
        this.inventory = plugin.getServer().createInventory(this, size, title);
        this.menuTypeid = menuTypeid;
        this.bet = Math.max(bet, 100);
    }

    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
    
    public String getMenuTypeid() {
        return menuTypeid;
    }

    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = Math.max(bet, 100);
    }

    public void increaseBet(int amount, int maxBet) {
        setBet(Math.min(bet + amount, maxBet));
    }

    public void decreaseBet(int amount) {
        setBet(Math.max(bet - amount, 100));
    }


}
