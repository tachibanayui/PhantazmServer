package org.phantazm.zombies.equipment.perk.effect;

import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.Model;
import com.github.steanky.element.core.annotation.document.Description;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.attribute.AttributeModifier;
import net.minestom.server.attribute.AttributeOperation;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.Attributes;
import org.phantazm.zombies.player.ZombiesPlayer;

import java.util.Objects;
import java.util.UUID;

@Description("""
        Modifies a player attribute for the duration the perk is active.
                
        Player attributes can be any of the attributes listed on https://minecraft.fandom.com/wiki/Attribute. However,
        only a subset of these *currently* have any effect. These include:
                
        * `generic.max_health`
        * `generic.movement_speed`
                
        There are also several custom attributes defined by Phantazm Zombies:
                
        * `phantazm.fire_rate`
        * `phantazm.revive_speed`
                
        `phantazm.fire_rate` is itself a multiplier. Its base value is 1. `phantazm.revive_speed` is the number of ticks
        it takes to revive a player. It defaults to 30.
                
        These attributes can be modified by providing some amount and an AttributeOperation. The operation may be any of
        the following values:
                
        * `ADDITION`
        * `MULTIPLY_BASE`
        * `MULTIPLY_TOTAL`
                
        The effects of these multipliers use the rules outlined at https://minecraft.fandom.com/wiki/Attribute#Modifiers.
        """)
@Model("zombies.perk.effect.attribute_modifier")
@Cache(false)
public class ModifierPerkEffectCreator implements PerkEffectCreator {
    private final Data data;

    public ModifierPerkEffectCreator(@NotNull Data data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public @NotNull PerkEffect forPlayer(@NotNull ZombiesPlayer zombiesPlayer) {
        return new Effect(data, zombiesPlayer);
    }

    private static class Effect implements PerkEffect {
        private final Data data;
        private final ZombiesPlayer zombiesPlayer;
        private final Attribute attribute;
        private final UUID uuid;
        private final String name;

        public Effect(@NotNull Data data, @NotNull ZombiesPlayer zombiesPlayer) {
            this.data = data;
            this.zombiesPlayer = Objects.requireNonNull(zombiesPlayer, "zombiesPlayer");

            this.attribute = Objects.requireNonNullElse(Attribute.fromKey(data.attribute), Attributes.NIL);
            this.uuid = UUID.randomUUID();
            this.name = this.uuid.toString();
        }

        @Override
        public void start() {
            zombiesPlayer.getPlayer().ifPresent(player -> player.getAttribute(attribute)
                    .addModifier(new AttributeModifier(uuid, name, data.amount, data.attributeOperation)));
        }

        @Override
        public void end() {
            zombiesPlayer.getPlayer().ifPresent(player -> player.getAttribute(attribute).removeModifier(uuid));
        }
    }

    @DataObject
    public record Data(@NotNull @Description("The modifier name.") String attribute,
                       @Description("The amount by which to modify this attribute.") double amount,
                       @NotNull @Description(
                               "The operation to use in order to modify this attribute.") AttributeOperation attributeOperation) {

    }
}
