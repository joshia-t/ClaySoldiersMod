package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class HorseMountModel extends EntityModel<MountEntityRenderState> {

    private final ModelPart bbMain;

    public HorseMountModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.bbMain = root.getChild("bb_main");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bbMain = root.addOrReplaceChild("bb_main",
            CubeListBuilder.create()
                .texOffs(12, 14).addBox(-1.0f, -3.0f, -5.0f, 1.0f, 3.0f, 1.0f)
                .texOffs(0,  15).addBox(-1.0f, -3.0f,  1.0f, 1.0f, 3.0f, 1.0f)
                .texOffs(4,  15).addBox( 2.0f, -3.0f,  1.0f, 1.0f, 3.0f, 1.0f)
                .texOffs(8,  15).addBox( 2.0f, -3.0f, -5.0f, 1.0f, 3.0f, 1.0f)
                .texOffs(0,   0).addBox(-1.0f, -5.0f, -5.0f, 4.0f, 2.0f, 7.0f),
            PartPose.offset(0.0f, 24.0f, 0.0f));

        bbMain.addOrReplaceChild("Neck_r1",
            CubeListBuilder.create().texOffs(0, 9).addBox(-1.0f, -2.0f, -2.0f, 2.0f, 2.0f, 4.0f),
            PartPose.offsetAndRotation(1.0f, -5.0f, -6.0f, -0.9599f, 0.0f, 0.0f));

        bbMain.addOrReplaceChild("Head_r1",
            CubeListBuilder.create().texOffs(12, 9).addBox(-1.0f, -2.0f, -2.0f, 2.0f, 2.0f, 3.0f),
            PartPose.offsetAndRotation(1.0f, -6.0f, -6.0f, 0.3054f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        // Animation can be added here later (walk cycle, idle bob, etc.)
    }
}
