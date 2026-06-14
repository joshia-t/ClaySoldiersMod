package io.github.joshiat.claylegion.gametest;

import com.mojang.authlib.GameProfile;
import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.drop.DropStackMetadata;
import io.github.joshiat.claylegion.entity.possession.SoldierPossessionManager;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeSpec;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * In-world integration tests executed by the Fabric/Mojang GameTest framework.
 * Run headlessly via {@code gradlew runGametest}.
 */
public final class ClayLegionGameTests {

    private static final String ARENA = "clay-legion:arena";
    private static final BlockPos SPAWN = new BlockPos(1, 1, 1);

    // ── Upgrade system ─────────────────────────────────────────────────────

    @GameTest(structure = ARENA)
    public void arrowPickupGrantsStickAndFlint(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);

        Vec3 pos = soldier.position();
        ItemEntity arrowDrop = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z,
            new ItemStack(Items.ARROW, 1));
        arrowDrop.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(arrowDrop);

        // Pickup scans run every 10 ticks; wait until both upgrades land.
        helper.succeedWhen(() -> {
            if (!soldier.hasUpgrade(UpgradeFlags.STICK | UpgradeFlags.FLINT)) {
                helper.fail("Arrow shortcut did not grant Stick+Flint upgrades");
            }
        });
    }

    @GameTest(structure = ARENA)
    public void incompatibleAndPrerequisiteRulesHold(GameTestHelper helper) {
        UpgradeSpec rabbitHide = UpgradeRegistry.getSpec(UpgradeFlags.RABBIT_HIDE);
        if (rabbitHide.canEquipOnto(UpgradeFlags.LEATHER)) {
            helper.fail("Rabbit hide must be incompatible with leather");
        }

        UpgradeSpec goldIngot = UpgradeRegistry.getSpec(UpgradeFlags.GOLD_INGOT);
        if (goldIngot.canEquipOnto(0L)) {
            helper.fail("Gold ingot must require the gold nugget king upgrade");
        }
        if (!goldIngot.canEquipOnto(UpgradeFlags.GOLD_NUGGET)) {
            helper.fail("Gold ingot should equip once the king upgrade is present");
        }

        UpgradeSpec wool = UpgradeRegistry.getSpec(UpgradeFlags.WOOL);
        if (wool.canEquipOnto(0L)) {
            helper.fail("Wool must require leather or rabbit hide");
        }
        if (!wool.canEquipOnto(UpgradeFlags.RABBIT_HIDE)) {
            helper.fail("Wool should equip on top of rabbit hide");
        }

        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        if (!soldier.forceEquipUpgrade(UpgradeFlags.STICK)) {
            helper.fail("Fresh soldier should accept a stick");
        }
        if (soldier.forceEquipUpgrade(UpgradeFlags.BONE)) {
            helper.fail("Main-hand slot must be exclusive: bone equipped over stick");
        }

        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void fireDamageStartsCombustion(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);

        soldier.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lava(), 1.0f);

        if (!soldier.isCombusting()) {
            helper.fail("Expected fire damage to start combustion");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void cactusGrantsFireImmunity(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.forceEquipUpgrade(UpgradeFlags.CACTUS);

        float before = soldier.getSoldierHealth();
        soldier.hurtServer(helper.getLevel(), helper.getLevel().damageSources().lava(), 4.0f);

        if (soldier.isCombusting()) {
            helper.fail("Cactus soldier should not combust");
        }
        if (soldier.getSoldierHealth() < before) {
            helper.fail("Cactus soldier should take no fire damage");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void stringGrantsExplosionImmunity(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.forceEquipUpgrade(UpgradeFlags.STRING);

        float before = soldier.getSoldierHealth();
        soldier.hurtServer(helper.getLevel(),
            helper.getLevel().damageSources().explosion(null, null), 10.0f);

        if (soldier.getSoldierHealth() < before) {
            helper.fail("String soldier should take no explosion damage");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void diamondBoostsMaxHealthAndHeals(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.setSoldierHealth(5.0f);

        soldier.forceEquipUpgrade(UpgradeFlags.DIAMOND);

        if (soldier.getSoldierMaxHealth() != ClaySoldierEntity.MAX_HEALTH + 10.0f) {
            helper.fail("Diamond should add +10 max health, got " + soldier.getSoldierMaxHealth());
        }
        if (soldier.getSoldierHealth() != soldier.getSoldierMaxHealth()) {
            helper.fail("Diamond should fully heal on equip");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void enemySoldiersEngageInCombat(GameTestHelper helper) {
        ClaySoldierEntity red = spawnSoldier(helper, 1);
        ClaySoldierEntity blue = spawnSoldier(helper, 2);
        blue.setPos(red.getX() + 0.6, red.getY(), red.getZ());

        helper.succeedWhen(() -> {
            boolean fightStarted =
                red.getSoldierHealth() < ClaySoldierEntity.MAX_HEALTH
                || blue.getSoldierHealth() < ClaySoldierEntity.MAX_HEALTH
                || red.isSoldierDead() || blue.isSoldierDead()
                || red.isRemoved() || blue.isRemoved();
            if (!fightStarted) {
                helper.fail("Opposing soldiers did not engage");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void wheatPacifistsKeepThePeace(GameTestHelper helper) {
        ClaySoldierEntity red = spawnSoldier(helper, 1);
        ClaySoldierEntity blue = spawnSoldier(helper, 2);
        blue.setPos(red.getX() + 0.6, red.getY(), red.getZ());
        red.forceEquipUpgrade(UpgradeFlags.WHEAT);
        blue.forceEquipUpgrade(UpgradeFlags.WHEAT);

        helper.runAfterDelay(60, () -> {
            if (red.getSoldierHealth() < ClaySoldierEntity.MAX_HEALTH
                || blue.getSoldierHealth() < ClaySoldierEntity.MAX_HEALTH) {
                helper.fail("Wheat pacifists attacked each other");
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA)
    public void pickBlockReturnsTeamDoll(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 5);

        ItemStack picked = soldier.getPickResult();
        if (picked == null || !(picked.getItem() instanceof SoldierDollItem doll)) {
            helper.fail("Pick block should return a soldier doll");
            return;
        }
        if (doll.getTeamId(picked) != 5) {
            helper.fail("Picked doll must preserve team id, got " + doll.getTeamId(picked));
        }

        ClaySoldierEntity brick = spawnSoldier(helper, 0);
        brick.setBrickSoldier(true);
        ItemStack pickedBrick = brick.getPickResult();
        if (pickedBrick == null || !pickedBrick.is(ItemRegistry.BRICK_SOLDIER_DOLL)) {
            helper.fail("Brick soldier pick block should return the brick doll");
        }

        helper.succeed();
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void dollResurrectionBudgetIsFiniteAndDecrements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 center = helper.absoluteVec(Vec3.atCenterOf(SPAWN));

        // A spent doll (0 revivals left) must never be revived.
        ClaySoldierEntity medic = spawnSoldier(helper, 3);
        medic.forceEquipUpgrade(UpgradeFlags.CLAY_BALL);

        ItemStack spentDoll = new ItemStack(ItemRegistry.SOLDIER_DOLL);
        SoldierDollItem.setTeamIdOnStack(spentDoll, 3);
        DropStackMetadata.setSoldierUses(spentDoll, 0);
        ItemEntity spentDrop = new ItemEntity(level, center.x, center.y, center.z, spentDoll);
        spentDrop.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(spentDrop);

        // A doll with one revival left revives once, and the revived soldier
        // carries a budget of zero into its next death.
        ItemStack lastUseDoll = new ItemStack(ItemRegistry.SOLDIER_DOLL);
        SoldierDollItem.setTeamIdOnStack(lastUseDoll, 3);
        DropStackMetadata.setSoldierUses(lastUseDoll, 1);
        ItemEntity lastUseDrop = new ItemEntity(level, center.x, center.y, center.z, lastUseDoll);
        lastUseDrop.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(lastUseDrop);

        helper.runAfterDelay(60, () -> {
            List<ClaySoldierEntity> revived = level.getEntitiesOfClass(
                ClaySoldierEntity.class,
                medic.getBoundingBox().inflate(4.0),
                s -> s != medic && s.getTeamId() == 3
            );
            if (revived.size() != 1) {
                helper.fail("Expected exactly one revival from the 1-use doll, got " + revived.size());
                return;
            }
            if (revived.get(0).getResurrectionUsesRemaining() != 0) {
                helper.fail("Revived soldier should carry a depleted resurrection budget, got "
                    + revived.get(0).getResurrectionUsesRemaining());
                return;
            }
            if (!spentDrop.isAlive()) {
                helper.fail("Spent doll must not be consumed");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA)
    public void deathDropsDollAndUpgradesWithDurability(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ClaySoldierEntity soldier = spawnSoldier(helper, 4);
        soldier.forceEquipUpgrade(UpgradeFlags.STICK);

        soldier.applySoldierDamage(1000.0f, (byte) -1);

        List<ItemEntity> drops = level.getEntitiesOfClass(
            ItemEntity.class,
            helper.getBounds().inflate(2.0),
            ItemEntity::isAlive
        );

        ItemEntity dollDrop = null;
        ItemEntity stickDrop = null;
        for (ItemEntity drop : drops) {
            if (drop.getItem().getItem() instanceof SoldierDollItem) {
                dollDrop = drop;
            } else if (drop.getItem().is(Items.STICK)) {
                stickDrop = drop;
            }
        }

        if (dollDrop == null) {
            helper.fail("Dead soldier must drop its doll");
            return;
        }
        if (stickDrop == null) {
            helper.fail("Dead soldier must drop its stick upgrade (issue #2)");
            return;
        }
        if (DropStackMetadata.getUpgradeFlagOrZero(stickDrop.getItem()) != UpgradeFlags.STICK) {
            helper.fail("Dropped stick should carry its upgrade flag metadata");
            return;
        }
        if (DropStackMetadata.getUpgradeUsesOrDefault(stickDrop.getItem(), 0) != 20) {
            helper.fail("Dropped stick should keep its remaining durability, got "
                + DropStackMetadata.getUpgradeUsesOrDefault(stickDrop.getItem(), 0));
            return;
        }
        helper.succeed();
    }

    // ── Incompatibility rules (issue #19) ──────────────────────────────────

    @GameTest(structure = ARENA)
    public void incompatibilitiesAreSymmetric(GameTestHelper helper) {
        // Both equip orders must be rejected for every banned pair.
        long[][] bannedPairs = {
            {UpgradeFlags.LEATHER, UpgradeFlags.RABBIT_HIDE},
            {UpgradeFlags.DIAMOND, UpgradeFlags.DIAMOND_BLOCK},
            {UpgradeFlags.SUGAR, UpgradeFlags.DIAMOND},
            {UpgradeFlags.FEATHER, UpgradeFlags.IRON_INGOT},
            {UpgradeFlags.GUNPOWDER, UpgradeFlags.MAGMA_CREAM},
            {UpgradeFlags.ENDER_PEARL, UpgradeFlags.WHEAT_SEEDS},
        };

        for (long[] pair : bannedPairs) {
            for (int order = 0; order < 2; order++) {
                long first = pair[order];
                long second = pair[1 - order];

                ClaySoldierEntity soldier = spawnSoldier(helper, 0);
                if (!soldier.forceEquipUpgrade(first)) {
                    helper.fail("Fresh soldier should accept " + UpgradeFlags.nameOf(first));
                    return;
                }
                if (soldier.forceEquipUpgrade(second)) {
                    helper.fail(UpgradeFlags.nameOf(second) + " must be rejected while holding "
                        + UpgradeFlags.nameOf(first));
                    return;
                }
                soldier.discard();
            }
        }
        helper.succeed();
    }

    // ── Clay Nexus (issues #37, #33) ───────────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 130)
    public void nexusStaysDormantUntilDollInserted(GameTestHelper helper) {
        // Floor slot: the harness seals the structure with barriers, so the
        // spawn space above the nexus must stay inside the arena airspace.
        BlockPos nexusPos = new BlockPos(1, 0, 1);
        helper.setBlock(nexusPos, io.github.joshiat.claylegion.registry.BlockRegistry.CLAY_NEXUS);

        helper.runAfterDelay(100, () -> {
            List<ClaySoldierEntity> spawned = helper.getLevel().getEntitiesOfClass(
                ClaySoldierEntity.class, helper.getBounds().inflate(3.0), e -> true);
            if (!spawned.isEmpty()) {
                helper.fail("Dormant nexus must not spawn soldiers, found " + spawned.size());
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 200)
    public void nexusSpawnsAfterDollInsertedAndKillsSummonsOnRemoval(GameTestHelper helper) {
        BlockPos nexusPos = new BlockPos(1, 0, 1);
        helper.setBlock(nexusPos, io.github.joshiat.claylegion.registry.BlockRegistry.CLAY_NEXUS);

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(nexusPos))
            instanceof io.github.joshiat.claylegion.block.entity.ClayNexusBlockEntity nexus)) {
            helper.fail("Clay nexus block entity missing");
            return;
        }
        nexus.setTemplateFromDoll(new ItemStack(ItemRegistry.SOLDIER_DOLL));

        helper.succeedWhen(() -> {
            List<ClaySoldierEntity> summons = helper.getLevel().getEntitiesOfClass(
                ClaySoldierEntity.class, helper.getBounds().inflate(3.0), ClaySoldierEntity::isNexusSummon);
            if (summons.isEmpty()) {
                helper.fail("Nexus with template should spawn soldiers");
                return;
            }

            // Removing the block must take its summons with it (issue #33).
            helper.setBlock(nexusPos, net.minecraft.world.level.block.Blocks.AIR);
            List<ClaySoldierEntity> survivors = helper.getLevel().getEntitiesOfClass(
                ClaySoldierEntity.class, helper.getBounds().inflate(3.0),
                s -> s.isNexusSummon() && !s.isRemoved());
            if (!survivors.isEmpty()) {
                helper.fail("Destroying the nexus must remove its summons, " + survivors.size() + " survived");
            }
        });
    }

    @GameTest(structure = ARENA)
    public void clayHutFeatureBuildsWithDormantNexus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The arena floor (3x3) is too small for the 5x5 hut footprint, so
        // lay a solid 7x7 pad just outside and build there.
        BlockPos pad = helper.absolutePos(new BlockPos(1, 0, 1)).offset(8, 0, 8);
        for (int x = -1; x < 6; x++) {
            for (int z = -1; z < 6; z++) {
                level.setBlock(pad.offset(x, -1, z),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
        }

        boolean placed = io.github.joshiat.claylegion.registry.FeatureRegistry.CLAY_HUT.place(
            new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                java.util.Optional.empty(),
                level,
                level.getChunkSource().getGenerator(),
                level.getRandom(),
                pad,
                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));

        if (!placed) {
            helper.fail("Clay hut feature refused to place on solid ground");
            return;
        }
        if (!level.getBlockState(pad.offset(2, 0, 2))
            .is(io.github.joshiat.claylegion.registry.BlockRegistry.CLAY_NEXUS)) {
            helper.fail("Clay hut should contain the nexus at its center");
            return;
        }
        if (!level.getBlockState(pad.offset(0, 0, 0)).is(net.minecraft.world.level.block.Blocks.TERRACOTTA)) {
            helper.fail("Clay hut walls missing");
            return;
        }
        if (!level.getBlockState(pad.offset(2, 0, 4)).isAir()
            || !level.getBlockState(pad.offset(2, 1, 4)).isAir()) {
            helper.fail("Clay hut doorway missing");
            return;
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void soldiersBlockBlockPlacement(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        BlockPos occupied = soldier.blockPosition();

        boolean unobstructed = helper.getLevel().isUnobstructed(
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
            occupied,
            net.minecraft.world.phys.shapes.CollisionContext.empty());
        if (unobstructed) {
            helper.fail("Block placement should be obstructed by a clay soldier (issue #34)");
            return;
        }

        soldier.discard();
        boolean clearAfter = helper.getLevel().isUnobstructed(
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
            occupied,
            net.minecraft.world.phys.shapes.CollisionContext.empty());
        if (!clearAfter) {
            helper.fail("Placement should be allowed again once the soldier is gone");
            return;
        }
        helper.succeed();
    }

    // ── Swarm AI: exploration + contact intel ──────────────────────────────

    private static final String ARENA_LARGE = "clay-legion:arena_large";

    /**
     * A soldier sealed in a two-cell pocket with a rumor beyond the wall:
     * exploration must enter, exhaust the pocket, and stamp both cells as
     * BLOCKED with a flow vector — visible to its team only.
     */
    @GameTest(structure = ARENA_LARGE, maxTicks = 400, padding = 12)
    public void explorationSealsDeadEndPocket(GameTestHelper helper) {
        // Sealed 1x2 pocket around cells (4,4)-(4,5).
        placeWall(helper, 4, 3);
        placeWall(helper, 3, 4);
        placeWall(helper, 5, 4);
        placeWall(helper, 3, 5);
        placeWall(helper, 5, 5);
        placeWall(helper, 4, 6);

        ClaySoldierEntity soldier = spawnSoldierAt(helper, 40, 4.5, 4.5);
        Vec3 goal = helper.absoluteVec(new Vec3(8.5, 1.0, 4.5));

        var nav = io.github.joshiat.claylegion.entity.nav.TeamNavIndex
            .get(helper.getLevel()).forTeam(40);
        var enemyNav = io.github.joshiat.claylegion.entity.nav.TeamNavIndex
            .get(helper.getLevel()).forTeam(41);
        BlockPos deepCell = helper.absolutePos(new BlockPos(4, 1, 5));
        long deepKey = io.github.joshiat.claylegion.entity.nav.TeamNavMemory.pack(
            deepCell.getX(), deepCell.getY(), deepCell.getZ());

        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            // Ongoing-battle rumor keeps the soldier motivated.
            soldier.noteEnemySighting(null, goal.x, goal.y, goal.z, 1.0f, now, false);

            if (!nav.isBlocked(deepKey, now)) {
                helper.fail("Pocket cell should be marked blocked by the swarm map");
                return;
            }
            if (nav.getFlow(deepKey, now)
                == io.github.joshiat.claylegion.entity.nav.TeamNavMemory.FLOW_NONE) {
                helper.fail("Blocked pocket cells should carry an outbound flow vector");
                return;
            }
            if (enemyNav.isBlocked(deepKey, now) || enemyNav.isVisited(deepKey, now)) {
                helper.fail("Nav intel must stay team-private");
            }
        });
    }

    /**
     * Corridor maze with the exit on the opposite side from the rumor:
     * pure goal-chasing pins the soldier in a corner forever; exploration
     * mode must map the corridor and get it out the far side.
     */
    @GameTest(structure = ARENA_LARGE, maxTicks = 900, padding = 12)
    public void explorationEscapesCorridorMaze(GameTestHelper helper) {
        for (int z = 0; z <= 6; z++) {
            placeWall(helper, 2, z);
            placeWall(helper, 4, z);
        }

        ClaySoldierEntity soldier = spawnSoldierAt(helper, 43, 3.5, 4.5);
        Vec3 goal = helper.absoluteVec(new Vec3(7.5, 1.0, 0.5));

        var nav = io.github.joshiat.claylegion.entity.nav.TeamNavIndex
            .get(helper.getLevel()).forTeam(43);
        BlockPos corridorCell = helper.absolutePos(new BlockPos(3, 1, 2));
        long corridorKey = io.github.joshiat.claylegion.entity.nav.TeamNavMemory.pack(
            corridorCell.getX(), corridorCell.getY(), corridorCell.getZ());

        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            soldier.noteEnemySighting(null, goal.x, goal.y, goal.z, 1.0f, now, false);

            if (!nav.isVisited(corridorKey, now) && !nav.isBlocked(corridorKey, now)) {
                helper.fail("Exploration should have mapped the corridor");
                return;
            }
            double soldierRelX = soldier.getX() - helper.absoluteVec(Vec3.ZERO).x;
            if (soldierRelX < 4.5) {
                helper.fail("Soldier has not escaped the corridor yet, relX=" + soldierRelX);
            }
        });
    }

    @GameTest(structure = ARENA_LARGE, maxTicks = 200, padding = 12)
    public void contactIntelSpreadsBetweenAllies(GameTestHelper helper) {
        ClaySoldierEntity runner = spawnSoldierAt(helper, 42, 3.5, 4.5);
        ClaySoldierEntity listener = spawnSoldierAt(helper, 42, 5.5, 4.5);
        listener.applyRoot();

        // The runner heard a rumor; the listener hasn't.
        Vec3 rumor = helper.absoluteVec(new Vec3(8.5, 1.0, 8.5));
        long gameTime = helper.getLevel().getGameTime();
        runner.noteEnemySighting(null, rumor.x, rumor.y, rumor.z, 1.0f, gameTime, false);

        helper.succeedWhen(() -> {
            // Keep the runner's memory fresh so it has something to share.
            long now = helper.getLevel().getGameTime();
            runner.noteEnemySighting(null, rumor.x, rumor.y, rumor.z, 1.0f, now, false);

            if (listener.getTargetMemoryConfidence() <= 0.0f) {
                helper.fail("Contact with the runner should hand the rumor to the listener");
            }
        });
    }

    /**
     * After a soldier escapes the corridor, it must leave a breadcrumb trail —
     * forward flow vectors on VISITED cells — so the rest of the stuck group
     * can follow it out, and flag the team's route as found.
     */
    @GameTest(structure = ARENA_LARGE, maxTicks = 900, padding = 12)
    public void explorationLeavesBreadcrumbTrailForGroup(GameTestHelper helper) {
        for (int z = 0; z <= 6; z++) {
            placeWall(helper, 2, z);
            placeWall(helper, 4, z);
        }

        ClaySoldierEntity soldier = spawnSoldierAt(helper, 44, 3.5, 4.5);
        Vec3 goal = helper.absoluteVec(new Vec3(7.5, 1.0, 0.5));
        var nav = io.github.joshiat.claylegion.entity.nav.TeamNavIndex
            .get(helper.getLevel()).forTeam(44);

        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            soldier.noteEnemySighting(null, goal.x, goal.y, goal.z, 1.0f, now, false);

            if (!nav.routeFoundRecently(now)) {
                helper.fail("Escaping soldier should flag the team route as found");
                return;
            }
            // A VISITED (not blocked) cell carrying a flow vector can only come
            // from the forward exit-trail — dead-end flows sit on BLOCKED cells.
            boolean[] forwardBreadcrumb = {false};
            nav.forEachLiveCell(now, (key, state, flow) -> {
                if (state == io.github.joshiat.claylegion.entity.nav.TeamNavMemory.CellState.VISITED
                    && flow != io.github.joshiat.claylegion.entity.nav.TeamNavMemory.FLOW_NONE) {
                    forwardBreadcrumb[0] = true;
                }
            });
            if (!forwardBreadcrumb[0]) {
                helper.fail("Escape should leave a forward breadcrumb trail for the group");
            }
        });
    }

    /**
     * Naughty ignorers must not be permanently stuck by stale "blocked" intel.
     * The corridor route is pre-sealed in the team map (simulating a wall the
     * swarm sealed that has since been removed / mis-sealed); an ignorer with
     * no known route must still re-probe and escape.
     */
    @GameTest(structure = ARENA_LARGE, maxTicks = 900, padding = 12)
    public void ignorerReprobesStaleSealAndEscapes(GameTestHelper helper) {
        for (int z = 0; z <= 6; z++) {
            placeWall(helper, 2, z);
            placeWall(helper, 4, z);
        }

        ClaySoldierEntity soldier = spawnSoldierAt(helper, 45, 3.5, 4.5);
        soldier.forceMazeIgnorer(true);
        Vec3 goal = helper.absoluteVec(new Vec3(7.5, 1.0, 0.5));

        // Pre-seal the open corridor cells as BLOCKED — stale intel.
        var nav = io.github.joshiat.claylegion.entity.nav.TeamNavIndex
            .get(helper.getLevel()).forTeam(45);
        long t0 = helper.getLevel().getGameTime();
        for (int z = 4; z <= 8; z++) {
            BlockPos abs = helper.absolutePos(new BlockPos(3, 1, z));
            nav.markBlocked(io.github.joshiat.claylegion.entity.nav.TeamNavMemory.pack(
                abs.getX(), abs.getY(), abs.getZ()), t0);
        }

        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            soldier.noteEnemySighting(null, goal.x, goal.y, goal.z, 1.0f, now, false);

            double soldierRelX = soldier.getX() - helper.absoluteVec(Vec3.ZERO).x;
            if (soldierRelX < 4.5) {
                helper.fail("Ignorer should re-probe the stale seal and escape, relX=" + soldierRelX);
            }
        });
    }

    private static void placeWall(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.TERRACOTTA);
        helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.TERRACOTTA);
    }

    private static ClaySoldierEntity spawnSoldierAt(GameTestHelper helper, int teamId,
                                                    double relX, double relZ) {
        ServerLevel level = helper.getLevel();
        ClaySoldierEntity soldier = EntityRegistry.CLAY_SOLDIER.create(level, EntitySpawnReason.COMMAND);
        if (soldier == null) {
            throw new IllegalStateException("Failed to create Clay Soldier entity");
        }
        soldier.setTeamId(teamId);
        Vec3 pos = helper.absoluteVec(new Vec3(relX, 1.0, relZ));
        soldier.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(soldier);
        return soldier;
    }

    // ── Loot seeking (issue #40) ───────────────────────────────────────────

    // Loot/mount scans reach 8 blocks and gossip travels level-wide, so these
    // AI tests use generous plot padding and test-unique team ids to stay
    // isolated from neighboring test plots.

    @GameTest(structure = ARENA, maxTicks = 150, padding = 10)
    public void soldiersWalkToDistantUpgrades(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 30);

        // Well outside the 0.55-block passive pickup radius, but inside the
        // sealed arena (the harness walls the structure with barriers).
        ItemEntity stick = new ItemEntity(helper.getLevel(),
            soldier.getX() + 1.2, soldier.getY(), soldier.getZ(),
            new ItemStack(Items.STICK));
        stick.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(stick);

        helper.succeedWhen(() -> {
            if (!soldier.hasUpgrade(UpgradeFlags.STICK)) {
                helper.fail("Soldier should walk to and equip the distant stick");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 150, padding = 10)
    public void soldiersGearUpBeforePursuingDistantEnemies(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 31);

        ClaySoldierEntity enemy = spawnSoldier(helper, 32);
        enemy.setPos(soldier.getX() + 2.6, soldier.getY(), soldier.getZ() + 2.6);
        enemy.forceEquipUpgrade(UpgradeFlags.WHEAT);
        enemy.applyRoot();

        ItemEntity stick = new ItemEntity(helper.getLevel(),
            soldier.getX() - 1.2, soldier.getY(), soldier.getZ(),
            new ItemStack(Items.STICK));
        stick.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(stick);

        helper.succeedWhen(() -> {
            if (!soldier.hasUpgrade(UpgradeFlags.STICK)) {
                helper.fail("Soldier should grab the stick before chasing the distant enemy");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 200, padding = 10)
    public void soldiersMountUpEvenWithEnemiesAround(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 33);

        ClaySoldierEntity enemy = spawnSoldier(helper, 34);
        enemy.setPos(soldier.getX() + 2.6, soldier.getY(), soldier.getZ() + 2.6);
        enemy.forceEquipUpgrade(UpgradeFlags.WHEAT);
        enemy.applyRoot();

        io.github.joshiat.claylegion.entity.mount.BaseMountEntity mount =
            EntityRegistry.HORSE_MOUNT.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (mount == null) {
            helper.fail("Failed to create horse mount");
            return;
        }
        mount.setPos(soldier.getX() - 1.2, soldier.getY(), soldier.getZ());
        helper.getLevel().addFreshEntity(mount);

        helper.succeedWhen(() -> {
            if (!soldier.isPassenger()) {
                helper.fail("Soldier should board the nearby mount despite the distant enemy");
            }
        });
    }

    // ── Environmental damage (issue #38) ───────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 120)
    public void lavaDamagesSoldiers(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.applyRoot(); // keep it in the pool
        helper.setBlock(new BlockPos(1, 1, 1), net.minecraft.world.level.block.Blocks.LAVA);

        helper.succeedWhen(() -> {
            if (soldier.getSoldierHealth() >= ClaySoldierEntity.MAX_HEALTH && !soldier.isRemoved()) {
                helper.fail("Lava should damage soldiers");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 120)
    public void burningDamagesSoldiers(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.setRemainingFireTicks(100);

        helper.succeedWhen(() -> {
            if (soldier.getSoldierHealth() >= ClaySoldierEntity.MAX_HEALTH && !soldier.isRemoved()) {
                helper.fail("Being on fire should damage soldiers");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 60)
    public void lightningDamagesSoldiers(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);

        net.minecraft.world.entity.LightningBolt bolt =
            net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (bolt == null) {
            helper.fail("Failed to create lightning bolt");
            return;
        }
        bolt.setPos(soldier.position());
        soldier.thunderHit(helper.getLevel(), bolt);

        helper.succeedWhen(() -> {
            if (soldier.getSoldierHealth() >= ClaySoldierEntity.MAX_HEALTH && !soldier.isRemoved()) {
                helper.fail("Lightning should damage soldiers");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void arrowsDamageSoldiers(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.applyRoot();

        net.minecraft.world.entity.projectile.arrow.Arrow arrow =
            net.minecraft.world.entity.EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (arrow == null) {
            helper.fail("Failed to create arrow");
            return;
        }
        arrow.setPos(soldier.getX() - 1.5, soldier.getY() + 0.3, soldier.getZ());
        arrow.setDeltaMovement(1.2, 0.0, 0.0);
        helper.getLevel().addFreshEntity(arrow);

        helper.succeedWhen(() -> {
            if (soldier.getSoldierHealth() >= ClaySoldierEntity.MAX_HEALTH && !soldier.isRemoved()) {
                helper.fail("Arrows should damage soldiers");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void splashPotionsAffectSoldiers(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.applyRoot();

        ItemStack potionStack = new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION);
        potionStack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
            new net.minecraft.world.item.alchemy.PotionContents(
                net.minecraft.world.item.alchemy.Potions.HARMING));

        net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion potion =
            net.minecraft.world.entity.EntityType.SPLASH_POTION.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (potion == null) {
            helper.fail("Failed to create splash potion");
            return;
        }
        potion.setItem(potionStack);
        potion.setPos(soldier.getX(), soldier.getY() + 1.5, soldier.getZ());
        potion.setDeltaMovement(0.0, -0.4, 0.0);
        helper.getLevel().addFreshEntity(potion);

        helper.succeedWhen(() -> {
            if (soldier.getSoldierHealth() >= ClaySoldierEntity.MAX_HEALTH && !soldier.isRemoved()) {
                helper.fail("Harming splash potions should damage soldiers");
            }
        });
    }

    // ── Nexus orders (issue #32) ───────────────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 100, padding = 10)
    public void holdOrderStandsGround(GameTestHelper helper) {
        ClaySoldierEntity holder = spawnSoldier(helper, 35);
        holder.setNexusOrder(io.github.joshiat.claylegion.entity.NexusOrder.HOLD);

        ClaySoldierEntity bait = spawnSoldier(helper, 36);
        bait.setPos(holder.getX() + 2.5, holder.getY(), holder.getZ());
        bait.forceEquipUpgrade(UpgradeFlags.WHEAT);

        double startX = holder.getX();
        double startZ = holder.getZ();

        helper.runAfterDelay(50, () -> {
            double driftSq = (holder.getX() - startX) * (holder.getX() - startX)
                + (holder.getZ() - startZ) * (holder.getZ() - startZ);
            if (driftSq > 0.5 * 0.5) {
                helper.fail("Hold order should keep the soldier at its post, drifted "
                    + Math.sqrt(driftSq) + " blocks");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 120, padding = 10)
    public void guardOrderReturnsTowardHome(GameTestHelper helper) {
        ClaySoldierEntity guard = spawnSoldier(helper, 37);
        guard.setNexusOrder(io.github.joshiat.claylegion.entity.NexusOrder.GUARD);

        // Home is far outside the guard radius, due east. The arena wall stops
        // real progress, so assert directional intent: the guard presses east.
        BlockPos home = guard.blockPosition().offset(20, 0, 0);
        guard.setNexusHomePos(home);
        double startX = guard.getX();

        helper.succeedWhen(() -> {
            if (guard.getX() < startX + 0.25) {
                helper.fail("Guard should walk toward its home anchor, x moved "
                    + (guard.getX() - startX));
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 200)
    public void nexusOrdersPropagateToLiveSummons(GameTestHelper helper) {
        BlockPos nexusPos = new BlockPos(1, 0, 1);
        helper.setBlock(nexusPos, io.github.joshiat.claylegion.registry.BlockRegistry.CLAY_NEXUS);

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(nexusPos))
            instanceof io.github.joshiat.claylegion.block.entity.ClayNexusBlockEntity nexus)) {
            helper.fail("Clay nexus block entity missing");
            return;
        }
        nexus.setTemplateFromDoll(new ItemStack(ItemRegistry.SOLDIER_DOLL));

        helper.succeedWhen(() -> {
            List<ClaySoldierEntity> summons = helper.getLevel().getEntitiesOfClass(
                ClaySoldierEntity.class, helper.getBounds().inflate(3.0), ClaySoldierEntity::isNexusSummon);
            if (summons.isEmpty()) {
                helper.fail("Nexus should spawn soldiers first");
                return;
            }

            ClaySoldierEntity summon = summons.get(0);
            if (summon.getNexusOrder() != io.github.joshiat.claylegion.entity.NexusOrder.MARCH) {
                helper.fail("Summons should start under the default MARCH order");
                return;
            }

            // Cycling re-issues the new order to living summons immediately.
            if (nexus.cycleOrder() != io.github.joshiat.claylegion.entity.NexusOrder.GUARD) {
                helper.fail("Order should cycle MARCH -> GUARD");
                return;
            }
            if (summon.getNexusOrder() != io.github.joshiat.claylegion.entity.NexusOrder.GUARD) {
                helper.fail("Existing summons must receive the new order immediately");
            }
        });
    }

    // ── Combat status sync (issue #24) ─────────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 100)
    public void statusFlagsSyncToEntityData(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.applyPoison();
        soldier.applyRoot();

        // Flags propagate on the next status tick.
        helper.succeedWhen(() -> {
            if (!soldier.hasStatusFlag(ClaySoldierEntity.STATUS_POISONED)) {
                helper.fail("Poison should set the synced POISONED status bit");
            }
            if (!soldier.hasStatusFlag(ClaySoldierEntity.STATUS_ROOTED)) {
                helper.fail("Root should set the synced ROOTED status bit");
            }
            if (soldier.hasStatusFlag(ClaySoldierEntity.STATUS_COMBUSTING)) {
                helper.fail("COMBUSTING bit must not be set without a burn");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 160)
    public void statusFlagsClearWhenEffectsExpire(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        soldier.applyRoot(); // 60 ticks

        helper.succeedWhen(() -> {
            if (soldier.isRooted() || soldier.getStatusFlags() != 0) {
                helper.fail("Status byte should clear once effects expire, flags=" + soldier.getStatusFlags());
            }
        });
    }

    // ── Recipes (issue #21) ────────────────────────────────────────────────

    @GameTest(structure = ARENA)
    public void legacyRecipesAreLoaded(GameTestHelper helper) {
        var recipeManager = helper.getLevel().getServer().getRecipeManager();

        long modRecipes = recipeManager.getRecipes().stream()
            .filter(holder -> holder.id().identifier().getNamespace().equals("clay-legion"))
            .count();
        if (modRecipes < 69) {
            helper.fail("Expected at least 69 clay-legion recipes, found " + modRecipes);
            return;
        }

        String[] representative = {
            "soldier_dolls", "soldier_doll_team_red", "horse_doll_dirt", "pegasus_doll_cake",
            "turtle_doll_cobble", "bunny_doll_pink", "gecko_doll_oak", "clay_nexus", "lexicon"
        };
        for (String path : representative) {
            var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("clay-legion", path));
            if (recipeManager.byKey(key).isEmpty()) {
                helper.fail("Missing recipe clay-legion:" + path);
                return;
            }
        }
        helper.succeed();
    }

    // ── Status effects (issue #20) ─────────────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 160)
    public void poisonDamagesOverTimeAndExpires(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        float before = soldier.getSoldierHealth();
        soldier.applyPoison();

        if (!soldier.isPoisoned()) {
            helper.fail("applyPoison should start the poison effect");
            return;
        }

        helper.succeedWhen(() -> {
            boolean damaged = soldier.getSoldierHealth() < before;
            boolean expired = !soldier.isPoisoned();
            if (!damaged || !expired) {
                helper.fail("Poison should tick damage and then expire (health="
                    + soldier.getSoldierHealth() + ", poisoned=" + soldier.isPoisoned() + ")");
            }
            if (soldier.isSoldierDead()) {
                helper.fail("Poison must never be lethal");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void blindnessDropsTargetAndSuppressesAggro(GameTestHelper helper) {
        ClaySoldierEntity red = spawnSoldier(helper, 1);
        ClaySoldierEntity blue = spawnSoldier(helper, 2);
        blue.setPos(red.getX() + 0.9, red.getY(), red.getZ());

        // Let them acquire each other, then blind one and verify it disengages.
        helper.runAfterDelay(10, () -> {
            red.applyBlindness();
            if (!red.isBlinded()) {
                helper.fail("applyBlindness should start the blindness effect");
                return;
            }
            if (red.getCachedTarget() != null) {
                helper.fail("Blindness must drop the current target");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void rootPinsSoldierInPlace(GameTestHelper helper) {
        ClaySoldierEntity red = spawnSoldier(helper, 1);
        ClaySoldierEntity blue = spawnSoldier(helper, 2);
        // Enemy nearby so the rooted soldier has every reason to chase.
        blue.setPos(red.getX() + 2.5, red.getY(), red.getZ());
        blue.forceEquipUpgrade(UpgradeFlags.WHEAT);

        red.applyRoot();
        double startX = red.getX();
        double startZ = red.getZ();

        helper.runAfterDelay(40, () -> {
            double driftSq = (red.getX() - startX) * (red.getX() - startX)
                + (red.getZ() - startZ) * (red.getZ() - startZ);
            if (driftSq > 0.25) {
                helper.fail("Rooted soldier should stay pinned, drifted " + Math.sqrt(driftSq) + " blocks");
                return;
            }
            helper.succeed();
        });
    }

    // ── Projectile payloads (issue #15) ────────────────────────────────────

    @GameTest(structure = ARENA, maxTicks = 100)
    public void snowProjectileSlowsTarget(GameTestHelper helper) {
        ClaySoldierEntity target = spawnSoldier(helper, 2);
        fireProjectileAt(helper, EntityRegistry.SNOW_PROJECTILE.create(helper.getLevel(), EntitySpawnReason.COMMAND),
            UpgradeFlags.SNOW, target);

        helper.succeedWhen(() -> {
            if (!target.isSlowed()) {
                helper.fail("Snow projectile should slow the target");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void fireChargeProjectileBurnsTarget(GameTestHelper helper) {
        ClaySoldierEntity target = spawnSoldier(helper, 2);
        fireProjectileAt(helper, EntityRegistry.FIRE_CHARGE_PROJECTILE.create(helper.getLevel(), EntitySpawnReason.COMMAND),
            UpgradeFlags.FIRE_CHARGE, target);

        helper.succeedWhen(() -> {
            if (!target.isCombusting()) {
                helper.fail("Fire charge projectile should ignite the target");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void emeraldProjectilePiercesThroughTwoTargets(GameTestHelper helper) {
        ClaySoldierEntity first = spawnSoldier(helper, 2);
        ClaySoldierEntity second = spawnSoldier(helper, 2);
        second.setPos(first.getX() + 0.9, first.getY(), first.getZ());

        fireProjectileAt(helper, EntityRegistry.EMERALD_PROJECTILE.create(helper.getLevel(), EntitySpawnReason.COMMAND),
            UpgradeFlags.EMERALD, first);

        helper.succeedWhen(() -> {
            boolean firstHit = first.getSoldierHealth() < first.getSoldierMaxHealth() || first.isRemoved();
            boolean secondHit = second.getSoldierHealth() < second.getSoldierMaxHealth() || second.isRemoved();
            if (!firstHit || !secondHit) {
                helper.fail("Emerald projectile should pierce through and hit both targets");
            }
        });
    }

    @GameTest(structure = ARENA, maxTicks = 100)
    public void emeraldProjectileBypassesArmor(GameTestHelper helper) {
        ClaySoldierEntity target = spawnSoldier(helper, 2);
        target.forceEquipUpgrade(UpgradeFlags.LEATHER);
        target.forceEquipUpgrade(UpgradeFlags.BOWL);

        fireProjectileAt(helper, EntityRegistry.EMERALD_PROJECTILE.create(helper.getLevel(), EntitySpawnReason.COMMAND),
            UpgradeFlags.EMERALD, target);

        // Piercing damage is 3.0 * 1.35 = 4.05; armored RANGED damage would be
        // ~1.0. Health below 17 proves the reductions were bypassed.
        helper.succeedWhen(() -> {
            if (target.getSoldierHealth() > 17.0f) {
                helper.fail("Emerald should bypass armor reductions, health=" + target.getSoldierHealth());
            }
        });
    }

    /** Spawns a pacifist shooter with the payload upgrade and a projectile flying at the target. */
    private static void fireProjectileAt(GameTestHelper helper,
                                         io.github.joshiat.claylegion.entity.projectile.ClayProjectileEntity projectile,
                                         long payloadUpgrade, ClaySoldierEntity target) {
        ClaySoldierEntity shooter = spawnSoldier(helper, 1);
        // Wheat keeps the shooter's own AI out of the test.
        shooter.forceEquipUpgrade(UpgradeFlags.WHEAT);
        shooter.forceEquipUpgrade(payloadUpgrade);

        Vec3 origin = target.position().add(-1.0, 0.4, 0.0);
        Vec3 direction = target.position().add(0.0, 0.16, 0.0).subtract(origin).normalize();
        projectile.setShooter(shooter);
        projectile.setPos(origin);
        projectile.setDeltaMovement(direction.scale(0.55));
        helper.getLevel().addFreshEntity(projectile);
    }

    // ── Possession ─────────────────────────────────────────────────────────

    @GameTest(structure = ARENA)
    public void possessionStartsAndSuppressesAi(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        ServerPlayer player = makeHeadlessPlayer(helper);

        SoldierPossessionManager manager = SoldierPossessionManager.getInstance();
        manager.startPossession(player, soldier);

        if (!manager.isPossessing(player)) {
            helper.fail("Possession session did not start");
        }
        if (!soldier.isPossessed()) {
            helper.fail("Soldier was not marked possessed");
        }

        manager.endPossession(player);
        if (manager.isPossessing(player) || soldier.isPossessed()) {
            helper.fail("Possession did not end cleanly");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void possessionForcesSoulReturnOnBodyDamage(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        ServerPlayer player = makeHeadlessPlayer(helper);

        SoldierPossessionManager manager = SoldierPossessionManager.getInstance();
        manager.startPossession(player, soldier);
        if (!manager.isPossessing(player)) {
            helper.fail("Possession session did not start");
        }

        // Simulate the frozen body taking a hit.
        player.hurtTime = 10;
        manager.tick(helper.getLevel().getServer());

        if (manager.isPossessing(player)) {
            helper.fail("Body damage must force the soul back");
        }
        if (soldier.isPossessed()) {
            helper.fail("Soldier should regain AI control after forced return");
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void possessionEndsWhenSoldierDies(GameTestHelper helper) {
        ClaySoldierEntity soldier = spawnSoldier(helper, 0);
        ServerPlayer player = makeHeadlessPlayer(helper);

        SoldierPossessionManager manager = SoldierPossessionManager.getInstance();
        manager.startPossession(player, soldier);
        if (!manager.isPossessing(player)) {
            helper.fail("Possession session did not start");
        }

        soldier.applySoldierDamage(1000.0f, (byte) -1);
        manager.tick(helper.getLevel().getServer());

        if (manager.isPossessing(player)) {
            helper.fail("Possession must end when the soldier dies");
        }
        helper.succeed();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static ClaySoldierEntity spawnSoldier(GameTestHelper helper, int teamId) {
        ServerLevel level = helper.getLevel();
        ClaySoldierEntity soldier = EntityRegistry.CLAY_SOLDIER.create(level, EntitySpawnReason.COMMAND);
        if (soldier == null) {
            throw new IllegalStateException("Failed to create Clay Soldier entity");
        }
        soldier.setTeamId(teamId);
        Vec3 center = helper.absoluteVec(Vec3.atCenterOf(SPAWN)).add(0.0, -0.4, 0.0);
        soldier.setPos(center);
        level.addFreshEntity(soldier);
        return soldier;
    }

    /** A connectionless ServerPlayer standing in the arena — enough for possession logic. */
    private static ServerPlayer makeHeadlessPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(
            level.getServer(),
            level,
            new GameProfile(UUID.randomUUID(), "possession-test"),
            ClientInformation.createDefault()
        );
        Vec3 center = helper.absoluteVec(Vec3.atCenterOf(SPAWN));
        player.setPos(center.x, center.y, center.z);
        return player;
    }
}
