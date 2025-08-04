package me.duart.blackjackLite.gui;

import com.google.common.collect.HashMultimap;
import me.duart.blackjackLite.BlackjackLite;
import me.duart.blackjackLite.util.BlackjackGame;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.duart.blackjackLite.gui.BorderAnimator.animateBlackjack;

public class BlackjackMenus {

    private final BlackjackLite plugin;

    private final HashMultimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
    private static final String NAMESPACE = "blakcjacklite";
    private static final NamespacedKey ID_KEY = new NamespacedKey(NAMESPACE, "id");

    int IRREGULAR_CHEST = 9 * 5; // 5 rows

    public BlackjackMenus(BlackjackLite plugin) {
        this.plugin = plugin;
    }

    public Inventory openInitialMenu(int bet) {
        Component title = BlackjackLite.instance.mini().deserialize("<b><color:#ff5d05>ʙᴊʟɪᴛᴇ </b>⌇⌇<b> ʜᴀᴢ ᴛᴜ ᴀᴘᴜᴇsᴛᴀ");
        BlackjackInventoryHolder holder = new BlackjackInventoryHolder(plugin, title, IRREGULAR_CHEST, "initial_menu", bet);
        Inventory inv = holder.getInventory();

        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(22, createItem(Material.PAPER, "<yellow>Apuesta actual: <green>" + bet, "current_bet"));

        inv.setItem(11, createItem(Material.LIME_WOOL, "<green>+10", "plus_10"));
        inv.setItem(20, createItem(Material.LIME_WOOL, "<green>+100", "plus_100"));
        inv.setItem(29, createItem(Material.LIME_WOOL, "<green>+1 000", "plus_1000"));

        inv.setItem(15, createItem(Material.RED_WOOL, "<red>-10", "minus_10"));
        inv.setItem(24, createItem(Material.RED_WOOL, "<red>-100", "minus_100"));
        inv.setItem(33, createItem(Material.RED_WOOL, "<red>-1 000", "minus_1000"));

        inv.setItem(18, createItem(Material.BARRIER, "<red>Reiniciar apuesta", "reset_bet"));
        inv.setItem(26, createItem(Material.EMERALD_BLOCK, "<green>Iniciar Juego", "start_game"));
        inv.setItem(44, createItem(Material.BUCKET, "<gray>Cerrar", "close"));
        inv.setItem(0, createItemWithLore(Material.BOOK, "<red>¿Cómo jugar?", "how_to_play", List.of(howToPlay)));

        return inv;
    }

    public Inventory openGameMenu(int bet, @NotNull BlackjackGame game) {
        Component title = BlackjackLite.instance.mini().deserialize("<b><color:#ff3f38>⌇⌇ ᴍᴇsᴀ ᴅᴇ ᴊᴜᴇɢᴏ");
        BlackjackInventoryHolder holder = new BlackjackInventoryHolder(plugin, title, IRREGULAR_CHEST, "game_menu", bet);
        Inventory inv = holder.getInventory();

        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        int dealerStartSlots = 10;
        for (int i = 0; i < 7; i++) {
            inv.setItem(dealerStartSlots + i, createItem(Material.YELLOW_STAINED_GLASS_PANE, "<gray>Espacio de Cartas del Dealer", "placeholder"));
        }

        int playerStartSlots = 28;
        for (int i = 0; i < 7; i++) {
            inv.setItem(playerStartSlots + i, createItem(Material.YELLOW_STAINED_GLASS_PANE, "<gray>Espacio de Cartas del Jugador", "placeholder"));
        }

        inv.setItem(27, createItem(Material.OAK_SIGN, "<gold>Tu total: <white>" + game.getPlayerTotal(), "player_total"));

        String totalText;
        if (game.getDealerHand().size() == 2) {
            int visible = game.getDealerHand().getFirst().value();
            totalText = visible + " + ?";
        } else {
            totalText = String.valueOf(game.getDealerTotal());
        }
        inv.setItem(9, createItem(Material.OAK_SIGN, "<gold>Total dealer: <white>" + totalText, "dealer_total"));

        inv.setItem(35, createItem(Material.GOLD_NUGGET, "<yellow>Apuesta: <green>" + bet, "bet"));

        inv.setItem(20, createItem(Material.GREEN_WOOL, "<green>Pedir Carta", "hit"));
        inv.setItem(22, createItem(Material.RED_WOOL, "<red>Plantarse", "stand"));

        Material boolMat = game.canDoubleDown() ? Material.PURPLE_WOOL : Material.GRAY_STAINED_GLASS_PANE;
        String doubleDownName = game.canDoubleDown() ? ("<blue>Doblar la apuesta") : ("<gray>No disponible");
        inv.setItem(24, createItem(boolMat, doubleDownName, "double_down"));

        inv.setItem(17, createItem(Material.BOOK, "<gray>Cartas restantes: <white>" + game.getRemainingDeckSize(), "deck_left"));
        inv.setItem(44, createItem(Material.BUCKET, "<gray>Cerrar", "close"));

        /* ---- DEALER CARDS (row 2, slots 10-16 → max 7 cards) ---- */
        List<BlackjackGame.Card> dealerCards = game.getDealerHand();
        int[] dealerCardSlots = {13, 12, 14, 11, 15, 10, 16};
        for (int i = 0; i < dealerCards.size() && i < dealerCardSlots.length; i++) {
            int slot = dealerCardSlots[i];
            if (i == 1 && dealerCards.size() == 2) {
                inv.setItem(slot, createItem(Material.MAP, "<red>Carta oculta", "hidden_card"));
            } else {
                inv.setItem(slot, createCardItem(dealerCards.get(i)));
            }
        }

        /* ---- PLAYER CARDS (row 4, slots 28-34 → max 7 cards) ---- */
        List<BlackjackGame.Card> playerCards = game.getPlayerHand();
        int[] playerCardSlots = {31, 30, 32, 29, 33, 28, 34};
        for (int i = 0; i < playerCards.size() && i < playerCardSlots.length; i++) {
            int slot = playerCardSlots[i];
            inv.setItem(slot, createCardItem(playerCards.get(i)));
        }

        return inv;
    }

