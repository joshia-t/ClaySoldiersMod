package io.github.joshiat.claylegion.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Lightweight debug spawner item for Phase 2 testing.
 *
 * This intentionally behaves like a spawn egg substitute without using vanilla
 * SpawnEggItem so we can keep custom entity wiring simple during migration.
 */
public class EntitySpawnerItem extends Item {

    private final EntityType<? extends Entity> spawnType;

    public EntitySpawnerItem(Properties properties, EntityType<? extends Entity> spawnType) {
        super(properties);
        this.spawnType = spawnType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = pos.relative(face);

        Entity entity = spawnType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        entity.setYRot(level.getRandom().nextFloat() * 360.0f);
        entity.setXRot(0.0f);

        level.addFreshEntity(entity);

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
