package io.github.joshiat.claylegion.entity.team;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRegistryTest {

    @Test
    void registersAllThirtyNineLegacyTeams() {
        assertEquals(39, TeamRegistry.size(), "Legacy parity requires exactly 39 teams");
    }

    @Test
    void everyTeamIdResolvesToItself() {
        Set<String> names = new HashSet<>();
        for (int id = 0; id < 39; id++) {
            SoldierTeam team = TeamRegistry.getById(id);
            assertNotNull(team, "Team " + id + " missing");
            assertEquals(id, team.teamId(), "Team " + id + " resolves to wrong id");
            assertTrue(names.add(team.name()), "Duplicate team name: " + team.name());
        }
    }

    @Test
    void unknownIdsFallBackToWhite() {
        assertEquals(0, TeamRegistry.getById(-1).teamId());
        assertEquals(0, TeamRegistry.getById(999).teamId());
    }

    @Test
    void dyeTeamsMatchVanillaDyeOrder() {
        // Team ids 0-15 must mirror the dye order used by the team-doll recipes.
        assertEquals("White", TeamRegistry.getById(0).name());
        assertEquals("Red", TeamRegistry.getById(14).name());
        assertEquals("Black", TeamRegistry.getById(15).name());
    }

    @Test
    void enemyCheckIsByTeamIdOnly() {
        SoldierTeam white = TeamRegistry.getById(0);
        SoldierTeam red = TeamRegistry.getById(14);
        assertTrue(white.isEnemyOf(red));
        assertTrue(red.isEnemyOf(white));
        assertFalse(white.isEnemyOf(TeamRegistry.getById(0)));
    }
}
