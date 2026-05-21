package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Port of the original 1.7-era ModelHorseMount from the OLD codebase.
 *
 * Coordinate conversion from 1.7 (y-down, feet at y=11.75) to modern y-up
 * with feet at y=0: {@code y_modern = -y_1.7 + 11.75}. All x/z values
 * preserved verbatim. X and Z rotations are negated because the y-axis
 * flipped; Y rotations stay the same.
 *
 * Texture: 64x32 sheet. UV offsets match the OLD model so all 18 variant
 * textures under textures/entity/horses/ map correctly.
 */
public class HorseMountModel extends EntityModel<MountEntityRenderState> {

    private static final String HEAD = "head";
    private static final String EAR1 = "ear1";
    private static final String EAR2 = "ear2";
    private static final String BODY = "body";
    private static final String NECK = "neck";
    private static final String MANE = "mane";
    private static final String LEG1 = "leg1";
    private static final String LEG2 = "leg2";
    private static final String LEG3 = "leg3";
    private static final String LEG4 = "leg4";
    private static final String TAIL = "tail";

    private final ModelPart head;
    private final ModelPart ear1;
    private final ModelPart ear2;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart mane;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart tail;

    public HorseMountModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.head  = root.getChild(HEAD);
        this.ear1  = root.getChild(EAR1);
        this.ear2  = root.getChild(EAR2);
        this.body  = root.getChild(BODY);
        this.neck  = root.getChild(NECK);
        this.mane  = root.getChild(MANE);
        this.leg1  = root.getChild(LEG1);
        this.leg2  = root.getChild(LEG2);
        this.leg3  = root.getChild(LEG3);
        this.leg4  = root.getChild(LEG4);
        this.tail  = root.getChild(TAIL);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        addHorseGeometry(mesh.getRoot());
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Builds the horse body parts on the given root, no LayerDefinition wrapping. */
    static void addHorseGeometry(PartDefinition root) {
        // head: OLD pivot (0, -3.75, -7.75), box(-1, 0, -4, 2x2x4, dil 0.2)
        root.addOrReplaceChild(HEAD,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0f, -2.0f, -4.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.2f)),
            PartPose.offset(0.0f, 15.5f, -7.75f));

        // ear1 / ear2: share head pivot. Box(-1.25/0.25, -0.8, -1, 1x1x1, dil 0.1)
        root.addOrReplaceChild(EAR1,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.25f, -0.2f, -1.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.1f)),
            PartPose.offset(0.0f, 15.5f, -7.75f));

        root.addOrReplaceChild(EAR2,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(0.25f, -0.2f, -1.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.1f)),
            PartPose.offset(0.0f, 15.5f, -7.75f));

        // body: OLD pivot (0, 0, 0), box(-2, 0, -4, 4x4x8, dil 0.0)
        root.addOrReplaceChild(BODY,
            CubeListBuilder.create().texOffs(0, 8)
                .addBox(-2.0f, -4.0f, -4.0f, 4.0f, 4.0f, 8.0f),
            PartPose.offset(0.0f, 11.75f, 0.0f));

        // neck: OLD pivot (0, 0, -2), box(-1, 0, -6, 2x2x6, dil 0.4), xRot=-0.6
        root.addOrReplaceChild(NECK,
            CubeListBuilder.create().texOffs(12, 0)
                .addBox(-1.0f, -2.0f, -6.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.4f)),
            PartPose.offsetAndRotation(0.0f, 11.75f, -2.0f, 0.6f, 0.0f, 0.0f));

        // mane: OLD pivot (0, 0, -2), box(-1, -1.1, -6, 2x1x6, dil 0.0), xRot=-0.6
        root.addOrReplaceChild(MANE,
            CubeListBuilder.create().texOffs(28, 0)
                .addBox(-1.0f, 0.1f, -6.0f, 2.0f, 1.0f, 6.0f),
            PartPose.offsetAndRotation(0.0f, 11.75f, -2.0f, 0.6f, 0.0f, 0.0f));

        // legs: OLD pivot (+/-1, 3.75, +/-2.75), box(-1, 0, -1, 2x8x2, dil 0.25)
        root.addOrReplaceChild(LEG1,
            CubeListBuilder.create().texOffs(24, 10)
                .addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.25f)),
            PartPose.offset(-1.0f, 8.0f, -2.75f));

        root.addOrReplaceChild(LEG2,
            CubeListBuilder.create().texOffs(24, 10).mirror()
                .addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.25f)),
            PartPose.offset(1.0f, 8.0f, -2.75f));

        root.addOrReplaceChild(LEG3,
            CubeListBuilder.create().texOffs(24, 10)
                .addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.25f)),
            PartPose.offset(-1.0f, 8.0f, 2.75f));

        root.addOrReplaceChild(LEG4,
            CubeListBuilder.create().texOffs(24, 10).mirror()
                .addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.25f)),
            PartPose.offset(1.0f, 8.0f, 2.75f));

        // tail: OLD pivot (0, 0, 3.75), box(-0.5, 0, -0.5, 1x5x1, dil 0.15)
        root.addOrReplaceChild(TAIL,
            CubeListBuilder.create().texOffs(36, 11)
                .addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.15f)),
            PartPose.offset(0.0f, 11.75f, 3.75f));
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        resetParts();

        // Walk cycle: ported from OLD setRotationAngles. The OLD formula was
        //   leg1.xRot = cos(limbSwing * 0.6662 + PI) * 2 * limbSwingAmount * 0.25
        // We don't have limbSwing here, so drive it from animTime * horizontalSpeed.
        // Diagonal gait: leg1 (front-left) and leg3 (back-left) move together;
        // leg2 (front-right) and leg4 (back-right) opposite.
        // After y-flip, xRot is negated relative to the OLD model.
        float gait = Mth.clamp(state.horizontalSpeed * 8.0f, 0.0f, 1.0f);
        float swingPhase = state.animTime * 0.6662f;
        float swingMag = 0.5f * gait;
        float swingA = -((float) Math.cos(swingPhase + Math.PI)) * swingMag;
        float swingB = -((float) Math.cos(swingPhase)) * swingMag;
        leg1.xRot = swingA;
        leg3.xRot = swingA;
        leg2.xRot = swingB;
        leg4.xRot = swingB;

        // Tail: OLD was xRot = 0.3 + leg1.xRot^2. After y-flip negation, we
        // negate the constant too so the tail visually droops the same way.
        tail.xRot = -(0.3f + leg1.xRot * leg1.xRot);

        // Head + ears: track yaw so the head looks where it walks. We don't
        // have look targets on MountEntityRenderState yet, so keep neutral.
        head.xRot = 0.0f;
        head.yRot = 0.0f;
        ear1.xRot = head.xRot;
        ear2.xRot = head.xRot;
        ear1.yRot = head.yRot;
        ear2.yRot = head.yRot;
    }

    private void resetParts() {
        head.xRot = head.yRot = head.zRot = 0.0f;
        ear1.xRot = ear1.yRot = ear1.zRot = 0.0f;
        ear2.xRot = ear2.yRot = ear2.zRot = 0.0f;
        body.xRot = body.yRot = body.zRot = 0.0f;
        leg1.xRot = leg1.yRot = leg1.zRot = 0.0f;
        leg2.xRot = leg2.yRot = leg2.zRot = 0.0f;
        leg3.xRot = leg3.yRot = leg3.zRot = 0.0f;
        leg4.xRot = leg4.yRot = leg4.zRot = 0.0f;
        tail.xRot = tail.yRot = tail.zRot = 0.0f;
        // neck and mane keep their PartPose tilt; setupAnim does not animate them
    }
}
