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
