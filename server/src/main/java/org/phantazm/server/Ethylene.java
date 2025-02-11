package org.phantazm.server;

import com.github.steanky.element.core.key.KeyParser;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.mapper.MappingProcessorSource;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import net.minestom.server.item.ItemStack;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.parser.SNBTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;

/**
 * Initializes features related to Ethylene.
 */
public final class Ethylene {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mob.class);

    private static MappingProcessorSource mappingProcessorSource;
    private static KeyParser keyParser;

    static void initialize(@NotNull KeyParser keyParser) {
        Ethylene.keyParser = Objects.requireNonNull(keyParser, "keyParser");

        mappingProcessorSource =
                MappingProcessorSource.builder().withCustomSignature(vec3I()).withCustomSignature(sound())
                        .withCustomSignature(style()).withCustomSignature(textColor()).withCustomSignature(vec3D())
                        .withCustomSignature(bounds3D()).withCustomSignature(rgbLike()).withScalarSignature(key())
                        .withScalarSignature(uuid()).withScalarSignature(component()).withScalarSignature(itemStack())
                        .withScalarSignature(titlePartComponent()).withScalarSignature(namedTextColor())
                        .withScalarSignature(particle())
                        .withTypeImplementation(Object2IntOpenHashMap.class, Object2IntMap.class)
                        .withTypeImplementation(IntOpenHashSet.class, IntSet.class).withStandardSignatures()
                        .withStandardTypeImplementations().ignoringLengths().build();

        LOGGER.info("Ethylene initialized.");
    }

    private static Signature<Vec3I> vec3I() {
        return Signature.builder(Token.ofClass(Vec3I.class),
                (ignored, args) -> Vec3I.immutable(args.get(0), args.get(1), args.get(2)),
                vec -> List.of(vec.x(), vec.y(), vec.z()), Map.entry("x", Token.INTEGER), Map.entry("y", Token.INTEGER),
                Map.entry("z", Token.INTEGER)).matchingNames().matchingTypeHints().build();
    }

    private static Signature<Vec3D> vec3D() {
        return Signature.builder(Token.ofClass(Vec3D.class),
                (ignored, args) -> Vec3D.immutable(args.get(0), args.get(1), args.get(2)),
                vec -> List.of(vec.x(), vec.y(), vec.z()), Map.entry("x", Token.DOUBLE), Map.entry("y", Token.DOUBLE),
                Map.entry("z", Token.DOUBLE)).matchingNames().matchingTypeHints().build();
    }

    private static Signature<Sound> sound() {
        return Signature.builder(Token.ofClass(Sound.class), (ignored, args) -> {
                            double volume = args.get(2);
                            double pitch = args.get(3);
                            return Sound.sound((Key)args.get(0), (Sound.Source)args.get(1), (float)volume, (float)pitch);
                        }, sound -> List.of(sound.name(), sound.source(), sound.volume(), sound.pitch()),
                        Map.entry("name", Token.ofClass(Key.class)), Map.entry("source", Token.ofClass(Sound.Source.class)),
                        Map.entry("volume", Token.DOUBLE), Map.entry("pitch", Token.DOUBLE)).matchingNames().matchingTypeHints()
                .build();
    }

    private static Signature<Bounds3D> bounds3D() {
        return Signature.builder(Token.ofClass(Bounds3D.class), (ignored, args) -> {
                            return Bounds3D.immutable(args.get(0), args.get(1));
                        }, bounds -> List.of(bounds.immutableOrigin(), bounds.immutableLengths()),
                        Map.entry("origin", Token.ofClass(Vec3D.class)), Map.entry("lengths", Token.ofClass(Vec3D.class)))
                .build();
    }

    private static Signature<Style> style() {
        return Signature.builder(Token.ofClass(Style.class), (ignored, args) -> {
                            TextColor textColor = args.get(0);
                            TextDecoration[] decorations = args.get(1);
                            return Style.style(textColor, decorations);
                        }, style -> {
                            Collection<Object> textColors = new ArrayList<>(1);
                            textColors.add(style.color());
                            return textColors;
                        }, Map.entry("textColor", Token.ofClass(TextColor.class)),
                        Map.entry("textDecorations", Token.ofClass(TextDecoration[].class))).matchingNames().matchingTypeHints()
                .build();
    }

    private static Signature<RGBLike> rgbLike() {
        return Signature.builder(Token.ofClass(RGBLike.class), (ignored, args) -> {
                            int r = args.get(0);
                            int g = args.get(1);
                            int b = args.get(2);
                            return new Color(r, g, b);
                        }, color -> List.of(color.red(), color.green(), color.blue()), Map.entry("r", Token.INTEGER),
                        Map.entry("g", Token.INTEGER), Map.entry("b", Token.INTEGER)).matchingNames().matchingTypeHints()
                .build();
    }

    private static Signature<TextColor> textColor() {
        return Signature.builder(Token.ofClass(TextColor.class), (ignored, args) -> {
                            int r = args.get(0);
                            int g = args.get(1);
                            int b = args.get(2);
                            return TextColor.color(r, g, b);
                        }, textColor -> List.of(textColor.red(), textColor.green(), textColor.blue()), Map.entry("r", Token.INTEGER),
                        Map.entry("g", Token.INTEGER), Map.entry("b", Token.INTEGER)).matchingNames().matchingTypeHints()
                .build();
    }

    private static ScalarSignature<Particle> particle() {
        return ScalarSignature.of(Token.ofClass(Particle.class),
                element -> Particle.fromNamespaceId(element.asString()),
                particle -> particle == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(particle.key().toString()));
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ScalarSignature<ItemStack> itemStack() {
        return ScalarSignature.of(Token.ofClass(ItemStack.class), element -> {
            try {
                return ItemStack.fromItemNBT((NBTCompound)new SNBTParser(new StringReader(element.asString())).parse());
            }
            catch (NBTException e) {
                return ItemStack.AIR;
            }
        }, itemStack -> itemStack == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(itemStack.toItemNBT().toSNBT()));
    }

    private static ScalarSignature<TitlePart<Component>> titlePartComponent() {
        ConfigPrimitive title = ConfigPrimitive.of("TITLE");
        ConfigPrimitive subtitle = ConfigPrimitive.of("SUBTITLE");

        return ScalarSignature.of(new Token<>() {
        }, element -> {
            String value = element.asString();
            return switch (value) {
                case "TITLE" -> TitlePart.TITLE;
                case "SUBTITLE" -> TitlePart.SUBTITLE;
                default -> throw new IllegalArgumentException("Unexpected TitlePart '" + value + "'");
            };
        }, titlePart -> titlePart == null ? ConfigPrimitive.NULL : titlePart == TitlePart.TITLE ? title : subtitle);
    }

    public static @NotNull MappingProcessorSource getMappingProcessorSource() {
        return FeatureUtils.check(mappingProcessorSource);
    }

    @SuppressWarnings("PatternValidation")
    private static ScalarSignature<Key> key() {
        return ScalarSignature.of(Token.ofClass(Key.class), element -> keyParser.parseKey(element.asString()),
                key -> key == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(key.asString()));
    }

    private static ScalarSignature<TextColor> namedTextColor() {
        return ScalarSignature.of(Token.ofClass(TextColor.class),
                element -> NamedTextColor.NAMES.valueOr(element.asString().toLowerCase(Locale.ROOT),
                        NamedTextColor.WHITE), textColor -> {
                    if (textColor == null) {
                        return ConfigPrimitive.NULL;
                    }

                    NamedTextColor named = NamedTextColor.namedColor(textColor.value());
                    if (named == null) {
                        return ConfigPrimitive.NULL;
                    }

                    return ConfigPrimitive.of(named.toString());
                });
    }

    private static ScalarSignature<Component> component() {
        return ScalarSignature.of(Token.ofClass(Component.class),
                element -> MiniMessage.miniMessage().deserialize(element.asString()), component -> component == null
                                                                                                   ? ConfigPrimitive.NULL
                                                                                                   : ConfigPrimitive.of(
                                                                                                           MiniMessage.miniMessage()
                                                                                                                   .serialize(
                                                                                                                           component)));
    }

    private static ScalarSignature<UUID> uuid() {
        return ScalarSignature.of(Token.ofClass(UUID.class), element -> UUID.fromString(element.asString()),
                uuid -> uuid == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(uuid.toString()));
    }
}
