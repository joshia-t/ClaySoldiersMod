package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Pegasus = Horse + two wings. Wings share the 64x32 horse texture sheet
 * (UV at (0, 22)), so a pegasus is rendered with one of the standard horse
 * variant textures and the wings simply pick up the wing UV from that sheet.
 */
public class PegasusMountModel extends HorseMountModel {

    private static final String WING_LEFT = "wing_left";
    private static final String WING_RIGHT = "wing_right";

    private final ModelPart wingLeft;
    private final ModelPart wingRight;

    public PegasusMountModel(ModelPart root) {
        super(root);
        this.wingLeft = root.getChild(WING_LEFT);
        this.wingRight = root.getChild(WING_RIGHT);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        HorseMountModel.addHorseGeometry(root);

        // wings: OLD positioned them at z=2.75 (over the rump). Move to z=-0.75 so
        // wing center lands near the shoulders, and raise y slightly to sit on the back.
        root.addOrReplaceChild(WING_LEFT,
            CubeListBuilder.create().texOffs(0, 22)
                .addBox(-12.5f, -1.25f, -2.25f, 13.0f, 1.0f, 5.0f),
            PartPose.offset(-1.5f, 9.5f, -0.75f));

        root.addOrReplaceChild(WING_RIGHT,
            CubeListBuilder.create().texOffs(0, 22).mirror()
                .addBox(-0.5f, -1.25f, -2.25f, 13.0f, 1.0f, 5.0f),
            PartPose.offset(1.5f, 9.5f, -0.75f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        super.setupAnim(state);

        // OLD wing animation used a "wingSwingAmount" tracked on the entity.
        // We don't have that, so drive from animTime. Z rotations negated after y-flip.
        float t = state.animTime * 0.5f;
        wingLeft.yRot  =  0.2f + (float) Math.sin(t) / 6.0f;
        wingRight.yRot = -0.2f - (float) Math.sin(t) / 6.0f;
        // OLD: zRot=-0.125 - cos(t)/10, zRot=0.125 + cos(t)/10. Negate after y-flip:
        wingLeft.zRot  =  0.125f + (float) Math.cos(t) / 10.0f;
        wingRight.zRot = -0.125f - (float) Math.cos(t) / 10.0f;
    }
}
