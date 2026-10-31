package com.leyline.hollowforged.client;
import com.leyline.hollowforged.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
/** Bounded client presentation. Surface replicas never alter terrain or collision. */
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class RelicEffects {
 private static final Minecraft MC=Minecraft.getInstance();
 private static final Vector3f BRASS=new Vector3f(.76F,.59F,.29F),STONE=new Vector3f(.40F,.43F,.41F),PALE=new Vector3f(.77F,.75F,.64F);
 private static final Map<Long,State> states=new HashMap<>();
 private static final List<Burst> bursts=new ArrayList<>();
 private static final List<Wave> waves=new ArrayList<>();
 private static final Map<Long,Long> lastOpening=new HashMap<>();
 private static long renderedTiles;
 public static String metrics(){if(renderedTiles==0)throw new IllegalStateException("Ground waves never rendered");return "ground tile draws="+renderedTiles;}
 private static Object world;private static long clock;
 private record State(HFEffects.Effect event,long born,long end){}
 private record Burst(HFEffects.Effect event,long born){}
 private record Tile(BlockPos pos,BlockState state,double distance){}
 private record Wave(HFEffects.Effect event,long born,List<Tile> tiles){}
 static {HFEffects.receive=RelicEffects::accept;}
 // Installed by client registration as well, before the first network event arrives.
 public static void install(){HFEffects.receive=RelicEffects::accept;}
 private static long key(int entity,int kind){return ((long)entity<<32)|(kind&0xffffffffL);}
 public static void accept(HFEffects.Effect e){
  if(MC.level==null)return;resetWorld();
  EffectsReview.received(e.kind());
  long key=key(e.entity(),e.kind());
  if(e.mode()==2){if(states.remove(key)!=null){sound(e,e.kind()==HFEffects.SIGHT?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.DEEPSLATE_STEP,.4F,.65F);dust(at(e).add(0,e.kind()==HFEffects.SIGHT?1.9:.2,0),8,STONE,.05);if(e.kind()==HFEffects.SOVEREIGN)circle(at(e).add(0,.12,0),16,32,STONE,-.03);}return;}
  if(e.mode()==1){State previous=states.get(key);states.put(key,new State(e,previous==null?clock:previous.born,clock+e.duration()));
   if(previous==null){if(e.kind()!=HFEffects.LAUNCH)opening(e);if(e.kind()==HFEffects.SOVEREIGN)addWave(e);}
   if(states.size()>384){var oldest=states.entrySet().stream().min(Comparator.comparingLong(x->x.getValue().born));oldest.ifPresent(x->states.remove(x.getKey()));}return;
  }
  if(e.kind()==HFEffects.WITNESS){states.remove(key(e.entity(),HFEffects.SIGHT_CHARGE));}
  if(e.kind()==HFEffects.FRACTURE||e.kind()==HFEffects.ARGUMENT)states.remove(key(e.entity(),HFEffects.HAMMER_CHARGE));
  opening(e);bursts.add(new Burst(e,clock));if(bursts.size()>64)bursts.remove(0);
  if(isGround(e))addWave(e);
  if(Boolean.getBoolean("lc_hollowforged.effects_review"))ReviewCapture.evidence("FX_RECEIVED_"+e.kind());
 }
 private static void resetWorld(){if(world!=MC.level){states.clear();bursts.clear();waves.clear();lastOpening.clear();world=MC.level;clock=0;}}
 private static boolean isGround(HFEffects.Effect e){return e.kind()==HFEffects.FRACTURE||e.kind()==HFEffects.ARGUMENT||e.kind()==HFEffects.FALL||e.kind()==HFEffects.PUSH||e.kind()==HFEffects.MELEE||e.kind()==HFEffects.LAUNCH&&e.radius()>2;}
 private static void opening(HFEffects.Effect e){
  int k=e.kind();long soundKey=key(e.entity(),k);if(lastOpening.getOrDefault(soundKey,-100L)+4>clock)return;lastOpening.put(soundKey,clock);
  if(k==HFEffects.REJECT){if(MC.player!=null&&e.entity()==MC.player.getId())sound(e,SoundEvents.STONE_BUTTON_CLICK_OFF,.25F,.6F);return;}
  if(k==HFEffects.READY){sound(e,SoundEvents.BEACON_POWER_SELECT,.65F,.8F);dust(at(e).add(0,1,0),12,BRASS,.12);return;}
  if(k==HFEffects.SIGHT||k==HFEffects.SIGHT_CHARGE||k==HFEffects.WITNESS||k==HFEffects.EXPOSED){sound(e,SoundEvents.AMETHYST_BLOCK_CHIME,k==HFEffects.WITNESS?.75F:.3F,k==HFEffects.WITNESS?.7F:1.1F);dust(at(e).add(0,k==HFEffects.EXPOSED?1.5:1.9,0),10,BRASS,.08);return;}
  if(k==HFEffects.HAMMER_CHARGE||k==HFEffects.GROUND_CHARGE){sound(e,SoundEvents.DEEPSLATE_HIT,.55F,.55F);return;}
  if(k==HFEffects.SHATTERED||k==HFEffects.STUN){dust(at(e).add(0,1,0),10,k==HFEffects.STUN?PALE:STONE,.12);return;}
  sound(e,SoundEvents.DEEPSLATE_BREAK,k==HFEffects.ARGUMENT?1.6F:k==HFEffects.ANCHOR?.5F:.9F,k==HFEffects.ARGUMENT?.5F:.7F);
  if(k==HFEffects.ARGUMENT||k==HFEffects.REFUSAL)sound(e,SoundEvents.ANVIL_LAND,k==HFEffects.ARGUMENT?.65F:.5F,.6F);
  if(k==HFEffects.LAUNCH&&e.radius()<=2)sound(e,SoundEvents.PLAYER_ATTACK_STRONG,.8F,.6F);
  dust(at(e).add(0,.2,0),k==HFEffects.ARGUMENT?32:16,STONE,.16);
  if(k==HFEffects.UNBROKEN||k==HFEffects.REFUSAL)dust(at(e).add(0,1,0),14,BRASS,.08);
 }
 private static void sound(HFEffects.Effect e,SoundEvent s,float volume,float pitch){MC.level.playLocalSound(e.position().x,e.position().y,e.position().z,s,SoundSource.PLAYERS,volume,pitch,false);}
 private static Vec3 at(HFEffects.Effect e){Entity entity=MC.level.getEntity(e.entity());return e.mode()==1&&entity!=null?entity.position():e.position();}
 private static void dust(Vec3 p,int count,Vector3f color,double speed){
  var r=MC.level.random;for(int i=0;i<count;i++)MC.level.addParticle(new DustParticleOptions(color,.65F),p.x+(r.nextDouble()-.5)*.6,p.y+(r.nextDouble()-.5)*.5,p.z+(r.nextDouble()-.5)*.6,(r.nextDouble()-.5)*speed,r.nextDouble()*speed,(r.nextDouble()-.5)*speed);
 }
 private static void circle(Vec3 center,double radius,int count,Vector3f color,double up){
  for(int i=0;i<count;i++){double angle=2*Math.PI*i/count;MC.level.addParticle(new DustParticleOptions(color,.65F),center.x+Math.cos(angle)*radius,center.y,center.z+Math.sin(angle)*radius,Math.cos(angle)*.015,up,Math.sin(angle)*.015);}
 }
 private static Tile floor(double x,double z,double originY,double distance){
  int bx=Mth.floor(x),bz=Mth.floor(z);
  for(int y=Mth.floor(originY+2);y>=Mth.floor(originY-6);y--){var pos=new BlockPos(bx,y,bz);if(!MC.level.hasChunkAt(pos))return null;var state=MC.level.getBlockState(pos);if(!state.getFluidState().isEmpty())continue;if(!state.getCollisionShape(MC.level,pos).isEmpty()&&MC.level.getBlockState(pos.above()).getCollisionShape(MC.level,pos.above()).isEmpty())return new Tile(pos,state,distance);}
  return null;
 }
 private static void addWave(HFEffects.Effect e){
  List<Tile> tiles=new ArrayList<>();boolean strip=e.kind()==HFEffects.FRACTURE;double radius=e.radius();Vec3 dir=e.direction();
  sample:for(int dx=-(int)radius-1;dx<=radius+1;dx++)for(int dz=-(int)radius-1;dz<=radius+1;dz++){
   double x=Mth.floor(e.position().x)+dx+.5,z=Mth.floor(e.position().z)+dz+.5;double rx=x-e.position().x,rz=z-e.position().z;
   double distance=strip?rx*dir.x+rz*dir.z:Math.hypot(rx,rz);
   if(distance<0||distance>radius||strip&&Math.abs(rx*dir.z-rz*dir.x)>3.5)continue;
   Tile tile=floor(x,z,e.position().y,distance);if(tile!=null)tiles.add(tile);if(tiles.size()>=1500)break sample;
  }
  waves.add(new Wave(e,clock,tiles));if(waves.size()>8)waves.remove(0);
 }
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  if(MC.level==null){world=null;states.clear();bursts.clear();waves.clear();return;}resetWorld();if(MC.isPaused())return;clock++;if(clock%40==0)lastOpening.entrySet().removeIf(x->clock-x.getValue()>20);
  states.entrySet().removeIf(x->x.getValue().end<clock||MC.level.getEntity(x.getValue().event.entity())==null);
  for(State state:states.values()){
   var e=state.event;Entity entity=MC.level.getEntity(e.entity());if(entity==null||entity.isInvisible()||MC.player==null||entity.distanceToSqr(MC.player)>64*64)continue;
   Vec3 p=entity.position();long age=clock-state.born;int k=e.kind();
   if(k==HFEffects.LAUNCH&&clock%3==0){dust(p.add(0,.1,0),3,STONE,.03);continue;}
   if(k==HFEffects.SIGHT_CHARGE||k==HFEffects.HAMMER_CHARGE||k==HFEffects.GROUND_CHARGE){
    if(clock%2!=0)continue;double progress=Math.min(1,age/(k==HFEffects.HAMMER_CHARGE?60.:20.));
    double angle=age*.35;double r=(1-progress)*.9+.25;double height=k==HFEffects.SIGHT_CHARGE?entity.getBbHeight()+.1:k==HFEffects.HAMMER_CHARGE?1.2:.12;
    for(int i=0;i<3;i++){double a=angle+i*2*Math.PI/3;Vec3 q=p.add(k==HFEffects.HAMMER_CHARGE?entity.getLookAngle().multiply(.8,0,.8):Vec3.ZERO).add(Math.cos(a)*r,height,Math.sin(a)*r);dust(q,1,progress>=1?BRASS:STONE,.015);}
    if(k==HFEffects.HAMMER_CHARGE&&age==60){dust(p.add(0,1.2,0),14,BRASS,.12);}continue;
   }
   if(k==HFEffects.EXPOSED||k==HFEffects.WITNESS||k==HFEffects.STUN||k==HFEffects.SHATTERED){
    if(clock%4!=0)continue;double h=entity.getBbHeight()+.2;double a=age*.14;
    if(k==HFEffects.SHATTERED){dust(p.add(0,entity.getBbHeight()*.5,0),2,STONE,.025);}
    else for(int i=0;i<(k==HFEffects.STUN?3:2);i++){double angle=a+i*Math.PI*2/(k==HFEffects.STUN?3:2);dust(p.add(Math.cos(angle)*.35,h,Math.sin(angle)*.35),1,k==HFEffects.STUN?PALE:BRASS,.005);}continue;
   }
   if(k==HFEffects.SOVEREIGN&&clock%6==0){
    for(int i=0;i<28;i++){double a=i*2*Math.PI/28;Tile tile=floor(p.x+Math.cos(a)*16,p.z+Math.sin(a)*16,p.y,16);if(tile!=null)dust(new Vec3(tile.pos.getX()+.5,tile.pos.getY()+1.05,tile.pos.getZ()+.5),1,BRASS,0);}
   }else if(k==HFEffects.ANCHOR&&clock%8==0){for(int i=0;i<8;i++){double a=i*Math.PI/4;dust(p.add(Math.cos(a)*.9,.08,Math.sin(a)*.9),1,STONE,0);}}
   else if((k==HFEffects.UNBROKEN||k==HFEffects.REFUSAL)&&clock%8==0){double a=age*.12;dust(p.add(Math.cos(a)*.5,1.1,Math.sin(a)*.5),2,k==HFEffects.REFUSAL?PALE:BRASS,.005);}
   else if(k==HFEffects.SIGHT&&clock%20==0)dust(p.add(0,entity.getBbHeight()+.08,0),2,BRASS,.005);
  }
  if(clock%2==0&&MC.player!=null)for(var player:MC.level.players())if(player.swinging&&HFState.hammer(player)&&player.distanceToSqr(MC.player)<48*48){
   float swing=player.getAttackAnim(0);if(swing>.12F&&swing<.72F){Vec3 forward=player.getLookAngle().multiply(1,0,1).normalize();double a=(swing-.5)*1.5;Vec3 tip=player.position().add(0,1.05,0).add(forward.scale(1.3)).add(forward.z*a,0,-forward.x*a);dust(tip,2,STONE,.01);}
  }
  for(Burst burst:bursts){var e=burst.event;long age=clock-burst.born;
   if(e.kind()==HFEffects.WITNESS&&age<=20&&clock%2==0)circle(e.position().add(0,.3,0),e.radius()*age/20.,48,BRASS,.005);
   if(e.kind()==HFEffects.LAUNCH&&e.radius()<=2&&age<12&&clock%2==0)dust(e.position().add(0,age*.08,0),5,STONE,.08);
  }
  bursts.removeIf(b->clock-b.born>b.event.duration()+12);
  for(Wave wave:waves){
   double age=clock-wave.born;double speed=wave.event.radius()/Math.max(8,wave.event.duration()-8);double front=age*speed;
   if(clock%2!=0)continue;int count=0;
   for(Tile tile:wave.tiles){if(Math.abs(tile.distance-front)>.8||count++>=56)continue;var pos=tile.pos;
    MC.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,tile.state),pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,0,.08,0);
    if(count%4==0)dust(new Vec3(pos.getX()+.5,pos.getY()+1.12,pos.getZ()+.5),1,STONE,.035);
   }
  }
  waves.removeIf(w->clock-w.born>w.event.duration()+12);
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||MC.level==null||MC.player==null||waves.isEmpty())return;
  float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);Vec3 camera=event.getCamera().getPosition();var buffers=MC.renderBuffers().bufferSource();PoseStack pose=event.getPoseStack();int drawn=0;
  for(Wave wave:waves){
   if(wave.event.position().distanceToSqr(camera)>80*80)continue;double speed=wave.event.radius()/Math.max(8,wave.event.duration()-8);
   for(Tile tile:wave.tiles){double age=clock+partial-wave.born-tile.distance/speed;if(age<=0||age>=8)continue;if(drawn++>1600)break;
    renderedTiles++;float envelope=Mth.sin((float)(age/8*Math.PI));float power=wave.event.kind()==HFEffects.ARGUMENT?.52F:wave.event.kind()==HFEffects.FRACTURE?.32F:wave.event.kind()==HFEffects.MELEE?.13F:.24F;
    float rise=envelope*power*(float)(1-.45*tile.distance/Math.max(1,wave.event.radius()));var p=tile.pos;
    pose.pushPose();pose.translate(p.getX()-camera.x+.5,p.getY()-camera.y+.5+rise,p.getZ()-camera.z+.5);pose.mulPose(Axis.XP.rotationDegrees(envelope*3));pose.translate(-.5,-.5,-.5);
    MC.getBlockRenderer().renderSingleBlock(tile.state,pose,buffers,LevelRenderer.getLightColor(MC.level,p.above()),OverlayTexture.NO_OVERLAY,ModelData.EMPTY,null);pose.popPose();
   }
  }
  if(drawn>0)buffers.endBatch();
 }
}
