package io.github.joshiat.claylegion.item;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Soldier doll item — right-clicking a surface spawns a ClaySoldierEntity.
 * Team ID defaults to 0 (White) for Phase 1; Phase 4 adds per-item team via DataComponents.
 */
public class SoldierDollItem extends Item {

    private static final String TEAM_TAG = "team";

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

        // Read team from custom_data: /give @s clay-legion:soldier_doll[minecraft:custom_data={team:5}]
        ItemStack stack = context.getItemInHand();
        int teamId = getTeamIdFromStack(stack);
        soldier.setTeamId(teamId);
        soldier.setBrickSoldier(stack.getItem() == io.github.joshiat.claylegion.registry.ItemRegistry.BRICK_SOLDIER_DOLL);

        soldier.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        soldier.setYRot(level.getRandom().nextFloat() * 360f);
        soldier.setXRot(0f);

        level.addFreshEntity(soldier);
        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    /** Convenience factory — Phase 4 will encode team via DataComponent. */
    public static ItemStack forTeam(int teamId) {
        ItemStack stack = new ItemStack(io.github.joshiat.claylegion.registry.ItemRegistry.SOLDIER_DOLL);
        setTeamIdOnStack(stack, teamId);
        return stack;
    }

    public static int getTeamIdFromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        CompoundTag tag = customData.copyTag();
        return tag.contains(TEAM_TAG) ? tag.getInt(TEAM_TAG).orElse(0) : 0;
    }

    public static void setTeamIdOnStack(ItemStack stack, int teamId) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TEAM_TAG, teamId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
