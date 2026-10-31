package com.leyline.harrower;

import com.leyline.harrower.combat.*;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Harrower extends Monster {
 public static final int NONE=0,FIST=1,SLAM=2,HURT=3,DEATH=4,LEFT_FIST=5;
 public static final int FIST_IMPACT=18,FIST_DURATION=48,SLAM_IMPACT=40,SLAM_DURATION=105,DEATH_DURATION=600;
 private static final java.util.List<EntityDataAccessor<Float>> RIDER=java.util.stream.IntStream.range(0,3).mapToObj(i->SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT)).toList();
 private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> SERIAL=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Float> CLOCK=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT);
 private static final EntityDataAccessor<Integer> SUPPRESSIONS=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Float> PHASE=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT);
 private static final java.util.List<EntityDataAccessor<Float>> FEET=java.util.stream.IntStream.range(0,6).mapToObj(i->SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT)).toList();
 private final float[] oldFeet=new float[]{1.6875F,.625F,-.3125F,-1.6875F,.625F,-.3125F},currentFeet=oldFeet.clone();
 private int chipSequence;public int chipSequence(){return chipSequence;}
 private int contactReaction;private float impactShake;
 public int contactReaction(){return contactReaction;}
 public float contactReaction(float partial){return (float)Math.sin(Math.PI*Math.max(0,contactReaction-partial)/8);}
 public float impactShake(){return impactShake;}
 @Override public void handleEntityEvent(byte id){if(id==62){chipSequence++;contactReaction=8;return;}if(id==60||id==61){impactShake=id==61?1:.55F;return;}super.handleEntityEvent(id);}
 private float oldPhase,currentPhase,turnLean,previousTurnLean,oldClock,currentClock;private int clockSerial;
 private static final EntityDataAccessor<Float> CORPSE_LIFT=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT),CORPSE_TILT=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT);
 private static final EntityDataAccessor<Float> CORPSE_ROLL=SynchedEntityData.defineId(Harrower.class,EntityDataSerializers.FLOAT);
 private final CorpseGrounding corpseGrounding=new CorpseGrounding();private double deathStartingY;
 public void syncCorpseGrounding(float lift,float tilt,float roll){entityData.set(CORPSE_ROLL,roll);entityData.set(CORPSE_LIFT,lift);entityData.set(CORPSE_TILT,tilt);}
 public float corpseRoll(){return entityData.get(CORPSE_ROLL);}
 public float corpseLift(){return entityData.get(CORPSE_LIFT);}public float corpseTilt(){return entityData.get(CORPSE_TILT);}
 private final HeavyGait gait=new HeavyGait(this);
 private final HarrowerCombat combat=new HarrowerCombat(this);
 public Harrower(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=0;setPersistenceRequired();moveControl=new HeavyMoveControl(this);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,-1);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER_BORDER,16);}
 private boolean navigationGeometry;
 public float navigationWidth(){return 3.6F;}
 public net.minecraft.world.phys.AABB navigationBounds(net.minecraft.world.phys.AABB bounds){double half=navigationWidth()*.5;return new net.minecraft.world.phys.AABB(getX()-half,bounds.minY,getZ()-half,getX()+half,bounds.maxY,getZ()+half);}
 public boolean navigationGeometry(){return navigationGeometry;}
 void navigationGeometry(boolean value){navigationGeometry=value;}
 @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){return new HarrowerNavigation(this,level);}
 @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player){super.stopSeenByPlayer(player);combat.encounter.untrack(player);}
 @Override public void onRemovedFromLevel(){if(combat!=null)combat.encounter.stopBar();super.onRemovedFromLevel();}
 public HarrowerCombat combat(){return combat;}
 /** Keep vanilla modifier ordering, but bypass MAX_HEALTH's 1024 range for this boss only.
  * The raw base and modifiers already synchronize through vanilla attribute packets. */
 @Override public double getAttributeValue(net.minecraft.core.Holder<Attribute> attribute){
  if(!attribute.equals(Attributes.MAX_HEALTH))return super.getAttributeValue(attribute);
  AttributeInstance instance=getAttribute(attribute);double base=instance.getBaseValue();
  for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE)base+=modifier.amount();
  double value=base;
  for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)value+=base*modifier.amount();
  for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)value*=1+modifier.amount();
  return Double.isFinite(value)?Math.max(1,value):3000;
 }
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,3000).add(Attributes.ARMOR,16).add(Attributes.ARMOR_TOUGHNESS,12).add(Attributes.MOVEMENT_SPEED,.286).add(Attributes.FOLLOW_RANGE,64).add(Attributes.ATTACK_DAMAGE,28).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.STEP_HEIGHT,1.2);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CORPSE_ROLL,0F);b.define(CORPSE_LIFT,0F);b.define(CORPSE_TILT,0F);for(var point:RIDER)b.define(point,0F);b.define(ACTION,0);b.define(SERIAL,0);b.define(CLOCK,0F);b.define(SUPPRESSIONS,0);b.define(PHASE,0F);for(int i=0;i<6;i++)b.define(FEET.get(i),new float[]{1.6875F,.625F,-.3125F,-1.6875F,.625F,-.3125F}[i]);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new CombatGoal());goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.65){@Override public boolean canUse(){return combat.autonomous()&&!combat.active()&&action()==NONE&&super.canUse();}});goalSelector.addGoal(6,new RandomLookAroundGoal(this));}
 private final class CombatGoal extends Goal {
  CombatGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
  @Override public boolean canUse(){return combat.active()||getTarget()!=null||action()!=NONE;}
  @Override public boolean canContinueToUse(){return canUse();}
  @Override public boolean requiresUpdateEveryTick(){return true;}
  @Override public void tick(){} // Controller runs once after native movement, never twice through goal scheduling.
 }
 public int action(){return entityData.get(ACTION);}
 public int actionSequence(){return entityData.get(SERIAL);}
 public float actionSeconds(float partial){if(action()==DEATH)return (entityData.get(CLOCK)+partial)/20F;if(isDeadOrDying()&&action()!=DEATH)return (deathTime+partial)/20F;return (level().isClientSide?Mth.lerp(partial,oldClock,currentClock):entityData.get(CLOCK))/20F;}
 public void setCombatPose(int action,float clock,boolean restart){entityData.set(ACTION,action);entityData.set(CLOCK,clock);if(restart)entityData.set(SERIAL,entityData.get(SERIAL)+1);}
 public void setSuppressionMask(int mask){entityData.set(SUPPRESSIONS,mask);}
 public int suppressionMask(){return entityData.get(SUPPRESSIONS);}
 public boolean isCharging(){float tick=level().isClientSide?currentClock:entityData.get(CLOCK);return action()==CombatMove.CHARGE.id&&tick>=40&&tick<174;}
 public boolean locomotion(){return action()==NONE||isCharging();}
 public static boolean isFist(int action){return action==FIST||action==LEFT_FIST;}
 public void demonstrate(int action){combat.force(CombatMove.byId(action));}
 public void groundImpact(Vec3 point,float scale){gait.stomp(point,scale);}
 public int footfallCount(){return gait.footfalls();}
 public float serverPhase(){return entityData.get(PHASE);}
 public float serverAnkle(int side,int axis){return entityData.get(FEET.get(side*3+axis));}
 void syncPhase(float phase){entityData.set(PHASE,phase);}
 void syncFoot(int side,float x,float y,float z){entityData.set(FEET.get(side*3),x);entityData.set(FEET.get(side*3+1),y);entityData.set(FEET.get(side*3+2),z);}
 public float gaitPhase(float partial){return Mth.lerp(partial,oldPhase,currentPhase);}
 public float ankle(int side,int axis,float partial){int i=side*3+axis;return Mth.lerp(partial,oldFeet[i],currentFeet[i]);}
 public float turnLean(float partial){return Mth.lerp(partial,previousTurnLean,turnLean);}
 @Override protected net.minecraft.world.entity.ai.control.BodyRotationControl createBodyControl(){return new net.minecraft.world.entity.ai.control.BodyRotationControl(this){@Override public void clientTick(){yBodyRot=getYRot();}};}
 @Override protected float tickHeadTurn(float wanted,float speed){yBodyRot=getYRot();return speed;}
 @Override public int getMaxHeadYRot(){return 25;}
 @Override public int getHeadRotSpeed(){return 2;}
 @Override protected void playStepSound(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){}
 @Override public void tick(){super.tick();if(contactReaction>0)contactReaction--;impactShake=Math.max(0,impactShake-.08F);
  if(!level().isClientSide&&isAlive()){combat.tick();gait.tick();}
  previousTurnLean=turnLean;turnLean=Mth.lerp(.15F,turnLean,Mth.clamp(Mth.wrapDegrees(getYRot()-yRotO)/1.5F,-1,1));
  if(level().isClientSide){oldPhase=currentPhase;currentPhase=entityData.get(PHASE);for(int i=0;i<6;i++){oldFeet[i]=currentFeet[i];currentFeet[i]=entityData.get(FEET.get(i));}
   oldClock=currentClock;currentClock=entityData.get(CLOCK);if(clockSerial!=actionSequence()){clockSerial=actionSequence();oldClock=currentClock;}
  }
 }
 @Override public boolean isInvulnerableTo(DamageSource source){if(source.is(DamageTypes.FELL_OUT_OF_WORLD))return false;return source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||source.is(DamageTypes.FALL)||source.is(DamageTypes.IN_WALL)||super.isInvulnerableTo(source);}
 @Override protected boolean canAddPassenger(Entity passenger){return combat.captured(passenger)&&getPassengers().isEmpty();}
 @Override public boolean shouldRiderSit(){return false;}
 public void syncRider(Vec3 point){Vec3 local=point.subtract(position());entityData.set(RIDER.get(0),(float)local.x);entityData.set(RIDER.get(1),(float)local.y);entityData.set(RIDER.get(2),(float)local.z);}
 @Override public Vec3 getPassengerRidingPosition(Entity passenger){
  if(level().isClientSide&&getPassengers().contains(passenger))return position().add(entityData.get(RIDER.get(0)),entityData.get(RIDER.get(1)),entityData.get(RIDER.get(2))).add(passenger.getVehicleAttachmentPoint(this));
  if(combat.captured(passenger))return combat.ridingPoint(passenger).add(passenger.getVehicleAttachmentPoint(this));
  return super.getPassengerRidingPosition(passenger);
 }
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){return level().isClientSide?passenger.position():combat.safeDismount(passenger);}
 @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide&&isDeadOrDying()){deathStartingY=getY();getNavigation().stop();setNoGravity(false);setDeltaMovement(Vec3.ZERO);setZza(0);setXxa(0);combat.onDeath();playSound(HarrowerMod.GRIND.get(),1.4F,.65F);}}
 @Override protected void tickDeath(){
  deathTime++;
  if(!level().isClientSide){
   setNoGravity(false);setDeltaMovement(0,Math.min(0,getDeltaMovement().y),0);
   setZza(0);setXxa(0);setCombatPose(DEATH,deathTime,false);corpseGrounding.update(this,deathStartingY);
   if(deathTime==340||deathTime==375)playSound(HarrowerMod.GRIND.get(),1.2F,deathTime==340?.58F:.48F);
   if(deathTime==120)gait.stomp(position().add(Vec3.directionFromRotation(0,getYRot()).scale(4)).add(0,.625,0),3);
   if(deathTime>=DEATH_DURATION)remove(RemovalReason.KILLED);
  }
 }
 @Override public void remove(RemovalReason reason){if(!level().isClientSide&&combat!=null)combat.removed();super.remove(reason);}
 /** Terrain collision is narrower than the arms' damageable silhouette. During collapse
  * the center of support travels with the chest, so the original ledge cannot hold it aloft. */
 public net.minecraft.world.phys.AABB terrainCollisionBox(net.minecraft.world.phys.AABB bounds){
  boolean dying=isDeadOrDying()||action()==DEATH;
  float t=dying?Mth.clamp((actionSeconds(0)*20-60)/60,0,1):0;
  float shift=t*t*(3-2*t);Vec3 center=position().add(Vec3.directionFromRotation(0,getYRot()).scale(5.5*shift));
  double half=dying?Mth.lerp(shift,1.8,1.3):1.8;
  if(!dying){
   // Weight is carried by the soles, not an invisible square behind the ankles.
   double a=Math.toRadians(getYRot()),c=Math.abs(Math.cos(a)),s=Math.abs(Math.sin(a));
   Vec3 soleCenter=position().add(Vec3.directionFromRotation(0,getYRot()).scale(-.25));
   double x=1.8*c+.7*s,z=1.8*s+.7*c;
   return new net.minecraft.world.phys.AABB(soleCenter.x-x,bounds.minY,soleCenter.z-z,soleCenter.x+x,bounds.maxY,soleCenter.z+z);
  }
  return new net.minecraft.world.phys.AABB(center.x-half,bounds.minY,center.z-half,center.x+half,bounds.minY+Mth.lerp(shift,11.5,4.8),center.z+half);
 }
 @Override protected void doPush(Entity other){if(combat!=null&&combat.charging())return;super.doPush(other);}
 @Override public boolean isPushable(){return false;}
 @Override public boolean removeWhenFarAway(double distance){return false;}
 @Override protected boolean shouldDespawnInPeaceful(){return false;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putDouble("HarrowerDeathStartingY",deathStartingY);tag.putInt("HarrowerCombatSchema",1);CompoundTag data=new CompoundTag();combat.save(data);tag.put("HarrowerCombat",data);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);deathStartingY=tag.contains("HarrowerDeathStartingY")?tag.getDouble("HarrowerDeathStartingY"):getY();
  if(Math.abs(getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()-.26)<1e-6)getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.286);
  if(tag.getInt("HarrowerCombatSchema")<1){float fraction=getHealth()/getMaxHealth();getAttribute(Attributes.MAX_HEALTH).setBaseValue(3000);getAttribute(Attributes.ARMOR).setBaseValue(16);getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(12);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.286);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(28);setHealth(getMaxHealth()*fraction);}
  else combat.load(tag.getCompound("HarrowerCombat"));
 }
}
