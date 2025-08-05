package me.duart.blackjackLite.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.gui.BlackjackInventoryHolder;
import me.duart.blackjackLite.gui.BlackjackMenus;
import me.duart.blackjackLite.util.BlackjackGame;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class BlackjackCommand implements BasicCommand {

    private final BlackjackMenus menus;
    private final MiniMessage mini = BlackjackLite.instance().mini();

    public BlackjackCommand(@NotNull BlackjackLite plugin) {
        this.menus = new BlackjackMenus(plugin);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {
        if (stack.getSender() instanceof Player player) {
            UUID uuid = player.getUniqueId();

            if (args.length == 0) {
                try {
                    Optional<BlackjackGame> session = BlackjackLite.instance().getSessionDB().load(uuid);
                    if (session.isPresent()) {
                        BlackjackGame game = session.get();
                        BlackjackLite.instance().getBlackjackListener().getActiveGames().put(uuid, game);
                        player.sendMessage(mini.deserialize("<green><b>»</b> Has retomado tu partida anterior.</green>"));
                        player.openInventory(menus.openGameMenu(getCurrentBet(player), game));
                    } else {
                        player.sendMessage(mini.deserialize("<green><b>»</b> Usa: <gold>/bj <cantidad></gold> para empezar a jugar o <gold>/bj <jugador></gold> para ver estadísticas de un jugador."));
                    }
                } catch (SQLException e) {
                    player.sendMessage(mini.deserialize("<red><b>»</b> Ocurrió un error al recuperar tu sesión."));
                    BlackjackLite.logger().error("Error loading session for {}: {}", player.getName(), e.getMessage());
                }
                return;
            }

            if (args.length == 1) {
                int bet = parseBetAmount(args[0]);

                if (bet >= 0) {
                    if (bet < 100) {
                        player.sendMessage(mini.deserialize("<yellow><b>»</b> La apuesta mínima es de 100$."));
                        return;
                    }
                    if (!BlackjackLite.instance().hasEnough(player, bet)) {
                        player.sendMessage(mini.deserialize("<red><b>»</b> No tienes suficiente dinero para hacer esta apuesta."));
                        return;
                    }

                    try {
                        Optional<BlackjackGame> session = BlackjackLite.instance().getSessionDB().load(uuid);
                        if (session.isPresent()) {
                            BlackjackGame game = session.get();
                            BlackjackLite.instance().getBlackjackListener().getActiveGames().put(uuid, game);
                            player.sendMessage(mini.deserialize("<yellow><b>»</b> Has retomado tu partida anterior."));
                            player.openInventory(menus.openGameMenu(getCurrentBet(player), game));
                        } else {
                            player.openInventory(menus.openInitialMenu(bet));
                        }
                    } catch (SQLException e) {
                        player.sendMessage(mini.deserialize("<red><b>»</b> Ocurrió un error al verificar tu sesión anterior."));
                        BlackjackLite.instance().getLogger().warning("Error checking session: " + e.getMessage());
                    }
                    return;
                }

                showPlayerStats(player, args[0]);
                return;
            }

            player.sendMessage(mini.deserialize("<green><b>»</b> Usa: <gold>/bj <cantidad></gold> para empezar a jugar o <gold>/bj <jugador></gold> para ver estadísticas de un jugador."));

        } else if (stack.getSender() instanceof ConsoleCommandSender consoleSender) {
            if (args.length == 1) {
                int bet = parseBetAmount(args[0]);
                if (bet >= 0) {
                    consoleSender.sendMessage(mini.deserialize("<yellow><b>»</b> Este comando solo puede ser usado por jugadores. Usa: <gold>/bj <jugador></gold> para ver estadísticas."));
                    return;
                }
                showPlayerStats(consoleSender, args[0]);
                return;
            }
            consoleSender.sendMessage(mini.deserialize("<yellow><b>»</b> Uso inválido. Solo se pueden consultar estadísticas cuando se usa la consola. Usa: <gold>/bj <jugador></gold>."));
        }
    }

    @Override
    public @Nullable String permission() {
        return null; // for now
        //return "blackjack.use";
    }

    @Override
    public boolean canUse(@NotNull org.bukkit.command.CommandSender sender) {
        return sender instanceof Player || sender instanceof ConsoleCommandSender;
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            var suggestions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            try {
                suggestions.addAll(BlackjackLite.instance().getPlayerStatsDB().getAllNames());
            } catch (SQLException ignored) {/* */}

            return suggestions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    private void showPlayerStats(CommandSender sender, String targetName) {
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();

        try {
            var stats = BlackjackLite.instance().getPlayerStatsDB().getStats(targetUUID);
            if (stats == null) {
                sender.sendMessage(mini.deserialize("<red><b>»</b> No se encontraron estadísticas para <yellow>" + targetName));
                return;
            }

            var mini = BlackjackLite.instance().mini();

            String message = """
                    
                    <white>  ─── ･ ｡ﾟ☆: *.☽ .* :☆ﾟ. ───</white>
                    <gold>Estadísticas de <white><player></white>:
                     <gray>- Partidas jugadas:</gray> <white><games></white>
                     <gray>- Victorias:</gray> <green><wins></green>
                     <gray>- Derrotas:</gray> <red><losses></red>
                     <gray>- Ganancias totales:</gray> <green><winnings>$</green>
                     <gray>- Pérdidas totales:</gray> <red><lossesAmount>$</red>
                    <white>  ─── ･ ｡ﾟ☆: *.☽ .* :☆ﾟ. ───</white>
                    """.replace("<player>", stats.playerName())
                    .replace("<games>", String.valueOf(stats.gamesPlayed()))
                    .replace("<wins>", String.valueOf(stats.wins()))
                    .replace("<losses>", String.valueOf(stats.losses()))
                    .replace("<winnings>", String.valueOf(stats.totalWinnings()))
                    .replace("<lossesAmount>", String.valueOf(stats.totalLosses()));

            sender.sendMessage(mini.deserialize(message));
        } catch (SQLException e) {
            sender.sendMessage(mini.deserialize("<dark_red><b>»</b> Error al recuperar estadísticas."));
        }
    }

    // << -- Helper -- >> //

    private int parseBetAmount(@NotNull String input) {
        input = input.toLowerCase().trim();
        try {
            if (input.endsWith("k")) {
                return Integer.parseInt(input.substring(0, input.length() - 1)) * 1000;
            }
            if (input.endsWith("mil")) {
                return Integer.parseInt(input.substring(0, input.length() - 3)) * 1000;
            }
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int getCurrentBet(@NotNull Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BlackjackInventoryHolder holder))
            return 100; // fallback
        return holder.getBet();
    }
}
