package com.idk.biomegetter.entity.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;


public class BabyUnicornEntityModel extends UnicornEntityModel {

    public BabyUnicornEntityModel(ModelPart root) {
        super(root);
//        this.babyWalk = BabyUnicorn_Animation.WALK.bake(root); // своя анимация, свой файл-константа
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition F_leg_L = partdefinition.addOrReplaceChild("F_leg_L", CubeListBuilder.create(), PartPose.offset(3.0F, 10.0F, -4.5F));

        PartDefinition f_l_l_1 = F_leg_L.addOrReplaceChild("f_l_l_1", CubeListBuilder.create().texOffs(14, 27).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 2.0F, 0.0F));

        PartDefinition f_l_l_2 = f_l_l_1.addOrReplaceChild("f_l_l_2", CubeListBuilder.create().texOffs(38, 9).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.0F, 0.5F));

        PartDefinition f_l_l_3 = f_l_l_2.addOrReplaceChild("f_l_l_3", CubeListBuilder.create().texOffs(46, 9).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(22, 25).addBox(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 33).addBox(-1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.5F));

        PartDefinition F_leg_R = partdefinition.addOrReplaceChild("F_leg_R", CubeListBuilder.create(), PartPose.offset(-3.0F, 10.0F, -4.5F));

        PartDefinition f_l_r_1 = F_leg_R.addOrReplaceChild("f_l_r_1", CubeListBuilder.create().texOffs(26, 27).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

        PartDefinition f_l_r_2 = f_l_r_1.addOrReplaceChild("f_l_r_2", CubeListBuilder.create().texOffs(38, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 4.0F, 0.5F));

        PartDefinition f_l_r_3 = f_l_r_2.addOrReplaceChild("f_l_r_3", CubeListBuilder.create().texOffs(10, 39).addBox(1.0F, 0.0F, 0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(46, 13).addBox(1.0F, 0.0F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(22, 42).addBox(3.0F, 0.0F, 0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 6.0F, 0.0F));

        PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -5.0F, -6.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-2.0F, -5.0F, 0.0F, 4.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(12, 36).addBox(-0.5F, 1.0F, -6.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, -1.0F));

        PartDefinition cube_r1 = Body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(48, 5).addBox(0.0F, -1.0F, -2.0F, 0.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition cube_r2 = Body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(26, 45).addBox(1.0F, -2.0F, -2.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -5.0F, -0.4F, 1.5708F, 0.0F, 0.0F));

        PartDefinition Tail = Body.addOrReplaceChild("Tail", CubeListBuilder.create(), PartPose.offset(-0.5F, -5.0F, 8.0F));

        PartDefinition tail_1 = Tail.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(18, 42).addBox(0.0F, -1.0F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(38, 31).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

        PartDefinition tail_2 = tail_1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(0, 39).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(8, 48).addBox(0.5F, -1.5F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 0.5F, 4.0F, -0.1309F, 0.0F, 0.0F));

        PartDefinition tail_3 = tail_2.addOrReplaceChild("tail_3", CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, -0.1745F, 0.0F, 0.0F));

        PartDefinition tail_brush = tail_3.addOrReplaceChild("tail_brush", CubeListBuilder.create().texOffs(40, 36).addBox(-0.5F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.48F, 0.0F, 0.0F));

        PartDefinition B_leg_R = partdefinition.addOrReplaceChild("B_leg_R", CubeListBuilder.create(), PartPose.offset(-2.0F, 14.0F, 5.0F));

        PartDefinition b_l_r_1 = B_leg_R.addOrReplaceChild("b_l_r_1", CubeListBuilder.create().texOffs(24, 18).addBox(-1.6F, -2.0F, -2.3F, 3.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, 0.5F));

        PartDefinition b_l_r_2 = b_l_r_1.addOrReplaceChild("b_l_r_2", CubeListBuilder.create().texOffs(32, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.0F, 0.5F));

        PartDefinition b_l_r_3 = b_l_r_2.addOrReplaceChild("b_l_r_3", CubeListBuilder.create().texOffs(32, 45).addBox(-0.4F, 0.0F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(8, 46).addBox(1.6F, 0.0F, 0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 44).addBox(-0.4F, 0.0F, 0.5F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.6F, 7.0F, 0.0F));

        PartDefinition B_leg_L = partdefinition.addOrReplaceChild("B_leg_L", CubeListBuilder.create(), PartPose.offset(2.0F, 10.0F, 5.0F));

        PartDefinition b_l_l_1 = B_leg_L.addOrReplaceChild("b_l_l_1", CubeListBuilder.create().texOffs(24, 9).addBox(0.0F, -2.0F, -2.3F, 3.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.4F, 2.0F, 0.5F));

        PartDefinition b_l_l_2 = b_l_l_1.addOrReplaceChild("b_l_l_2", CubeListBuilder.create().texOffs(24, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.4F, 3.0F, 0.5F));

        PartDefinition b_l_l_3 = b_l_l_2.addOrReplaceChild("b_l_l_3", CubeListBuilder.create().texOffs(40, 5).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(12, 48).addBox(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(46, 29).addBox(-1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, 0.5F));

        PartDefinition neck_and_head = partdefinition.addOrReplaceChild("neck_and_head", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, -3.4F));

        PartDefinition neck_1 = neck_and_head.addOrReplaceChild("neck_1", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r3 = neck_1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -5.0F, -1.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -2.6F, 0.3491F, 0.0F, 0.0F));

        PartDefinition cube_r4 = neck_1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(18, 45).addBox(1.5F, -2.0F, -2.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.0F, 0.0F, 0.3491F, 0.0F, 0.0F));

        PartDefinition neck_2 = neck_1.addOrReplaceChild("neck_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.0F, -2.6F, -0.3927F, 0.0F, 0.0F));

        PartDefinition cube_r5 = neck_2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(40, 41).addBox(1.5F, -2.0F, -2.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.6F, -1.4F, 1.1345F, 0.0F, 0.0F));

        PartDefinition cube_r6 = neck_2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 25).addBox(-0.5F, -4.0F, -1.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.4F, -1.0F, 1.1345F, 0.0F, 0.0F));

        PartDefinition head = neck_2.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.3F, -3.7F, -0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r7 = head.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(46, 23).addBox(0.5F, -4.4838F, -0.7372F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -0.3F, -1.4F, 1.1781F, 0.0F, 0.0F));

        PartDefinition mouth_1_r1 = head.addOrReplaceChild("mouth_1_r1", CubeListBuilder.create().texOffs(10, 42).addBox(-0.5F, -4.2628F, -0.4838F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 4.7F, -3.4F, -0.3927F, 0.0F, 0.0F));

        PartDefinition cube_r8 = head.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 33).addBox(-2.0F, -1.2628F, -0.4838F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.7F, -1.4F, -0.3927F, 0.0F, 0.0F));

        PartDefinition cube_r9 = head.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(39, 18).addBox(1.5F, -2.0393F, -1.8991F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -1.0F, 0.6F, 1.8326F, 0.0F, 0.0F));

        PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create(), PartPose.offset(0.0F, 3.0F, -1.7F));

        PartDefinition mouth_2_r1 = mouth.addOrReplaceChild("mouth_2_r1", CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, -0.2F, -0.4F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition ears = head.addOrReplaceChild("ears", CubeListBuilder.create(), PartPose.offset(0.9F, 0.0F, -0.9F));

        PartDefinition cube_r10 = ears.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(18, 25).addBox(2.9F, -1.5164F, -0.7591F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(14, 25).addBox(0.1F, -1.5164F, -0.7591F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.9F, 0.0F, 0.0F, 1.2217F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

//    @Override
//    public void setupAnim(UnicornEntityRendererState state) {
//        super.setupAnim(state);
//
//        if (state.walkAnimationState.isStarted()) {
//            this.walk.apply(state.walkAnimationState, state.ageInTicks);
//        }
//    }
}