    public Inventory createResultMenu(@NotNull BlackjackGame.Result result, boolean playerHasBlackjack, @NotNull BlackjackGame game, int bet, int winnings, int totalEarnings) {
        Component title;

        if (playerHasBlackjack) {
            title = BlackjackLite.instance.mini().deserialize("<b><gradient:#FF0000:#000000:#000000:#FF0000>⌇⌇ ¡BLACKJACK! ⌇⌇</gradient></b>");
        } else {
            title = switch (result) {
                case WIN -> BlackjackLite.instance.mini().deserialize("<b><green>⌇⌇ ¡ɢᴀɴᴀsᴛᴇ!");
                case LOSE -> BlackjackLite.instance.mini().deserialize("<b><red>⌇⌇ ᴘᴇʀᴅɪsᴛᴇ... :(");
                case DRAW -> BlackjackLite.instance.mini().deserialize("<b><yellow>⌇⌇ ¡ᴇᴍᴘᴀᴛᴇ!");
            };
        }

        BlackjackInventoryHolder holder = new BlackjackInventoryHolder(plugin, title, IRREGULAR_CHEST, "result_menu", bet);
        Inventory inv = holder.getInventory();


        String formattedWinnings;
        Component formattedTotalEarnings;

        if (winnings > 0) {
            formattedWinnings = "<green>+$" + winnings;
        } else if (winnings < 0) {
            formattedWinnings = "<red>-$" + Math.abs(winnings);
        } else {
            formattedWinnings = "<yellow>$0";
        }

        if (totalEarnings > 0) {
            formattedTotalEarnings =  BlackjackLite.instance.mini().deserialize("<green>Ganancias Totales: +" + totalEarnings + "$");
        } else if (totalEarnings < 0) {
            formattedTotalEarnings = BlackjackLite.instance.mini().deserialize("<red>Ganancias Totales: " + totalEarnings + "$");
        } else {
            formattedTotalEarnings = BlackjackLite.instance.mini().deserialize("<yellow>Ganancias Totales: 0$");
        }

        ItemStack winningsItem = createItem(Material.GOLD_INGOT, "<yellow>Ganancias: " + formattedWinnings, "winnings");
        ItemMeta meta = winningsItem.getItemMeta();
        assert meta != null;

        meta.lore(List.of(formattedTotalEarnings));
        winningsItem.setItemMeta(meta);

        if (playerHasBlackjack) {
            animateBlackjack(plugin, inv, 400);
        } else {
            Material resultMat = switch (result) {
                case WIN -> Material.LIME_STAINED_GLASS_PANE;
                case LOSE -> Material.RED_STAINED_GLASS_PANE;
                case DRAW -> Material.ORANGE_STAINED_GLASS_PANE;
            };
            fillBorder(inv, resultMat);
        }

        int dealerStartSlots = 10;
        for (int i = 0; i < 7; i++) {
            inv.setItem(dealerStartSlots + i, createItem(Material.YELLOW_STAINED_GLASS_PANE, "<gray>Espacio vacio del Dealer", "placeholder"));
        }

        int playerStartSlots = 28;
        for (int i = 0; i < 7; i++) {
            inv.setItem(playerStartSlots + i, createItem(Material.YELLOW_STAINED_GLASS_PANE, "<gray>Espacio vacio del Jugador", "placeholder"));
        }

        inv.setItem(27, createItem(Material.OAK_SIGN, "<gold>Tu total: <white>" + game.getPlayerTotal(), "player_total"));
        inv.setItem(9, createItem(Material.OAK_SIGN, "<gold>Total dealer: <white>" + game.getDealerTotal(), "dealer_total"));

        inv.setItem(22, createItem(Material.GOLD_NUGGET, "<yellow>Apuesta Final: <green>" + bet, "final_bet"));
        inv.setItem(4, winningsItem);
        inv.setItem(36, createItem(Material.EMERALD_BLOCK, "<green>Jugar Otra Vez", "play_again"));
        inv.setItem(44, createItem(Material.BARRIER, "<red>Cerrar", "close"));
        inv.setItem(19, createItem(Material.LIME_WOOL, "+10", "plus_10"));
        inv.setItem(20, createItem(Material.LIME_WOOL, "+100", "plus_100"));
        inv.setItem(21, createItem(Material.LIME_WOOL, "+1 000", "plus_1000"));
        inv.setItem(23, createItem(Material.RED_WOOL, "-10", "minus_10"));
        inv.setItem(24, createItem(Material.RED_WOOL, "-100", "minus_100"));
        inv.setItem(25, createItem(Material.RED_WOOL, "-1 000", "minus_1000"));

        /* ---- DEALER CARDS (row 2,  → max 5 cards) ---- */
        List<BlackjackGame.Card> dealerCards = game.getDealerHand();
        int[] dealerCardSlots = {13, 12, 14, 11, 15, 10, 16};
        for (int i = 0; i < dealerCards.size() && i < dealerCardSlots.length; i++) {
            inv.setItem(dealerCardSlots[i], createCardItem(dealerCards.get(i)));
        }

        /* ---- PLAYER CARDS (row 5 → max 6 cards) ---- */
        List<BlackjackGame.Card> playerCards = game.getPlayerHand();
        int[] playerCardSlots = {31, 30, 32, 29, 33, 28, 34};
        for (int i = 0; i < playerCards.size() && i < playerCardSlots.length; i++) {
            int slot = playerCardSlots[i];
            inv.setItem(slot, createCardItem(playerCards.get(i)));
        }

        return inv;
    }

