package io.github.joshiat.claylegion.entity.team;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstraps all 39 soldier teams mapped by their integer ID.
 * Teams correspond to the 16 dye colors plus named special teams from the legacy mod.
 * Dye color ARGB values follow vanilla Minecraft DyeColor packed colors.
 */
public final class TeamRegistry {

    private static final Map<Integer, SoldierTeam> BY_ID = new HashMap<>();

    // 16 dye-color base teams (IDs 0–15)
    private static final int[][] DYE_TEAMS = {
        // { id, dyeArgb }
        {  0, 0xFFFFFFFF }, // White
        {  1, 0xFFFF8C00 }, // Orange
        {  2, 0xFFB400FF }, // Magenta
        {  3, 0xFF6AAAFF }, // Light Blue
        {  4, 0xFFFFFF00 }, // Yellow
        {  5, 0xFF7FFF00 }, // Lime
        {  6, 0xFFFF82B4 }, // Pink
        {  7, 0xFF777777 }, // Gray
        {  8, 0xFFBBBBBB }, // Light Gray
        {  9, 0xFF00D2CA }, // Cyan
        { 10, 0xFF8000FF }, // Purple
        { 11, 0xFF0000FF }, // Blue
        { 12, 0xFF6B3300 }, // Brown
        { 13, 0xFF00AA00 }, // Green
        { 14, 0xFFFF0000 }, // Red
        { 15, 0xFF222222 }, // Black
    };

    private static final String[] DYE_NAMES = {
        "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime",
        "Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue",
        "Brown", "Green", "Red", "Black"
    };

    // Additional named teams (IDs 16–38) matching legacy variant count of 39 total
    private static final Object[][] NAMED_TEAMS = {
        { 16, "Undead",      0xFF44DD44 },
        { 17, "Skeleton",    0xFFEEEECC },
        { 18, "Zombie",      0xFF779966 },
        { 19, "Creeper",     0xFF55FF55 },
        { 20, "Spider",      0xFF552200 },
        { 21, "Ghast",       0xFFFFFFEE },
        { 22, "Enderman",    0xFF111111 },
        { 23, "Blaze",       0xFFFF8800 },
        { 24, "Slime",       0xFF66FF66 },
        { 25, "Witch",       0xFF440044 },
        { 26, "Wither",      0xFF333333 },
        { 27, "Iron Golem",  0xFFCCBBAA },
        { 28, "Snow Golem",  0xFFDDEEFF },
        { 29, "Villager",    0xFFCC9966 },
        { 30, "Herobrine",   0xFF888888 },
        { 31, "Notch",       0xFF9966CC },
        { 32, "Dinnerbone",  0xFFCC6699 },
        { 33, "Jeb",         0xFFFFFF44 },
        { 34, "Dragon",      0xFF660066 },
        { 35, "Piglin",      0xFFFF9955 },
        { 36, "Hoglin",      0xFFCC4422 },
        { 37, "Warden",      0xFF004444 },
        { 38, "Allay",       0xFF88CCFF },
    };

    static {
        for (int i = 0; i < DYE_TEAMS.length; i++) {
            int id = DYE_TEAMS[i][0];
            int color = DYE_TEAMS[i][1];
            register(new SoldierTeam(id, DYE_NAMES[i], color));
        }
        for (Object[] row : NAMED_TEAMS) {
            register(new SoldierTeam((int) row[0], (String) row[1], (int) row[2]));
        }
    }

    private static void register(SoldierTeam team) {
        BY_ID.put(team.teamId(), team);
    }

    public static SoldierTeam getById(int id) {
        return BY_ID.getOrDefault(id, BY_ID.get(0)); // fallback to White
    }

    public static int size() {
        return BY_ID.size();
    }

    private TeamRegistry() {}
}
