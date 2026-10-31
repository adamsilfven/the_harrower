package com.leyline.harrower.ritual;

import com.leyline.harrower.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent, server-owned escrow. Chunk unload pauses it; reload revalidates before continuing.
 * Reserved item lives in this entity's NBT, not a second inventory or transient static map. */
public final class CairnRitual extends Entity {
 private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(CairnRitual.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> AXIS=SynchedEntityData.defineId(CairnRitual.class,EntityDataSerializers.BOOLEAN);
 private UUID owner;
 private BlockPos origin=BlockPos.ZERO;
 private Vec3 site=Vec3.ZERO;
 private ItemStack reserved=ItemStack.EMPTY;
 private boolean cancelling;
 private String cancellation="The Cairn falls quiet. Your Hollow Fragment has been returned.";
 public CairnRitual(EntityType<? extends CairnRitual> type,Level level){super(type,level);noPhysics=true;setInvulnerable(true);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(AGE,0);builder.define(AXIS,true);}
 public int age(){return entityData.get(AGE);}
 public boolean alongX(){return entityData.get(AXIS);}
 public CairnFrame frame(){return new CairnFrame(origin,alongX());}
 public void prepare(ServerPlayer player,CairnFrame frame,Vec3 spawn,ItemStack fragment){
  owner=player.getUUID();origin=frame.origin();site=spawn;reserved=fragment.copyWithCount(1);entityData.set(AXIS,frame.alongX());setPos(frame.center());
 }
 @Override public void tick(){
  super.tick();if(level().isClientSide)return;
  ServerLevel level=(ServerLevel)level();ServerPlayer player=owner==null?null:(ServerPlayer)level.getPlayerByUUID(owner);
  if(reserved.isEmpty()){CairnLocks.get(level).release(getUUID());discard();return;}
  if(cancelling){cancel(cancellation);return;}
  if(player==null||player.serverLevel()!=level||!player.isAlive()||player.isSpectator()||player.distanceToSqr(this)>48*48||level.getDifficulty()==Difficulty.PEACEFUL||!frame().valid(level)){
   cancel("The Cairn falls quiet. Your Hollow Fragment has been returned.");return;
  }
  int age=age()+1;entityData.set(AGE,age);
  if(age==1)level.playSound(null,blockPosition(),HarrowerMod.GRIND.get(),SoundSource.HOSTILE,1.1F,.55F);
  if(age==40)level.playSound(null,blockPosition(),HarrowerMod.HOLLOW_CUE.get(),SoundSource.HOSTILE,.85F,.8F);
  // 160..219 intentionally emits no ritual sound or particles. Never mute unrelated audio.
  if(age==220)level.playSound(null,BlockPos.containing(site),HarrowerMod.BREATH.get(),SoundSource.HOSTILE,1.8F,.65F);
  if(age<260)return;
  if(!CairnSites.valid(level,player,frame(),site)){
   Vec3 alternative=CairnSites.find(level,player,frame());if(alternative!=null)site=alternative;else {cancel("There is no longer room for the Harrower. Your Hollow Fragment has been returned.");return;}}
  Harrower boss=HarrowerMod.HARROWER.get().create(level);
  if(boss==null){cancel("The summoning failed. Your Hollow Fragment has been returned.");return;}
  float yaw=(float)(Math.toDegrees(Math.atan2(player.getZ()-site.z,player.getX()-site.x))-90);
  boss.moveTo(site.x,site.y,site.z,yaw,0);boss.setYHeadRot(yaw);boss.yBodyRot=yaw;boss.setPersistenceRequired();boss.setTarget(player);
  if(!level.addFreshEntity(boss)){cancel("The summoning was refused. Your Hollow Fragment has been returned.");return;}
  boss.combat().encounter.setSummoner(player.getUUID());
  boss.combat().encounter.start(player);
  boss.setTarget(player);
  // Single commit point. No second shrink of the source item.
  reserved=ItemStack.EMPTY;CairnLocks.get(level).release(getUUID());level.playSound(null,boss.blockPosition(),HarrowerMod.IMPACT.get(),SoundSource.HOSTILE,1.5F,.7F);discard();
 }
 public void cancel(String message){
  if(level().isClientSide||reserved.isEmpty())return;
  cancelling=true;cancellation=message;
  // Refund at the opening, never inside the player's inventory. Works offline and after reload.
  ItemEntity refund=new ItemEntity(level(),getX(),getY()+.3,getZ(),reserved.copy());refund.setDefaultPickUpDelay();refund.setUnlimitedLifetime();
  // No thrower: a refund must not immediately trigger another ritual. Owner protection is temporary via vanilla pickup rules.
  refund.setTarget(owner);refund.getPersistentData().putBoolean("lc_cairn_refund",true);
  if(!level().addFreshEntity(refund))return; // Retry next tick if an external mod refuses the refund entity.
  reserved=ItemStack.EMPTY;CairnLocks.get((ServerLevel)level()).release(getUUID());
  if(owner!=null&&((ServerLevel)level()).getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer player)player.displayClientMessage(Component.literal(message),false);
  discard();
 }
 @Override protected void addAdditionalSaveData(CompoundTag tag){
  tag.putBoolean("Cancelling",cancelling);tag.putString("Cancellation",cancellation);tag.putInt("RitualAge",age());tag.putBoolean("AlongX",alongX());tag.putLong("Origin",origin.asLong());if(owner!=null)tag.putUUID("Summoner",owner);
  tag.putDouble("SiteX",site.x);tag.putDouble("SiteY",site.y);tag.putDouble("SiteZ",site.z);
  if(!reserved.isEmpty())tag.put("ReservedFragment",reserved.save(registryAccess()));
 }
 @Override protected void readAdditionalSaveData(CompoundTag tag){
  cancelling=tag.getBoolean("Cancelling");if(tag.contains("Cancellation"))cancellation=tag.getString("Cancellation");entityData.set(AGE,tag.getInt("RitualAge"));entityData.set(AXIS,tag.getBoolean("AlongX"));origin=BlockPos.of(tag.getLong("Origin"));owner=tag.hasUUID("Summoner")?tag.getUUID("Summoner"):null;
  site=new Vec3(tag.getDouble("SiteX"),tag.getDouble("SiteY"),tag.getDouble("SiteZ"));reserved=tag.contains("ReservedFragment")?ItemStack.parseOptional(registryAccess(),tag.getCompound("ReservedFragment")):ItemStack.EMPTY;
 }
 @Override public void kill(){if(!level().isClientSide)cancel("The Cairn falls quiet. Your Hollow Fragment has been returned.");}
 @Override public boolean isPickable(){return false;}
 @Override public boolean isPushable(){return false;}
}
