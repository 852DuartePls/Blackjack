package me.duart.blackjackLite;

import me.duart.blackjackLite.command.RegisterCommands;
import me.duart.blackjackLite.gui.BlackjackMenus;
import me.duart.blackjackLite.util.BlackjackJoinExitListeners;
import me.duart.blackjackLite.util.BlackjackInventoryListener;
import me.duart.blackjackLite.db.PlayerStatsDB;
import me.duart.blackjackLite.db.SessionDB;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

@SuppressWarnings("unused")
public final class BlackjackLite extends JavaPlugin {

    public static BlackjackLite instance;
    private static Economy economy;
    private final ComponentLogger logger = getComponentLogger();
    private final RegisterCommands registerCommands = new RegisterCommands();
    private PlayerStatsDB playerStatsDB;
    private SessionDB sessionDB;
    private BlackjackInventoryListener blackjackInventoryListener;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            playerStatsDB = new PlayerStatsDB(this);
        } catch (SQLException e) {
            getLogger().severe("Could not connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            sessionDB = new SessionDB(this);
        } catch (SQLException e) {
            getLogger().severe("Could not connect to database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            sessionDB.deleteExpiredSessions();
        } catch (SQLException e) {
            getLogger().warning("No se pudieron limpiar sesiones expiradas: " + e.getMessage());
        }

        BlackjackMenus menus = new BlackjackMenus(this);
        this.blackjackInventoryListener = new BlackjackInventoryListener(this, menus, sessionDB);
        BlackjackJoinExitListeners joinExitListeners = new BlackjackJoinExitListeners(this, sessionDB, blackjackInventoryListener.getActiveGames());
        getServer().getPluginManager().registerEvents(blackjackInventoryListener, this);
        getServer().getPluginManager().registerEvents(joinExitListeners, this);
        registerCommands.registerCommands(this);

        this.logger.info(Component.text("=========================", NamedTextColor.GREEN));
        this.logger.info(Component.text(">> Enabled!", NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        if (playerStatsDB != null) {
            try {
                playerStatsDB.close();
            } catch (SQLException e) {
                logger.error(Component.text("Failed to close database: " + e.getMessage()));
            }
        }

        if (sessionDB != null) {
            try {
                sessionDB.close();
            } catch (SQLException e) {
                logger.error(Component.text("Failed to close database: " + e.getMessage()));
            }
        }

        this.logger.info(Component.text(">> Disabled!", NamedTextColor.RED));
        this.logger.info(Component.text("=========================", NamedTextColor.RED));
    }

    // << -- Service Accessors -- >> //

    public static BlackjackLite instance() {
        return instance;
    }

    public PlayerStatsDB getPlayerStatsDB() {
        return playerStatsDB;
    }

    public static ComponentLogger logger() {
        return instance.logger;
    }

    public BlackjackInventoryListener getBlackjackListener() {
        return blackjackInventoryListener;
    }

    public SessionDB getSessionDB() {
        return sessionDB;
    }

    public MiniMessage mini() {
        return miniMessage;
    }

    // << -- Vault API -- >> //

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            logger.error("Vault is present, but no economy plugin was found. Please install one (EssentialsX-Eco, CMI, LiteEco, etc.).");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    public static void withdraw(Player player, double amount) {
        economy.withdrawPlayer(player, amount);
    }

    public static void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }
}
