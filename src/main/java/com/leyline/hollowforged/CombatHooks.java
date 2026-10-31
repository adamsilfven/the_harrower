package com.leyline.hollowforged;
import java.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
/** Damage stages are distinct: offensive multiplier, hit-local armor, post-armor defenses, death fallback. */
public final class CombatHooks {
 public record Strike(Player player,Entity target,boolean charged,boolean armored,float health){}
 private static final ThreadLocal<ArrayDeque<Strike>> STRIKES=ThreadLocal.withInitial(ArrayDeque::new);
 private static final ThreadLocal<Integer> SECONDARY=ThreadLocal.withInitial(()->0);
 public static void secondary(Runnable work){SECONDARY.set(SECONDARY.get()+1);try{work.run();}finally{SECONDARY.set(SECONDARY.get()-1);}}
 public static boolean secondary(){return SECONDARY.get()>0;}
 public static void begin(Player p,Entity target){float health=target instanceof LivingEntity l?l.getHealth()+l.getAbsorptionAmount():0;boolean charged=HFState.hammer(p)&&p.getAttackStrengthScale(.5F)>=.9F;boolean armored=target instanceof LivingEntity l&&(l.isBlocking()||l.getArmorValue()>=15);STRIKES.get().push(new Strike(p,target,charged,armored,health));if(!p.level().isClientSide()&&charged&&target instanceof LivingEntity l&&l.isBlocking())CombatAbilities.shield(l);}
 public static boolean charged(Player p,LivingEntity target){var s=STRIKES.get().peek();return !secondary()&&s!=null&&s.player()==p&&s.target()==target&&s.charged();}
 public static boolean chargedArmored(Player p,LivingEntity target){var s=STRIKES.get().peek();return charged(p,target)&&s.armored();}
 public static void end(Player p){
  var stack=STRIKES.get();if(stack.isEmpty())return;var s=stack.pop();
  if(!p.level().isClientSide()&&s.charged()&&s.target() instanceof LivingEntity target){
   float dealt=Math.max(0,s.health()-target.getHealth()-target.getAbsorptionAmount());
   if(dealt>0){
    CombatAbilities.launch(target,p.position(),2,.6);CombatAbilities.shield(target);
    secondary(()->{for(var enemy:CombatAbilities.enemies(p,target.position(),4))if(enemy!=target)enemy.hurt(p.damageSources().playerAttack(p),dealt*.4F);});
    HFEffects.burst(p,HFEffects.MELEE,target.position(),4,14);
   }
  }
  if(stack.isEmpty())STRIKES.remove();
 }
 public static boolean melee(DamageSource source){return source.getEntity() instanceof LivingEntity&&source.getDirectEntity()==source.getEntity()&&!source.is(DamageTypeTags.IS_PROJECTILE)&&!secondary();}
 public static float armorMultiplier(LivingEntity victim,DamageSource source){
  if(HFState.voidDamage(source))return 1;float multiplier=HFState.active(victim,"shattered")?.5F:1;
  if(source.getEntity() instanceof Player p){
   if(HFState.crown(p)){
    if(HFState.active(victim,HFState.mark(p,"exposed")))multiplier*=.6F;
    if(HFState.active(victim,HFState.mark(p,"witness")))multiplier*=.5F;
   }
   if(charged(p,victim))multiplier*=.5F;
  }
  return multiplier;
 }
 private static void incoming(LivingIncomingDamageEvent event){
  var victim=event.getEntity();var source=event.getSource();if(HFState.voidDamage(source))return;
  if(HFState.chest(victim)&&HFState.active(victim,"refusal_protection")){event.setCanceled(true);return;}
  float damage=event.getAmount();
  if(source.getEntity() instanceof Player p){
   if(HFState.crown(p)){
    if(HFState.active(victim,HFState.mark(p,"exposed")))damage*=1.3F;
    if(HFState.active(victim,HFState.mark(p,"witness")))damage*=1.5F;
   }
   if(HFState.legs(p)&&HFState.data(p).getBoolean("anchor")&&melee(source))damage*=1.25F;
   if(chargedArmored(p,victim))damage*=1.5F;
  }
  event.setAmount(damage);
 }
 private static void reduced(LivingDamageEvent.Pre event){
  var victim=event.getEntity();if(HFState.voidDamage(event.getSource()))return;
  float damage=event.getNewDamage();
  if(HFState.chest(victim))damage*=HFState.active(victim,"unbroken")?.2F:.7F;
  if(HFState.legs(victim)&&HFState.data(victim).getBoolean("anchor")&&melee(event.getSource()))damage*=.75F;
  if(HFState.chest(victim)&&HFState.active(victim,"unbroken")&&damage>0&&HFState.data(victim).getLong("fx_hit_after")<=HFState.now(victim)){HFState.data(victim).putLong("fx_hit_after",HFState.now(victim)+6);HFEffects.burst(victim,HFEffects.UNBROKEN,victim.position().add(0,1,0),.7,8);}
  event.setNewDamage(damage);
 }
 private static void death(LivingDeathEvent event){
  var e=event.getEntity();if(e.level().isClientSide()||HFState.voidDamage(event.getSource())||!HFState.chest(e)||HFState.active(e,"refusal_cd"))return;
  HFState.until(e,"refusal_cd",6000);HFState.until(e,"refusal_protection",160);event.setCanceled(true);e.setHealth(e.getMaxHealth()*.5F);
  for(var effect:List.copyOf(e.getActiveEffects()))if(effect.getEffect().value().getCategory()==MobEffectCategory.HARMFUL)e.removeEffect(effect.getEffect());
  e.clearFire();HFEffects.burst(e,HFEffects.REFUSAL,e.position(),3,20);
 }
 private static void knockback(LivingKnockBackEvent event){if(HFState.forceImmune(event.getEntity()))event.setCanceled(true);}
 private static void explosion(ExplosionKnockbackEvent event){if(HFState.forceImmune(event.getAffectedEntity()))event.setKnockbackVelocity(Vec3.ZERO);}
 private static final net.minecraft.resources.ResourceLocation SLOW=Hollowforged.id("zone_slow"),STUN=Hollowforged.id("stun");
 public static void slow(LivingEntity e,float amount,int ticks){if(HFState.controlImmune(e))return;var d=HFState.data(e);if(!HFState.active(e,"slow")||amount>=d.getFloat("slow_amount")){d.putFloat("slow_amount",amount);HFState.until(e,"slow",ticks);}}
 public static void stun(LivingEntity e,int ticks){if(HFState.controlImmune(e)||HFState.forceImmune(e))return;HFState.until(e,"stun",ticks);HFEffects.mark(e,HFEffects.STUN,ticks);e.stopUsingItem();if(e instanceof Mob m)m.getNavigation().stop();}
 public static boolean stunned(Entity e){return e instanceof LivingEntity l&&!HFState.controlImmune(l)&&!HFState.forceImmune(l)&&HFState.active(e,"stun");}
 private static void entityTick(EntityTickEvent.Pre event){
  if(!(event.getEntity() instanceof LivingEntity e)||e.level().isClientSide())return;
  var movement=e.getAttribute(Attributes.MOVEMENT_SPEED);if(movement==null)return;
  setModifier(movement,SLOW,HFState.active(e,"slow")?-HFState.data(e).getFloat("slow_amount"):0);
  setModifier(movement,STUN,stunned(e)?-1:0);
  if(stunned(e))e.stopUsingItem();
  if(HFState.now(e)%200==0){var d=HFState.data(e);for(String key:List.copyOf(d.getAllKeys()))if((key.startsWith("witness_")||key.startsWith("exposed_"))&&d.getLong(key)<=HFState.now(e))d.remove(key);}
 }
 public static void setModifier(AttributeInstance attribute,net.minecraft.resources.ResourceLocation key,double amount){
  if(attribute==null)return;var existing=attribute.getModifier(key);if(existing!=null&&Math.abs(existing.amount()-amount)<.000001)return;
  attribute.removeModifier(key);if(amount!=0)attribute.addTransientModifier(new AttributeModifier(key,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
 }
 public static void register(){
  NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL,CombatHooks::incoming);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CombatHooks::reduced);
  NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CombatHooks::death);NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,CombatHooks::knockback);NeoForge.EVENT_BUS.addListener(CombatHooks::explosion);NeoForge.EVENT_BUS.addListener(CombatHooks::entityTick);
 }
}
