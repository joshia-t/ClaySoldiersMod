package io.github.joshiat.claylegion.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class ModEntityModelLayers {

    public static final ModelLayerLocation CLAY_SOLDIER = layer("clay_soldier");
    public static final ModelLayerLocation CLAY_MOUNT = layer("clay_mount");
    public static final ModelLayerLocation CLAY_HORSE = layer("clay_horse");
    public static final ModelLayerLocation CLAY_PEGASUS = layer("clay_pegasus");
    public static final ModelLayerLocation CLAY_TURTLE = layer("clay_turtle");
    public static final ModelLayerLocation CLAY_BUNNY = layer("clay_bunny");
    public static final ModelLayerLocation CLAY_GECKO = layer("clay_gecko");

    public static void registerModelLayers() {
        ModelLayerRegistry.registerModelLayer(CLAY_SOLDIER, ClaySoldierEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_MOUNT, MountEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_HORSE, HorseMountModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_PEGASUS, PegasusMountModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_TURTLE, TurtleMountModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_BUNNY, BunnyMountModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_GECKO, GeckoMountModel::getTexturedModelData);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("clay-legion", name), "main");
    }

    private ModEntityModelLayers() {}
}
