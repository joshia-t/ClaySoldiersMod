package io.github.joshiat.claylegion.entity.mount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * 10 turtle variants, some with multiple sub-textures.
 */
public enum TurtleVariant {
    COBBLE("cobble1.png", "cobble2.png"),
    MOSSY("mossy.png"),
    SANDSTONE("sandstone1.png", "sandstone2.png", "sandstone3.png"),
    NETHERRACK("netherrack.png"),
    ENDSTONE("endstone.png"),
    LAPIS("lapis.png"),
    MELON("melon1.png", "melon2.png"),
    PUMPKIN("pumpkin1.png", "pumpkin2.png"),
    CAKE("cake.png"),
    KAWAKO("spec_kawako.png");

    public final Identifier[] textures;
    private int firstIndex = -1;

    TurtleVariant(String... fileNames) {
        this.textures = Arrays.stream(fileNames)
            .map(f -> Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/turtles/" + f))
            .toArray(Identifier[]::new);
    }

    public static final Identifier[] ALL_TEXTURES;

    static {
        List<Identifier> all = new ArrayList<>();
        for (TurtleVariant v : values()) {
            v.firstIndex = all.size();
            all.addAll(Arrays.asList(v.textures));
        }
        ALL_TEXTURES = all.toArray(new Identifier[0]);
    }

    public byte pickRandomIndex(RandomSource random) {
        return (byte) (firstIndex + random.nextInt(textures.length));
    }

    public static Identifier textureFor(byte variantIndex) {
        int i = variantIndex & 0xff;
        if (i < 0 || i >= ALL_TEXTURES.length) i = 0;
        return ALL_TEXTURES[i];
    }
}
