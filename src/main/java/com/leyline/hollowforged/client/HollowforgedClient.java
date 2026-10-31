package com.leyline.hollowforged.client;
import java.util.EnumMap;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
@EventBusSubscriber(modid=Hollowforged.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HollowforgedClient {
 private static final EnumMap<EquipmentSlot,HumanoidModel<?>> MODELS=new EnumMap<>(EquipmentSlot.class);
 private static ModelLayerLocation layer(EquipmentSlot slot){return new ModelLayerLocation(Hollowforged.id("armor_"+slot.getName()),"main");}
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){for(var s:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})e.registerLayerDefinition(layer(s),()->HollowArmorGeometry.layer(s));}
 @SubscribeEvent public static void reloaded(EntityRenderersEvent.AddLayers e){MODELS.clear();}
 @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent e){
  e.registerItem(new IClientItemExtensions(){
   @Override public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.player.LocalPlayer p,net.minecraft.world.entity.HumanoidArm arm,ItemStack item,float partial,float equip,float swing){
    float sign=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
    // Match the reference hammer's quick vanilla-style diagonal first-person sweep.
    // This is visual only; third-person motion and the approved rest anchor are unchanged.
    float root=net.minecraft.util.Mth.sqrt(swing);
    float arc=net.minecraft.util.Mth.sin(root*net.minecraft.util.Mth.PI);
    pose.translate(sign*(.42F-.40F*arc),-.60F-equip*.6F+.20F*net.minecraft.util.Mth.sin(root*2*net.minecraft.util.Mth.PI),-.78F-.20F*net.minecraft.util.Mth.sin(swing*net.minecraft.util.Mth.PI));
    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(sign*-20F*net.minecraft.util.Mth.sin(swing*swing*net.minecraft.util.Mth.PI)));
    pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(sign*-20F*arc));
    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-80F*arc));
    WorldshardGrip.firstPerson(pose,arm,HammerSwingCurve.sample(0));return true;
   }
  },Hollowforged.WORLDSHARD.get());
  e.registerItem(new IClientItemExtensions(){
   @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){var model=(LivingMountainModel)MODELS.computeIfAbsent(slot,key->new LivingMountainModel(HollowArmorGeometry.bake(key),key));model.prepare(entity,Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));return model;}
  },Hollowforged.CROWN.get(),Hollowforged.CARAPACE.get(),Hollowforged.GREAVES.get(),Hollowforged.TREADS.get());
 }
}
