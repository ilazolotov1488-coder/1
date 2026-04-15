package space.visuals.utility.render.entity;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.model.Dilation;
import space.visuals.client.modules.impl.render.CustomModelType;
import space.visuals.utility.mixin.accessors.BipedEntityModelAccessor;

public final class CustomModelsRenderer {
    private static final RabbitModel RABBIT_MODEL = new RabbitModel(RabbitModel.createModel());
    private static final DemonModel DEMON_MODEL = new DemonModel(DemonModel.createModel());
    private static final FreddyModel FREDDY_MODEL = new FreddyModel(FreddyModel.createModel());
    private static final AmogusModel AMOGUS_MODEL = new AmogusModel(AmogusModel.createModel());

    private CustomModelsRenderer() {}

    public static boolean render(CustomModelType type, EntityModel<?> baseModel, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        if (type == null || !(baseModel instanceof BipedEntityModel)) return false;
        BipedEntityModelAccessor accessor = (BipedEntityModelAccessor) baseModel;
        ModelPart head = accessor.getHead();
        ModelPart leftArm = accessor.getLeftArm();
        ModelPart rightArm = accessor.getRightArm();
        ModelPart leftLeg = accessor.getLeftLeg();
        ModelPart rightLeg = accessor.getRightLeg();
        CustomModel model = getModel(type.getModelKey());
        if (model == null) return false;
        model.applyAngles(head, leftArm, rightArm, leftLeg, rightLeg);
        matrices.push();
        applyTransform(type.getModelKey(), matrices);
        model.render(matrices, vertices, light, overlay, color);
        matrices.pop();
        return true;
    }

    private static CustomModel getModel(CustomModelType.ModelKey key) {
        return switch (key) {
            case RABBIT -> RABBIT_MODEL;
            case DEMON -> DEMON_MODEL;
            case FREDDY -> FREDDY_MODEL;
            case AMOGUS -> AMOGUS_MODEL;
        };
    }

    private static void applyTransform(CustomModelType.ModelKey key, MatrixStack matrices) {
        switch (key) {
            case RABBIT -> { matrices.scale(1.25f, 1.25f, 1.25f); matrices.translate(0f, -0.3f, 0f); }
            case FREDDY -> { matrices.scale(0.75f, 0.65f, 0.75f); matrices.translate(0f, 0.85f, 0f); }
            case AMOGUS -> matrices.translate(0f, -0.5f, 0f);
            default -> {}
        }
    }

    private static void copyAngles(ModelPart source, ModelPart target, float extraPitch, float extraYaw, float extraRoll) {
        target.pitch = source.pitch + extraPitch;
        target.yaw = source.yaw + extraYaw;
        target.roll = source.roll + extraRoll;
    }