    public ItemStack createCardItem(BlackjackGame.@NotNull Card card) {
        String displayName = card.getDisplayName();

        if (!displayName.contains(card.getSuitSymbol())) {
            displayName += " " + card.getSuitSymbol();
        }

        return createItem(Material.FILLED_MAP, displayName, "card_" + card.name());
    }

    @NotNull
    public ItemStack createItem(Material mat, String itemName, String id) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.STRING, id);
        meta.displayName(BlackjackLite.instance.mini().deserialize(itemName));
        meta.setAttributeModifiers(modifiers);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    public ItemStack createItemWithLore(Material mat, String itemName, String id, @NotNull List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.STRING, id);
        meta.displayName(BlackjackLite.instance.mini().deserialize(itemName));
        meta.setAttributeModifiers(modifiers);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.lore(loreLines.stream().map(line -> BlackjackLite.instance.mini().deserialize(line)).toList());

        item.setItemMeta(meta);
        return item;
    }

    public static @Nullable String getItemID(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(ID_KEY, PersistentDataType.STRING);
    }

    private void fillBorder(@NotNull Inventory inv, Material mat) {
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, createItem(mat, " ", "filler"));
            }
        }
    }

    public static void playSound(Player player, Sound sound, float vol, float pitch) {
        if (player != null) player.playSound(player.getLocation(), sound, vol, pitch);
    }

    public static final String[] howToPlay = {
            "<white>✦ . 　⁺ 　 . 　⁺ 　 . ✦ . 　⁺ 　 . 　⁺ 　 . ✦",
            "",
            "<yellow>Objetivo:</yellow> Tener una mano mayor que la del crupier, pero",
            "sin tener cartas mayores que 21.",
            "",
            "<gold>Cartas:</gold> 2-10 valen su número, J/Q/K valen 10,",
            "As vale 1 o 11 dependiendo si supera 21 o no",
            "sumando las demas cartas.",
            "",
            "<green>¿Cómo jugar?</green>",
            "<white>1. Apuesta: <yellow>/blackjack <cantidad></white>",
            "<white>2. Recibes 2 cartas. El crupier también.</white>",
            "<white>3. Elige: <red>Hit</red> (carta), <yellow>Stand</yellow> (plantarte), <gold>Double</gold> (doblar).</white>",
            "<white>4. El crupier juega y se compara la mano.</white>",
            "",
            "<gold>¿Ganas?</gold> Si superas al crupier sin pasarte de 21.",
            "<red>¡Buena suerte!</red>",
            "",
            "<white>✦ . 　⁺ 　 . 　⁺ 　 . ✦ . 　⁺ 　 . 　⁺ 　 . ✦"
    };
}
