package io.github.joshiat.claylegion.entity.team;

/**
 * Represents a single clay soldier team.
 * teamId is a compact integer used as the DataTracker key and for ally/enemy checks.
 * dyeColor is the ARGB packed int used to tint the base soldier texture.
 */
public record SoldierTeam(int teamId, String name, int dyeColor) {

    public boolean isEnemyOf(SoldierTeam other) {
        return this.teamId != other.teamId;
    }
}
