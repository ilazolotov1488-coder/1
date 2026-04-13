package space.visuals.client.modules.impl.render.taksa;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class TaksaModel {

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public TaksaModel() {
        ModelData modelData = getModelData();
        ModelPartData rootData = modelData.getRoot();
        ModelPart root = rootData.createPart(60, 36);

        head          = root.getChild("head");
        neck          = root.getChild("neck");
        body          = root.getChild("body");
        frontLeftLeg  = root.getChild("frontLeftLeg");
        frontRightLeg = root.getChild("frontRightLeg");
        leftBackLeg   = root.getChild("leftBackLeg");
        rightBackLeg  = root.getChild("rightBackLeg");
        tail          = root.getChild("tail");
    }

    public static ModelData getModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData headData = root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-3, -3, -4, 6, 6, 4)
                .uv(21, 0).cuboid(-1.5f, 0, -7, 3, 3, 3),
            ModelTransform.pivot(0, 10.5f, -6.8f));

        headData.addChild("leftEar",
            ModelPartBuilder.create()
                .uv(32, 4).cuboid(0, -5, -1.5f, 1, 3, 3)
                .uv(34, 1).cuboid(0, -5.5f, -0.75f, 1, 1, 1),
            ModelTransform.pivot(3, 3, -2));

        headData.addChild("rightEar",
            ModelPartBuilder.create()
                .uv(32, 4).cuboid(-1, -5, -1.5f, 1, 3, 3)
                .uv(34, 1).cuboid(-1, -5.5f, -0.75f, 1, 1, 1),
            ModelTransform.pivot(-3, 3, -2));

        root.addChild("neck",
            ModelPartBuilder.create()
                .uv(15, 7).cuboid(-2.95f, -1, -4, 5.9f, 5, 6),
            ModelTransform.of(0, 10.5f, -5, -0.43633232f, 0, 0));

        ModelPartData bodyData = root.addChild("body",
            ModelPartBuilder.create(),
            ModelTransform.pivot(0, 13.5f, -5));

        bodyData.addChild("chest",
            ModelPartBuilder.create()
                .uv(32, 13).cuboid(-4, -3.5f, -3, 8, 7, 6),
            ModelTransform.pivot(0, 0, 3));

        bodyData.addChild("back",
            ModelPartBuilder.create()
                .uv(3, 19).cuboid(-3, -3, -0.5f, 6, 6, 11),
            ModelTransform.pivot(0, -0.5f, 5.5f));

        root.addChild("frontLeftLeg",
            ModelPartBuilder.create().uv(42, 0).cuboid(-1, 0, -1, 2, 5, 2),
            ModelTransform.pivot(1.5f, 16, -3));

        root.addChild("frontRightLeg",
            ModelPartBuilder.create().uv(42, 0).cuboid(-1, 0, -1, 2, 5, 2),
            ModelTransform.pivot(-1.5f, 16, -3));

        root.addChild("leftBackLeg",
            ModelPartBuilder.create().uv(52, 0).cuboid(-1, 0, -1, 2, 5, 2),
            ModelTransform.pivot(1.5f, 16, 9));

        root.addChild("rightBackLeg",
            ModelPartBuilder.create().uv(52, 0).cuboid(-1, 0, -1, 2, 5, 2),
            ModelTransform.pivot(-1.5f, 16, 9));

        root.addChild("tail",
            ModelPartBuilder.create()
                .uv(2, 12).cuboid(-1, 2, -1, 2, 8, 2),
            ModelTransform.of(0, 9, 10, (float)(Math.PI / 8), 0, 0));

        return data;
    }

    public void setAngles(float ageInTicks, TaksaBrain brain) {
        head.yaw   = brain.getYaw()   * ((float)Math.PI / 180f);
        head.pitch = brain.getPitch() * ((float)Math.PI / 180f);

        float swing    = brain.limbSwing;
        float swingAmt = brain.limbSwingAmount;

        frontLeftLeg.pitch  = (float)Math.cos(swing * 0.6662f)           * 1.4f * swingAmt;
        frontRightLeg.pitch = (float)Math.cos(swing * 0.6662f + Math.PI) * 1.4f * swingAmt;
        leftBackLeg.pitch   = (float)Math.cos(swing * 0.6662f + Math.PI) * 1.4f * swingAmt;
        rightBackLeg.pitch  = (float)Math.cos(swing * 0.6662f)           * 1.4f * swingAmt;

        if (brain.isLay()) {
            frontLeftLeg.pitch  = (float)Math.toRadians(-90);
            frontRightLeg.pitch = (float)Math.toRadians(-90);
            leftBackLeg.pitch   = (float)Math.toRadians(90);
            rightBackLeg.pitch  = (float)Math.toRadians(90);
            frontLeftLeg.yaw    = (float)Math.toRadians(-22);
            frontRightLeg.yaw   = (float)Math.toRadians(22);
            leftBackLeg.yaw     = (float)Math.toRadians(22);
            rightBackLeg.yaw    = (float)Math.toRadians(-22);
        } else {
            frontLeftLeg.yaw = frontRightLeg.yaw = leftBackLeg.yaw = rightBackLeg.yaw = 0;
        }

        tail.pitch = (float)Math.toRadians(brain.isLay() ? 45 : 22);
        tail.roll  = (float)(Math.toRadians(-22.5) + (Math.PI / 8)
                     + Math.cos(ageInTicks * 0.15f) * 0.3f);
    }

    public void render(MatrixStack ms, VertexConsumer vc, int light, int overlay, TaksaBrain brain, float ageInTicks) {
        ms.push();
        ms.translate(0, 1.2f - (brain.isLay() ? 0.3f : 0f), 0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBody()));

        setAngles(ageInTicks, brain);

        head.render(ms, vc, light, overlay);
        neck.render(ms, vc, light, overlay);
        body.render(ms, vc, light, overlay);
        frontLeftLeg.render(ms, vc, light, overlay);
        frontRightLeg.render(ms, vc, light, overlay);
        leftBackLeg.render(ms, vc, light, overlay);
        rightBackLeg.render(ms, vc, light, overlay);
        tail.render(ms, vc, light, overlay);

        ms.pop();
    }
}
