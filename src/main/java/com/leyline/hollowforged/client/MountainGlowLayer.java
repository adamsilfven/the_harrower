package com.leyline.hollowforged.client;
import com.leyline.hollowforged.Hollowforged;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
/** A masked fullbright layer shows only the deep chest cleft, not the stone shell. */
public final class MountainGlowLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
 private final LivingMountainModel glow;
 public MountainGlowLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);glow=new LivingMountainModel(HollowArmorGeometry.bake(EquipmentSlot.CHEST),EquipmentSlot.CHEST);}
 @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer p,float swing,float amount,float partial,float age,float yaw,float pitch){
  if(p.isInvisible()||!p.getItemBySlot(EquipmentSlot.CHEST).is(Hollowforged.CARAPACE.get()))return;
  var parent=getParentModel();glow.head.copyFrom(parent.head);glow.hat.copyFrom(parent.hat);glow.body.copyFrom(parent.body);glow.leftArm.copyFrom(parent.leftArm);glow.rightArm.copyFrom(parent.rightArm);glow.leftLeg.copyFrom(parent.leftLeg);glow.rightLeg.copyFrom(parent.rightLeg);glow.prepare(p,partial);
  int alpha=145+(int)(45*Math.sin((p.tickCount+partial)*.052));int color=(alpha<<24)|0xE8E4CD;
  glow.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucentEmissive(Hollowforged.id("textures/models/armor/hollowforged_glow.png"))),15728880,OverlayTexture.NO_OVERLAY,color);
 }
}
