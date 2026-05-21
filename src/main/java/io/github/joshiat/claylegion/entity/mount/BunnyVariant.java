package io.github.joshiat.claylegion.entity.mount;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * 16 color variants for the bunny mount (one texture each).
 */
public enum BunnyVariant {
    WHITE("white.png"),
    LIGHT_GRAY("light_gray.png"),
    GRAY("gray.png"),
    BLACK("black.png"),
    BROWN("brown.png"),
    RED("red.png"),
    ORANGE("orange.png"),
    YELLOW("yellow.png"),
    LIME("lime.png"),
    GREEN("green.png"),
    CYAN("cyan.png"),
    LIGHT_BLUE("light_blue.png"),
    BLUE("blue.png"),
    PURPLE("purple.png"),
    MAGENTA("magenta.png"),
    PINK("pink.png");

    public final Identifier[] textures;
    private int firstIndex = -1;

    BunnyVariant(String fileName) {
        this.textures = new Identifier[] {
            Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/bunnies/" + fileName)
        };
    }

    public static final Identifier[] ALL_TEXTURES;

    static {
        List<Identifier> all = new ArrayList<>();
        for (BunnyVariant v : values()) {
            v.firstIndex = all.size();
            for (Identifier id : v.textures) all.add(id);
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
