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
 * One shared "superset" mount mesh. Every part any mount needs exists in the
 * mesh; each mount type shows its subset and reshapes shared parts via a
 * per-type scale/offset/rotation table in {@link #setupAnim}. Keeps a single
 * mesh + single texture + single draw path for the 500+-entity budget while
 * still giving each animal a distinct silhouette.
 *
 * Coordinate convention (matches ClaySoldierEntityModel): EntityRenderer does
 * not apply the LivingEntity Y flip, so y=0 is the ground, +y is up, and -z is
 * forward (the animal faces -z).
 */
public class MountEntityModel extends EntityModel<MountEntityRenderState> {

    private static final String NECK = "neck";
    private static final String SNOUT = "snout";
    private static final String TAIL = "tail";
    private static final String FRONT_LEFT_LEG = "front_left_leg";
    private static final String FRONT_RIGHT_LEG = "front_right_leg";
    private static final String BACK_LEFT_LEG = "back_left_leg";
    private static final String BACK_RIGHT_LEG = "back_right_leg";
    private static final String WING_LEFT = "wing_left";
    private static final String WING_RIGHT = "wing_right";
    private static final String SHELL = "shell";
    private static final String EARS = "ears";
    private static final String CREST = "crest";

    /** Base leg pivot height; a full-length leg reaches the ground from here. */
    private static final float LEG_PIVOT_Y = 7.0f;

    /** Squared distance beyond which tiny cosmetic parts are culled. */
    private static final double LOD_FINE_SQ = 32.0 * 32.0;

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart shell;
    private final ModelPart ears;
    private final ModelPart crest;
    private final ModelPart[] all;

    public MountEntityModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild(PartNames.BODY);
        this.neck = root.getChild(NECK);
        this.head = root.getChild(PartNames.HEAD);
        this.snout = root.getChild(SNOUT);
        this.tail = root.getChild(TAIL);
        this.frontLeftLeg = root.getChild(FRONT_LEFT_LEG);
        this.frontRightLeg = root.getChild(FRONT_RIGHT_LEG);
        this.backLeftLeg = root.getChild(BACK_LEFT_LEG);
        this.backRightLeg = root.getChild(BACK_RIGHT_LEG);
        this.wingLeft = root.getChild(WING_LEFT);
        this.wingRight = root.getChild(WING_RIGHT);
        this.shell = root.getChild(SHELL);
        this.ears = root.getChild(EARS);
        this.crest = root.getChild(CREST);
        this.all = new ModelPart[] {
            body, neck, head, snout, tail,
            frontLeftLeg, frontRightLeg, backLeftLeg, backRightLeg,
            wingLeft, wingRight, shell, ears, crest
        };
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(PartNames.BODY,
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0f, 0.0f, -6.0f, 6.0f, 5.0f, 12.0f),
            PartPose.offset(0.0f, 7.0f, 0.0f));

        root.addOrReplaceChild(NECK,
            CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.5f, -1.0f, -2.0f, 3.0f, 6.0f, 2.0f),
            PartPose.offsetAndRotation(0.0f, 12.0f, -6.0f, -0.7f, 0.0f, 0.0f));

        root.addOrReplaceChild(PartNames.HEAD,
            CubeListBuilder.create().texOffs(34, 0)
                .addBox(-2.5f, -2.0f, -5.0f, 5.0f, 4.0f, 5.0f),
            PartPose.offset(0.0f, 17.0f, -8.0f));

        root.addOrReplaceChild(SNOUT,
            CubeListBuilder.create().texOffs(34, 10)
                .addBox(-1.5f, -1.5f, -3.0f, 3.0f, 3.0f, 3.0f),
            PartPose.offset(0.0f, 16.0f, -12.5f));

        root.addOrReplaceChild(TAIL,
            CubeListBuilder.create().texOffs(20, 18)
                .addBox(-1.0f, -7.0f, 0.0f, 2.0f, 7.0f, 2.0f),
            PartPose.offsetAndRotation(0.0f, 11.0f, 6.0f, 0.3f, 0.0f, 0.0f));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(28, 18)
            .addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f);
        root.addOrReplaceChild(FRONT_LEFT_LEG, leg, PartPose.offset(2.0f, LEG_PIVOT_Y, -4.0f));
        root.addOrReplaceChild(FRONT_RIGHT_LEG, leg, PartPose.offset(-2.0f, LEG_PIVOT_Y, -4.0f));
        root.addOrReplaceChild(BACK_LEFT_LEG, leg, PartPose.offset(2.0f, LEG_PIVOT_Y, 4.0f));
        root.addOrReplaceChild(BACK_RIGHT_LEG, leg, PartPose.offset(-2.0f, LEG_PIVOT_Y, 4.0f));

        root.addOrReplaceChild(WING_LEFT,
            CubeListBuilder.create()
                .texOffs(36, 26).addBox(0.0f, -0.5f, -3.0f, 6.0f, 1.0f, 7.0f)
                .texOffs(36, 36).addBox(5.0f, -0.5f, -1.0f, 5.0f, 1.0f, 4.0f),
            PartPose.offset(3.0f, 11.0f, -1.0f));

        root.addOrReplaceChild(WING_RIGHT,
            CubeListBuilder.create()
                .texOffs(36, 26).addBox(-6.0f, -0.5f, -3.0f, 6.0f, 1.0f, 7.0f)
                .texOffs(36, 36).addBox(-10.0f, -0.5f, -1.0f, 5.0f, 1.0f, 4.0f),
            PartPose.offset(-3.0f, 11.0f, -1.0f));

        root.addOrReplaceChild(SHELL,
            CubeListBuilder.create().texOffs(0, 36)
                .addBox(-4.5f, 0.0f, -7.0f, 9.0f, 5.0f, 13.0f),
            PartPose.offset(0.0f, 11.0f, 0.0f));

        root.addOrReplaceChild(EARS,
            CubeListBuilder.create()
                .texOffs(0, 26).addBox(0.4f, 0.0f, -0.5f, 1.2f, 5.0f, 1.0f)
                .texOffs(6, 26).addBox(-1.6f, 0.0f, -0.5f, 1.2f, 5.0f, 1.0f),
            PartPose.offset(0.0f, 19.0f, -8.0f));

        root.addOrReplaceChild(CREST,
            CubeListBuilder.create().texOffs(20, 28)
                .addBox(-0.5f, 0.0f, 0.0f, 1.0f, 2.0f, 11.0f),
            PartPose.offset(0.0f, 12.0f, -5.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MountEntityRenderState state) {
        resetParts();

        // 1=Horse 2=Pegasus 3=Turtle 4=Bunny 5=Gecko
        int type = state.mountType;
        boolean pegasus = type == 2;
        boolean turtle = type == 3;
        boolean bunny = type == 4;
        boolean gecko = type == 5;

        // Accessories default off; the core quadruped is always on.
        snout.visible = !turtle;
        wingLeft.visible = pegasus;
        wingRight.visible = pegasus;
        shell.visible = turtle;
        ears.visible = bunny;
        crest.visible = gecko;
        tail.visible = !turtle;

        // Per-type leg length drives the grounded stance offset so feet stay
        // on y=0: a leg scaled to s reaches LEG_PIVOT_Y*s, so everything drops
        // by LEG_PIVOT_Y*(1-s).
        float legScaleY;
        switch (type) {
            case 3 -> legScaleY = 0.42f;   // turtle: stubby
            case 4 -> legScaleY = 0.85f;   // bunny: grounded on the hind legs
            case 5 -> legScaleY = 0.50f;   // gecko: short, sprawled
            default -> legScaleY = 1.0f;   // horse / pegasus
        }
        float stance = LEG_PIVOT_Y * (legScaleY - 1.0f);
        for (ModelPart p : all) {
            p.y += stance;
        }

        scaleLegs(legScaleY);

        switch (type) {
            case 3 -> shapeTurtle();
            case 4 -> shapeBunny();
            case 5 -> shapeGecko();
            default -> { /* horse / pegasus keep base proportions */ }
        }

        applyWalk(state);

        if (pegasus) {
            float flap = Mth.sin(state.animTime * 0.9f) * 0.38f + 0.10f;
            wingLeft.zRot = flap;
            wingRight.zRot = -flap;
            wingLeft.yRot = -0.15f;
            wingRight.yRot = 0.15f;
        }
        if (bunny) {
            ears.xRot += Mth.sin(state.animTime * 0.3f) * 0.05f;
        }
        if (type == 1 || pegasus) {
            tail.yRot = Mth.sin(state.animTime * 0.25f) * 0.20f;
        }

        applyLod(state);
    }

    private void resetParts() {
        for (ModelPart p : all) {
            p.x = 0.0f;
            p.y = 0.0f;
            p.z = 0.0f;
            p.xRot = 0.0f;
            p.yRot = 0.0f;
            p.zRot = 0.0f;
            p.xScale = 1.0f;
            p.yScale = 1.0f;
            p.zScale = 1.0f;
            p.visible = true;
        }
    }

    private void scaleLegs(float scaleY) {
        frontLeftLeg.yScale = scaleY;
        frontRightLeg.yScale = scaleY;
        backLeftLeg.yScale = scaleY;
        backRightLeg.yScale = scaleY;
    }

    private void shapeTurtle() {
        body.yScale = 0.55f;
        body.xScale = 1.20f;
        body.zScale = 0.95f;
        neck.yScale = 0.45f;
        neck.xRot = -0.20f;
        head.yScale = 0.70f;
        head.xScale = 0.80f;
        // Splay the stubby legs outward.
        frontLeftLeg.zRot = -0.55f;
        backLeftLeg.zRot = -0.55f;
        frontRightLeg.zRot = 0.55f;
        backRightLeg.zRot = 0.55f;
        shell.xScale = 1.05f;
    }

    private void shapeBunny() {
        body.zScale = 0.60f;
        body.xScale = 0.90f;
        body.yScale = 1.00f;
        neck.yScale = 0.25f;
        head.zScale = 0.90f;
        snout.xScale = 0.70f;
        snout.zScale = 0.70f;
        ears.yScale = 1.45f;
        ears.xRot = -0.22f;
        ears.zScale = 0.80f;
        // Tucked front legs, big crouched haunches.
        frontLeftLeg.yScale = 0.50f;
        frontRightLeg.yScale = 0.50f;
        frontLeftLeg.xRot = 0.55f;
        frontRightLeg.xRot = 0.55f;
        backLeftLeg.xScale = 1.45f;
        backRightLeg.xScale = 1.45f;
        backLeftLeg.zScale = 1.70f;
        backRightLeg.zScale = 1.70f;
        backLeftLeg.xRot = -0.55f;
        backRightLeg.xRot = -0.55f;
        tail.yScale = 0.25f;
        tail.zScale = 1.60f;
        tail.xScale = 1.60f;
    }

    private void shapeGecko() {
        body.zScale = 1.60f;
        body.yScale = 0.50f;
        body.xScale = 0.90f;
        neck.yScale = 0.30f;
        neck.xRot = 0.10f;
        head.yScale = 0.55f;
        head.zScale = 1.30f;
        head.xScale = 0.90f;
        snout.zScale = 1.30f;
        snout.yScale = 0.70f;
        // Long tail laid out flat behind.
        tail.yScale = 2.60f;
        tail.xScale = 0.65f;
        tail.zScale = 0.65f;
        tail.xRot = 1.55f;
        // Sprawled lizard legs.
        frontLeftLeg.zRot = -1.00f;
        backLeftLeg.zRot = -1.00f;
        frontRightLeg.zRot = 1.00f;
        backRightLeg.zRot = 1.00f;
    }

    private void applyWalk(MountEntityRenderState state) {
        float gait = Mth.clamp(state.horizontalSpeed * 6.0f, 0.0f, 1.0f);
        if (gait <= 0.001f) {
            return;
        }
        float walk = Mth.sin(state.animTime * 0.55f) * 0.65f * gait;
        // Diagonal gait: FL/BR together, FR/BL opposite.
        frontLeftLeg.xRot += walk;
        backRightLeg.xRot += walk;
        frontRightLeg.xRot += -walk;
        backLeftLeg.xRot += -walk;
    }

    private void applyLod(MountEntityRenderState state) {
        if (state.distanceToCameraSq > LOD_FINE_SQ) {
            snout.visible = false;
            crest.visible = false;
            tail.visible = false;
        }
    }
}
