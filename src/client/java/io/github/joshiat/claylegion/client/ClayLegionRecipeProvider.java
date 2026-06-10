package io.github.joshiat.claylegion.client;

import io.github.joshiat.claylegion.registry.BlockRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

/**
 * Data-generated crafting recipes ported from the legacy mod (issue #21).
 *
 * Legacy shapes preserved:
 *  - soldiers: soul sand + clay block (shapeless) → 4 dolls; doll + dye recolors the team
 *  - horses:  M S M / M _ M  (M = material, S = soul sand) → 2; feather on top = pegasus
 *  - turtles: _ T T / S S T → 2
 *  - bunnies: W S W (same-color wool) → 4
 *  - geckos:  sapling / soul sand / sapling (vertical) → 2
 *  - nexus:   clay-diamond-clay / soulsand-obsidian-soulsand / obsidian row
 */
public class ClayLegionRecipeProvider extends FabricRecipeProvider {

    public ClayLegionRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput exporter) {
        return new RecipeProvider(registries, exporter) {
            @Override
            public void buildRecipes() {
                soldierRecipes();
                horseAndPegasusRecipes();
                turtleRecipes();
                bunnyRecipes();
                geckoRecipes();
                lexiconRecipe();
                nexusRecipe();
            }

            private void soldierRecipes() {
                // Soul sand + clay block → 4 clay soldiers.
                this.shapeless(RecipeCategory.MISC, ItemRegistry.SOLDIER_DOLL, 4)
                    .requires(Items.SOUL_SAND)
                    .requires(Items.CLAY)
                    .unlockedBy("has_clay", has(Items.CLAY))
                    .save(this.output, key("soldier_dolls"));

                // Doll + dye → team-colored doll (teams 0-15 mirror the dye order).
                Item[] dyes = {
                    Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
                    Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE,
                    Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
                    Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE
                };
                String[] teamNames = {
                    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
                };
                for (int teamId = 0; teamId < dyes.length; teamId++) {
                    this.shapeless(RecipeCategory.MISC, teamDoll(teamId))
                        .requires(ItemRegistry.SOLDIER_DOLL)
                        .requires(dyes[teamId])
                        .group("clay-legion:team_doll")
                        .unlockedBy("has_doll", has(ItemRegistry.SOLDIER_DOLL))
                        .save(this.output, key("soldier_doll_team_" + teamNames[teamId]));
                }
            }

            private ItemStackTemplate teamDoll(int teamId) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("team", teamId);
                DataComponentPatch components = DataComponentPatch.builder()
                    .set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                    .build();
                return new ItemStackTemplate(
                    ItemRegistry.SOLDIER_DOLL.builtInRegistryHolder(), 1, components);
            }

            private void horseAndPegasusRecipes() {
                record HorseMaterial(String name, ItemLike material, ItemLike horse, ItemLike pegasus) {}
                HorseMaterial[] materials = {
                    new HorseMaterial("clay", Items.CLAY, ItemRegistry.CLAY_HORSE_SPAWNER, ItemRegistry.CLAY_PEGASUS_SPAWNER),
                    new HorseMaterial("dirt", Items.DIRT, ItemRegistry.DIRT_HORSE_SPAWNER, ItemRegistry.DIRT_PEGASUS_SPAWNER),
                    new HorseMaterial("sand", Items.SAND, ItemRegistry.SAND_HORSE_SPAWNER, ItemRegistry.SAND_PEGASUS_SPAWNER),
                    new HorseMaterial("gravel", Items.GRAVEL, ItemRegistry.GRAVEL_HORSE_SPAWNER, ItemRegistry.GRAVEL_PEGASUS_SPAWNER),
                    new HorseMaterial("snow", Items.SNOW_BLOCK, ItemRegistry.SNOW_HORSE_SPAWNER, ItemRegistry.SNOW_PEGASUS_SPAWNER),
                    new HorseMaterial("grass", Items.SHORT_GRASS, ItemRegistry.GRASS_HORSE_SPAWNER, ItemRegistry.GRASS_PEGASUS_SPAWNER),
                    new HorseMaterial("lapis", Items.LAPIS_LAZULI, ItemRegistry.LAPIS_HORSE_SPAWNER, ItemRegistry.LAPIS_PEGASUS_SPAWNER),
                    new HorseMaterial("carrot", Items.CARROT, ItemRegistry.CARROT_HORSE_SPAWNER, ItemRegistry.CARROT_PEGASUS_SPAWNER),
                    new HorseMaterial("soulsand", Items.SOUL_SAND, ItemRegistry.SOULSAND_HORSE_SPAWNER, ItemRegistry.SOULSAND_PEGASUS_SPAWNER),
                    new HorseMaterial("cake", Items.CAKE, ItemRegistry.CAKE_HORSE_SPAWNER, ItemRegistry.CAKE_PEGASUS_SPAWNER),
                };

                for (HorseMaterial mat : materials) {
                    this.shaped(RecipeCategory.MISC, mat.horse(), 2)
                        .pattern("MSM")
                        .pattern("M M")
                        .define('M', mat.material())
                        .define('S', Items.SOUL_SAND)
                        .group("clay-legion:horse_doll")
                        .unlockedBy("has_material", has(mat.material()))
                        .save(this.output, key("horse_doll_" + mat.name()));

                    this.shaped(RecipeCategory.MISC, mat.pegasus(), 2)
                        .pattern(" F ")
                        .pattern("MSM")
                        .pattern("M M")
                        .define('F', Items.FEATHER)
                        .define('M', mat.material())
                        .define('S', Items.SOUL_SAND)
                        .group("clay-legion:pegasus_doll")
                        .unlockedBy("has_material", has(mat.material()))
                        .save(this.output, key("pegasus_doll_" + mat.name()));
                }
            }

            private void turtleRecipes() {
                record TurtleMaterial(String name, ItemLike material, ItemLike spawner) {}
                TurtleMaterial[] materials = {
                    new TurtleMaterial("cobble", Items.COBBLESTONE, ItemRegistry.COBBLE_TURTLE_SPAWNER),
                    new TurtleMaterial("mossy", Items.MOSSY_COBBLESTONE, ItemRegistry.MOSSY_TURTLE_SPAWNER),
                    new TurtleMaterial("sandstone", Items.SANDSTONE, ItemRegistry.SANDSTONE_TURTLE_SPAWNER),
                    new TurtleMaterial("netherrack", Items.NETHERRACK, ItemRegistry.NETHERRACK_TURTLE_SPAWNER),
                    new TurtleMaterial("endstone", Items.END_STONE, ItemRegistry.ENDSTONE_TURTLE_SPAWNER),
                    new TurtleMaterial("lapis", Items.LAPIS_LAZULI, ItemRegistry.LAPIS_TURTLE_SPAWNER),
                    new TurtleMaterial("melon", Items.MELON, ItemRegistry.MELON_TURTLE_SPAWNER),
                    new TurtleMaterial("pumpkin", Items.PUMPKIN, ItemRegistry.PUMPKIN_TURTLE_SPAWNER),
                    new TurtleMaterial("cake", Items.CAKE, ItemRegistry.CAKE_TURTLE_SPAWNER),
                };

                for (TurtleMaterial mat : materials) {
                    this.shaped(RecipeCategory.MISC, mat.spawner(), 2)
                        .pattern(" TT")
                        .pattern("SST")
                        .define('T', mat.material())
                        .define('S', Items.SOUL_SAND)
                        .group("clay-legion:turtle_doll")
                        .unlockedBy("has_material", has(mat.material()))
                        .save(this.output, key("turtle_doll_" + mat.name()));
                }
            }

            private void bunnyRecipes() {
                record BunnyMaterial(String name, ItemLike wool, ItemLike spawner) {}
                BunnyMaterial[] materials = {
                    new BunnyMaterial("white", Items.WHITE_WOOL, ItemRegistry.WHITE_BUNNY_SPAWNER),
                    new BunnyMaterial("light_gray", Items.LIGHT_GRAY_WOOL, ItemRegistry.LIGHT_GRAY_BUNNY_SPAWNER),
                    new BunnyMaterial("gray", Items.GRAY_WOOL, ItemRegistry.GRAY_BUNNY_SPAWNER),
                    new BunnyMaterial("black", Items.BLACK_WOOL, ItemRegistry.BLACK_BUNNY_SPAWNER),
                    new BunnyMaterial("brown", Items.BROWN_WOOL, ItemRegistry.BROWN_BUNNY_SPAWNER),
                    new BunnyMaterial("red", Items.RED_WOOL, ItemRegistry.RED_BUNNY_SPAWNER),
                    new BunnyMaterial("orange", Items.ORANGE_WOOL, ItemRegistry.ORANGE_BUNNY_SPAWNER),
                    new BunnyMaterial("yellow", Items.YELLOW_WOOL, ItemRegistry.YELLOW_BUNNY_SPAWNER),
                    new BunnyMaterial("lime", Items.LIME_WOOL, ItemRegistry.LIME_BUNNY_SPAWNER),
                    new BunnyMaterial("green", Items.GREEN_WOOL, ItemRegistry.GREEN_BUNNY_SPAWNER),
                    new BunnyMaterial("cyan", Items.CYAN_WOOL, ItemRegistry.CYAN_BUNNY_SPAWNER),
                    new BunnyMaterial("light_blue", Items.LIGHT_BLUE_WOOL, ItemRegistry.LIGHT_BLUE_BUNNY_SPAWNER),
                    new BunnyMaterial("blue", Items.BLUE_WOOL, ItemRegistry.BLUE_BUNNY_SPAWNER),
                    new BunnyMaterial("purple", Items.PURPLE_WOOL, ItemRegistry.PURPLE_BUNNY_SPAWNER),
                    new BunnyMaterial("magenta", Items.MAGENTA_WOOL, ItemRegistry.MAGENTA_BUNNY_SPAWNER),
                    new BunnyMaterial("pink", Items.PINK_WOOL, ItemRegistry.PINK_BUNNY_SPAWNER),
                };

                for (BunnyMaterial mat : materials) {
                    this.shaped(RecipeCategory.MISC, mat.spawner(), 4)
                        .pattern("WSW")
                        .define('W', mat.wool())
                        .define('S', Items.SOUL_SAND)
                        .group("clay-legion:bunny_doll")
                        .unlockedBy("has_wool", has(mat.wool()))
                        .save(this.output, key("bunny_doll_" + mat.name()));
                }
            }

            private void geckoRecipes() {
                record GeckoMaterial(String name, ItemLike sapling, ItemLike spawner) {}
                GeckoMaterial[] materials = {
                    new GeckoMaterial("oak", Items.OAK_SAPLING, ItemRegistry.OAK_GECKO_SPAWNER),
                    new GeckoMaterial("birch", Items.BIRCH_SAPLING, ItemRegistry.BIRCH_GECKO_SPAWNER),
                    new GeckoMaterial("jungle", Items.JUNGLE_SAPLING, ItemRegistry.JUNGLE_GECKO_SPAWNER),
                    new GeckoMaterial("acacia", Items.ACACIA_SAPLING, ItemRegistry.ACACIA_GECKO_SPAWNER),
                    new GeckoMaterial("darkoak", Items.DARK_OAK_SAPLING, ItemRegistry.DARKOAK_GECKO_SPAWNER),
                    new GeckoMaterial("pine", Items.SPRUCE_SAPLING, ItemRegistry.PINE_GECKO_SPAWNER),
                };

                for (GeckoMaterial mat : materials) {
                    this.shaped(RecipeCategory.MISC, mat.spawner(), 2)
                        .pattern("P")
                        .pattern("S")
                        .pattern("P")
                        .define('P', mat.sapling())
                        .define('S', Items.SOUL_SAND)
                        .group("clay-legion:gecko_doll")
                        .unlockedBy("has_sapling", has(mat.sapling()))
                        .save(this.output, key("gecko_doll_" + mat.name()));
                }
            }

            private void lexiconRecipe() {
                this.shapeless(RecipeCategory.MISC, ItemRegistry.LEXICON)
                    .requires(Items.BOOK)
                    .requires(Items.CLAY_BALL)
                    .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                    .save(this.output, key("lexicon"));
            }

            private void nexusRecipe() {
                this.shaped(RecipeCategory.MISC, BlockRegistry.CLAY_NEXUS)
                    .pattern("CDC")
                    .pattern("SOS")
                    .pattern("OOO")
                    .define('C', Items.CLAY_BALL)
                    .define('D', Items.DIAMOND)
                    .define('S', Items.SOUL_SAND)
                    .define('O', Items.OBSIDIAN)
                    .unlockedBy("has_diamond", has(Items.DIAMOND))
                    .save(this.output, key("clay_nexus"));
            }

            private ResourceKey<Recipe<?>> key(String path) {
                return ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("clay-legion", path));
            }
        };
    }

    @Override
    public String getName() {
        return "ClayLegionRecipes";
    }
}
