package io.github.joshiat.claylegion.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Clay Lexicon — an in-game wiki covering soldiers, teams, upgrades, and
 * mounts (issue #23).
 *
 * <p>The item itself is inert on the server; the client initializer registers a
 * use callback that opens the LexiconScreen, keeping all GUI code client-side.
 */
public class LexiconItem extends Item {

    public LexiconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Success on both sides: the client-side use callback opens the screen.
        return InteractionResult.SUCCESS;
    }
}
