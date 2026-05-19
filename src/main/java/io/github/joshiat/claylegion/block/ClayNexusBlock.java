package io.github.joshiat.claylegion.block;

import com.mojang.serialization.MapCodec;
import io.github.joshiat.claylegion.block.entity.ClayNexusBlockEntity;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClayNexusBlock extends BaseEntityBlock implements EntityBlock {

    public static final MapCodec<ClayNexusBlock> CODEC = simpleCodec(ClayNexusBlock::new);

    public ClayNexusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClayNexusBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ClayNexusBlockEntity nexus)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            int delta = player.isShiftKeyDown() ? -1 : 1;
            nexus.adjustMaxSpawnLimit(delta);
            sendFeedback(player, nexus, "Max spawn limit");
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ClayNexusBlockEntity nexus)) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof SoldierDollItem) {
            if (!level.isClientSide()) {
                nexus.setTemplateFromDoll(stack);
                sendFeedback(player, nexus, "Template");
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.REDSTONE)) {
            if (!level.isClientSide()) {
                int delta = player.isShiftKeyDown() ? -20 : 20;
                nexus.adjustSpawnDelay(delta);
                sendFeedback(player, nexus, "Spawn delay");
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.CLAY_BALL)) {
            if (!level.isClientSide()) {
                int delta = player.isShiftKeyDown() ? -1 : 1;
                nexus.adjustSpawnCount(delta);
                sendFeedback(player, nexus, "Spawn count");
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
            ? null
            : createTickerHelper(type, BlockEntityRegistry.CLAY_NEXUS, ClayNexusBlockEntity::serverTick);
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, net.minecraft.core.Direction direction) {
        if (blockGetter.getBlockEntity(blockPos) instanceof ClayNexusBlockEntity nexus) {
            return Mth.clamp(nexus.getActiveSummonCountHint(), 0, 15);
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    private static void sendFeedback(Player player, ClayNexusBlockEntity nexus, String changedField) {
        player.sendSystemMessage(Component.literal(changedField + ": " + nexus.describeSettings()));
    }
}