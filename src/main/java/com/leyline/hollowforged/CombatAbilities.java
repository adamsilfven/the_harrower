package com.leyline.hollowforged;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
/** All targets, geometry, timers and damage are selected on the logical server. */
public final class CombatAbilities {
 public static final TagKey<EntityType<?>> HOSTILES=TagKey.create(Registries.ENTITY_TYPE,Hollowforged.id("hostiles"));
 private static final ResourceLocation REACH=Hollowforged.id("sovereign_reach"),WINDUP=Hollowforged.id("argument_windup");
 public static List<LivingEntity> enemies(LivingEntity owner,Vec3 center,double radius){
  var list=owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->e!=owner&&HFState.hostile(e)&&e.position().distanceToSqr(center)<=radius*radius&&!owner.isAlliedTo(e));
  list.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(center)));return list.size()>512?list.subList(0,512):list;
 }
 public static void ring(LivingEntity source,Vec3 center,double radius,int points){
  HFEffects.burst(source,HFEffects.FALL,center,radius,18);
 }
 public static void launch(LivingEntity enemy,Vec3 center,double outward,double up){
  if(HFState.controlImmune(enemy)||HFState.forceImmune(enemy))return;
  double resistance=1-enemy.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);Vec3 direction=enemy.position().subtract(center).multiply(1,0,1);if(direction.lengthSqr()<.00001)direction=new Vec3(1,0,0);direction=direction.normalize();
  enemy.push(direction.x*outward*resistance,up*resistance,direction.z*outward*resistance);enemy.hurtMarked=true;
 }
 public static void shield(LivingEntity victim){if(victim instanceof Player player&&player.isBlocking()){var stack=player.getUseItem();player.getCooldowns().addCooldown(stack.getItem(),100);player.stopUsingItem();player.level().broadcastEntityEvent(player,(byte)30);}else if(victim.isBlocking())victim.stopUsingItem();}
 public static boolean supported(Player p){return p.onGround()&&!p.isInWater()&&!p.isInLava()&&p.getY()>p.level().getMinBuildHeight()&&!p.level().getBlockState(p.blockPosition().below()).getCollisionShape(p.level(),p.blockPosition().below()).isEmpty();}
 public static boolean activate(ServerPlayer p,int ability){return activate(p,ability,false);}
 public static void releaseHammer(ServerPlayer p,int heldTicks){
  if(Boolean.getBoolean("lc_hollowforged.review"))com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_HAMMER_RELEASE ticks={}",heldTicks);
  if(heldTicks<12)activate(p,5);
  else if(heldTicks>=60)activate(p,6,true);
  else p.displayClientMessage(Component.literal("Last Argument canceled — hold for 3 seconds to ready it."),true);
 }
 private static boolean activate(ServerPlayer p,int ability,boolean charged){
  if(ability<0||ability>=HFState.NAMES.length||!p.isAlive()||p.isSpectator()||!HFState.equipped(p,ability))return false;
  var d=HFState.data(p);long now=HFState.now(p);if(d.getLong("cd_"+ability)>now){HFEffects.burst(p,HFEffects.REJECT,p.position(),0,4);p.displayClientMessage(Component.literal(HFState.NAMES[ability]+": "+((d.getLong("cd_"+ability)-now+19)/20)+"s remaining"),true);return false;}
  if(ability==4&&(!supported(p)||p.isPassenger()||p.getAbilities().flying||p.isFallFlying())){HFEffects.burst(p,HFEffects.REJECT,p.position(),0,4);p.displayClientMessage(Component.literal("Cataclysm needs solid ground."),true);return false;}
  if(ability==6&&HFState.active(p,"windup"))return false;
  switch(ability){
   case 0->d.putBoolean("awareness",!d.getBoolean("awareness"));
   case 1->{for(var target:enemies(p,p.position(),32)){HFState.until(target,HFState.mark(p,"witness"),400);HFEffects.target(p,target,HFEffects.WITNESS,400);}HFEffects.burst(p,HFEffects.WITNESS,p.position(),32,24);}
   case 2->HFState.until(p,"unbroken",400);
   case 3->{HFState.until(p,"sovereign",500);d.putLong("pulse",now+100);}
   case 4->{HFState.until(p,"cataclysm",300);d.putBoolean("cataclysm_airborne",false);d.putDouble("launch_y",p.getY());d.putDouble("launch_peak",p.getY());p.setDeltaMovement(p.getDeltaMovement().x,1.9,p.getDeltaMovement().z);p.hurtMarked=true;HFEffects.burst(p,HFEffects.LAUNCH,p.position(),2,20);}
   case 5->fracture(p);
   case 6->{if(charged)ultimate(p);else {HFState.until(p,"windup",60);}}
  }
  if(Boolean.getBoolean("lc_hollowforged.review"))com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_ACTIVATED ability={}",ability);
  if(ability>0)d.putLong("cd_"+ability,now+HFState.COOLDOWNS[ability]);
  p.displayClientMessage(Component.literal(HFState.NAMES[ability]+(ability==0?(d.getBoolean("awareness")?": aware":": quiet"):"")),true);HFNetwork.sync(p);return true;
 }
 public static void fracture(ServerPlayer p){
  Vec3 forward=p.getLookAngle().multiply(1,0,1);if(forward.lengthSqr()<.0001)forward=new Vec3(0,0,1);final Vec3 dir=forward.normalize();
  CombatHooks.secondary(()->{for(var enemy:enemies(p,p.position(),26)){
   Vec3 delta=enemy.position().subtract(p.position());double along=delta.dot(dir),side=Math.abs(delta.x*dir.z-delta.z*dir.x);
   if(along>=0&&along<=24&&side<=3.5&&Math.abs(delta.y)<=4){
    HFState.until(enemy,"shattered",300);HFEffects.mark(enemy,HFEffects.SHATTERED,300);enemy.hurt(p.damageSources().playerAttack(p),40);launch(enemy,p.position(),1.5,.8);if(!HFState.controlImmune(enemy)){enemy.stopUsingItem();if(enemy instanceof Mob m)m.getNavigation().stop();}
   }
  }});
  HFEffects.burst(p,HFEffects.FRACTURE,p.position(),24,22);
 }
 public static void ultimate(ServerPlayer p){
  CombatHooks.secondary(()->{for(var enemy:enemies(p,p.position(),20)){HFState.until(enemy,"shattered",600);HFEffects.mark(enemy,HFEffects.SHATTERED,600);enemy.hurt(p.damageSources().playerAttack(p),100);launch(enemy,p.position(),2.5,1);CombatHooks.stun(enemy,80);}});
  HFEffects.burst(p,HFEffects.ARGUMENT,p.position(),20,32);
 }
 public static void landing(ServerPlayer p,float distance){
  if(!HFState.boots(p)||p.getY()<=p.level().getMinBuildHeight()||HFState.data(p).getLong("impact_tick")==HFState.now(p))return;
  boolean empowered=HFState.active(p,"cataclysm")&&HFState.data(p).getBoolean("cataclysm_airborne");
  if(!empowered&&distance<5)return;
  if(Boolean.getBoolean("lc_hollowforged.review"))com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_LANDING empowered={} fall={} cataclysmPeak={}",empowered,distance,HFState.data(p).getDouble("launch_peak")-HFState.data(p).getDouble("launch_y"));
  p.resetFallDistance();var d=HFState.data(p);d.putLong("impact_tick",HFState.now(p));d.remove("cataclysm");
  double extra=Math.clamp(distance-5,0,20),radius=empowered?16:4+extra*.2;float damage=empowered?60:10+(float)extra;
  CombatHooks.secondary(()->{for(var enemy:enemies(p,p.position(),radius)){enemy.hurt(p.damageSources().playerAttack(p),damage);launch(enemy,p.position(),empowered?2:.8+extra*.04,empowered?.8:.5+extra*.02);if(empowered)CombatHooks.stun(enemy,40);}});
  HFEffects.burst(p,empowered?HFEffects.LAUNCH:HFEffects.FALL,p.position(),radius,empowered?24:18);
 }
 private static void fall(LivingFallEvent event){if(event.getEntity() instanceof ServerPlayer p&&HFState.boots(p))landing(p,event.getDistance());}
 private static void anchor(ServerPlayer p){
  var d=HFState.data(p);Vec3 pos=p.position();boolean grounded=supported(p);
  if(!HFState.legs(p)||!grounded){d.putBoolean("anchor",false);d.putInt("still",0);}
  else if(d.getBoolean("anchor")){
   if(pos.distanceToSqr(new Vec3(d.getDouble("anchor_x"),d.getDouble("anchor_y"),d.getDouble("anchor_z")))>9){d.putBoolean("anchor",false);d.putInt("still",0);}
  }else{
   double delta=pos.distanceToSqr(new Vec3(d.getDouble("last_x"),d.getDouble("last_y"),d.getDouble("last_z")));
   int still=delta<=.0001?d.getInt("still")+1:0;d.putInt("still",still);
   if(still>=60){d.putBoolean("anchor",true);d.putDouble("anchor_x",pos.x);d.putDouble("anchor_y",pos.y);d.putDouble("anchor_z",pos.z);}
  }
  d.putDouble("last_x",pos.x);d.putDouble("last_y",pos.y);d.putDouble("last_z",pos.z);
 }
 private static void gaze(ServerPlayer p){
  var d=HFState.data(p);if(!HFState.crown(p)){d.remove("gaze");d.putInt("gaze_ticks",0);return;}
  Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(48));LivingEntity found=null;double closest=Double.MAX_VALUE;
  for(var e:enemies(p,p.position(),48)){
   var hit=e.getBoundingBox().inflate(.15).clip(eye,end);if(hit.isEmpty()||!p.hasLineOfSight(e))continue;double distance=hit.get().distanceToSqr(eye);if(distance<closest){found=e;closest=distance;}
  }
  if(found==null){d.remove("gaze");d.putInt("gaze_ticks",0);return;}
  String id=found.getUUID().toString();int ticks=id.equals(d.getString("gaze"))?d.getInt("gaze_ticks")+4:4;d.putString("gaze",id);d.putInt("gaze_ticks",Math.min(ticks,40));
  if(ticks>=40){HFState.until(found,HFState.mark(p,"exposed"),300);HFEffects.target(p,found,HFEffects.EXPOSED,300);}
 }
 public static int[] reveals(ServerPlayer p){
  if(!HFState.crown(p))return new int[0];boolean aware=HFState.data(p).getBoolean("awareness");
  return enemies(p,p.position(),64).stream().filter(e->aware&&p.distanceToSqr(e)<=48*48||HFState.active(e,HFState.mark(p,"witness"))||HFState.active(e,HFState.mark(p,"exposed"))).limit(256).mapToInt(Entity::getId).toArray();
 }
 public static String targetStatus(ServerPlayer p){
  if(!HFState.crown(p)&&!HFState.hammer(p))return "";
  Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(48));LivingEntity selected=null;double nearest=Double.MAX_VALUE;
  for(var target:enemies(p,p.position(),48)){var hit=target.getBoundingBox().inflate(.15).clip(eye,end);if(hit.isPresent()&&p.hasLineOfSight(target)&&hit.get().distanceToSqr(eye)<nearest){selected=target;nearest=hit.get().distanceToSqr(eye);}}
  if(selected==null)return "";var text=new StringBuilder();String[] keys={HFState.mark(p,"exposed"),HFState.mark(p,"witness"),"shattered"},labels={"Exposed","Final Witness","Shattered"};
  for(int i=0;i<keys.length;i++)if((i==2||HFState.crown(p))&&HFState.active(selected,keys[i])){if(!text.isEmpty())text.append(" · ");text.append(labels[i]).append(" ").append((HFState.data(selected).getLong(keys[i])-HFState.now(p)+19)/20).append("s");}
  return text.toString();
 }
 private static void tick(PlayerTickEvent.Post event){
  if(!(event.getEntity() instanceof ServerPlayer p))return;var d=HFState.data(p);long now=HFState.now(p);
  if(!p.isAlive()||p.isSpectator()){HFState.clearMovement(p);return;}
  anchor(p);
  if(Boolean.getBoolean("lc_hollowforged.review")&&now%40==0)com.mojang.logging.LogUtils.getLogger().info("HOLLOWFORGED_ANCHOR ground={} supported={} still={} anchored={} y={}",p.onGround(),supported(p),d.getInt("still"),d.getBoolean("anchor"),p.getY());
  boolean sovereign=HFState.legs(p)&&HFState.active(p,"sovereign"),windup=HFState.hammer(p)&&HFState.active(p,"windup");
  CombatHooks.setModifier(p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE),REACH,sovereign?.4:0);
  CombatHooks.setModifier(p.getAttribute(Attributes.MOVEMENT_SPEED),WINDUP,windup||HFState.hammer(p)&&p.isUsingItem()&&p.getUsedItemHand()==net.minecraft.world.InteractionHand.MAIN_HAND?-.8:0);
  if(d.contains("windup")){
   if(!HFState.hammer(p))d.remove("windup");else if(!HFState.active(p,"windup")){d.remove("windup");ultimate(p);}
  }
  if(HFState.active(p,"cataclysm")){
   d.putDouble("launch_peak",Math.max(d.getDouble("launch_peak"),p.getY()));
   if(!HFState.boots(p)){d.remove("cataclysm");}else if(!p.onGround())d.putBoolean("cataclysm_airborne",true);else if(d.getBoolean("cataclysm_airborne"))landing(p,p.fallDistance);
  }
  if(now%4==0)gaze(p);
  if(now%10==0){
   boolean anchored=HFState.legs(p)&&d.getBoolean("anchor");
   if(anchored||sovereign)for(var enemy:enemies(p,p.position(),sovereign?16:8)){
    if(anchored&&p.distanceToSqr(enemy)<=64)CombatHooks.slow(enemy,.3F,12);
    if(sovereign&&enemy.getDeltaMovement().dot(p.position().subtract(enemy.position()))>0)CombatHooks.slow(enemy,.5F,12);
   }
   HFNetwork.sync(p);
  }
  HFEffects.tick(p);
  if(sovereign&&now>=d.getLong("pulse")){d.putLong("pulse",now+100);for(var e:enemies(p,p.position(),16))launch(e,p.position(),1,.2);HFEffects.burst(p,HFEffects.PUSH,p.position(),16,20);}
 }
 private static void teleport(EntityTeleportEvent event){
  if(!(event.getEntity() instanceof LivingEntity enemy)||!HFState.hostile(enemy)||HFState.controlImmune(enemy)||!(enemy.level() instanceof ServerLevel level))return;
  for(var p:level.players())if(p.isAlive()&&!p.isSpectator()&&HFState.legs(p)&&HFState.active(p,"sovereign")&&(p.position().distanceToSqr(event.getPrev())<=256||p.position().distanceToSqr(event.getTarget())<=256)){event.setCanceled(true);return;}
 }
 private static void playerClone(PlayerEvent.Clone event){
  var old=HFState.data(event.getOriginal());var fresh=HFState.data(event.getEntity());
  for(String key:old.getAllKeys())if(key.startsWith("cd_")||key.equals("refusal_cd"))fresh.putLong(key,old.getLong(key));
  HFState.clearMovement(event.getEntity());
 }
 private static void joined(PlayerEvent.PlayerLoggedInEvent event){HFState.clearMovement(event.getEntity());}
 private static void changed(PlayerEvent.PlayerChangedDimensionEvent event){HFState.clearMovement(event.getEntity());}
 private static void commands(RegisterCommandsEvent event){
  var root=Commands.literal("hollowforged").executes(ctx->{var p=ctx.getSource().getPlayerOrException();HFNetwork.sync(p);ctx.getSource().sendSuccess(()->Component.literal("Hollowforged: /hollowforged sight | witness | unbroken | sovereign | cataclysm | fracture | argument. Configure the Hollowforged controls in Options > Controls."),false);return 1;});
  if(Boolean.getBoolean("lc_hollowforged.review"))root.then(Commands.literal("fxpreview").then(Commands.argument("kind",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,19)).executes(ctx->{HFEffects.preview(ctx.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"kind"),false);return 1;}))).then(Commands.literal("fxstop").then(Commands.argument("kind",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,19)).executes(ctx->{HFEffects.preview(ctx.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"kind"),true);return 1;})));
  String[] names={"sight","witness","unbroken","sovereign","cataclysm","fracture","argument"};
  for(int i=0;i<names.length;i++){final int ability=i;root.then(Commands.literal(names[i]).executes(ctx->activate(ctx.getSource().getPlayerOrException(),ability)?1:0));}event.getDispatcher().register(root);
 }
 public static void register(){
  NeoForge.EVENT_BUS.addListener(CombatAbilities::tick);NeoForge.EVENT_BUS.addListener(CombatAbilities::fall);NeoForge.EVENT_BUS.addListener(CombatAbilities::teleport);NeoForge.EVENT_BUS.addListener(CombatAbilities::playerClone);NeoForge.EVENT_BUS.addListener(CombatAbilities::joined);NeoForge.EVENT_BUS.addListener(CombatAbilities::changed);NeoForge.EVENT_BUS.addListener(CombatAbilities::commands);
 }
}
