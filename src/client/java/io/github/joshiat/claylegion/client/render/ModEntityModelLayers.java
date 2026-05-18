package io.github.joshiat.claylegion.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class ModEntityModelLayers {

    public static final ModelLayerLocation CLAY_SOLDIER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("clay-legion", "clay_soldier"), "main"
    );

    public static final ModelLayerLocation CLAY_MOUNT = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("clay-legion", "clay_mount"), "main"
    );

    public static void registerModelLayers() {
        ModelLayerRegistry.registerModelLayer(CLAY_SOLDIER, ClaySoldierEntityModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(CLAY_MOUNT, MountEntityModel::getTexturedModelData);
    }

    private ModEntityModelLayers() {}
}
