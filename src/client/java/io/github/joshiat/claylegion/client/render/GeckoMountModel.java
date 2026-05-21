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
 * Port of OLD ModelGeckoMount with y_modern = 24 - y_1.7, X/Z rotations negated.
 */
public class GeckoMountModel extends EntityModel<MountEntityRenderState> {

    private static final String HEAD = "head";
    private static final String NOSE = "nose";
    private static final String BODY = "body";
    private static final String RIGHT_ARM = "right_arm";
    private static final String LEFT_ARM = "left_arm";
    private static final String RIGHT_LEG = "right_leg";
    private static final String LEFT_LEG = "left_leg";
    private static final String TAIL = "tail";

    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public GeckoMountModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.head = root.getChild(HEAD);
        this.nose = root.getChild(NOSE);
        this.body = root.getChild(BODY);
        this.rightArm = root.getChild(RIGHT_ARM);
        this.leftArm = root.getChild(LEFT_ARM);
        this.rightLeg = root.getChild(RIGHT_LEG);
        this.leftLeg = root.getChild(LEFT_LEG);
        this.tail = root.getChild(TAIL);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // head: OLD pivot (0, 21, -4), addBox(-1, 0, -3, 2x1x2)
        root.addOrReplaceChild(HEAD,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0f, -1.0f, -3.0f, 2.0f, 1.0f, 2.0f),
            PartPose.offset(0.0f, 3.0f, -4.0f));

        // nose: OLD pivot (0, 21, -4), addBox(-1.5, -1, -1.5, 3x2x2)
        root.addOrReplaceChild(NOSE,
            CubeListBuilder.create().texOffs(0, 8)
                .addBox(-1.5f, -1.0f, -1.5f, 3.0f, 2.0f, 2.0f),
            PartPose.offset(0.0f, 3.0f, -4.0f));

        // body: OLD pivot (0, 22, -2), addBox(-1.5, -1, -1.5, 3x1x7)
        root.addOrReplaceChild(BODY,
            CubeListBuilder.create().texOffs(8, 0)
                .addBox(-1.5f, 0.0f, -1.5f, 3.0f, 1.0f, 7.0f),
            PartPose.offset(0.0f, 2.0f, -2.0f));

        // limbs: OLD addBox(_, 0, _, 1x4x1). zRot +/-1.226894 (negated after y-flip),
        // yRot +/-0.2602503 (preserved).
        root.addOrReplaceChild(RIGHT_ARM,
            CubeListBuilder.create().texOffs(0, 3).mirror()
                .addBox(-1.0f, -4.0f, -1.0f, 1.0f, 4.0f, 1.0f),
            PartPose.offsetAndRotation(-1.0f, 2.0f, -2.0f, 0.0f, -0.2602503f, -1.226894f));

        root.addOrReplaceChild(LEFT_ARM,
            CubeListBuilder.create().texOffs(0, 3).mirror()
                .addBox(0.0f, -4.0f, -1.0f, 1.0f, 4.0f, 1.0f),
            PartPose.offsetAndRotation(1.0f, 2.0f, -2.0f, 0.0f, 0.2602503f, 1.226894f));

        root.addOrReplaceChild(RIGHT_LEG,
            CubeListBuilder.create().texOffs(4, 3).mirror()
                .addBox(-1.0f, -4.0f, 0.0f, 1.0f, 4.0f, 1.0f),
            PartPose.offsetAndRotation(-1.0f, 2.0f, 2.0f, 0.0f, 0.2602503f, -1.226894f));

        root.addOrReplaceChild(LEFT_LEG,
            CubeListBuilder.create().texOffs(4, 3)
                .addBox(0.0f, -4.0f, 0.0f, 1.0f, 4.0f, 1.0f),
            PartPose.offsetAndRotation(1.0f, 2.0f, 2.0f, 0.0f, -0.2602503f, 1.226894f));

        // tail: OLD pivot (0, 21.5, 3), addBox(-0.5, 0, -0.5, 1x8x1), xRot=1.487144
        root.addOrReplaceChild(TAIL,
            CubeListBuilder.create().texOffs(0, 12)
                .addBox(-0.5f, -8.0f, -0.5f, 1.0f, 8.0f, 1.0f),
            PartPose.offsetAndRotation(0.0f, 2.5f, 3.0f, -1.487144f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        // OLD walk: diagonal gait, tail yaw swing. xRot negated after y-flip; yRot kept.
        float gait = Mth.clamp(state.horizontalSpeed * 8.0f, 0.0f, 1.0f);
        float phase = state.animTime * 0.6662f;
        float a = -((float) Math.cos(phase + Math.PI)) * 0.5f * gait;
        float b = -((float) Math.cos(phase)) * 0.5f * gait;
        rightArm.xRot = a;
        leftArm.xRot = b;
        rightLeg.xRot = b;
        leftLeg.xRot = a;
        // tail yRot retained (Y axis unchanged by y-flip)
        tail.yRot = ((float) Math.cos(phase + Math.PI)) * 0.5f * gait;
    }
}
