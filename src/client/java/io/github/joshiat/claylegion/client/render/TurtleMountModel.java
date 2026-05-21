package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Port of OLD ModelTurtleMount with y_modern = 24 - y_1.7, X/Z rotations negated.
 */
public class TurtleMountModel extends EntityModel<MountEntityRenderState> {

    private static final String HEAD = "head";
    private static final String SHELL_MAIN = "shell_main";
    private static final String SHELL_TOP = "shell_top";
    private static final String LEFT_LEG_FRONT = "left_leg_front";
    private static final String RIGHT_LEG_FRONT = "right_leg_front";
    private static final String LEFT_LEG_BACK = "left_leg_back";
    private static final String RIGHT_LEG_BACK = "right_leg_back";

    private final ModelPart head;
    private final ModelPart shellMain;
    private final ModelPart shellTop;
    private final ModelPart leftLegFront;
    private final ModelPart rightLegFront;
    private final ModelPart leftLegBack;
    private final ModelPart rightLegBack;

    public TurtleMountModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.head = root.getChild(HEAD);
        this.shellMain = root.getChild(SHELL_MAIN);
        this.shellTop = root.getChild(SHELL_TOP);
        this.leftLegFront = root.getChild(LEFT_LEG_FRONT);
        this.rightLegFront = root.getChild(RIGHT_LEG_FRONT);
        this.leftLegBack = root.getChild(LEFT_LEG_BACK);
        this.rightLegBack = root.getChild(RIGHT_LEG_BACK);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // head: OLD pivot (0, 21, -5), addBox(-1, -1, -2, 2x2x2)
        root.addOrReplaceChild(HEAD,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0f, -1.0f, -2.0f, 2.0f, 2.0f, 2.0f),
            PartPose.offset(0.0f, 3.0f, -5.0f));

        // shellMain: OLD pivot (0, 20, -3), addBox(-3, 0, -2, 6x2x7)
        root.addOrReplaceChild(SHELL_MAIN,
            CubeListBuilder.create().texOffs(8, 0)
                .addBox(-3.0f, -2.0f, -2.0f, 6.0f, 2.0f, 7.0f),
            PartPose.offset(0.0f, 4.0f, -3.0f));

        // shellTop: OLD pivot (0, 20, -3), addBox(-2, -1, -1, 4x1x5)
        root.addOrReplaceChild(SHELL_TOP,
            CubeListBuilder.create().texOffs(8, 9)
                .addBox(-2.0f, 0.0f, -1.0f, 4.0f, 1.0f, 5.0f),
            PartPose.offset(0.0f, 4.0f, -3.0f));

        // legs: OLD pivot y=22, addBox(_, 0, _, 1x3x1). Old zRot +1.003822 (right) / -1.003822 (left), negated after y-flip.
        root.addOrReplaceChild(RIGHT_LEG_FRONT,
            CubeListBuilder.create().texOffs(0, 4)
                .addBox(-1.0f, -3.0f, -1.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(-2.0f, 2.0f, -3.0f, 0.0f, 0.0f, -1.003822f));

        root.addOrReplaceChild(LEFT_LEG_FRONT,
            CubeListBuilder.create().texOffs(0, 4).mirror()
                .addBox(0.0f, -3.0f, -1.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(2.0f, 2.0f, -3.0f, 0.0f, 0.0f, 1.003822f));

        root.addOrReplaceChild(RIGHT_LEG_BACK,
            CubeListBuilder.create().texOffs(4, 4)
                .addBox(-1.0f, -3.0f, 0.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(-2.0f, 2.0f, 0.0f, 0.0f, 0.0f, -1.003822f));

        root.addOrReplaceChild(LEFT_LEG_BACK,
            CubeListBuilder.create().texOffs(4, 4).mirror()
                .addBox(0.0f, -3.0f, 0.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 1.003822f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        // OLD: front-right/back-left in one phase, front-left/back-right opposite.
        // After y-flip, xRot negated.
        float gait = Mth.clamp(state.horizontalSpeed * 8.0f, 0.0f, 1.0f);
        float phase = state.animTime * 0.6662f;
        float a = -((float) Math.cos(phase + Math.PI)) * 0.5f * gait;
        float b = -((float) Math.cos(phase)) * 0.5f * gait;
        rightLegFront.xRot = a;
        leftLegBack.xRot = a;
        leftLegFront.xRot = b;
        rightLegBack.xRot = b;
    }
}