    private interface CustomModel {
        void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg);
        void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color);
    }

    // ==================== RABBIT ====================
    private static final class RabbitModel implements CustomModel {
        private final ModelPart root, rabbitHead, rabbitLarm, rabbitRarm, rabbitLleg, rabbitRleg;
        private RabbitModel(ModelPart root) {
            this.root = root;
            ModelPart bone = root.getChild("rabbit_bone");
            this.rabbitHead = bone.getChild("rabbit_head");
            this.rabbitLarm = bone.getChild("rabbit_larm");
            this.rabbitRarm = bone.getChild("rabbit_rarm");
            this.rabbitLleg = bone.getChild("rabbit_lleg");
            this.rabbitRleg = bone.getChild("rabbit_rleg");
        }
        @Override public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            copyAngles(head, rabbitHead, 0, 0, 0);
            copyAngles(leftArm, rabbitLarm, 0, 0, -0.0873f);
            copyAngles(rightArm, rabbitRarm, 0, 0, 0.0873f);
            copyAngles(leftLeg, rabbitLleg, 0, 0, 0);
            copyAngles(rightLeg, rabbitRleg, 0, 0, 0);
        }
        @Override public void render(MatrixStack m, VertexConsumer v, int l, int o, int c) { root.render(m, v, l, o, c); }
        static ModelPart createModel() {
            ModelData md = new ModelData(); ModelPartData root = md.getRoot();
            ModelPartData bone = root.addChild("rabbit_bone", ModelPartBuilder.create().uv(28,45).cuboid(-5,-13,-5,10,11,8), ModelTransform.pivot(0,24,0));
            bone.addChild("rabbit_rleg", ModelPartBuilder.create().uv(0,0).cuboid(-2,0,-2,4,2,4), ModelTransform.pivot(-3,-2,-1));
            bone.addChild("rabbit_larm", ModelPartBuilder.create().uv(0,0).cuboid(0,0,-2,2,8,4), ModelTransform.of(5,-13,-1,0,0,-0.0873f));
            bone.addChild("rabbit_rarm", ModelPartBuilder.create().uv(0,0).cuboid(-2,0,-2,2,8,4), ModelTransform.of(-5,-13,-1,0,0,0.0873f));
            bone.addChild("rabbit_lleg", ModelPartBuilder.create().uv(0,0).cuboid(-2,0,-2,4,2,4), ModelTransform.pivot(3,-2,-1));
            bone.addChild("rabbit_head", ModelPartBuilder.create().uv(0,0).cuboid(-3,0,-4,6,1,6).uv(56,0).cuboid(-5,-9,-5,2,3,2).uv(56,0).mirrored().cuboid(3,-9,-5,2,3,2).mirrored(false).uv(0,45).cuboid(-4,-11,-4,8,11,8).uv(46,0).cuboid(1,-20,0,3,9,1).uv(46,0).cuboid(-4,-20,0,3,9,1), ModelTransform.pivot(0,-14,-1));
            return TexturedModelData.of(md, 64, 64).createModel();
        }
    }

    // ==================== DEMON ====================
    private static final class DemonModel implements CustomModel {
        private final ModelPart root, head, leftArm, rightArm, leftLeg, rightLeg;
        private DemonModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("demon_head");
            this.leftArm = root.getChild("demon_left_arm");
            this.rightArm = root.getChild("demon_right_arm");
            this.leftLeg = root.getChild("demon_left_leg");
            this.rightLeg = root.getChild("demon_right_leg");
        }
        @Override public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            copyAngles(head, this.head, 0,0,0); copyAngles(leftArm, this.leftArm, 0,0,0);
            copyAngles(rightArm, this.rightArm, 0,0,0); copyAngles(leftLeg, this.leftLeg, 0,0,0); copyAngles(rightLeg, this.rightLeg, 0,0,0);
        }
        @Override public void render(MatrixStack m, VertexConsumer v, int l, int o, int c) { root.render(m, v, l, o, c); }
        static ModelPart createModel() {
            ModelData md = new ModelData(); ModelPartData root = md.getRoot();
            ModelPartData head = root.addChild("demon_head", ModelPartBuilder.create().uv(0,0).cuboid(-4,-4,-3,8,8,8,new Dilation(0.3f)), ModelTransform.pivot(0,-6,-1));
            head.addChild("demon_left_horn", ModelPartBuilder.create().uv(32,8).cuboid(13.4346f,-5.2071f,2.7071f,6,2,2,new Dilation(0.1f)).uv(0,0).cuboid(17.4346f,-10.4071f,2.7071f,2,5,2,new Dilation(0.1f)), ModelTransform.of(-8,8,0,-0.3927f,0.3927f,-0.5236f));
            head.addChild("demon_right_horn", ModelPartBuilder.create().uv(32,8).mirrored().cuboid(-19.4346f,-5.2071f,2.7071f,6,2,2,new Dilation(0.1f)).mirrored(false).uv(0,0).mirrored().cuboid(-19.4346f,-10.4071f,2.7071f,2,5,2,new Dilation(0.1f)).mirrored(false), ModelTransform.of(8,8,0,-0.3927f,-0.3927f,0.5236f));
            ModelPartData body = root.addChild("demon_body", ModelPartBuilder.create().uv(0,16).cuboid(-4.5f,-1.7028f,1.4696f,8,12,4), ModelTransform.of(0.5f,-0.1f,-3.5f,0.1745f,0,0));
            body.addChild("demon_left_wing", ModelPartBuilder.create().uv(40,12).cuboid(-7.0072f,-0.5972f,0.7515f,12,13,0), ModelTransform.of(8.25f,-2,10,0.0873f,-0.829f,0.1745f));
            body.addChild("demon_right_wing", ModelPartBuilder.create().uv(40,12).mirrored().cuboid(-4.9928f,-0.5972f,0.7515f,12,13,0).mirrored(false), ModelTransform.of(-9.25f,-2,10,0.0873f,0.829f,-0.1745f));
            root.addChild("demon_left_arm", ModelPartBuilder.create().uv(24,16).cuboid(-1.1f,-1.05f,0,4,14,4), ModelTransform.of(5.4f,-1.25f,-2,0,0,-0.2182f));
            root.addChild("demon_right_arm", ModelPartBuilder.create().uv(24,16).mirrored().cuboid(-2.9f,-1.05f,0,4,14,4).mirrored(false), ModelTransform.of(-5.4f,-1.25f,-2,0,0,0.2182f));
            ModelPartData ll = root.addChild("demon_left_leg", ModelPartBuilder.create().uv(48,22).cuboid(-3.25f,-2.25f,-1,4,9,4), ModelTransform.pivot(3,10,0));
            ModelPartData ll1 = ll.addChild("demon_left_leg1", ModelPartBuilder.create().uv(34,34).cuboid(0.95f,4.6f,8.0511f,3,5,3), ModelTransform.of(-1.7f,-0.1f,-3.55f,-0.5236f,0,0));
            ll1.addChild("demon_left_bone2", ModelPartBuilder.create().uv(26,0).cuboid(-0.7f,-1.15f,9.3f,4,2,4).uv(40,0).cuboid(-0.7f,-1.15f,7.3f,4,2,2), ModelTransform.of(1.4f,15,0.25f,0.5236f,0,0));
            ModelPartData lb3 = ll1.addChild("demon_left_bone3", ModelPartBuilder.create(), ModelTransform.of(-1,0,-2,0,-0.0873f,-0.2618f));
            lb3.addChild("demon_left_bone7", ModelPartBuilder.create().uv(16,34).cuboid(-0.7911f,-10.1159f,8.0029f,4,4,5).uv(0,32).cuboid(-0.7911f,-15.1159f,4.0029f,4,9,4), ModelTransform.pivot(1.9f,12,0.25f));
            ModelPartData rl = root.addChild("demon_right_leg", ModelPartBuilder.create().uv(48,22).mirrored().cuboid(-0.75f,-2.25f,-1,4,9,4).mirrored(false), ModelTransform.pivot(-3,10,0));
            ModelPartData rl3 = rl.addChild("demon_right_leg3", ModelPartBuilder.create().uv(34,34).mirrored().cuboid(-3.95f,4.6f,8.0511f,3,5,3).mirrored(false), ModelTransform.of(1.7f,-0.1f,-3.55f,-0.5236f,0,0));
            rl3.addChild("demon_right_bone4", ModelPartBuilder.create().uv(26,0).mirrored().cuboid(-3.3f,-1.15f,9.3f,4,2,4).mirrored(false).uv(40,0).mirrored().cuboid(-3.3f,-1.15f,7.3f,4,2,2).mirrored(false), ModelTransform.of(-1.4f,15,0.25f,0.5236f,0,0));
            ModelPartData rb5 = rl3.addChild("demon_right_bone5", ModelPartBuilder.create(), ModelTransform.of(1,0,-2,0,0.0873f,0.2618f));
            rb5.addChild("demon_right_bone6", ModelPartBuilder.create().uv(16,34).mirrored().cuboid(-3.2089f,-10.1159f,8.0029f,4,4,5).mirrored(false).uv(0,32).mirrored().cuboid(-3.2089f,-15.1159f,4.0029f,4,9,4).mirrored(false), ModelTransform.pivot(-1.9f,12,0.25f));
            return TexturedModelData.of(md, 64, 64).createModel();
        }
    }

    // ==================== FREDDY ====================
    private static final class FreddyModel implements CustomModel {
        private final ModelPart root, head, leftArm, rightArm, leftLeg, rightLeg;
        private FreddyModel(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("freddy_body");
            this.head = body.getChild("freddy_head");
            this.leftArm = body.getChild("freddy_left_arm");
            this.rightArm = body.getChild("freddy_right_arm");
            this.leftLeg = body.getChild("freddy_left_leg");
            this.rightLeg = body.getChild("freddy_right_leg");
        }
        @Override public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            copyAngles(head, this.head, 0,0,0); copyAngles(leftArm, this.leftArm, 0,0,0);
            copyAngles(rightArm, this.rightArm, 0,0,0); copyAngles(leftLeg, this.leftLeg, 0,0,0); copyAngles(rightLeg, this.rightLeg, 0,0,0);
        }
        @Override public void render(MatrixStack m, VertexConsumer v, int l, int o, int c) { root.render(m, v, l, o, c); }
        static ModelPart createModel() {
            ModelData md = new ModelData(); ModelPartData root = md.getRoot();
            ModelPartData fb = root.addChild("freddy_body", ModelPartBuilder.create().uv(0,0).cuboid(-1,-14,-1,2,24,2), ModelTransform.pivot(0,-9,0));
            fb.addChild("freddy_torso", ModelPartBuilder.create().uv(8,0).cuboid(-6,-9,-4,12,18,8), ModelTransform.of(0,0,0,0.0174533f,0,0));
            ModelPartData ra = fb.addChild("freddy_right_arm", ModelPartBuilder.create().uv(48,0).cuboid(-1,0,-1,2,10,2), ModelTransform.of(-6.5f,-8,0,0,0,0.2617994f));
            ra.addChild("freddy_right_arm_pad", ModelPartBuilder.create().uv(70,10).cuboid(-2.5f,0,-2.5f,5,9,5), ModelTransform.pivot(0,0.5f,0));
            ModelPartData ra2 = ra.addChild("freddy_right_arm2", ModelPartBuilder.create().uv(90,20).cuboid(-1,0,-1,2,8,2), ModelTransform.of(0,9.6f,0,-0.17453292f,0,0));
            ra2.addChild("freddy_right_arm_pad2", ModelPartBuilder.create().uv(0,26).cuboid(-2.5f,0,-2.5f,5,7,5), ModelTransform.pivot(0,0.5f,0));
            ra2.addChild("freddy_right_hand", ModelPartBuilder.create().uv(20,26).cuboid(-2,0,-2.5f,4,4,5), ModelTransform.of(0,8,0,0,0,-0.05235988f));
            ModelPartData la = fb.addChild("freddy_left_arm", ModelPartBuilder.create().uv(62,10).cuboid(-1,0,-1,2,10,2), ModelTransform.of(6.5f,-8,0,0,0,-0.2617994f));
            la.addChild("freddy_left_arm_pad", ModelPartBuilder.create().uv(38,54).cuboid(-2.5f,0,-2.5f,5,9,5), ModelTransform.pivot(0,0.5f,0));
            ModelPartData la2 = la.addChild("freddy_left_arm2", ModelPartBuilder.create().uv(90,48).cuboid(-1,0,-1,2,8,2), ModelTransform.of(0,9.6f,0,-0.17453292f,0,0));
            la2.addChild("freddy_left_arm_pad2", ModelPartBuilder.create().uv(0,58).cuboid(-2.5f,0,-2.5f,5,7,5), ModelTransform.pivot(0,0.5f,0));
            la2.addChild("freddy_left_hand", ModelPartBuilder.create().uv(58,56).cuboid(-1,0,-2.5f,4,4,5), ModelTransform.of(0,8,0,0,0,0.05235988f));
            ModelPartData rl = fb.addChild("freddy_right_leg", ModelPartBuilder.create().uv(90,8).cuboid(-1,0,-1,2,10,2), ModelTransform.pivot(-3.3f,12.5f,0));
            rl.addChild("freddy_right_leg_pad", ModelPartBuilder.create().uv(73,33).cuboid(-3,0,-3,6,9,6), ModelTransform.pivot(0,0.5f,0));
            ModelPartData rl2 = rl.addChild("freddy_right_leg2", ModelPartBuilder.create().uv(20,35).cuboid(-1,0,-1,2,8,2), ModelTransform.of(0,9.6f,0,0.03490659f,0,0));
            rl2.addChild("freddy_right_leg_pad2", ModelPartBuilder.create().uv(0,39).cuboid(-2.5f,0,-3,5,7,6), ModelTransform.pivot(0,0.5f,0));
            rl2.addChild("freddy_right_foot", ModelPartBuilder.create().uv(22,39).cuboid(-2.5f,0,-6,5,3,8), ModelTransform.of(0,8,0,-0.03490659f,0,0));
            ModelPartData ll = fb.addChild("freddy_left_leg", ModelPartBuilder.create().uv(54,10).cuboid(-1,0,-1,2,10,2), ModelTransform.pivot(3.3f,12.5f,0));
            ll.addChild("freddy_left_leg_pad", ModelPartBuilder.create().uv(48,39).cuboid(-3,0,-3,6,9,6), ModelTransform.pivot(0,0.5f,0));
            ModelPartData ll2 = ll.addChild("freddy_left_leg2", ModelPartBuilder.create().uv(72,48).cuboid(-1,0,-1,2,8,2), ModelTransform.of(0,9.6f,0,0.03490659f,0,0));
            ll2.addChild("freddy_left_leg_pad2", ModelPartBuilder.create().uv(16,50).cuboid(-2.5f,0,-3,5,7,6), ModelTransform.pivot(0,0.5f,0));
            ll2.addChild("freddy_left_foot", ModelPartBuilder.create().uv(72,50).cuboid(-2.5f,0,-6,5,3,8), ModelTransform.of(0,8,0,-0.03490659f,0,0));
            fb.addChild("freddy_crotch", ModelPartBuilder.create().uv(56,0).cuboid(-5.5f,0,-3.5f,11,3,7), ModelTransform.pivot(0,9.5f,0));
            ModelPartData fh = fb.addChild("freddy_head", ModelPartBuilder.create().uv(39,22).cuboid(-5.5f,-8,-4.5f,11,8,9), ModelTransform.pivot(0,-13,-0.5f));
            fh.addChild("freddy_jaw", ModelPartBuilder.create().uv(49,65).cuboid(-5,0,-4.5f,10,3,9), ModelTransform.of(0,0.5f,0,0.08726646f,0,0));
            fh.addChild("freddy_nose", ModelPartBuilder.create().uv(17,67).cuboid(-4,-2,-3,8,4,3), ModelTransform.pivot(0,-2,-4.5f));
            ModelPartData er = fh.addChild("freddy_ear_right", ModelPartBuilder.create().uv(8,0).cuboid(-1,-3,-0.5f,2,3,1), ModelTransform.of(-4.5f,-5.5f,0,0.05235988f,0,-1.0471976f));
            er.addChild("freddy_ear_right_pad", ModelPartBuilder.create().uv(85,0).cuboid(-2,-5,-1,4,4,2), ModelTransform.pivot(0,-1,0));
            ModelPartData el = fh.addChild("freddy_ear_left", ModelPartBuilder.create().uv(40,0).cuboid(-1,-3,-0.5f,2,3,1), ModelTransform.of(4.5f,-5.5f,0,0.05235988f,0,1.0471976f));
            el.addChild("freddy_ear_left_pad", ModelPartBuilder.create().uv(40,39).cuboid(-2,-5,-1,4,4,2), ModelTransform.pivot(0,-1,0));
            ModelPartData hat = fh.addChild("freddy_hat", ModelPartBuilder.create().uv(70,24).cuboid(-3,-0.5f,-3,6,1,6), ModelTransform.of(0,-8.4f,0,-0.0174533f,0,0));
            hat.addChild("freddy_hat2", ModelPartBuilder.create().uv(78,61).cuboid(-2,-4,-2,4,4,4), ModelTransform.of(0,0.1f,0,-0.0174533f,0,0));
            return TexturedModelData.of(md, 100, 80).createModel();
        }
    }

    // ==================== AMOGUS ====================
    private static final class AmogusModel implements CustomModel {
        private final ModelPart root, leftLeg, rightLeg;
        private AmogusModel(ModelPart root) {
            this.root = root;
            this.leftLeg = root.getChild("amogus_left_leg");
            this.rightLeg = root.getChild("amogus_right_leg");
        }
        @Override public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            copyAngles(leftLeg, this.leftLeg, 0,0,0);
            copyAngles(rightLeg, this.rightLeg, 0,0,0);
        }
        @Override public void render(MatrixStack m, VertexConsumer v, int l, int o, int c) { root.render(m, v, l, o, c); }
        static ModelPart createModel() {
            ModelData md = new ModelData(); ModelPartData root = md.getRoot();
            root.addChild("amogus_body", ModelPartBuilder.create().uv(34,8).cuboid(-4,6,-3,8,12,6).uv(15,10).cuboid(-3,9,3,6,8,3).uv(26,0).cuboid(-3,5,-3,6,1,6), ModelTransform.pivot(0,0,0));
            root.addChild("amogus_eye", ModelPartBuilder.create().uv(0,10).cuboid(-3,7,-4,6,4,1), ModelTransform.pivot(0,0,0));
            root.addChild("amogus_left_leg", ModelPartBuilder.create().uv(0,0).cuboid(2.9f,0,-1.5f,3,6,3), ModelTransform.pivot(-2,18,0));
            root.addChild("amogus_right_leg", ModelPartBuilder.create().uv(13,0).cuboid(-5.9f,0,-1.5f,3,6,3), ModelTransform.pivot(2,18,0));
            return TexturedModelData.of(md, 64, 64).createModel();
        }
    }
}
