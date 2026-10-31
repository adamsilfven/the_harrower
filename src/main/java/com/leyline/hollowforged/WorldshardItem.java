package com.leyline.hollowforged;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
public final class WorldshardItem extends Item {
 public WorldshardItem(Properties properties){super(properties);}
 @Override public int getEnchantmentValue(){return 20;}
 @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.fail(player.getItemInHand(hand));
  player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
 }
 @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
  if(!level.isClientSide()&&72000-remaining==60){HFEffects.burst(entity,HFEffects.READY,entity.position(),1,10);if(entity instanceof Player p)p.displayClientMessage(Component.literal("The Last Argument is ready — release to strike"),true);}
 }
 @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
  if(entity instanceof ServerPlayer p&&HFState.hammer(p))CombatAbilities.releaseHammer(p,72000-remaining);
 }
}
