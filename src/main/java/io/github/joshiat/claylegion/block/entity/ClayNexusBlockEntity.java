package io.github.joshiat.claylegion.block.entity;

import io.github.joshiat.claylegion.config.ClayLegionConfig;
import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.NexusSummonIndex;
import io.github.joshiat.claylegion.entity.TargetingProfiler;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class ClayNexusBlockEntity extends BlockEntity {

    private static final int DEFAULT_SPAWN_DELAY = 40;
    private static final int MAX_DELAY = 24000;
    private static final int SPAWN_STEP_INTERVAL = 2;

    private UUID nexusId = UUID.randomUUID();
    private int templateTeamId;
    private boolean templateBrick;
    // The nexus stays dormant until a soldier doll is inserted (issue #37).
    private boolean hasTemplate;
    private int maxSpawnLimit = ClayLegionConfig.getNexusMaxSpawnLimit();
    private int spawnDelay = DEFAULT_SPAWN_DELAY;
    private int spawnCount = ClayLegionConfig.getNexusMaxSpawnCount();
    private int spawnCooldown;
    private int pendingSpawnCount;
    private int activeSummonCountHint;

    public ClayNexusBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CLAY_NEXUS, pos, blockState);
        clampSettings();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ClayNexusBlockEntity nexus) {
        if (level instanceof ServerLevel serverLevel) {
            nexus.serverTick(serverLevel);
        }
    }

    public int getActiveSummonCountHint() {
        return activeSummonCountHint;
    }

    public void adjustMaxSpawnLimit(int delta) {
        clampSettings();
        maxSpawnLimit = Mth.clamp(maxSpawnLimit + delta, 0, ClayLegionConfig.getNexusMaxSpawnLimit());
        setChanged();
    }

    public void adjustSpawnDelay(int delta) {
        spawnDelay = Mth.clamp(spawnDelay + delta, 0, MAX_DELAY);
        setChanged();
    }

    public void adjustSpawnCount(int delta) {
        clampSettings();
        spawnCount = Mth.clamp(spawnCount + delta, 0, ClayLegionConfig.getNexusMaxSpawnCount());
        setChanged();
    }

    public void setTemplateFromDoll(ItemStack stack) {
        if (!(stack.getItem() instanceof SoldierDollItem dollItem)) {
            return;
        }

        templateTeamId = dollItem.getTeamId(stack);
        templateBrick = dollItem == io.github.joshiat.claylegion.registry.ItemRegistry.BRICK_SOLDIER_DOLL;
        hasTemplate = true;
        setChanged();
        syncToClients();
    }

    /** True once a doll has been inserted; spawning is disabled until then. */
    public boolean hasTemplate() {
        return hasTemplate;
    }

    public int getTemplateTeamId() {
        return templateTeamId;
    }

    public boolean isTemplateBrick() {
        return templateBrick;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String describeSettings() {
        return (hasTemplate ? "team=" + templateTeamId + (templateBrick ? " brick" : "") : "dormant (insert a doll)")
            + ", limit=" + maxSpawnLimit
            + ", delay=" + spawnDelay
            + ", count=" + spawnCount
            + ", cooldown=" + spawnCooldown
            + ", pending=" + pendingSpawnCount;
    }

    /**
     * Linked summons die with their nexus, regardless of how the block was
     * removed — mining, explosions, pistons, or /setblock (issue #33).
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            removeLinkedSummons(serverLevel);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String persistedId = input.getString("NexusId").orElse(null);
        if (persistedId != null && !persistedId.isBlank()) {
            try {
                nexusId = UUID.fromString(persistedId);
            } catch (IllegalArgumentException ignored) {
                nexusId = UUID.randomUUID();
            }
        }

        templateTeamId = input.getInt("TemplateTeamId").orElse(0);
        templateBrick = input.getBooleanOr("TemplateBrick", false);
        hasTemplate = input.getBooleanOr("HasTemplate", false);
        maxSpawnLimit = input.getInt("MaxSpawnLimit").orElse(ClayLegionConfig.getNexusMaxSpawnLimit());
        spawnDelay = input.getInt("SpawnDelay").orElse(DEFAULT_SPAWN_DELAY);
        spawnCount = input.getInt("SpawnCount").orElse(ClayLegionConfig.getNexusMaxSpawnCount());
        spawnCooldown = input.getInt("SpawnCooldown").orElse(0);
        pendingSpawnCount = input.getInt("PendingSpawnCount").orElse(0);
        clampSettings();
    }

    /** Template data reaches clients so the renderer can display the spawn (issue #31). */
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("HasTemplate", hasTemplate);
        tag.putInt("TemplateTeamId", templateTeamId);
        tag.putBoolean("TemplateBrick", templateBrick);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("NexusId", nexusId.toString());
        output.putInt("TemplateTeamId", templateTeamId);
        output.putBoolean("TemplateBrick", templateBrick);
        output.putBoolean("HasTemplate", hasTemplate);
        output.putInt("MaxSpawnLimit", maxSpawnLimit);
        output.putInt("SpawnDelay", spawnDelay);
        output.putInt("SpawnCount", spawnCount);
        output.putInt("SpawnCooldown", spawnCooldown);
        output.putInt("PendingSpawnCount", pendingSpawnCount);
    }

    private void serverTick(ServerLevel serverLevel) {
        clampSettings();

        boolean profiling = TargetingProfiler.isEnabled();
        long nexusStart = profiling ? System.nanoTime() : 0L;
        long gameTime = serverLevel.getGameTime();

        if (spawnCooldown > 0) {
            spawnCooldown--;
        }

        // Dormant until a soldier doll is inserted (issue #37).
        if (!hasTemplate) {
            return;
        }

        if (pendingSpawnCount <= 0 && spawnCount > 0 && maxSpawnLimit > 0 && spawnCooldown <= 0) {
            long countStart = profiling ? System.nanoTime() : 0L;
            int activeSummons = countActiveSummons(serverLevel);
            if (profiling) {
                TargetingProfiler.recordCombatSample("nexusSummonCountTime", "nexusSummonCountCalls", System.nanoTime() - countStart, gameTime);
            }
            activeSummonCountHint = activeSummons;
            int available = Math.max(0, maxSpawnLimit - activeSummons);
            pendingSpawnCount = Math.min(spawnCount, available);
            spawnCooldown = spawnDelay;
            if (pendingSpawnCount > 0) {
                setChanged();
            }
        }

        if (pendingSpawnCount > 0 && (serverLevel.getGameTime() % SPAWN_STEP_INTERVAL) == 0L) {
            if (trySpawnOne(serverLevel)) {
                pendingSpawnCount--;
                activeSummonCountHint++;
                setChanged();
            }
        }

        if (profiling) {
            TargetingProfiler.recordCombatSample("nexusTickTime", "nexusTicks", System.nanoTime() - nexusStart, gameTime);
        }
    }

    private boolean trySpawnOne(ServerLevel serverLevel) {
        ClaySoldierEntity soldier = io.github.joshiat.claylegion.registry.EntityRegistry.CLAY_SOLDIER.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (soldier == null) {
            return false;
        }

        BlockPos.MutableBlockPos mutablePos = worldPosition.mutable();
        for (int attempt = 0; attempt < 6; attempt++) {
            double offsetX = (serverLevel.getRandom().nextDouble() - 0.5D) * 1.4D;
            double offsetZ = (serverLevel.getRandom().nextDouble() - 0.5D) * 1.4D;
            mutablePos.set(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ());
            soldier.setPos(mutablePos.getX() + 0.5D + offsetX, mutablePos.getY(), mutablePos.getZ() + 0.5D + offsetZ);
            if (!serverLevel.noCollision(soldier, soldier.getBoundingBox())) {
                continue;
            }

            soldier.setTeamId(templateTeamId);
            soldier.setBrickSoldier(templateBrick);
            soldier.setNexusSummon(true);
            soldier.setNexusOriginId(nexusId);
            soldier.setYRot(serverLevel.getRandom().nextFloat() * 360.0f);
            soldier.setXRot(0.0f);
            boolean spawned = serverLevel.addFreshEntity(soldier);
            if (spawned) {
                // Register immediately so same-tick nexus removal can still clean up this soldier.
                NexusSummonIndex.get(serverLevel).register(soldier);
            }
            return spawned;
        }

        return false;
    }

    private int countActiveSummons(ServerLevel serverLevel) {
        return NexusSummonIndex.get(serverLevel).countActive(nexusId);
    }

    public void removeLinkedSummons(ServerLevel serverLevel) {
        int removed = NexusSummonIndex.get(serverLevel).removeAllForNexus(serverLevel, nexusId);
        if (removed > 0) {
            activeSummonCountHint = 0;
            pendingSpawnCount = 0;
            setChanged();
        }
    }

    private void clampSettings() {
        maxSpawnLimit = Mth.clamp(maxSpawnLimit, 0, ClayLegionConfig.getNexusMaxSpawnLimit());
        spawnDelay = Mth.clamp(spawnDelay, 0, MAX_DELAY);
        spawnCount = Mth.clamp(spawnCount, 0, ClayLegionConfig.getNexusMaxSpawnCount());
        pendingSpawnCount = Math.max(0, pendingSpawnCount);
        activeSummonCountHint = Math.max(0, activeSummonCountHint);
    }
}