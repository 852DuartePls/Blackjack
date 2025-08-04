package me.duart.blackjackLite.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.gui.BlackjackMenus;
import me.duart.blackjackLite.util.BlackjackGame;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
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
                        player.sendMessage(Component.text("Has retomado tu partida anterior."));
                        player.openInventory(menus.openGameMenu(game.getBet(), game));
                    } else {
                        player.sendMessage(Component.text("Usa: /bj <cantidad> o /bj <jugador> para ver estadísticas."));
                    }
                } catch (SQLException e) {
                    player.sendMessage(Component.text("Ocurrió un error al recuperar tu sesión."));
                    BlackjackLite.logger().error("Error loading session for {}: {}", player.getName(), e.getMessage());
                }
                return;
            }

            if (args.length == 1 && isNumeric(args[0])) {
                int bet = parseBetAmount(args[0]);

                if (bet == -1) {
                    player.sendMessage(Component.text("Número inválido: " + args[0]));
                    return;
                }

                if (bet < 100) {
                    player.sendMessage(Component.text("La apuesta mínima es de 100$."));
                    return;
                }

                if (!BlackjackLite.instance().hasEnough(player, bet)) {
                    player.sendMessage(Component.text("No tienes suficiente dinero para hacer esta apuesta."));
                    return;
                }

                try {
                    var session = BlackjackLite.instance().getSessionDB().load(player.getUniqueId());

                    if (session.isPresent()) {
                        player.sendMessage(Component.text("Has retomado tu partida anterior."));
                        player.openInventory(menus.openGameMenu(session.get().getBet(), session.get()));
                    } else {
                        player.openInventory(menus.openInitialMenu(bet));
                    }
                } catch (SQLException e) {
                    player.sendMessage(Component.text("Ocurrió un error al verificar tu sesión anterior."));
                    BlackjackLite.instance().getLogger().warning("An error occurred while checking previous session: " + e.getMessage());
                }
                return;
            }

            if (args.length == 1) {
                showPlayerStats(player, args[0]);
                return;
            }

            player.sendMessage(Component.text("Usa: /bj <cantidad> o /bj <jugador> para ver estadísticas."));

        } else if (stack.getSender() instanceof ConsoleCommandSender consoleSender) {
            if (args.length == 1 && isNumeric(args[0])) {
                consoleSender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores. Usa: /bj <jugador> para ver estadísticas."));
                return;
            }

            if (args.length == 1) {
                showPlayerStats(consoleSender, args[0]);
                return;
            }

            consoleSender.sendMessage(Component.text("Uso inválido. Solo se pueden consultar estadísticas desde la consola. Usa: /bj <jugador>"));
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
                sender.sendMessage(Component.text("No se encontraron estadísticas para " + targetName));
                return;
            }

            var mini = BlackjackLite.instance().mini();

            String message = """
            
            <green>─── ･ ｡ﾟ☆: *.☽ .* :☆ﾟ. ───</green>
            <gold>Estadísticas de <white><player></white>:
             <gray>- Partidas jugadas:</gray> <white><games></white>
             <gray>- Victorias:</gray> <green><wins></green>
             <gray>- Derrotas:</gray> <red><losses></red>
             <gray>- Ganancias totales:</gray> <green><winnings>$</green>
             <gray>- Pérdidas totales:</gray> <red><lossesAmount>$</red>
            <green>─── ･ ｡ﾟ☆: *.☽ .* :☆ﾟ. ───</green>
            """.replace("<player>", stats.playerName())
                    .replace("<games>", String.valueOf(stats.gamesPlayed()))
                    .replace("<wins>", String.valueOf(stats.wins()))
                    .replace("<losses>", String.valueOf(stats.losses()))
                    .replace("<winnings>", String.valueOf(stats.totalWinnings()))
                    .replace("<lossesAmount>", String.valueOf(stats.totalLosses()));

            // Send message to sender (either Player or Console)
            sender.sendMessage(mini.deserialize(message));
        } catch (SQLException e) {
            sender.sendMessage(Component.text("Error al recuperar estadísticas."));
        }
    }

    // << -- Helpers -- >> //

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Check for "k" or "mil" suffixes and multiply the value by 1000
    // mil = thousand in Spanish
    private int parseBetAmount(@NotNull String input) {
        if (input.toLowerCase().endsWith("k")) {
            String numberPart = input.substring(0, input.length() - 1);
            try {
                int value = Integer.parseInt(numberPart);
                return value * 1000;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        if (input.toLowerCase().endsWith("mil")) {
            String numberPart = input.substring(0, input.length() - 3);
            try {
                int value = Integer.parseInt(numberPart);
                return value * 1000;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
