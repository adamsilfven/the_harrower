package com.leyline.hollowforged.mixin.client;
import com.leyline.hollowforged.HFState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LightTexture.class)
public abstract class DarkVisionMixin {
 @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
 private boolean hollowforged$vision(LocalPlayer player,Holder<MobEffect> effect){return effect.equals(MobEffects.NIGHT_VISION)&&HFState.crown(player)||player.hasEffect(effect);}
 @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
 private float hollowforged$steady(LivingEntity player,float partial){return HFState.crown(player)?1F:GameRenderer.getNightVisionScale(player,partial);}
}
