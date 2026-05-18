package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Shared low-poly mount model with simple type-dependent silhouette toggles.
 */
public class MountEntityModel extends EntityModel<MountEntityRenderState> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart shell;
    private final ModelPart ears;
    private final ModelPart crest;

    public MountEntityModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.root = root;
        this.body = root.getChild(PartNames.BODY);
        this.head = root.getChild(PartNames.HEAD);
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.wingLeft = root.getChild("wing_left");
        this.wingRight = root.getChild("wing_right");
        this.shell = root.getChild("shell");
        this.ears = root.getChild("ears");
        this.crest = root.getChild("crest");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
            PartNames.BODY,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0f, 2.0f, -4.0f, 6.0f, 4.0f, 8.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            PartNames.HEAD,
            CubeListBuilder.create().texOffs(0, 12)
                .addBox(-2.0f, 5.0f, -7.0f, 4.0f, 3.0f, 3.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "front_left_leg",
            CubeListBuilder.create().texOffs(18, 12)
                .addBox(1.2f, 0.0f, -2.8f, 1.4f, 3.0f, 1.4f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "front_right_leg",
            CubeListBuilder.create().texOffs(18, 12)
                .addBox(-2.6f, 0.0f, -2.8f, 1.4f, 3.0f, 1.4f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "back_left_leg",
            CubeListBuilder.create().texOffs(24, 12)
                .addBox(1.2f, 0.0f, 1.4f, 1.4f, 3.0f, 1.4f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "back_right_leg",
            CubeListBuilder.create().texOffs(24, 12)
                .addBox(-2.6f, 0.0f, 1.4f, 1.4f, 3.0f, 1.4f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "wing_left",
            CubeListBuilder.create().texOffs(0, 18)
                .addBox(3.0f, 3.0f, -3.0f, 3.0f, 1.0f, 6.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "wing_right",
            CubeListBuilder.create().texOffs(0, 18)
                .addBox(-6.0f, 3.0f, -3.0f, 3.0f, 1.0f, 6.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "shell",
            CubeListBuilder.create().texOffs(14, 18)
                .addBox(-3.5f, 3.0f, -4.5f, 7.0f, 2.0f, 9.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "ears",
            CubeListBuilder.create().texOffs(0, 26)
                .addBox(-1.5f, 8.0f, -6.8f, 3.0f, 2.0f, 1.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "crest",
            CubeListBuilder.create().texOffs(10, 26)
                .addBox(-0.5f, 7.8f, -6.2f, 1.0f, 1.0f, 3.0f),
            PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        body.xRot = 0.0f;
        head.xRot = 0.0f;
        head.yRot = 0.0f;
        wingLeft.zRot = 0.0f;
        wingRight.zRot = 0.0f;

        float walk = Mth.sin(state.animTime * 0.55f) * 0.65f * Mth.clamp(state.horizontalSpeed * 6.0f, 0.0f, 1.0f);
        frontLeftLeg.xRot = walk;
        frontRightLeg.xRot = -walk;
        backLeftLeg.xRot = -walk;
        backRightLeg.xRot = walk;

        // 1=Horse 2=Pegasus 3=Turtle 4=Bunny 5=Gecko
        boolean pegasus = state.mountType == 2;
        boolean turtle = state.mountType == 3;
        boolean bunny = state.mountType == 4;
        boolean gecko = state.mountType == 5;

        wingLeft.visible = pegasus;
        wingRight.visible = pegasus;
        shell.visible = turtle;
        ears.visible = bunny;
        crest.visible = gecko;

        if (pegasus) {
            float flap = Mth.sin(state.animTime * 0.95f) * 0.32f;
            wingLeft.zRot = flap;
            wingRight.zRot = -flap;
        }

        if (turtle) {
            body.y = 0.6f;
            head.y = 0.6f;
        } else {
            body.y = 0.0f;
            head.y = 0.0f;
        }
    }
}
