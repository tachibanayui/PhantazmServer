package org.phantazm.zombies.map.handler;

import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.map.Round;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BasicRoundHandler implements RoundHandler {
    private final List<Round> rounds;

    private Round currentRound;
    private int roundIndex;

    private boolean hasEnded;

    public BasicRoundHandler(@NotNull List<Round> rounds) {
        this.rounds = Objects.requireNonNull(rounds, "rounds");

        if (rounds.isEmpty()) {
            hasEnded = true;
        }
    }

    @Override
    public void tick(long time) {
        if (currentRound != null) {
            currentRound.tick(time);

            if (!currentRound.isActive()) {
                if (++roundIndex < rounds.size()) {
                    currentRound = rounds.get(roundIndex);
                    currentRound.startRound(time);
                }
                else {
                    hasEnded = true;
                    currentRound = null;
                }
            }
        }
    }

    @Override
    public int roundCount() {
        return rounds.size();
    }

    @Override
    public int currentRoundIndex() {
        return roundIndex;
    }

    @Override
    public void setCurrentRound(int roundIndex) {
        Objects.checkIndex(roundIndex, rounds.size());

        this.roundIndex = roundIndex;
        Round newCurrent = rounds.get(roundIndex);
        if (newCurrent == currentRound) {
            currentRound.endRound();
            currentRound.startRound(System.currentTimeMillis());
            return;
        }

        currentRound = newCurrent;
        currentRound.startRound(System.currentTimeMillis());
    }

    @Override
    public @NotNull Optional<Round> currentRound() {
        return Optional.ofNullable(currentRound);
    }

    @Override
    public boolean hasEnded() {
        return hasEnded;
    }
}
