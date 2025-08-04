package me.duart.blackjackLite.command;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.duart.blackjackLite.BlackjackLite;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class RegisterCommands {

    public void registerCommands(@NotNull BlackjackLite plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event ->
                        event.registrar().register("blackjack", List.of("bj", "bjlite"), new BlackjackCommand(plugin))
        );
    }
}
