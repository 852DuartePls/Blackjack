package me.duart.blackjackLite.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlackjackGame {

    private List<Card> deck;
    private int bet;
    private static final Gson GSON = new Gson();
    private List<Card> playerHand = new ArrayList<>();
    private List<Card> dealerHand = new ArrayList<>();

    public BlackjackGame(int bet) {
        this.deck = createDeck();
        shuffleDeck();
        this.bet = bet;
    }

    private @NotNull List<Card> createDeck() {
        List<Card> newDeck = new ArrayList<>();
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        String[] suits = {"♥", "♦", "♣", "♠"};

        for (String suit : suits) {
            for (String rank : ranks) {
                int value = switch (rank) {
                    case "J", "Q", "K" -> 10;
                    case "A" -> 11;
                    default -> Integer.parseInt(rank);
                };
                newDeck.add(new Card(rank, suit, value));
            }
        }

        return newDeck;
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    public void startGame() {
        playerHand.clear();
        dealerHand.clear();
        drawCard(playerHand);
        drawCard(dealerHand);
        drawCard(playerHand);
        drawCard(dealerHand);
    }

    public void drawCard(List<Card> hand) {
        if (!deck.isEmpty()) {
            hand.add(deck.removeFirst());
        }
    }

    private int calculateTotal(@NotNull List<Card> hand) {
        int total = 0;
        int aceCount = 0;

        for (Card card : hand) {
            total += card.value();
            if (card.name().equals("A")) aceCount++;
        }

        // If the total is greater than 21, it will subtract 10 from the value of the Ace
        while (total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }

        return total;
    }

    public List<Card> getPlayerHand() {
        return Collections.unmodifiableList(playerHand);
    }

    public List<Card> getDealerHand() {
        return Collections.unmodifiableList(dealerHand);
    }

    public void hitPlayer() {
        drawCard(playerHand);
    }

    public int getPlayerTotal() {
        return calculateTotal(playerHand);
    }

    public int getDealerTotal() {
        return calculateTotal(dealerHand);
    }

    public void dealerPlay() {
        while (getDealerTotal() < 17) {
            drawCard(dealerHand);
        }
    }

    public boolean canDoubleDown() {
        return playerHand.size() == 2;
    }

    public int getRemainingDeckSize() {
        return deck.size();
    }

    public boolean isBlackjack(@NotNull List<Card> hand) {
        return hand.size() == 2 && calculateTotal(hand) == 21;
    }

    public boolean isBust(List<Card> hand) {
        return calculateTotal(hand) > 21;
    }

    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = Math.max(bet, 100);
    }

    public enum Result {WIN, LOSE, DRAW}

    public Result determineResult() {
        int playerTotal = calculateTotal(playerHand);
        int dealerTotal = calculateTotal(dealerHand);

        if (isBlackjack(playerHand)) {
            return Result.WIN;
        }
        if (isBust(playerHand)) {
            return Result.LOSE;
        }
        if (isBust(dealerHand)) {
            return Result.WIN;
        }
        if (playerTotal > dealerTotal) {
            return Result.WIN;
        }
        if (playerTotal < dealerTotal) {
            return Result.LOSE;
        }
        return Result.DRAW;
    }

    public String serialize() {
        return GSON.toJson(Map.of(
                "deck", deck,
                "bet", bet,
                "playerHand", playerHand,
                "dealerHand", dealerHand
        ));
    }

    public static @NotNull BlackjackGame deserialize(String json, int bet) {
        Map<?,?> map = GSON.fromJson(json, Map.class);
        BlackjackGame game = new BlackjackGame(bet);
        game.deck = GSON.fromJson(GSON.toJson(map.get("deck")), new TypeToken<List<Card>>(){}.getType());
        game.playerHand = GSON.fromJson(GSON.toJson(map.get("playerHand")), new TypeToken<List<Card>>(){}.getType());
        game.dealerHand = GSON.fromJson(GSON.toJson(map.get("dealerHand")), new TypeToken<List<Card>>(){}.getType());
        return game;
    }

    public record Card(String name, String suit, int value) {

        @Contract(pure = true)
        public @NotNull String getDisplayName() {
            String valueName = switch (name) {
                case "J" -> "Jota";
                case "Q" -> "Reina";
                case "K" -> "Rey";
                case "A" -> "As";
                default -> name;
            };

            if (name.equals("A")) {
                return valueName + " " + suit;
            } else {
                return valueName + " de " + suit;
            }
        }

        @Contract(pure = true)
        public @NotNull String getSuitSymbol() {
            return switch (suit) {
                case "♥" -> "♥";
                case "♦" -> "♦";
                case "♣" -> "♣";
                case "♠" -> "♠";
                default -> "";
            };
        }
    }
}

