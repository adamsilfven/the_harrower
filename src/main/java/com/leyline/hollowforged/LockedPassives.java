package com.leyline.hollowforged;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Locked, independent passives only. No landing attack or pending balance choices. */
public final class LockedPassives {
 private static final AttributeModifier SPEED=new AttributeModifier(Hollowforged.id("treads_speed"),.30,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
 private static final List<Holder<MobEffect>> BLOCKABLE=List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.POISON,MobEffects.WITHER,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.DIG_SLOWDOWN);
 public static void register(){
  NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,LockedPassives::applicable);
  NeoForge.EVENT_BUS.addListener(LockedPassives::tick);
  NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,LockedPassives::incoming);
 }
 static boolean blocks(LivingEntity entity,Holder<MobEffect> effect){
  if(HFState.chest(entity)&&HFState.active(entity,"unbroken")&&effect.is(HFState.STUN_EFFECTS))return true;
  if(entity.getItemBySlot(EquipmentSlot.HEAD).is(Hollowforged.CROWN.get())&&(effect.equals(MobEffects.BLINDNESS)||effect.equals(MobEffects.DARKNESS)))return true;
  if(entity.getItemBySlot(EquipmentSlot.CHEST).is(Hollowforged.CARAPACE.get())&&(effect.equals(MobEffects.POISON)||effect.equals(MobEffects.WITHER)))return true;
  return entity.getItemBySlot(EquipmentSlot.LEGS).is(Hollowforged.GREAVES.get())&&(effect.equals(MobEffects.MOVEMENT_SLOWDOWN)||effect.equals(MobEffects.DIG_SLOWDOWN));
 }
 private static void applicable(MobEffectEvent.Applicable event){
  if(blocks(event.getEntity(),event.getEffectInstance().getEffect()))event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
 }
 static void update(LivingEntity entity){
  var movement=entity.getAttribute(Attributes.MOVEMENT_SPEED);
  boolean boots=entity.getItemBySlot(EquipmentSlot.FEET).is(Hollowforged.TREADS.get());
  if(movement!=null){
   if(boots&&!movement.hasModifier(SPEED.id()))movement.addTransientModifier(SPEED);
   else if(!boots)movement.removeModifier(SPEED.id());
  }
  for(var effect:BLOCKABLE)if(blocks(entity,effect))entity.removeEffect(effect);
  if(HFState.chest(entity)){entity.clearFire();if(HFState.active(entity,"unbroken"))for(var effect:List.copyOf(entity.getActiveEffects()))if(effect.getEffect().is(HFState.STUN_EFFECTS))entity.removeEffect(effect.getEffect());}
 }
 private static void tick(PlayerTickEvent.Pre event){if(!event.getEntity().level().isClientSide())update(event.getEntity());}
 private static void incoming(LivingIncomingDamageEvent event){
  var source=event.getSource();var entity=event.getEntity();
  // Void is never intercepted, including by future custom damage tags.
  if(HFState.voidDamage(source))return;
  if(entity.getItemBySlot(EquipmentSlot.FEET).is(Hollowforged.TREADS.get())&&source.is(DamageTypeTags.IS_FALL))event.setCanceled(true);
  if(entity.getItemBySlot(EquipmentSlot.CHEST).is(Hollowforged.CARAPACE.get())&&(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypes.WITHER)))event.setCanceled(true);
 }
}
