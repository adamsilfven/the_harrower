package com.leyline.harrower.combat;

import com.leyline.harrower.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.particles.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundSource;

/** Server-authoritative Advance / Assault / Correction controller. No block mutations. */
public final class HarrowerCombat {
 public enum Mode { ADVANCE,ASSAULT,CORRECTION,WAITING }
 private static final ResourceLocation BLACK_ARMOR=ResourceLocation.fromNamespaceAndPath("lc_harrower","black_suppression");
 private static final ResourceLocation GREEN_SPEED=ResourceLocation.fromNamespaceAndPath("lc_harrower","green_suppression");
 private static final ResourceKey<DamageType> IMPACT=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("lc_harrower","impact"));
 private static final ResourceKey<DamageType> CRUSH=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("lc_harrower","ground_crush"));
 private static final ResourceKey<DamageType> WAVE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("lc_harrower","ground_wave"));
 private static final ResourceKey<DamageType> GRASP=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("lc_harrower","grasp"));
 public final Harrower boss;public final Encounter encounter;
 private final Map<String,Integer> cooldowns=new HashMap<>();private final Set<UUID> struck=new HashSet<>();
 private final EnumMap<LeylineBridge.Color,Integer> suppressionTicks=new EnumMap<>(LeylineBridge.Color.class);
 private EnumSet<LeylineBridge.Color> suppressions=EnumSet.noneOf(LeylineBridge.Color.class);
 private boolean nextLeft;private String lastGroup="";private float clock,aimYaw;private int pause=40,sequence,flinchCooldown,stationaryTicks;private Vec3 oldTarget,chargeStart,chargeDirection,waveCenter;
 private Vec3 dryDetour,lastAdvancePosition,recoveryPosition;private Vec3 backoffPoint,lastBackoffPosition;private int backoffTicks;private float recoveryYaw;private int detourSide,stalledAdvanceTicks,recoveryTicks,recoveryStallTicks,recoveryAttempts;private double chargeDistance,chargeLimit;private LivingEntity forcedTarget,captured;private Vec3 captureSafe,releaseFrom;private int releaseTicks;private float rescueDamage;
 private Vec3 dropFrom;private int graspPulses;
 private boolean interrupted,allowDismount,debug,shutdown,manualAttack;private boolean autonomous=true;private Mode mode=Mode.WAITING;
 public HarrowerCombat(Harrower boss){this.boss=boss;encounter=new Encounter(boss);}
 public CombatMove move(){return CombatMove.byId(boss.action());}
 public float clock(){return clock;}
 public boolean charging(){return move()==CombatMove.CHARGE&&clock>=40&&clock<174;}
 public boolean captured(Entity e){return captured==e&&!allowDismount;}
 public boolean phaseTransition(){return move()==CombatMove.TRANSITION;}
 public boolean has(LeylineBridge.Color color){return suppressions.contains(color);}
 public boolean active(){return encounter.started;}
 public boolean autonomous(){return autonomous;}
 public boolean backingUp(){return backoffPoint!=null;}
 public void autonomy(boolean enabled){autonomous=enabled;if(!enabled){boss.getNavigation().stop();recoveryTicks=0;backoffPoint=null;}}
 public void debug(boolean value){debug=value;}
 public boolean debug(){return debug;}
 public void forceTarget(LivingEntity target){forcedTarget=target;boss.setTarget(target);}
 public void force(CombatMove move){if(move==CombatMove.DEATH){boss.hurt(boss.damageSources().genericKill(),Float.MAX_VALUE);return;}begin(move);manualAttack=true;}
 private void begin(CombatMove move){
  manualAttack=false;if(captured!=null)emergencyRelease();clock=0;struck.clear();interrupted=false;rescueDamage=0;releaseTicks=0;graspPulses=0;dropFrom=null;
  aimYaw=boss.getYRot();boss.getNavigation().stop();boss.setSpeed(0);boss.setZza(0);boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);boss.setCombatPose(move.id,0,true);
  if(move.cooldown>0)cooldowns.put(move.group,move==CombatMove.RUPTURE&&encounter.stage<=2?160:move.cooldown);
  backoffPoint=null;lastBackoffPosition=null;backoffTicks=0;recoveryTicks=0;recoveryStallTicks=0;recoveryAttempts=0;stalledAdvanceTicks=0;lastAdvancePosition=null;recoveryPosition=null;
  if(move.attack())lastGroup=move.group;
  if(move==CombatMove.TRANSITION)mode=Mode.ASSAULT;
  if(move.attack()||move==CombatMove.TRANSITION)boss.playSound(HarrowerMod.cue(move.group),1.2F,1F);
  if(debug)log("BEGIN "+move+" stage="+encounter.stage+" target="+(boss.getTarget()==null?"none":boss.getTarget().getUUID()));
 }
 public void tick(){
  if(shutdown||boss.level().isClientSide||!boss.isAlive())return;
  encounter.tick();updateSuppressions();cooldowns.replaceAll((key,ticks)->Math.max(0,ticks-1));if(pause>0)pause--;if(flinchCooldown>0)flinchCooldown--;
  LivingEntity target=forcedTarget!=null&&forcedTarget.isAlive()?forcedTarget:encounter.chooseTarget();
  if(move()==CombatMove.NONE){boss.setTarget(target);if(transitionNeeded())return;idle(target);}
  else runAction(target);
  if(boss.tickCount%240==60&&move()==CombatMove.NONE)boss.playSound(HarrowerMod.BREATH.get(),.7F,.85F);

  preventTorsoEmbedding();
 }
 private boolean transitionNeeded(){
  if(!encounter.started)return false;float fraction=boss.getHealth()/boss.getMaxHealth();
  if(encounter.stage==1&&fraction<.70F){encounter.stage=2;begin(CombatMove.TRANSITION);return true;}
  if(encounter.stage==2&&fraction<.35F){encounter.stage=3;begin(CombatMove.TRANSITION);return true;}return false;
 }
 private void idle(LivingEntity target){
  if(!autonomous||target==null||!target.isAlive()){mode=Mode.WAITING;boss.getNavigation().stop();recoveryTicks=0;backoffPoint=null;return;}
  boss.getLookControl().setLookAt(target,2,2);double distance=boss.position().subtract(target.position()).horizontalDistance();
  if(oldTarget!=null&&oldTarget.distanceToSqr(target.position())<.003)stationaryTicks++;else stationaryTicks=Math.max(0,stationaryTicks-3);oldTarget=target.position();
  float bearing=Mth.wrapDegrees(yawTo(target)-boss.getYRot());
  boolean lane=canEngage(target);
  if(backoffPoint!=null){
   if(backoffTicks++<70&&horizontal(boss.position(),backoffPoint)>.3&&dryRoute(boss.position(),backoffPoint)){
    boss.getNavigation().stop();boss.getMoveControl().setWantedPosition(backoffPoint.x,backoffPoint.y,backoffPoint.z,.9);mode=Mode.ADVANCE;return;
   }
   backoffPoint=null;stalledAdvanceTicks=12;lastAdvancePosition=boss.position();recoveryAttempts=0;
  }
  if(recoveryTicks>0){
   boolean progressing=recoveryPosition==null||horizontal(boss.position(),recoveryPosition)>=.015||Math.abs(Mth.wrapDegrees(boss.getYRot()-recoveryYaw))>.25;
   recoveryYaw=boss.getYRot();
   recoveryPosition=boss.position();recoveryStallTicks=progressing?0:recoveryStallTicks+1;
   if(recoveryStallTicks<12&&(!lane||distance>7.5)&&!boss.getNavigation().isDone()){
    recoveryTicks--;mode=Mode.ADVANCE;return;
   }
   if(recoveryStallTicks>=12){stalledAdvanceTicks=12;lastAdvancePosition=boss.position();if(debug)log("RECOVERY_ABORT_STALLED");}
   recoveryTicks=0;boss.getNavigation().stop();
  }
  if(Math.abs(bearing)>50&&distance>5&&dryDetour==null){
   boss.getNavigation().stop();boss.setSpeed(0);boss.setZza(0);boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);
   boss.setYRot(Mth.approachDegrees(boss.getYRot(),yawTo(target),3F));mode=Mode.CORRECTION;return;
  }
  if(pause==0&&lane){CombatMove chosen=choose(distance,bearing,target);if(chosen!=CombatMove.NONE){mode=Math.abs(bearing)>65?Mode.CORRECTION:Mode.ASSAULT;begin(chosen);return;}}
  if(pause>0){mode=Mode.WAITING;boss.getNavigation().stop();return;}
  mode=Mode.ADVANCE;
  if(pause==0&&lane&&distance<=6.6&&Math.abs(bearing)<=65){CombatMove fist=nextLeft?CombatMove.FIST_LEFT:CombatMove.FIST_RIGHT;float wanted=yawTo(target)-StrikeVolumes.aimOffset(fist.side==0);boss.getNavigation().stop();boss.setYRot(Mth.approachDegrees(boss.getYRot(),wanted,1.5F));mode=Mode.CORRECTION;return;}
  if(!dryRoute(boss.position(),target.position())){
   if(dryDetour==null||horizontal(boss.position(),dryDetour)<1.2||!dryRoute(boss.position(),dryDetour))dryDetour=findDryDetour(target.position());
   if(dryDetour!=null){boss.getNavigation().stop();boss.getMoveControl().setWantedPosition(dryDetour.x,dryDetour.y,dryDetour.z,.9);return;}
   boss.getNavigation().stop();boss.setSpeed(0);boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);return;
  }else{dryDetour=null;detourSide=0;}
  if(!lane||distance>5.4||Math.abs(bearing)>80){
   if(lastAdvancePosition!=null&&horizontal(boss.position(),lastAdvancePosition)<.015)stalledAdvanceTicks++;else stalledAdvanceTicks=0;
   lastAdvancePosition=boss.position();
   // Do not overwrite a valid path every tick when direct steering is stuck on terrain.
   if(stalledAdvanceTicks>=12){stalledAdvanceTicks=0;
    if(lastBackoffPosition==null||horizontal(lastBackoffPosition,boss.position())>4){
     Vec3 backward=boss.position().subtract(target.position()).multiply(1,0,1).normalize().scale(2.4),end=boss.position().add(backward);
     boolean clear=dryRoute(boss.position(),end)&&boss.level().noCollision(boss,boss.terrainCollisionBox(boss.getBoundingBox()).move(backward));
     // Existing overlap at the ledge must not prevent an ordinary retreat out of it.
     // Native collision still governs every movement tick; the destination must be clear.
     for(int i=1;i<=8&&clear;i++){Vec3 at=boss.position().add(backward.scale(i/8.0));clear=walkSupport(at);}
     if(clear){backoffPoint=end;lastBackoffPosition=boss.position();backoffTicks=0;boss.getNavigation().stop();boss.getMoveControl().setWantedPosition(end.x,end.y,end.z,.9);if(debug)log("RECOVERY_BACKSTEP");return;}
    }
    net.minecraft.world.level.pathfinder.Path path=recoveryAttempts==0?boss.getNavigation().createPath(target,0):approachPath(target);
    recoveryAttempts++;
    if(path!=null&&path.canReach()&&boss.getNavigation().moveTo(path,.9)){
     recoveryTicks=400;recoveryStallTicks=0;recoveryPosition=boss.position();recoveryYaw=boss.getYRot();
     if(debug)log("RECOVERY_PATH attempt="+recoveryAttempts+" nodes="+path.getNodeCount());return;
    }
   }
   Vec3 step=target.position().subtract(boss.position()).multiply(1,0,1).normalize().scale(.6);
   boolean support=walkSupport(boss.position().add(step));
   boolean clearStep=boss.level().noCollision(boss,boss.terrainCollisionBox(boss.getBoundingBox()).move(step).move(0,1.2,0));
   if(support&&dryRoute(boss.position(),boss.position().add(step.scale(4)))&&(clearStep||boss.level().noCollision(boss,boss.terrainCollisionBox(boss.getBoundingBox()).move(step)))){boss.getNavigation().stop();boss.getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),.9);}else boss.getNavigation().moveTo(target,.9);
  }else boss.getNavigation().stop();
 }
 /** Try reachable dry intermediate ground when a full target path has stalled. */
 private net.minecraft.world.level.pathfinder.Path approachPath(LivingEntity target){
  double bearing=Math.atan2(target.getZ()-boss.getZ(),target.getX()-boss.getX());
  net.minecraft.world.level.pathfinder.Path best=null;double score=Double.MAX_VALUE;
  int preferred=recoveryAttempts%2==0?1:-1;
  for(int degrees:new int[]{45*preferred,-45*preferred,90*preferred,-90*preferred,135*preferred,-135*preferred}){
   double a=bearing+Math.toRadians(degrees),x=boss.getX()+Math.cos(a)*6,z=boss.getZ()+Math.sin(a)*6;
   for(int y=Mth.floor(boss.getY()+1.2);y>=Mth.floor(boss.getY()-3);y--){
    BlockPos floor=BlockPos.containing(x,y,z);if(!boss.level().hasChunkAt(floor))break;
    var state=boss.level().getBlockState(floor);var shape=state.getCollisionShape(boss.level(),floor);
    if(shape.isEmpty())continue;
    Vec3 end=new Vec3(x,y+shape.max(net.minecraft.core.Direction.Axis.Y),z);
    if(!dryRoute(boss.position(),end)||!boss.level().noCollision(boss,boss.terrainCollisionBox(boss.getBoundingBox()).move(end.subtract(boss.position()))))break;
    var path=boss.getNavigation().createPath(BlockPos.containing(end),0);
    if(path!=null&&path.canReach()&&path.getNodeCount()>1){
     double candidate=horizontal(end,target.position())+path.getNodeCount()*.15;
     if(candidate<score){score=candidate;best=path;}
    }break;
   }
  }return best;
 }
 private boolean walkSupport(Vec3 at){
  for(int y=Mth.floor(boss.getY()+1.2);y>=Mth.floor(boss.getY()-4);y--){
   BlockPos pos=BlockPos.containing(at.x,y,at.z);var state=boss.level().getBlockState(pos);
   if(state.getBlock() instanceof net.minecraft.world.level.block.BushBlock||state.is(net.minecraft.tags.BlockTags.LEAVES))continue;
   var shape=state.getCollisionShape(boss.level(),pos);if(shape.isEmpty())continue;
   double delta=y+shape.max(net.minecraft.core.Direction.Axis.Y)-boss.getY();return delta<=boss.maxUpStep()+.01&&delta>=-3.5;
  }return false;
 }
 public boolean safeAdvance(Vec3 toward){return dryRoute(boss.position(),toward);}
 private Vec3 findDryDetour(Vec3 target){
  Vec3 delta=target.subtract(boss.position());double base=Math.atan2(delta.z,delta.x),best=Double.MAX_VALUE;Vec3 chosen=null;
  for(int degrees:new int[]{60,-60,90,-90,120,-120})for(int length:new int[]{4,8}){
   if(detourSide!=0&&Integer.signum(degrees)!=detourSide)continue;
   double a=base+Math.toRadians(degrees);Vec3 end=boss.position().add(Math.cos(a)*length,0,Math.sin(a)*length);
   if(!dryRoute(boss.position(),end))continue;boolean supported=true;
   for(int i=1;i<=length*2;i++)if(!walkSupport(boss.position().lerp(end,i/(double)(length*2)))){supported=false;break;}
   if(!supported)continue;double score=horizontal(end,target)+length*.2;
   if(score<best){best=score;chosen=end;}
  }
  if(chosen!=null&&detourSide==0){Vec3 d=chosen.subtract(boss.position());detourSide=delta.x*d.z-delta.z*d.x>=0?1:-1;}
  return chosen;
 }
 private boolean dryRoute(Vec3 from,Vec3 to){
  int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)*2));
  for(int i=0;i<=steps;i++){
   Vec3 at=from.lerp(to,i/(double)steps);
   for(int y=Mth.floor(at.y+1);y>=Mth.floor(at.y-4);y--){
    BlockPos p=BlockPos.containing(at.x,y,at.z);if(!boss.level().getFluidState(p).isEmpty())return false;
    if(!boss.level().getBlockState(p).getCollisionShape(boss.level(),p).isEmpty())break;
   }
  }return true;
 }
 private CombatMove choose(double d,float bearing,LivingEntity target){
  boolean left=bearing<0;double elevation=Math.abs(target.getY()-boss.getY());
  if(elevation>4)return CombatMove.NONE;
  if(encounter.stage>=3&&d<=24&&ready("hollowing")){sequence++;return CombatMove.HOLLOWING;}
  if(encounter.stage<=2&&d>=3&&d<=16&&ready("rupture")&&(!lastGroup.equals("rupture")||d>8)){sequence++;return CombatMove.RUPTURE;}
  if(Math.abs(bearing)>135&&d<=6.5&&ready("heel")){sequence++;return left?CombatMove.HEEL_LEFT:CombatMove.HEEL_RIGHT;}
  if(Math.abs(bearing)>60&&Math.abs(bearing)<=150&&d<=8&&ready("sweep")){sequence++;return left?CombatMove.SWEEP_LEFT:CombatMove.SWEEP_RIGHT;}
  if(d<5.4&&ready("stomp")){sequence++;return left?CombatMove.STOMP_LEFT:CombatMove.STOMP_RIGHT;}
  if(encounter.stage>=2&&d<=5&&Math.abs(bearing)<60&&ready("grasp")&&!lastGroup.equals("grasp")){sequence++;return left?CombatMove.GRASP_LEFT:CombatMove.GRASP_RIGHT;}
  if(encounter.stage>=2&&d>=13&&d<=36&&Math.abs(bearing)<55&&ready("charge")&&boss.hasLineOfSight(target)&&dryRoute(boss.position(),target.position())){sequence++;return CombatMove.CHARGE;}
  if(d<=16&&d>=5.5&&ready("rupture")&&(d>7||Math.abs(bearing)>80||stationaryTicks>80)){sequence++;return CombatMove.RUPTURE;}
  if(d<=6.6&&Math.abs(bearing)<=65){CombatMove fist=nextLeft?CombatMove.FIST_LEFT:CombatMove.FIST_RIGHT;float wanted=bearing-StrikeVolumes.aimOffset(fist.side==0);if(Math.abs(Mth.wrapDegrees(wanted))<=12){sequence++;nextLeft=!nextLeft;return fist;}}
  return CombatMove.NONE;
 }
 private boolean ready(String group){return cooldowns.getOrDefault(group,0)==0;}
 private float yawTo(LivingEntity target){return (float)Math.toDegrees(Math.atan2(target.getZ()-boss.getZ(),target.getX()-boss.getX()))-90;}
 private void runAction(LivingEntity target){
  CombatMove move=move();float previous=clock;
  boolean windup=clock<move.windup;
  float rate=move.attack()&&windup&&has(LeylineBridge.Color.BLUE)?.8F:move.attack()&&move.recovery(clock)&&encounter.stage>=3?1/.85F:1;
  clock=Math.min(move.duration,clock+rate);boss.getNavigation().stop();
  if(move!=CombatMove.CHARGE){boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);boss.setZza(0);boss.setXxa(0);}
  if(!manualAttack&&move.attack()&&previous<move.windup*.75&&target!=null&&target.isAlive()){
   float desired=yawTo(target);
   if(move.group.equals("fist"))desired-=StrikeVolumes.aimOffset(move.side==0);
   if(move.grasp())desired-=StrikeVolumes.aimOffset(move.clip,move.hand(),move.windup);
   if(!move.footStrike())aimYaw=desired;
   boss.setYRot(Mth.approachDegrees(boss.getYRot(),aimYaw,3F));
  }
  boss.yBodyRot=boss.getYRot();boss.yHeadRot=boss.getYRot();
  boolean impact=previous<move.windup&&clock>=move.windup;
  if(move.handStrike()&&clock>=move.windup&&previous<move.windup+move.active)handContact(move,previous);
  if(windup&&move.attack())anticipation(move);
  if(impact){
   boss.playSound(HarrowerMod.IMPACT.get(),move.wave()?2.5F:1.6F,move.wave()?.85F:1F);
   boss.level().broadcastEntityEvent(boss,(byte)(move.wave()?61:60));
   if(move.footStrike()){Vec3 point=footPoint(move.side);localImpact(point,move.group.equals("heel")?3.5:6,move);boss.groundImpact(point.add(0,.625,0),2);}
   else if(move==CombatMove.RUPTURE){waveCenter=StrikeVolumes.groundPoint(boss,move.clip,move.windup);boss.groundImpact(waveCenter.add(0,.625,0),2.5F);}
   else if(move==CombatMove.HOLLOWING){waveCenter=boss.position();boss.playSound(HarrowerMod.BREATH.get(),2,.6F);}
   else if(move==CombatMove.CHARGE){chargeStart=boss.position();chargeDirection=Vec3.directionFromRotation(0,boss.getYRot());chargeDistance=0;LivingEntity aim=boss.getTarget();chargeLimit=aim==null?12:Math.max(0,Math.min(24,horizontal(aim.position(),boss.position())-2.25));}
   else if(move.grasp())tryCapture(move);
   else if(move.handStrike())boss.groundImpact(StrikeVolumes.groundPoint(boss,move.clip,move.windup).add(0,.625,0),1.6F);
  }
  if(move.wave()&&clock>=move.windup&&previous<move.windup+move.active)wave(move,previous);
  if(move==CombatMove.CHARGE&&clock>=40&&previous<174){if(!charge(move))return;}
  if(move.grasp())hold(move,previous);
  if(move==CombatMove.HOLLOWING&&windup&&boss.tickCount%8==0)chestDust();
  if(move==CombatMove.TRANSITION&&boss.tickCount%12==0)chestDust();
  boss.setCombatPose(move.id,clock,false);
  if(clock>=move.duration){if(captured!=null)emergencyRelease();begin(CombatMove.NONE);pause=move==CombatMove.STAGGER?0:move.attack()?14:10;}
 }
 private void anticipation(CombatMove move){
  if(boss.tickCount%5!=0)return;
  if(move.footStrike()){
   Vec3 at=footPoint(move.side);double radius=move.group.equals("heel")?3.5:6;
   warningRing(at,radius);
  }else if(move==CombatMove.HOLLOWING){
   Vec3 chest=boss.position().add(Vec3.directionFromRotation(0,boss.getYRot())).add(0,6,0);
   var level=(ServerLevel)boss.level();
   for(int i=0;i<6;i++){double a=boss.tickCount*.13+i*Math.PI/3;Vec3 start=chest.add(Math.cos(a)*2.5,Math.sin(a*1.7),Math.sin(a)*2.5);Vec3 inward=chest.subtract(start).scale(.08);level.sendParticles(ParticleTypes.ASH,start.x,start.y,start.z,0,inward.x,inward.y,inward.z,1);}
  }
 }
 private void warningRing(Vec3 center,double radius){
  int count=24;for(int i=0;i<count;i++){
   double a=i*Math.PI*2/count;Vec3 p=center.add(Math.cos(a)*radius,0,Math.sin(a)*radius);
   ((ServerLevel)boss.level()).sendParticles(ParticleTypes.POOF,p.x,groundAt(p)+.12,p.z,1,.04,.02,.04,0);
  }
 }
 private void handContact(CombatMove move,float previous){
  AABB box=StrikeVolumes.hand(boss,move.clip,clock,move.hand()).minmax(StrikeVolumes.hand(boss,move.clip,Math.max(move.windup,previous),move.hand()));
  if(debug)outline(box);
  for(LivingEntity victim:victims(box))if(!struck.contains(victim.getUUID())&&groundSight(victim)){
   struck.add(victim.getUUID());deal(victim,move);victim.knockback(.65,boss.getX()-victim.getX(),boss.getZ()-victim.getZ());
  }
  if(move.group.equals("sweep")&&boss.tickCount%3==0)dust(box.getCenter(),3);
 }
 private List<LivingEntity> victims(AABB bounds){return boss.level().getEntitiesOfClass(LivingEntity.class,bounds,e->e!=boss&&e.isAlive()&&!(e instanceof Harrower)&&!(e instanceof ServerPlayer p&&(p.isCreative()||p.isSpectator())));}
 public boolean canEngage(LivingEntity target){return groundSight(target)&&Math.abs(target.getY()-boss.getY())<=1.5;}
 private boolean groundSight(LivingEntity victim){return rayClear(boss.position().add(0,1.2,0),victim.position().add(0,Math.min(1,victim.getBbHeight()*.6),0));}
 private boolean rayClear(Vec3 from,Vec3 to){return boss.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getType()==HitResult.Type.MISS;}
 private DamageSource source(CombatMove move){var key=move.grasp()?GRASP:move.shockDamage()?WAVE:move.footStrike()?CRUSH:IMPACT;return new DamageSource(boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),boss);}
 public float outgoing(CombatMove move){float damage=move.damage;if(has(LeylineBridge.Color.WHITE))damage*=.85F;if(move.shockDamage()&&has(LeylineBridge.Color.RED))damage*=.65F;return damage;}
 private boolean deal(LivingEntity victim,CombatMove move){boolean result=victim.hurt(source(move),outgoing(move));if(debug)log("CONTACT "+move+" victim="+victim.getUUID()+" base="+outgoing(move)+" accepted="+result);return result;}
 private void localImpact(Vec3 point,double radius,CombatMove move){
  ring(point,radius);AABB area=new AABB(point.x-radius,point.y-.6,point.z-radius,point.x+radius,point.y+3,point.z+radius);
  for(LivingEntity victim:victims(area))if(horizontal(victim.position(),point)<=radius&&grounded(victim)&&rayClear(point.add(0,1.1,0),victim.position().add(0,.9,0))){
   if(deal(victim,move)&&move.group.equals("stomp")){victim.setDeltaMovement(victim.getDeltaMovement().add(0,.7,0));victim.hurtMarked=true;victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0));}
  }
 }
 private boolean grounded(LivingEntity victim){return victim.onGround()||(Math.abs(victim.getDeltaMovement().y)<.15&&!boss.level().noCollision(victim,victim.getBoundingBox().move(0,-.12,0)));}
 private double horizontal(Vec3 a,Vec3 b){return Math.hypot(a.x-b.x,a.z-b.z);}
 private Vec3 footPoint(int side){double a=Math.toRadians(boss.getYRot()),c=Math.cos(a),s=Math.sin(a),x=boss.serverAnkle(side,0),z=boss.serverAnkle(side,2);return boss.position().add(x*c+z*s,boss.serverAnkle(side,1)-.625,x*s-z*c);}
 private void wave(CombatMove move,float previous){
  if(waveCenter==null)waveCenter=boss.position();double speed=move==CombatMove.RUPTURE?.35:.5,range=move==CombatMove.RUPTURE?12:24;
  double before=Math.max(0,previous-move.windup)*speed,radius=Math.min(range,Math.max(0,clock-move.windup)*speed);
  if(move==CombatMove.RUPTURE)ring(waveCenter,radius);else hollowRing(waveCenter,radius);
  var area=new AABB(waveCenter.x-range,waveCenter.y-3,waveCenter.z-range,waveCenter.x+range,waveCenter.y+6,waveCenter.z+range);
  for(LivingEntity victim:victims(area)){
   double distance=horizontal(victim.position(),waveCenter);if(distance>range+victim.getBbWidth()*.5)continue;if(distance<before-.65||distance>radius+.65||struck.contains(victim.getUUID()))continue;
   if(move==CombatMove.RUPTURE){if(!grounded(victim)||Math.abs(victim.getY()-groundAt(victim.position()))>.65||!rayClear(waveCenter.add(0,1.1,0),victim.position().add(0,.8,0)))continue;}
   else if(solidCover(victim))continue;
   struck.add(victim.getUUID());boolean hit=deal(victim,move);if(hit&&move==CombatMove.RUPTURE){
    Vec3 outward=victim.position().subtract(waveCenter).multiply(1,0,1).normalize();
    if(outward.lengthSqr()<.01)outward=Vec3.directionFromRotation(0,boss.getYRot());
    double resistance=Mth.clamp(victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1);
    double impulseSpeed=1.2*(1-.4*resistance);Vec3 velocity=victim.getDeltaMovement();
    double addition=Math.max(0,impulseSpeed-velocity.dot(outward));
    victim.setDeltaMovement(velocity.x+outward.x*addition,Math.max(velocity.y,.32),velocity.z+outward.z*addition);victim.hurtMarked=true;
   }if(move==CombatMove.HOLLOWING&&victim instanceof ServerPlayer player)HollowingEffects.suppress(player,160);
  }
 }
 /** Four body rays must each meet a full collision cube. Partial blocks do not qualify. */
 public boolean solidCover(LivingEntity victim){
  Vec3 origin=boss.position().add(0,5,0);double width=victim.getBbWidth()*.4;
  for(Vec3 offset:new Vec3[]{new Vec3(-width,.3,0),new Vec3(width,.9,0),new Vec3(0,victim.getBbHeight()*.9,width),new Vec3(0,victim.getBbHeight()*.9,-width)}){
   Vec3 end=victim.position().add(offset),delta=end.subtract(origin);int steps=(int)Math.ceil(delta.length()*5);boolean blocked=false;
   for(int i=1;i<steps;i++){BlockPos p=BlockPos.containing(origin.add(delta.scale(i/(double)steps)));if(!boss.level().hasChunkAt(p))continue;var state=boss.level().getBlockState(p);if(state.isCollisionShapeFullBlock(boss.level(),p)){blocked=true;break;}}
   if(!blocked)return false;
  }return true;
 }
 private boolean charge(CombatMove move){
  if(chargeStart==null){chargeStart=boss.position();chargeDirection=Vec3.directionFromRotation(0,boss.getYRot());}
  chargeDistance=horizontal(boss.position(),chargeStart);double maximum=Math.min(chargeLimit,has(LeylineBridge.Color.GREEN)?16.8:24);
  Vec3 step=chargeDirection.scale(Math.min(.18*(has(LeylineBridge.Color.GREEN)?.8:1),Math.max(0,maximum-chargeDistance)));
  if(chargeDistance>=maximum||(boss.getTarget()!=null&&(horizontal(boss.position(),boss.getTarget().position())<=2.25||boss.getTarget().position().subtract(boss.position()).dot(chargeDirection)<=0))||!dryRoute(boss.position(),boss.position().add(step.scale(8)))){clock=174;boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);return true;}
  if(boss.horizontalCollision||boss.level().getBlockCollisions(boss,boss.terrainCollisionBox(boss.getBoundingBox()).move(step)).iterator().hasNext()){
   boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);boss.groundImpact(boss.position().add(chargeDirection.scale(4)).add(0,.625,0),2);begin(CombatMove.STAGGER);return false;
  }
  var core=new AABB(boss.getX()-4.6,boss.getY(),boss.getZ()-4.0,boss.getX()+4.6,boss.getY()+6,boss.getZ()+4.0);
  var front=core.minmax(core.move(step));
  var right=StrikeVolumes.hand(boss,move.clip,clock,"right_hand");var left=StrikeVolumes.hand(boss,move.clip,clock,"left_hand");
  var query=front.minmax(right).minmax(left);
  for(LivingEntity victim:victims(query))if(!struck.contains(victim.getUUID())&&chargeContact(victim.getBoundingBox(),step,right,left)&&groundSight(victim)){struck.add(victim.getUUID());deal(victim,move);victim.knockback(1.1,-chargeDirection.x,-chargeDirection.z);}
  if(!struck.isEmpty()){clock=174;boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);return true;}
  boss.setDeltaMovement(step.x,boss.getDeltaMovement().y,step.z);return true;
 }
 // The broad-phase AABB is only a query; contact follows the charge direction.
 private boolean chargeContact(AABB target,Vec3 step,AABB right,AABB left){
  Vec3 center=target.getCenter().subtract(boss.position());
  double halfX=target.getXsize()*.5,halfZ=target.getZsize()*.5;
  double forward=center.x*chargeDirection.x+center.z*chargeDirection.z;
  double forwardExtent=Math.abs(chargeDirection.x)*halfX+Math.abs(chargeDirection.z)*halfZ;
  double side=center.x*(-chargeDirection.z)+center.z*chargeDirection.x;
  double sideExtent=Math.abs(chargeDirection.z)*halfX+Math.abs(chargeDirection.x)*halfZ;
  if(forward+forwardExtent<=0)return false;
  boolean height=target.maxY>boss.getY()&&target.minY<boss.getY()+6;
  boolean body=height&&forward-forwardExtent<=4.0+step.length()&&Math.abs(side)<=3.2+sideExtent;
  boolean handReach=forward-forwardExtent<=4.8+step.length()&&Math.abs(side)<=3.8+sideExtent;
  return body||(handReach&&(right.intersects(target)||left.intersects(target)));
 }
 private void tryCapture(CombatMove move){
  AABB box=StrikeVolumes.hand(boss,move.clip,move.windup,move.hand()).inflate(.1);
  LivingEntity target=boss.getTarget();List<LivingEntity> candidates=victims(box);if(target!=null&&candidates.remove(target))candidates.add(0,target);
  for(LivingEntity victim:candidates)if(victim.distanceToSqr(boss)<=25&&groundSight(victim)&&!victim.isPassenger()){
   captured=victim;captureSafe=victim.position();boss.syncRider(ridingPoint(victim));if(!victim.startRiding(boss,true)){captured=null;continue;}if(debug)log("GRASP_CAPTURE "+victim.getUUID());return;
  }
  clock=move.windup+move.active;
 }
 private void hold(CombatMove move,float previous){
  if(captured==null)return;
  if(!captured.isAlive()||captured.isRemoved()||captured.getVehicle()!=boss){emergencyRelease();clock=move.windup+move.active;return;}
  captured.fallDistance=0;boss.syncRider(ridingPoint(captured));
  if(releaseTicks>0){releaseTicks--;if(releaseTicks==0){finishRelease(false);clock=move.windup+move.active;}return;}
  int due=Math.min(5,Math.max(0,(int)((clock-move.windup)/50)));
  while(!interrupted&&graspPulses<due&&captured!=null){
   graspPulses++;LivingEntity victim=captured;boolean accepted=deal(victim,move);
   boss.playSound(HarrowerMod.GRIND.get(),1.2F,.62F);
   if(debug)log("GRASP_PULSE number="+graspPulses+" clock="+clock+" accepted="+accepted);
   // A lethal pulse can synchronously release the victim through the death event.
   if(captured==null){clock=move.windup+move.active;return;}
  }
  if(previous<move.windup+move.active&&clock>=move.windup+move.active)finishRelease(!interrupted);
 }
 public void teammateDamage(DamageSource source,float actual){
  if(captured==null||interrupted||!(source.getEntity() instanceof ServerPlayer teammate)||teammate==captured)return;
  rescueDamage+=actual;if(rescueDamage>=40){interrupted=true;releaseFrom=ridingPoint(captured);releaseTicks=10;clock=Math.max(clock,move().windup+move().active-10);boss.setCombatPose(move().id,clock,true);if(debug)log("GRASP_INTERRUPTED actual="+rescueDamage);}
 }
 public Vec3 ridingPoint(Entity passenger){
  if(captured!=passenger)return boss.position().add(0,2,0);
  if(releaseTicks>0&&releaseFrom!=null){Vec3 end=new Vec3(captureSafe.x,groundAt(captureSafe)+.1,captureSafe.z);return releaseFrom.lerp(end,(10-releaseTicks)/10.0);}
  Vec3 torso=StrikeVolumes.grip(boss,move().clip,clock,move().hand());
  return torso.add(0,-passenger.getBbHeight()*.55,0);
 }
 public Vec3 safeDismount(LivingEntity passenger){
  if(dropFrom!=null)return dropFrom;
  if(captured==passenger&&!allowDismount)return ridingPoint(passenger);
  Vec3 preferred=captureSafe==null?passenger.position():captureSafe;
  for(int i=0;i<9;i++){
   Vec3 sample=i==0?preferred:boss.position().add(Math.cos((i-1)*Math.PI/4)*6,0,Math.sin((i-1)*Math.PI/4)*6);
   Vec3 floor=new Vec3(sample.x,groundAt(sample)+.02,sample.z);
   BlockPos support=BlockPos.containing(floor.add(0,-.08,0));
   if(!boss.level().getBlockState(support).getCollisionShape(boss.level(),support).isEmpty()&&boss.level().noCollision(passenger,passenger.getBoundingBox().move(floor.subtract(passenger.position()))))return floor;
  }return passenger.position();
 }
 private void finishRelease(boolean drop){
  LivingEntity victim=captured;if(victim==null)return;
  Vec3 point=ridingPoint(victim);dropFrom=drop?point:null;
  allowDismount=true;victim.stopRiding();allowDismount=false;
  victim.fallDistance=0;
  if(drop){
   // Keep the release at the raised hand. Vanilla gravity and fall damage take over.
   victim.teleportTo(point.x,point.y,point.z);victim.setOnGround(false);victim.setDeltaMovement(0,-.15,0);victim.hurtMarked=true;
   if(debug)log("GRASP_DROP clock="+clock+" height="+(point.y-groundAt(point))+" pulses="+graspPulses);
  }else victim.setDeltaMovement(Vec3.ZERO);
  captured=null;releaseTicks=0;dropFrom=null;
 }
 public void emergencyRelease(){
  if(captured==null)return;LivingEntity victim=captured;allowDismount=true;victim.stopRiding();allowDismount=false;
  if(victim.isAlive()&&captureSafe!=null&&victim.level()==boss.level()&&boss.level().noCollision(victim,victim.getBoundingBox().move(captureSafe.subtract(victim.position()))))victim.teleportTo(captureSafe.x,captureSafe.y,captureSafe.z);
  victim.fallDistance=0;victim.setDeltaMovement(Vec3.ZERO);captured=null;releaseTicks=0;
 }
 public void significantHit(float actual){boss.level().broadcastEntityEvent(boss,(byte)62);if(move()==CombatMove.NONE&&actual>=30&&flinchCooldown==0){begin(CombatMove.HURT);flinchCooldown=80;}}
 public double footLift(int side){CombatMove move=move();if(!move.footStrike()||move.side!=side)return 0;double t=clock;if(t<move.windup){double q=t/move.windup,u=q<.7?q/.7:(q-.7)/.3;u=u*u*(3-2*u);return .85*(q<.7?u:1-u);}double r=t-move.windup;if(r<10)return 0;if(r<30)return .45*Math.sin(Math.PI*(r-10)/20);return 0;}
 public double footBack(int side){if(!move().group.equals("heel")||move().side!=side)return 0;double t=clock;if(t<move().windup)return 2.5*Math.min(1,t/(move().windup*.7));if(t<move().windup+10)return 2.5;if(t<move().windup+30)return 2.5*(1-(t-move().windup-10)/20);return 0;}
 private void updateSuppressions(){
  var adapter=LeylineBridge.access();EnumSet<LeylineBridge.Color> next=EnumSet.noneOf(LeylineBridge.Color.class);
  if(adapter!=null){List<LeylineBridge.OwnedLeyline> eligible=new ArrayList<>();
   if(encounter.started&&encounter.summoner!=null)for(var leyline:adapter.ownedActive((ServerLevel)boss.level(),encounter.summoner))
    if(leyline.active()&&encounter.summoner.equals(leyline.owner())&&leyline.center().distanceToSqr(boss.position())<=4096){next.add(leyline.color());eligible.add(leyline);}
   adapter.updateSuppression((ServerLevel)boss.level(),boss.getUUID(),eligible,boss.level().getGameTime()+40);
  }
  if(!next.equals(suppressions)){suppressions=next;modifier(Attributes.ARMOR,BLACK_ARMOR,has(LeylineBridge.Color.BLACK)?-.5:0);modifier(Attributes.MOVEMENT_SPEED,GREEN_SPEED,has(LeylineBridge.Color.GREEN)?-.2:0);boss.setSuppressionMask(next.stream().mapToInt(c->1<<c.ordinal()).reduce(0,(a,b)->a|b));}
  for(var color:next)suppressionTicks.merge(color,1,Integer::sum);
 }
 private void modifier(net.minecraft.core.Holder<Attribute> attribute,ResourceLocation id,double amount){var instance=boss.getAttribute(attribute);instance.removeModifier(id);if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
 public void onDeath(){
  emergencyRelease();encounter.stopBar();var adapter=LeylineBridge.access();if(adapter!=null){int qualified=(int)suppressionTicks.values().stream().filter(t->t>=200).count();adapter.onDeath((ServerLevel)boss.level(),boss,new LeylineBridge.DeathContext(encounter.summoner,Map.copyOf(suppressionTicks),qualified));adapter.release((ServerLevel)boss.level(),boss.getUUID());}
  boss.setCombatPose(CombatMove.DEATH.id,0,true);shutdown=true;
 }
 public void removed(){emergencyRelease();encounter.stopBar();if(LeylineBridge.access()!=null)LeylineBridge.access().release((ServerLevel)boss.level(),boss.getUUID());}
 public void reset(){emergencyRelease();if(LeylineBridge.access()!=null)LeylineBridge.access().release((ServerLevel)boss.level(),boss.getUUID());encounter.reset();modifier(Attributes.ARMOR,BLACK_ARMOR,0);modifier(Attributes.MOVEMENT_SPEED,GREEN_SPEED,0);boss.setSuppressionMask(0);forcedTarget=null;boss.setTarget(null);sequence=0;nextLeft=false;lastGroup="";stationaryTicks=0;oldTarget=null;cooldowns.clear();suppressionTicks.clear();suppressions.clear();shutdown=false;begin(CombatMove.NONE);pause=40;}
 private void preventTorsoEmbedding(){
  for(ServerPlayer player:((ServerLevel)boss.level()).players())if(!player.isSpectator()&&player!=captured&&player.getY()+player.getBbHeight()>boss.getY()+4&&player.getY()<boss.getY()+9){double dx=player.getX()-boss.getX(),dz=player.getZ()-boss.getZ();if(Math.abs(dx)<3.4&&Math.abs(dz)<3.4){Vec3 push=new Vec3(dx,0,dz).normalize();if(push.lengthSqr()<.01)push=Vec3.directionFromRotation(0,boss.getYRot());player.setDeltaMovement(push.scale(.36));player.hurtMarked=true;}}
 }
 private double groundAt(Vec3 point){for(int y=Mth.floor(boss.getY()+2);y>=Mth.floor(boss.getY()-5);y--){BlockPos p=new BlockPos(Mth.floor(point.x),y,Mth.floor(point.z));if(!boss.level().hasChunkAt(p))return boss.getY();var shape=boss.level().getBlockState(p).getCollisionShape(boss.level(),p);if(!shape.isEmpty())return y+shape.max(net.minecraft.core.Direction.Axis.Y);}return boss.getY();}
 private void ring(Vec3 center,double radius){
  if(radius<.05||boss.tickCount%2!=0)return;
  int points=Math.min(64,Math.max(16,(int)(radius*6)));
  for(int i=0;i<points;i++){
   double angle=i*Math.PI*2/points;Vec3 at=center.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
   double y=groundAt(at);BlockPos floor=BlockPos.containing(at.x,y-.05,at.z);
   var state=boss.level().getBlockState(floor);if(state.isAir())continue;
   var level=(ServerLevel)boss.level();
   level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),at.x,y+.08,at.z,0,Math.cos(angle)*.07,.22,Math.sin(angle)*.07,1);
   if(i%3==0)level.sendParticles(ParticleTypes.POOF,at.x,y+.1,at.z,1,.15,.08,.15,.025);
  }
 }
 private void hollowRing(Vec3 center,double radius){
  if(radius<.05||boss.tickCount%2!=0)return;
  int count=64;for(int i=0;i<count;i++){double a=2*Math.PI*i/count;double x=center.x+Math.cos(a)*radius,z=center.z+Math.sin(a)*radius;
   double y=groundAt(new Vec3(x,center.y,z));var level=(ServerLevel)boss.level();
   level.sendParticles(ParticleTypes.LARGE_SMOKE,x,y+.65,z,2,.2,.4,.2,.04);
   if(i%3==0)level.sendParticles(ParticleTypes.SOUL,x,y+.7,z,0,Math.cos(a)*.18,.025,Math.sin(a)*.18,1);}

 }
 private void dust(Vec3 point,int count){((ServerLevel)boss.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState()),point.x,point.y,point.z,count,.10,.08,.10,.02);}
 private void chestDust(){dust(boss.position().add(Vec3.directionFromRotation(0,boss.getYRot()).scale(1)).add(0,6,0),4);}
 private void outline(AABB box){for(int i=0;i<8;i++)((ServerLevel)boss.level()).sendParticles(ParticleTypes.END_ROD,(i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ,1,0,0,0,0);}
 private void log(String value){org.slf4j.LoggerFactory.getLogger("HarrowerCombat").info("{} {}",boss.getUUID(),value);}
 public String status(){return "move="+move()+" detour="+dryDetour+" mode="+mode+" clock="+clock+" stage="+encounter.stage+" hp="+boss.getHealth()+"/"+boss.getMaxHealth()+" bar_viewers="+encounter.barViewers()+" bar_closed="+encounter.barClosed()+" roster="+encounter.roster()+" counted="+encounter.countedPlayers+" suppressions="+suppressions+" adapter="+LeylineBridge.status()+" cooldowns="+cooldowns+" rescue="+rescueDamage+" captured="+(captured==null?"none":captured.getUUID()+" mounted="+(captured.getVehicle()==boss));}
 public void save(CompoundTag tag){CompoundTag encounterTag=new CompoundTag();encounter.save(encounterTag);tag.put("Encounter",encounterTag);CompoundTag cd=new CompoundTag();cooldowns.forEach(cd::putInt);tag.put("Cooldowns",cd);tag.putInt("Sequence",sequence);tag.putBoolean("NextLeft",nextLeft);tag.putString("LastGroup",lastGroup);CompoundTag times=new CompoundTag();suppressionTicks.forEach((color,t)->times.putInt(color.name(),t));tag.put("SuppressionTicks",times);tag.putBoolean("Transition",phaseTransition());tag.putFloat("TransitionClock",clock);}
 public void load(CompoundTag tag){encounter.load(tag.getCompound("Encounter"));CompoundTag cd=tag.getCompound("Cooldowns");for(String key:cd.getAllKeys())cooldowns.put(key,cd.getInt(key));sequence=tag.getInt("Sequence");nextLeft=tag.getBoolean("NextLeft");lastGroup=tag.getString("LastGroup");CompoundTag times=tag.getCompound("SuppressionTicks");for(var color:LeylineBridge.Color.values())suppressionTicks.put(color,times.getInt(color.name()));pause=40;if(tag.getBoolean("Transition")){begin(CombatMove.TRANSITION);clock=tag.getFloat("TransitionClock");boss.setCombatPose(CombatMove.TRANSITION.id,clock,true);}}
}
