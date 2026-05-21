package io.github.joshiat.claylegion.entity.mount;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * 6 gecko variants (wood types). Each variant has two textures matching the
 * OLD two-pass renderer design: a body texture (base color) and a spots
 * texture (transparent overlay rendered on top, additively in the OLD system).
 */
public enum GeckoVariant {
    OAK("body_oak.png",     "spots_oak.png"),
    BIRCH("body_birch.png", "spots_birch.png"),
    JUNGLE("body_jungle.png", "spots_jungle.png"),
    ACACIA("body_acacia.png", "spots_acacia.png"),
    DARKOAK("body_darkoak.png", "spots_darkoak.png"),
    PINE("body_pine.png",   "spots_pine.png");

    private static final String BASE = "textures/entity/gecko/";

    public final Identifier bodyTexture;
    public final Identifier spotsTexture;
    private int firstIndex = -1;

    GeckoVariant(String bodyFile, String spotsFile) {
        this.bodyTexture  = Identifier.fromNamespaceAndPath("clay-legion", BASE + bodyFile);
        this.spotsTexture = Identifier.fromNamespaceAndPath("clay-legion", BASE + spotsFile);
    }

    // --- body texture lookup ---

    public static final Identifier[] ALL_BODY_TEXTURES;
    public static final Identifier[] ALL_SPOTS_TEXTURES;

    static {
        GeckoVariant[] vals = values();
        ALL_BODY_TEXTURES  = new Identifier[vals.length];
        ALL_SPOTS_TEXTURES = new Identifier[vals.length];
        for (int i = 0; i < vals.length; i++) {
            vals[i].firstIndex    = i;
            ALL_BODY_TEXTURES[i]  = vals[i].bodyTexture;
            ALL_SPOTS_TEXTURES[i] = vals[i].spotsTexture;
        }
    }

    /** Pick the texture index for this variant (one-to-one: 1 body per variant). */
    public byte pickRandomIndex(RandomSource random) {
        return (byte) firstIndex;
    }

    public static Identifier bodyTextureFor(byte variantIndex) {
        int i = variantIndex & 0xff;
        if (i >= ALL_BODY_TEXTURES.length) i = 0;
        return ALL_BODY_TEXTURES[i];
    }

    public static Identifier spotsTextureFor(byte variantIndex) {
        int i = variantIndex & 0xff;
        if (i >= ALL_SPOTS_TEXTURES.length) i = 0;
        return ALL_SPOTS_TEXTURES[i];
    }
}
