package me.duart.blackjackLite.db;

import com.google.gson.Gson;
import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.util.BlackjackGame;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SessionDB implements AutoCloseable {

    private static final Gson GSON = new Gson();
    private final Connection connection;

    public SessionDB(@NotNull BlackjackLite plugin) throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "sessions.db");
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        connection.setAutoCommit(true);
        init();
    }

    private void init() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS blackjack_sessions (
                uuid TEXT PRIMARY KEY,
                session_data TEXT NOT NULL,
                updated_at INTEGER DEFAULT (strftime('%s','now'))
            );
        """;
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    public void save(@NotNull UUID uuid, @NotNull BlackjackGame game) throws SQLException {
        String sql = "INSERT OR REPLACE INTO blackjack_sessions(uuid, session_data) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, serialize(game));
            statement.executeUpdate();
        }
    }

    public Optional<BlackjackGame> load(@NotNull UUID uuid) throws SQLException {
        deleteExpiredSessions();
        String sql = "SELECT session_data FROM blackjack_sessions WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(deserialize(rs.getString("session_data")));
            }
        }
        return Optional.empty();
    }

    public void delete(@NotNull UUID uuid) throws SQLException {
        String sql = "DELETE FROM blackjack_sessions WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void deleteExpiredSessions() throws SQLException {
        String sql = "DELETE FROM blackjack_sessions WHERE updated_at < strftime('%s', 'now') - 600";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private @NotNull String serialize(@NotNull BlackjackGame game) {
        return game.serialize();
    }

    private @NotNull BlackjackGame deserialize(@NotNull String json) {
        Map<?,?> map = GSON.fromJson(json, Map.class);
        int bet = ((Number) map.get("bet")).intValue();
        return BlackjackGame.deserialize(json, bet);
    }

    public boolean hasSession(@NotNull UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM blackjack_sessions WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            return ps.executeQuery().next();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}