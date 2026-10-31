package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.HFState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HoneyBlock.class)
public abstract class HoneyMixin {
 @Inject(method="entityInside",at=@At("HEAD"),cancellable=true)private void hollowforged$honey(BlockState s,Level l,BlockPos p,Entity e,CallbackInfo ci){if(e instanceof LivingEntity living&&HFState.legs(living))ci.cancel();}
}
