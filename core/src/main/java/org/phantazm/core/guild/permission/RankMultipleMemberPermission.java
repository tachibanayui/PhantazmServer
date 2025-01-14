package org.phantazm.core.guild.permission;

import org.jetbrains.annotations.NotNull;
import org.phantazm.core.guild.GuildMember;
import org.phantazm.core.guild.Ranked;

public class RankMultipleMemberPermission<TMember extends GuildMember & Ranked> implements
        MultipleMemberPermission<TMember> {

    private final int minimumKickRank;

    public RankMultipleMemberPermission(int minimumExecutionRank) {
        this.minimumKickRank = minimumExecutionRank;
    }

    @Override
    public boolean hasPermission(@NotNull TMember executor) {
        return executor.rank() >= minimumKickRank;
    }

    @Override
    public boolean canExecute(@NotNull TMember executor, @NotNull TMember target) {
        return executor.rank() > target.rank();
    }
}
