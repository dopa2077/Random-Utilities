package com.dopa.randomutilities.minichest.client;

import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class MiniChestModel extends Model<Float> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "mini_chest"), "main");

    private final ModelPart lid;
    private final ModelPart lock;

    public MiniChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Half-size chest in block space (centered 8×8×8), front toward +Z like vanilla.
        root.addOrReplaceChild(
                "bottom",
                CubeListBuilder.create().texOffs(0, 12).addBox(4.0F, 0.0F, 4.0F, 8.0F, 5.0F, 8.0F),
                PartPose.ZERO
        );
        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, -1.0F, 0.0F, 8.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 5.0F, 4.0F)
        );
        root.addOrReplaceChild(
                "lock",
                CubeListBuilder.create().texOffs(0, 25).addBox(7.0F, -2.0F, 8.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 5.0F, 4.0F)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Float open) {
        super.setupAnim(open);
        // resetPose() already restored the hinge PartPose; only add anim deltas on top.
        this.lid.xRot = -(open * (float) (Math.PI / 2.0));
        this.lock.xRot = this.lid.xRot;
        // Blockbench open anim is +1 Z, but BB faces -Z while this model faces +Z like vanilla,
        // so the hinge nudge must be mirrored.
        this.lid.z -= open;
        this.lock.z -= open;
    }
}
