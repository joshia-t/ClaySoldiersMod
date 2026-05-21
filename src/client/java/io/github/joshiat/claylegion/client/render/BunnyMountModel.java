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
 * Port of OLD ModelBunnyMount with y_modern = 24 - y_1.7, xRot/zRot negated.
 * Texture: 16x16 sheet matching OLD bunny textures.
 */
public class BunnyMountModel extends EntityModel<MountEntityRenderState> {

    private static final String HEAD = "head";
    private static final String BODY = "body";
    private static final String LEFT_LEG_FRONT = "left_leg_front";
    private static final String RIGHT_LEG_FRONT = "right_leg_front";
    private static final String LEFT_LEG_BACK = "left_leg_back";
    private static final String RIGHT_LEG_BACK = "right_leg_back";
    private static final String EAR_LEFT = "ear_left";
    private static final String EAR_RIGHT = "ear_right";
    private static final String TAIL = "tail";

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftLegFront;
    private final ModelPart rightLegFront;
    private final ModelPart leftLegBack;
    private final ModelPart rightLegBack;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart tail;

    public BunnyMountModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.head = root.getChild(HEAD);
        this.body = root.getChild(BODY);
        this.leftLegFront = root.getChild(LEFT_LEG_FRONT);
        this.rightLegFront = root.getChild(RIGHT_LEG_FRONT);
        this.leftLegBack = root.getChild(LEFT_LEG_BACK);
        this.rightLegBack = root.getChild(RIGHT_LEG_BACK);
        this.earLeft = root.getChild(EAR_LEFT);
        this.earRight = root.getChild(EAR_RIGHT);
        this.tail = root.getChild(TAIL);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // head: OLD pivot (0, 21.5, -1), addBox(-1, -1, -2, 2x2x2)
        root.addOrReplaceChild(HEAD,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0f, -1.0f, -2.0f, 2.0f, 2.0f, 2.0f),
            PartPose.offset(0.0f, 2.5f, -1.0f));

        // body: OLD pivot (0, 21.5, 1), addBox(-1.5, -2, -1.5, 3x3x2), xRot=1.570796
        root.addOrReplaceChild(BODY,
            CubeListBuilder.create().texOffs(0, 4)
                .addBox(-1.5f, -1.0f, -1.5f, 3.0f, 3.0f, 2.0f),
            PartPose.offsetAndRotation(0.0f, 2.5f, 1.0f, -1.570796f, 0.0f, 0.0f));

        // legs front: OLD pivot y=23, addBox(_, 0, 0, 1x1x1)
        root.addOrReplaceChild(LEFT_LEG_FRONT,
            CubeListBuilder.create().texOffs(0, 9)
                .addBox(-1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f),
            PartPose.offset(-0.5f, 1.0f, 1.0f));

        root.addOrReplaceChild(RIGHT_LEG_FRONT,
            CubeListBuilder.create().texOffs(0, 9).mirror()
                .addBox(0.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f),
            PartPose.offset(0.5f, 1.0f, 1.0f));

        // legs back: OLD pivot y=23, addBox(_, 0, -1, 1x1x1)
        root.addOrReplaceChild(LEFT_LEG_BACK,
            CubeListBuilder.create().texOffs(0, 9)
                .addBox(-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f),
            PartPose.offset(-0.5f, 1.0f, 0.0f));

        root.addOrReplaceChild(RIGHT_LEG_BACK,
            CubeListBuilder.create().texOffs(0, 9).mirror()
                .addBox(0.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f),
            PartPose.offset(0.5f, 1.0f, 0.0f));

        // ears: OLD pivot (0, 22, -1), box (_, -3.1, -1, 1x3x1), zRot=+/-0.6981317
        root.addOrReplaceChild(EAR_RIGHT,
            CubeListBuilder.create().texOffs(8, 0)
                .addBox(0.0f, 0.1f, -1.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(0.0f, 2.0f, -1.0f, 0.0f, 0.0f, 0.6981317f));

        root.addOrReplaceChild(EAR_LEFT,
            CubeListBuilder.create().texOffs(8, 0).mirror()
                .addBox(-1.0f, 0.1f, -1.0f, 1.0f, 3.0f, 1.0f),
            PartPose.offsetAndRotation(0.0f, 2.0f, -1.0f, 0.0f, 0.0f, -0.6981317f));

        // tail: OLD pivot (0, 21.5, 2), addBox(-0.5, 0, -0.5, 1x1x1), xRot=1.747395
        root.addOrReplaceChild(TAIL,
            CubeListBuilder.create().texOffs(4, 9)
                .addBox(-0.5f, -1.0f, -0.5f, 1.0f, 1.0f, 1.0f),
            PartPose.offsetAndRotation(0.0f, 2.5f, 2.0f, -1.747395f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        // OLD walk cycle: front legs together, back legs together, opposite phase.
        // After y-flip, xRot is negated.
        float gait = Mth.clamp(state.horizontalSpeed * 8.0f, 0.0f, 1.0f);
        float phase = state.animTime * 0.6662f;
        float front = -((float) Math.cos(phase)) * 0.5f * gait;
        float back  = -((float) Math.cos(phase + Math.PI)) * 0.5f * gait;
        leftLegFront.xRot = front;
        rightLegFront.xRot = front;
        leftLegBack.xRot = back;
        rightLegBack.xRot = back;
    }
}
