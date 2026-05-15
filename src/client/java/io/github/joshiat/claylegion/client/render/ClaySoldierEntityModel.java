package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Minimal soldier model — a single body cuboid sized to the soldier's bounding box.
 * Phase 5 will replace this with the full layered model (head, arms, capes, etc.)
 * Texture layout: 16×16 placeholder.
 */
public class ClaySoldierEntityModel extends EntityModel<ClaySoldierEntityRenderState> {

    private final ModelPart body;

    public ClaySoldierEntityModel(ModelPart root) {
        super(root);
        this.body = root.getChild(PartNames.BODY);
    }

    /**
     * Defines the model data baked at class-load time.
     * Body: 4 px wide, 8 px tall, 4 px deep. Feet sit on the ground (y=-8 pivot).
     */
    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 0).addBox(-2f, -8f, -2f, 4, 8, 4),
                PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(ClaySoldierEntityRenderState state) {
        // Phase 5: animate body parts here
    }
}
