package com.leyline.harrower.combat;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.EffectCure;
import com.leyline.harrower.HarrowerMod;

/** Option A: timed beneficial MobEffectInstances only. No skills/equipment/healing hook.
 * Durations age while suppressed; offline time ages only through the suppression deadline.
 * No effect tick callbacks run on stored instances. Full hidden chains/cures/flags are serialized.
 */
@EventBusSubscriber(modid=HarrowerMod.ID)
public final class HollowingEffects {
 private static final String KEY="lc_harrower_hollowing";
 private static boolean internal;
 private static long now(ServerPlayer p){return p.server.overworld().getGameTime();}
 public static boolean suppressed(ServerPlayer p){var data=p.getPersistentData().getCompound(KEY);return data.getLong("Until")>now(p);}
 public static void suppress(ServerPlayer p,int ticks){
  restoreIfExpired(p);CompoundTag data=p.getPersistentData().getCompound(KEY);data.putLong("Until",Math.max(data.getLong("Until"),now(p)+ticks));if(!data.contains("Effects"))data.put("Effects",new ListTag());p.getPersistentData().put(KEY,data);
  for(var effect:new ArrayList<>(p.getActiveEffects()))if(beneficial(effect)){
   capture(p,effect);internal=true;try{p.removeEffect(effect.getEffect());}finally{internal=false;}
  }
 }
 private static boolean beneficial(MobEffectInstance effect){return effect.getEffect().value().getCategory()==MobEffectCategory.BENEFICIAL&&!effect.getEffect().value().isInstantenous();}
 private static void capture(ServerPlayer p,MobEffectInstance incoming){
  CompoundTag data=p.getPersistentData().getCompound(KEY);ListTag records=data.getList("Effects",Tag.TAG_COMPOUND);CompoundTag saved=(CompoundTag)incoming.save();String id=saved.getString("id");long time=now(p);
  for(int i=0;i<records.size();i++){
   CompoundTag r=records.getCompound(i);if(!r.getCompound("Effect").getString("id").equals(id))continue;
   CompoundTag aged=age(r.getCompound("Effect"),(int)Math.min(Integer.MAX_VALUE,time-r.getLong("Captured")));
   MobEffectInstance previous=aged==null?null:MobEffectInstance.load(aged);if(previous!=null){previous.update(incoming);saved=(CompoundTag)previous.save();}records.remove(i);break;
  }
  CompoundTag record=new CompoundTag();record.put("Effect",saved);record.putLong("Captured",time);records.add(record);data.put("Effects",records);p.getPersistentData().put(KEY,data);
 }
 private static CompoundTag age(CompoundTag saved,int elapsed){String id=saved.getString("id");CompoundTag aged=ageDetails(saved.copy(),Math.max(0,elapsed));if(aged!=null)aged.putString("id",id);return aged;}
 private static CompoundTag ageDetails(CompoundTag tag,int ticks){
  int duration=tag.getInt("duration");if(duration!=-1)tag.putInt("duration",Math.max(0,duration-ticks));
  if(tag.contains("hidden_effect",Tag.TAG_COMPOUND)){CompoundTag hidden=ageDetails(tag.getCompound("hidden_effect"),ticks);if(hidden==null)tag.remove("hidden_effect");else tag.put("hidden_effect",hidden);}
  if(tag.getInt("duration")==0)return tag.contains("hidden_effect",Tag.TAG_COMPOUND)?tag.getCompound("hidden_effect"):null;return tag;
 }
 private static void restoreIfExpired(ServerPlayer p){
  if(!p.getPersistentData().contains(KEY))return;CompoundTag data=p.getPersistentData().getCompound(KEY);long until=data.getLong("Until");if(now(p)<until)return;
  ListTag records=data.getList("Effects",Tag.TAG_COMPOUND);p.getPersistentData().remove(KEY);internal=true;
  try{for(int i=0;i<records.size();i++){CompoundTag record=records.getCompound(i);CompoundTag aged=age(record.getCompound("Effect"),(int)Math.min(Integer.MAX_VALUE,Math.max(0,until-record.getLong("Captured"))));if(aged!=null){MobEffectInstance effect=MobEffectInstance.load(aged);if(effect!=null)p.addEffect(effect);}}}finally{internal=false;}
 }
 /** Standard removal by effect ID also clears the suspended copy, including null active instances. */
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void applicable(MobEffectEvent.Applicable event){
  if(internal||!(event.getEntity() instanceof ServerPlayer p)||!suppressed(p)||!beneficial(event.getEffectInstance())||!event.getApplicationResult())return;
  capture(p,event.getEffectInstance());event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void remove(MobEffectEvent.Remove event){
  if(internal||event.isCanceled()||!(event.getEntity() instanceof ServerPlayer p)||!p.getPersistentData().contains(KEY))return;
  var data=p.getPersistentData().getCompound(KEY);var records=data.getList("Effects",Tag.TAG_COMPOUND);
  for(int i=records.size()-1;i>=0;i--){var instance=MobEffectInstance.load(records.getCompound(i).getCompound("Effect"));if(instance!=null&&instance.getEffect().equals(event.getEffect()))records.remove(i);}
 }
 /** Explicit compatibility hook for curatives that operate outside standard effect removal. */
 public static void cure(ServerPlayer p,EffectCure cure){
  var data=p.getPersistentData().getCompound(KEY);var records=data.getList("Effects",Tag.TAG_COMPOUND);
  for(int i=records.size()-1;i>=0;i--){var effect=MobEffectInstance.load(records.getCompound(i).getCompound("Effect"));if(cure==null||effect!=null&&effect.getCures().contains(cure))records.remove(i);}
 }
 @SubscribeEvent public static void consumed(LivingEntityUseItemEvent.Finish event){if(event.getEntity() instanceof ServerPlayer p&&event.getItem().is(Items.MILK_BUCKET))cure(p,null);}
 @SubscribeEvent public static void tick(PlayerTickEvent.Pre event){if(event.getEntity() instanceof ServerPlayer p){restoreIfExpired(p);if(suppressed(p))for(var effect:new ArrayList<>(p.getActiveEffects()))if(beneficial(effect)){capture(p,effect);internal=true;try{p.removeEffect(effect.getEffect());}finally{internal=false;}}}}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)restoreIfExpired(p);}
 @SubscribeEvent public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)p.getPersistentData().remove(KEY);}
 @SubscribeEvent public static void clone(PlayerEvent.Clone event){event.getEntity().getPersistentData().remove(KEY);}
 public static String status(ServerPlayer p){return p.getPersistentData().getCompound(KEY).toString();}
}
