package io.github.joshiat.claylegion.entity.mount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Horse cosmetic variants. Each variant maps to one or more texture files
 * under assets/clay-legion/textures/entity/horses/.
 *
 * The mount entity stores a single byte {@code variant} that equals an index
 * into {@link #ALL_TEXTURES}. The enum value itself describes the "type"
 * (dirt, sand, clay, etc.); when a horse is spawned with a given type, a
 * random sub-texture from that type's list is chosen.
 */
public enum HorseVariant {
    CLAY("clay.png"),
    DIRT("dirt1.png", "dirt2.png", "dirt3.png", "dirt4.png"),
    SAND("sand.png"),
    GRAVEL("gravel1.png", "gravel2.png"),
    SNOW("snow.png"),
    GRASS("grass1.png", "grass2.png"),
    LAPIS("lapis.png"),
    CARROT("carrot1.png", "carrot2.png"),
    SOULSAND("soulsand.png"),
    CAKE("cake.png"),
    NIGHTMARE("spec_nightmare1.png", "spec_nightmare2.png");

    public final Identifier[] textures;
    private int firstIndex = -1;

    HorseVariant(String... fileNames) {
        this.textures = Arrays.stream(fileNames)
            .map(f -> Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/horses/" + f))
            .toArray(Identifier[]::new);
    }

    public static final Identifier[] ALL_TEXTURES;

    static {
        List<Identifier> all = new ArrayList<>();
        for (HorseVariant v : values()) {
            v.firstIndex = all.size();
            all.addAll(Arrays.asList(v.textures));
        }
        ALL_TEXTURES = all.toArray(new Identifier[0]);
    }

    /** Pick a random sub-texture index inside this variant's range. */
    public byte pickRandomIndex(RandomSource random) {
        return (byte) (firstIndex + random.nextInt(textures.length));
    }

    public static Identifier textureFor(byte variantIndex) {
        int i = variantIndex & 0xff;
        if (i < 0 || i >= ALL_TEXTURES.length) {
            i = 0;
        }
        return ALL_TEXTURES[i];
    }
}
