package me.duart.blackjackLite.util;

import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.db.SessionDB;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public final class BlackjackJoinExitListeners implements Listener {

    private final BlackjackLite plugin;
    private final SessionDB sessionDB;
    private final Map<UUID, BlackjackGame> activeGames;

    public BlackjackJoinExitListeners(@NotNull BlackjackLite plugin, @NotNull SessionDB sessionDB, @NotNull Map<UUID, BlackjackGame> activeGames) {
        this.plugin = plugin;
        this.sessionDB = sessionDB;
        this.activeGames = activeGames;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        BlackjackGame game = activeGames.remove(uuid);
        if (game != null) {
            try {
                sessionDB.save(uuid, game);
            } catch (SQLException ex) {
                plugin.getLogger().warning("Could not save session for " + uuid + ": " + ex.getMessage());
            }
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        try {
            if (sessionDB.hasSession(uuid)) {
                event.getPlayer().sendMessage(BlackjackLite.instance().mini()
                        .deserialize("<red><b>(!)</b></red> <green>Tu anterior partida ha terminado bruscamente<newline>" +
                                "<green>Escribe el comando <gold>/blackjack (/bj)</gold> para restaurarlo.")
                );
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not check session for " + uuid + ": " + ex.getMessage());
        }
    }
}