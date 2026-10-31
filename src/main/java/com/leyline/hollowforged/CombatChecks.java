package com.leyline.hollowforged;
import static com.leyline.hollowforged.DevelopmentChecks.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
/** Opt-in real-registry / live damage-pipeline checks; not a substitute for the full LC instance. */
public final class CombatChecks {
 public static void run(MinecraftServer server){
  var level=server.overworld();
  var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"CombatChecks")){
   @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
   @Override public float getAttackStrengthScale(float partial){return 1F;}
  };
  player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  var origin=level.getSharedSpawnPos().above(4);player.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5,0,0);
  var target=EntityType.ZOMBIE.create(level);target.setNoAi(true);target.moveTo(player.getX(),player.getY(),player.getZ()+4,0,0);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);target.getAttribute(Attributes.ARMOR).setBaseValue(20);target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(10);level.addFreshEntity(target);
  var source=player.damageSources().playerAttack(player);
  float expected=CombatRules.getDamageAfterAbsorb(target,20,source,20,10);target.hurt(source,20);near(1000-target.getHealth(),expected,"live unmarked armor-stage damage");
  player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Hollowforged.CROWN.get()));HFState.until(target,HFState.mark(player,"exposed"),300);HFState.until(target,HFState.mark(player,"witness"),400);
  near(CombatHooks.armorMultiplier(target,source),.3,"Exposed and Final Witness armor remainder");
  target.setHealth(1000);target.invulnerableTime=0;expected=CombatRules.getDamageAfterAbsorb(target,39,source,6,10);target.hurt(source,20);near(1000-target.getHealth(),expected,"live combined marks apply 1.95 damage once and 0.30 armor");
  var stranger=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"OtherWearer"));stranger.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Hollowforged.CROWN.get()));near(CombatHooks.armorMultiplier(target,stranger.damageSources().playerAttack(stranger)),1,"marks do not leak to a second wearer");
  HFState.until(target,"shattered",300);near(CombatHooks.armorMultiplier(target,source),.15,"Shattered composes once with distinct marks");HFState.until(target,"shattered",300);near(CombatHooks.armorMultiplier(target,source),.15,"refreshing Shattered does not stack another copy");
  target.getPersistentData().remove(Hollowforged.ID);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Hollowforged.WORLDSHARD.get()));
  // A player attack context captures charge before vanilla resets its attack timer.
  for(int i=0;i<30;i++)player.tick();CombatHooks.begin(player,target);near(CombatHooks.armorMultiplier(target,source),.5,"charged Sundering armor remainder");target.setHealth(1000);target.invulnerableTime=0;expected=CombatRules.getDamageAfterAbsorb(target,30,source,10,10);target.hurt(source,20);near(1000-target.getHealth(),expected,"live charged Sundering applies heavy armor bonus and penetration once");CombatHooks.end(player);
  player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Hollowforged.CARAPACE.get()));
  var hit=new LivingDamageEvent.Pre(player,new DamageContainer(target.damageSources().mobAttack(target),100));NeoForge.EVENT_BUS.post(hit);near(hit.getNewDamage(),70,"post-armor passive reduction is 30 percent");
  HFState.until(player,"unbroken",400);hit=new LivingDamageEvent.Pre(player,new DamageContainer(target.damageSources().mobAttack(target),100));NeoForge.EVENT_BUS.post(hit);near(hit.getNewDamage(),20,"active 80 percent replaces passive reduction");
  player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Hollowforged.GREAVES.get()));HFState.data(player).putBoolean("anchor",true);hit=new LivingDamageEvent.Pre(player,new DamageContainer(target.damageSources().mobAttack(target),100));NeoForge.EVENT_BUS.post(hit);near(hit.getNewDamage(),15,"Anchored and active chest compose to 85 percent melee reduction");
  hit=new LivingDamageEvent.Pre(player,new DamageContainer(player.damageSources().fellOutOfWorld(),100));NeoForge.EVENT_BUS.post(hit);near(hit.getNewDamage(),100,"void bypasses active and Anchored reductions");
  var knock=new LivingKnockBackEvent(player,3,1,0);NeoForge.EVENT_BUS.post(knock);check(knock.isCanceled(),"Greaves block native attack knockback");
  Vec3 motion=player.getDeltaMovement();player.push(2,1,2);check(player.getDeltaMovement().equals(motion),"Greaves block direct external push without resetting voluntary motion");player.setDeltaMovement(0,.42,0);near(player.getDeltaMovement().y,.42,"ordinary jump velocity remains available");
  check(new ItemStack(Hollowforged.TREADS.get()).canWalkOnPowderedSnow(player),"Treads use native powder snow walking hook");
  HFState.until(player,"refusal_protection",160);var voidHit=new LivingIncomingDamageEvent(player,new DamageContainer(player.damageSources().fellOutOfWorld(),100));NeoForge.EVENT_BUS.post(voidHit);check(!voidHit.isCanceled(),"void bypasses Refusal protection and every equipped passive");
  var regularHit=new LivingIncomingDamageEvent(player,new DamageContainer(target.damageSources().mobAttack(target),100));NeoForge.EVENT_BUS.post(regularHit);check(regularHit.isCanceled(),"Refusal protection blocks ordinary damage");
  player.setHealth(0);var death=new LivingDeathEvent(player,target.damageSources().mobAttack(target));NeoForge.EVENT_BUS.post(death);check(death.isCanceled(),"Refusal cancels eligible death");near(player.getHealth(),player.getMaxHealth()*.5,"Refusal restores half maximum health");check(HFState.active(player,"refusal_cd"),"Refusal starts persistent five-minute cooldown");
  death=new LivingDeathEvent(player,target.damageSources().mobAttack(target));NeoForge.EVENT_BUS.post(death);check(!death.isCanceled(),"Refusal cannot trigger again during cooldown");
  death=new LivingDeathEvent(player,player.damageSources().fellOutOfWorld());NeoForge.EVENT_BUS.post(death);check(!death.isCanceled(),"void death is never rescued");
  // Ability requests validate equipment/cooldowns, and real wave damage uses the specified geometry.
  check(!CombatAbilities.activate(player,7),"invalid ability index rejected");
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);check(!CombatAbilities.activate(player,5),"Fracture rejected without Worldshard");
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Hollowforged.WORLDSHARD.get()));player.setYRot(0);player.setXRot(0);
  var inWave=EntityType.ZOMBIE.create(level);inWave.setNoAi(true);inWave.moveTo(player.getX()+1,player.getY(),player.getZ()+10);inWave.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);inWave.getAttribute(Attributes.ARMOR).setBaseValue(0);inWave.setHealth(1000);level.addFreshEntity(inWave);
  var outWave=EntityType.ZOMBIE.create(level);outWave.setNoAi(true);outWave.moveTo(player.getX()+6,player.getY(),player.getZ()+10);outWave.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);outWave.getAttribute(Attributes.ARMOR).setBaseValue(0);outWave.setHealth(1000);level.addFreshEntity(outWave);
  check(CombatAbilities.enemies(player,player.position(),26).contains(inWave),"wave fixture is in the loaded hostile query");
  check(CombatAbilities.activate(player,5),"server-authoritative Fracture activation accepted");near(inWave.getHealth(),960,"Fracture deals 40 premitigation damage inside 24 by 7 volume");near(outWave.getHealth(),1000,"Fracture does not hit outside 7-block width");check(HFState.active(inWave,"shattered"),"Fracture applies Shattered");check(!CombatAbilities.activate(player,5),"Fracture rejects repeat activation during cooldown");
  player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Hollowforged.CROWN.get()));check(CombatAbilities.activate(player,1),"Final Witness activates with Crown");check(HFState.active(inWave,HFState.mark(player,"witness")),"Final Witness marks nearby hostile for wearer");
  HFState.data(inWave).remove(HFState.mark(player,"witness"));inWave.invulnerableTime=0;CombatAbilities.ultimate(player);near(inWave.getHealth(),860,"Last Argument deals 100 premitigation damage");check(CombatHooks.stunned(inWave),"Last Argument stuns ordinary survivors");
  var wither=EntityType.WITHER.create(level);CombatHooks.stun(wither,80);check(!CombatHooks.stunned(wither),"boss control immunity survives ultimate stun");
  inWave.invulnerableTime=0;HFState.data(player).putLong("impact_tick",-1);player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Hollowforged.TREADS.get()));float old=inWave.getHealth();CombatAbilities.landing(player,4);near(inWave.getHealth(),old,"routine short falls do not create Aftershock");
  inWave.moveTo(player.getX(),player.getY(),player.getZ()+2);inWave.invulnerableTime=0;CombatAbilities.landing(player,5);near(inWave.getHealth(),old-10,"five-block Aftershock deals 10 premitigation damage");
  HFState.data(player).putLong("impact_tick",-1);HFState.until(player,"cataclysm",300);HFState.data(player).putBoolean("cataclysm_airborne",true);inWave.invulnerableTime=0;old=inWave.getHealth();CombatAbilities.landing(player,25);near(inWave.getHealth(),old-60,"Cataclysm replaces incidental Aftershock on empowered landing");
  check(!HFState.active(player,"cataclysm"),"Cataclysm consumes one empowered landing");
  player.setOnGround(false);HFState.data(player).remove("cd_4");check(!CombatAbilities.activate(player,4),"Cataclysm cannot launch from midair or rescue a fall");
  HFState.data(player).remove("cd_5");HFState.data(player).remove("cd_6");CombatAbilities.releaseHammer(player,30);check(!HFState.active(player,"cd_5")&&!HFState.active(player,"cd_6"),"releasing an incomplete ultimate cancels without spending either cooldown");
  CombatAbilities.releaseHammer(player,2);check(HFState.active(player,"cd_5"),"short native hammer release activates Fracture");
  CombatAbilities.releaseHammer(player,60);check(HFState.active(player,"cd_6")&&!HFState.active(player,"windup"),"completed native hammer hold commits ultimate without a second windup");
  inWave.discard();outWave.discard();
  // Real lethal void hit while every protective state is active.
  player.invulnerableTime=0;player.hurt(player.damageSources().fellOutOfWorld(),1000);check(player.isDeadOrDying(),"live void lethality with active chest, anchor and Refusal protection");
  // Reward probability boundaries, all six color counts, duration and owner identity.
  for(int colors=0;colors<=5;colors++){near(EncounterRewards.count(colors,0.999),1,"Core guarantee at "+colors+" colors");check(EncounterRewards.count(colors,.05*colors)==1,"Core strict probability boundary at "+colors+" colors");if(colors>0)check(EncounterRewards.count(colors,.05*colors-.000001)==2,"Core bonus below threshold at "+colors+" colors");}
  for(int colors=0;colors<=5;colors++){int bonus=0;for(int i=0;i<100000;i++)bonus+=EncounterRewards.count(colors,(i+.5)/100000.0)-1;check(bonus==colors*5000,"100000-stratum Core distribution matches "+(colors*5)+" percent bonus");}
  var owner=UUID.randomUUID();EncounterRewards.begin(target,owner,UUID.randomUUID());
  for(var c:EncounterRewards.Color.values())EncounterRewards.suppression(target,UUID.randomUUID(),c,true);check(EncounterRewards.qualifying(target)==0,"other players cannot qualify Leylines");
  for(var c:EncounterRewards.Color.values()){EncounterRewards.suppression(target,owner,c,true);var d=target.getPersistentData().getCompound("hollowforged_encounter");d.putLong(c.name()+"_duration",199);}
  check(EncounterRewards.qualifying(target)==0,"historical 199-tick suppression does not substitute for a death snapshot");
  for(var c:EncounterRewards.Color.values())target.getPersistentData().getCompound("hollowforged_encounter").putLong(c.name()+"_duration",200);
  check(EncounterRewards.qualifying(target)==0,"historical 200-tick suppression cannot fabricate death eligibility");EncounterRewards.suppression(target,owner,EncounterRewards.Color.WHITE,false);check(EncounterRewards.qualifying(target)==0,"legacy suppression hook cannot alter death-time loot");
  var roundtrip=HFState.data(player).copy();var saved=new net.minecraft.nbt.CompoundTag();saved.put(Hollowforged.ID,roundtrip);check(saved.getCompound(Hollowforged.ID).getLong("refusal_cd")==HFState.data(player).getLong("refusal_cd"),"cooldown deadline survives NBT copy");
  var serialized=new net.minecraft.nbt.CompoundTag();player.saveWithoutId(serialized);var restoredPlayer=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RestoreCheck"));restoredPlayer.load(serialized);
  check(HFState.data(restoredPlayer).getLong("cd_5")==HFState.data(player).getLong("cd_5"),"actual entity save/load preserves ability cooldown");
  var clonePlayer=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"CloneCheck"));NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clonePlayer,player,true));
  check(HFState.data(clonePlayer).getLong("refusal_cd")==HFState.data(player).getLong("refusal_cd"),"death clone preserves rescue cooldown");check(!HFState.active(clonePlayer,"cataclysm"),"death clone never carries recovery launch state");
  check(!player.isPushedByFluid(),"Greaves prevent fluid displacement without disabling swimming");
  // Actual jump factor over honey; the disposable fixture restores the block immediately.
  var below=player.blockPosition().below();var oldBlock=level.getBlockState(below);level.setBlockAndUpdate(below,net.minecraft.world.level.block.Blocks.HONEY_BLOCK.defaultBlockState());
  player.setHealth(20);player.setOnGround(true);player.setDeltaMovement(Vec3.ZERO);player.jumpFromGround();near(player.getDeltaMovement().y,.42,"Greaves preserve native jump power on honey");
  HFState.data(player).putBoolean("anchor",false);HFState.data(player).putInt("still",0);player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
  for(int i=0;i<61;i++)NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
  check(HFState.data(player).getBoolean("anchor"),"61 stationary grounded player ticks establish Anchored");player.setOnGround(false);NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));check(!HFState.data(player).getBoolean("anchor"),"leaving ground clears Anchored");
  level.setBlockAndUpdate(below,oldBlock);
  HFState.until(player,"sovereign",500);player.setHealth(20);level.addNewPlayer(player);
  var teleport=new net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand(target,player.getX()+30,player.getY(),player.getZ());NeoForge.EVENT_BUS.post(teleport);check(teleport.isCanceled(),"Sovereign blocks compatible hostile teleport leaving its zone");
  HFState.data(player).remove("sovereign");teleport=new net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand(target,player.getX()+30,player.getY(),player.getZ());NeoForge.EVENT_BUS.post(teleport);check(!teleport.isCanceled(),"hostile teleport works after Sovereign expires");level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
  var harrowerType=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lc_harrower","harrower"));
  if(harrowerType.isPresent()&&harrowerType.get().create(level) instanceof LivingEntity harrower){
   check(HFState.controlImmune(harrower),"loaded Harrower remains control immune");CombatHooks.stun(harrower,80);check(!CombatHooks.stunned(harrower),"equipment stun never freezes loaded Harrower");
   HFState.until(harrower,"shattered",600);float health=harrower.getHealth();harrower.setInvulnerable(true);harrower.hurt(stranger.damageSources().playerAttack(stranger),40);near(harrower.getHealth(),health,"native boss invulnerability survives Shattered");
   harrower.setInvulnerable(false);harrower.invulnerableTime=0;harrower.hurt(stranger.damageSources().playerAttack(stranger),40);check(harrower.getHealth()<health,"loaded Harrower accepts ordinary native equipment damage");
   check(EncounterRewards.qualifying(harrower)==0,"development Harrower has no fabricated legitimate reward qualification");
   harrower.setHealth(0);var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();NeoForge.EVENT_BUS.post(new LivingDropsEvent(harrower,stranger.damageSources().playerAttack(stranger),drops,true));
   var pending=harrower.getPersistentData().getCompound(com.leyline.harrower.loot.DelayedDeathLoot.KEY).getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND);
   check(drops.isEmpty()&&pending.size()==1&&net.minecraft.world.item.ItemStack.parseOptional(level.registryAccess(),pending.getCompound(0).getCompound("Item")).is(Hollowforged.HOLLOW_CORE.get()),"standalone Core is reserved until crumble");
   EncounterRewards.begin(harrower,UUID.randomUUID(),UUID.randomUUID());NeoForge.EVENT_BUS.post(new LivingDropsEvent(harrower,stranger.damageSources().playerAttack(stranger),drops,true));check(drops.isEmpty()&&pending.size()==1,"legacy registration cannot mint a second reserved Core");
   var repeated=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();NeoForge.EVENT_BUS.post(new LivingDropsEvent(harrower,stranger.damageSources().playerAttack(stranger),repeated,true));check(repeated.isEmpty()&&pending.size()==1,"repeated drop event cannot duplicate escrow");harrower.discard();
  }
  var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess());
  var view=new HFNetwork.View("minecraft:overworld",new int[]{3,9},new int[]{0,1800,3600,2400,1200,600,6000},127,127,"Exposed 15s");HFNetwork.View.CODEC.encode(buffer,view);var decoded=HFNetwork.View.CODEC.decode(buffer);check(java.util.Arrays.equals(decoded.reveals(),view.reveals())&&java.util.Arrays.equals(decoded.cooldowns(),view.cooldowns())&&decoded.targetStatus().equals(view.targetStatus()),"network state round trip preserves private reveals, cooldowns and target status");buffer.release();
  long beforeDimension=HFState.data(clonePlayer).getLong("cd_5");HFState.until(clonePlayer,"cataclysm",300);HFState.until(clonePlayer,"windup",60);NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(clonePlayer,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
  check(!HFState.active(clonePlayer,"cataclysm")&&!HFState.active(clonePlayer,"windup"),"dimension change cancels stored landing and windup states");check(HFState.data(clonePlayer).getLong("cd_5")==beforeDimension,"dimension change cannot reset ability cooldown");
  target.discard();
 }
}
