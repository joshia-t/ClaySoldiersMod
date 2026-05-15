package io.github.joshiat.claylegion.item;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Soldier doll item — right-clicking a surface spawns a ClaySoldierEntity.
 * Team ID defaults to 0 (White) for Phase 1; Phase 4 adds per-item team via DataComponents.
 */
public class SoldierDollItem extends Item {

    public SoldierDollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = pos.relative(face);

        ClaySoldierEntity soldier = EntityRegistry.CLAY_SOLDIER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (soldier == null) return InteractionResult.FAIL;

        // Phase 1: always spawn as team 0 (White). Phase 4 adds DataComponent team selection.
        soldier.setTeamId(0);

        soldier.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        soldier.setYRot(level.getRandom().nextFloat() * 360f);
        soldier.setXRot(0f);

        level.addFreshEntity(soldier);

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /** Convenience factory — Phase 4 will encode team via DataComponent. */
    public static ItemStack forTeam(int teamId) {
        return new ItemStack(io.github.joshiat.claylegion.registry.ItemRegistry.SOLDIER_DOLL);
    }
}
