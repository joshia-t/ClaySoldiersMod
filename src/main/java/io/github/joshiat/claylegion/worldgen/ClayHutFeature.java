package io.github.joshiat.claylegion.worldgen;

import com.mojang.serialization.Codec;
import io.github.joshiat.claylegion.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Clay Hut (issue #27): a small abandoned potter's hut that generates rarely
 * on the surface, sheltering a dormant Clay Nexus. Since #37 the nexus only
 * activates once a player inserts a doll, so found huts are safe loot, not
 * an ambush.
 *
 * Layout (5x5 footprint, 4 tall):
 *  - packed-mud floor, terracotta walls with a south doorway
 *  - clay roof ring with a terracotta cap
 *  - the nexus pedestal at the center
 */
public class ClayHutFeature extends Feature<NoneFeatureConfiguration> {

    private static final int SIZE = 5;
    private static final int WALL_HEIGHT = 3;

    public ClayHutFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // Require reasonably solid ground under the full footprint.
        int solidGround = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos below = origin.offset(x, -1, z);
                if (level.getBlockState(below).isSolidRender()) {
                    solidGround++;
                }
            }
        }
        if (solidGround < (SIZE * SIZE) * 3 / 4) {
            return false;
        }

        BlockState floor = Blocks.PACKED_MUD.defaultBlockState();
        BlockState wall = Blocks.TERRACOTTA.defaultBlockState();
        BlockState roof = Blocks.CLAY.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean isEdge = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;

                // Floor sits flush with the terrain surface.
                level.setBlock(origin.offset(x, -1, z), floor, 2);

                for (int y = 0; y < WALL_HEIGHT; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!isEdge) {
                        level.setBlock(pos, air, 2);
                        continue;
                    }
                    // South doorway: two air blocks in the middle of the wall.
                    boolean doorway = z == SIZE - 1 && x == SIZE / 2 && y < 2;
                    level.setBlock(pos, doorway ? air : wall, 2);
                }

                // Roof: clay ring with a terracotta center cap.
                level.setBlock(origin.offset(x, WALL_HEIGHT, z), isEdge ? roof : wall, 2);
            }
        }

        // The centerpiece: a dormant Clay Nexus waiting for a commander.
        level.setBlock(origin.offset(SIZE / 2, 0, SIZE / 2),
            BlockRegistry.CLAY_NEXUS.defaultBlockState(), 2);

        return true;
    }
}
