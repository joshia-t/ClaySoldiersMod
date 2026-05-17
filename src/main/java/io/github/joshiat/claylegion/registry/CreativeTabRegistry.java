package io.github.joshiat.claylegion.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class CreativeTabRegistry {

    public static final ResourceKey<CreativeModeTab> CLAY_LEGION_TAB_KEY = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(),
        Identifier.fromNamespaceAndPath("clay-legion", "clay_legion")
    );

    public static final CreativeModeTab CLAY_LEGION_TAB = FabricCreativeModeTab.builder()
        .icon(() -> new ItemStack(ItemRegistry.RED_SOLDIER_DOLL))
        .title(net.minecraft.network.chat.Component.translatable("creativeTab.clay-legion.clay_legion"))
        .displayItems((parameters, output) -> {
            output.accept(ItemRegistry.SOLDIER_DOLL);
            output.accept(ItemRegistry.RED_SOLDIER_DOLL);
            output.accept(ItemRegistry.BRICK_SOLDIER_DOLL);
            output.accept(ItemRegistry.HORSE_SPAWNER);
            output.accept(ItemRegistry.PEGASUS_SPAWNER);
            output.accept(ItemRegistry.TURTLE_SPAWNER);
            output.accept(ItemRegistry.BUNNY_SPAWNER);
            output.accept(ItemRegistry.GECKO_SPAWNER);
        })
        .build();

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CLAY_LEGION_TAB_KEY, CLAY_LEGION_TAB);
    }

    private CreativeTabRegistry() {}
}
