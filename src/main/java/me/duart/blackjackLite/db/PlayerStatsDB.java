package me.duart.blackjackLite.db;

import me.duart.blackjackLite.BlackjackLite;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerStatsDB {

    private final Connection connection;

    public PlayerStatsDB(@NotNull BlackjackLite plugin) throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "blackjack_stats.db");
        File parentDir = dbFile.getParentFile();

        if (!parentDir.exists() && parentDir.mkdirs()) {
            plugin.getLogger().info("Created directories for database.");
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        init();
    }

    private void init() throws SQLException {
        String create = """
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid TEXT PRIMARY KEY,
                        player_name TEXT NOT NULL,
                        games_played INTEGER DEFAULT 0,
                        wins INTEGER DEFAULT 0,
                        losses INTEGER DEFAULT 0,
                        total_winnings INTEGER DEFAULT 0,
                        total_losses INTEGER DEFAULT 0
                    );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(create);
        }
    }

    public void incrementGamesPlayed(UUID uuid) throws SQLException {
        ensureExists(uuid);
        String update = "UPDATE player_stats SET games_played = games_played + 1 WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    public void incrementWins(UUID uuid, int bet) throws SQLException {
        ensureExists(uuid);
        String update = "UPDATE player_stats SET wins = wins + 1, total_winnings = total_winnings + ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setInt(1, bet);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    public void incrementLosses(UUID uuid, int bet) throws SQLException {
        ensureExists(uuid);
        String update = "UPDATE player_stats SET losses = losses + 1, total_losses = total_losses + ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setInt(1, bet);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    public void ensureExists(@NotNull UUID uuid) throws SQLException {
        String insert = """
                    INSERT OR IGNORE INTO player_stats (uuid, player_name)
                    VALUES (?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            statement.executeUpdate();
        }
    }

    @Nullable
    public PlayerStats getStats(@NotNull UUID uuid) throws SQLException {
        String query = "SELECT * FROM player_stats WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new PlayerStats(
                        uuid,
                        resultSet.getString("player_name"),
                        resultSet.getInt("games_played"),
                        resultSet.getInt("wins"),
                        resultSet.getInt("losses"),
                        resultSet.getInt("total_winnings"),
                        resultSet.getInt("total_losses")
                );
            }
        }
        return null;
    }

    public List<String> getAllNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String query = "SELECT DISTINCT player_name FROM player_stats";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                names.add(resultSet.getString("player_name"));
            }
        }
        return names;
    }

    public void close() throws SQLException {
        connection.close();
    }

    public record PlayerStats(UUID uuid, String playerName, int gamesPlayed, int wins, int losses, int totalWinnings, int totalLosses) {
    }

}