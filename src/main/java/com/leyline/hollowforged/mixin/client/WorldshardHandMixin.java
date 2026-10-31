package com.leyline.hollowforged.mixin.client;
import com.leyline.hollowforged.Hollowforged;
import com.leyline.hollowforged.client.WorldshardGrip;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandLayer.class)
public abstract class WorldshardHandMixin {
 @Inject(method="renderArmWithItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
 private void hollowforged$endGrip(LivingEntity entity,ItemStack item,ItemDisplayContext context,HumanoidArm arm,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
  if(item.is(Hollowforged.WORLDSHARD.get())&&((ItemInHandLayer<?,?>)(Object)this).getParentModel() instanceof HumanoidModel<?> model)WorldshardGrip.thirdPerson(pose,model,entity,arm);
 }
}
