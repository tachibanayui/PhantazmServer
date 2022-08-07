package com.github.phantazmnetwork.zombies.game.coin;

import org.jetbrains.annotations.NotNull;

public interface PlayerCoins {

    @NotNull TransactionResult runTransaction(@NotNull Transaction transaction);

    default @NotNull TransactionResult modify(int change) {
        return runTransaction(new Transaction(change));
    }

    int getCoins();

    void applyTransaction(@NotNull TransactionResult result);

}
