package me.duart.blackjackLite.util;

import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.db.SessionDB;
import me.duart.blackjackLite.gui.BlackjackInventoryHolder;
import me.duart.blackjackLite.gui.BlackjackMenus;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.duart.blackjackLite.gui.BlackjackMenus.getItemID;
import static me.duart.blackjackLite.gui.BlackjackMenus.playSound;

public class BlackjackInventoryListener implements Listener {
    private final BlackjackLite plugin;
    private final BlackjackMenus menus;
    private final SessionDB sessionDB;

    public final Map<UUID, BlackjackGame> activeGames = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalEarnings = new HashMap<>();

    public BlackjackInventoryListener(BlackjackLite plugin, BlackjackMenus menus, SessionDB sessionDB) {
        this.plugin = plugin;
        this.menus = menus;
        this.sessionDB = sessionDB;
    }

    // << -- Event Handlers -- >> //

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlackjackInventoryHolder holder)) return;
        if (!holder.getMenuTypeid().equals("game_menu")) return;

        UUID uuid = event.getPlayer().getUniqueId();
        BlackjackGame game = activeGames.remove(uuid);
        if (game == null) return;

        if (game.getPlayerHand() == null || game.getDealerHand() == null || game.isBust(game.getPlayerHand())) {
            return;
        }

        try {
            sessionDB.save(uuid, game);
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not save session on close for " + uuid + ": " + ex.getMessage());
        }
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof BlackjackInventoryHolder holder)) return;

        UUID uuid = event.getWhoClicked().getUniqueId();

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
            totalEarnings.put(uuid, 0);
        }

        event.setCancelled(true);

        switch (holder.getMenuTypeid()) {
            case "initial_menu" -> handleBettingActions(event, holder, uuid);
            case "game_menu" -> handleGameActions(event, holder, uuid);
            case "result_menu" -> handleResultActions(event, uuid);
        }
    }

    // << -- Actual Logic -- >> //

    private void handleBettingActions(@NotNull InventoryClickEvent event, BlackjackInventoryHolder holder, UUID playerUUID) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String id = getItemID(clickedItem);
        if (id == null) return;

        int currentBet = holder.getBet();

        switch (id) {
            case "plus_10" -> adjustBet(event, holder, 10);
            case "plus_100" -> adjustBet(event, holder, 100);
            case "plus_1000" -> adjustBet(event, holder, 1000);
            case "minus_10" -> adjustBet(event, holder, -10);
            case "minus_100" -> adjustBet(event, holder, -100);
            case "minus_1000" -> adjustBet(event, holder, -1000);
            case "reset_bet" -> {
                holder.setBet(100);
                playSound((Player) event.getWhoClicked(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.6f, 1.2f);
            }

            case "start_game" -> {
                if (!BlackjackLite.instance.hasEnough(player, currentBet)) {
                    player.sendMessage(BlackjackLite.instance.mini().deserialize("<red>You don't have enough money to bet this amount."));
                    return;
                }
                BlackjackLite.withdraw(player, currentBet);
                BlackjackGame game = new BlackjackGame();
                game.startGame();
                playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.3f, 1.0f);
                activeGames.put(playerUUID, game);
                player.openInventory(menus.openGameMenu(currentBet, game));
                return;
            }
            case "close" -> {
                player.closeInventory();
                playSound(player, Sound.ENTITY_VILLAGER_WORK_MASON, 0.4f, 1.5f);
                return;
            }
            default -> {
                return;
            }
        }

        updateBetUI(event.getInventory(), holder.getBet());
    }

    private void handleGameActions(@NotNull InventoryClickEvent event, BlackjackInventoryHolder holder, UUID playerUUID) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String id = getItemID(clickedItem);
        if (id == null) return;

        BlackjackGame game = activeGames.get(playerUUID);
        if (game == null) return;

        int bet = holder.getBet();

        switch (id) {
            case "hit" -> {
                game.hitPlayer();
                if (game.isBust(game.getPlayerHand())) {
                    finishRound(playerUUID, game, BlackjackGame.Result.LOSE, holder);
                    return;
                }
                playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.25f, 1.0f);
                refreshGameMenu(event, playerUUID);
            }

            case "stand" -> {
                game.dealerPlay();
                finishRound(playerUUID, game, game.determineResult(), holder);
                playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.25f, 1.0f);
                return;
            }

            case "double_down" -> {
                if (!game.canDoubleDown()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1F, 1F);
                    return;
                }
                if (bet * 2 > getMaxBet()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1F, 1F);
                    player.sendMessage(BlackjackLite.instance.mini().deserialize("<red>Apuesta máxima excedida."));
                    return;
                }
                holder.setBet(bet * 2);
                game.hitPlayer();
                game.dealerPlay();
                playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.25f, 1.0f);
                finishRound(playerUUID, game, game.determineResult(), holder);
                return;
            }

            case "close" -> {
                event.getWhoClicked().closeInventory();
                playSound(player, Sound.ENTITY_VILLAGER_WORK_MASON, 0.4f, 1.5f);
                return;
            }
            default -> {
                return;
            }
        }

        refreshGameMenu(event, playerUUID);
    }

    private void handleResultActions(@NotNull InventoryClickEvent event, UUID playerUUID) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String id = BlackjackMenus.getItemID(item);
        if (id == null) return;

        switch (id) {
            case "play_again" -> {
                Inventory inv = event.getInventory();
                if (!(inv.getHolder() instanceof BlackjackInventoryHolder holder)) return;

                int newBet = holder.getBet();

                if (BlackjackLite.instance.hasEnough(player, newBet)) {
                    BlackjackLite.withdraw(player, newBet);

                    BlackjackGame newGame = new BlackjackGame();
                    newGame.startGame();
                    activeGames.put(playerUUID, newGame);

                    playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.3f, 1.0f);
                    event.getWhoClicked().openInventory(menus.openGameMenu(newBet, newGame));
                } else {
                    event.getWhoClicked().sendMessage(BlackjackLite.instance.mini().deserialize("<red>You don't have enough money to bet this amount."));
                    playSound(player, Sound.ENTITY_VILLAGER_NO, 0.4f, 1.5f);
                }
            }

            case "close" -> {
                event.getWhoClicked().closeInventory();
                playSound(player, Sound.ENTITY_VILLAGER_WORK_MASON, 0.4f, 1.5f);
            }

            case "plus_10", "plus_100", "plus_1000",
                 "minus_10", "minus_100", "minus_1000" -> {
                Inventory inv = event.getInventory();
                if (!(inv.getHolder() instanceof BlackjackInventoryHolder holder)) return;
                int delta = switch (id) {
                    case "plus_10" -> 10;
                    case "plus_100" -> 100;
                    case "plus_1000" -> 1000;
                    case "minus_10" -> -10;
                    case "minus_100" -> -100;
                    case "minus_1000" -> -1000;
                    default -> 0;
                };
                adjustBet(event, holder, delta);
                inv.setItem(22, menus.createItem(Material.PAPER, "<yellow>Apuesta actual: <green>" + holder.getBet(), "final_bet"));
            }
        }
    }

    // ---- Helpers ---- //

    private void finishRound(UUID uuid, @NotNull BlackjackGame game, @NotNull BlackjackGame.Result result, @NotNull BlackjackInventoryHolder holder) {
        int bet = holder.getBet();
        int payout = switch (result) {
            case WIN -> bet * 2;
            case DRAW -> bet;
            case LOSE -> 0;
        };

        int winnings = payout - bet;
        totalEarnings.put(uuid, totalEarnings.getOrDefault(uuid, 0) + winnings);

        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;

        if (payout > 0) {
            BlackjackLite.deposit(player, payout);
        }

        try {
            plugin.getPlayerStatsDB().incrementGamesPlayed(uuid);
            switch (result) {
                case WIN -> plugin.getPlayerStatsDB().incrementWins(uuid, bet);
                case LOSE -> plugin.getPlayerStatsDB().incrementLosses(uuid, bet);
                case DRAW -> { /* nothing else to count */ }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update stats for " + uuid + ": " + e.getMessage());
        }

        boolean playerHasBlackjack = game.isBlackjack(game.getPlayerHand());
        Sound endSound;
        if (playerHasBlackjack) {
            BlackjackMenus.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);
        } else {
            endSound = switch (result) {
                case WIN -> Sound.ENTITY_PLAYER_LEVELUP;
                case LOSE -> Sound.ENTITY_WITHER_SPAWN;
                case DRAW -> Sound.BLOCK_ANVIL_LAND;
            };
            BlackjackMenus.playSound(player, endSound, 0.5f, 1.2f);
        }

        player.openInventory(menus.createResultMenu(result, playerHasBlackjack, game, bet, winnings,
                totalEarnings.getOrDefault(uuid, 0)));

        try {
            sessionDB.delete(uuid);
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not delete session for " + uuid + ": " + e.getMessage());
        }
    }

    private void refreshGameMenu(@NotNull InventoryClickEvent event, UUID uuid) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof BlackjackInventoryHolder holder)) return;

        int currentBet = holder.getBet();
        BlackjackGame game = activeGames.get(uuid);

        if (game == null) {
            try {
                Optional<BlackjackGame> optionalGame = sessionDB.load(uuid);
                if (optionalGame.isPresent()) {
                    game = optionalGame.get();
                    activeGames.put(uuid, game);
                    plugin.getLogger().info("Restored Blackjack session for " + uuid);
                } else {
                    event.getWhoClicked().sendMessage(BlackjackLite.instance.mini().deserialize("<red>No se encontró una sesión activa de blackjack."));
                    event.getWhoClicked().closeInventory();
                    return;
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error loading blackjack session for " + uuid + ": " + e.getMessage());
                event.getWhoClicked().sendMessage(BlackjackLite.instance.mini().deserialize("<red>Hubo un error al intentar recuperar tu sesión."));
                event.getWhoClicked().closeInventory();
                return;
            }
        }

        Inventory newMenu = menus.openGameMenu(currentBet, game);
        InventoryView view = event.getWhoClicked().getOpenInventory();
        view.getTopInventory().setContents(newMenu.getContents());
        ((Player) event.getWhoClicked()).updateInventory();

    }

    private void updateBetUI(@NotNull Inventory inv, int bet) {
        inv.setItem(22, menus.createItem(Material.PAPER, "<yellow>Apuesta actual: <green>" + bet, "current_bet"));
    }

    private int getMaxBet() {
        return 100_000;
    }

    private void adjustBet(@NotNull InventoryClickEvent event, @NotNull BlackjackInventoryHolder holder, int delta) {
        int oldBet = holder.getBet();
        int nextBet = oldBet + delta;
        holder.setBet(Math.max(100, Math.min(getMaxBet(), nextBet)));
        updateBetUI(event.getInventory(), holder.getBet());
        playSound((Player) event.getWhoClicked(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 1.5f);
    }

    public Map<UUID, BlackjackGame> getActiveGames() {
        return activeGames;
    }
}
