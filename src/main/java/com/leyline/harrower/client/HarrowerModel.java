package com.leyline.harrower.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.leyline.harrower.Harrower;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class HarrowerModel extends EntityModel<Harrower> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("lc_harrower","harrower"),"main");
 private float crumbleTick;private Harrower corpse;
 private Transition corpseState;
 private final org.joml.Matrix4f corpseWorld=new org.joml.Matrix4f();
 /** The same placement used by LivingEntityRenderer, before the bone hierarchy. */
 public void corpseWorldPose(Harrower e,float yaw,float partial,float scale){
  var at=e.getPosition(partial);
  corpseWorld.translation((float)at.x,(float)at.y,(float)at.z).scale(scale)
   .rotateY((180-yaw)*Mth.DEG_TO_RAD).translate(0,e.corpseLift(),0)
   .rotateX(-e.corpseTilt()*Mth.DEG_TO_RAD).rotateZ(-e.corpseRoll()*Mth.DEG_TO_RAD)
   .scale(-1,-1,1).translate(0,-1.501F,0);
 }
 private record FragmentGround(double height){}
 private boolean settledCorpse;private final ModelPart root;private final Map<String,ModelPart> parts=new HashMap<>();private final JsonObject clips=read("animations/harrower.json");
 public static ModelPart rockyRoot(){return RockGeometry.bake(read("models/entity/harrower.json"));}
 public HarrowerModel(ModelPart root){this.root=root;Map<String,String> parents=new HashMap<>();for(JsonElement e:read("models/entity/harrower.json").getAsJsonArray("bones")){JsonObject b=e.getAsJsonObject();parents.put(b.get("name").getAsString(),b.get("parent").isJsonNull()?null:b.get("parent").getAsString());}for(String n:parents.keySet())findPart(n,parents);}
 private ModelPart findPart(String n,Map<String,String> parents){if(parts.containsKey(n))return parts.get(n);String parent=parents.get(n);ModelPart p=(parent==null?root:findPart(parent,parents)).getChild(n);parts.put(n,p);return p;}
 private static JsonObject read(String path){try(InputStream s=HarrowerModel.class.getResourceAsStream("/assets/lc_harrower/"+path)){if(s==null)throw new IOException("Missing "+path);return JsonParser.parseReader(new InputStreamReader(s,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}}
 private static float n(JsonArray a,int i){return a.get(i).getAsFloat();}
 public static LayerDefinition layer(){
  MeshDefinition mesh=new MeshDefinition();Map<String,PartDefinition> defs=new HashMap<>();Map<String,float[]> pivots=new HashMap<>();
  for(JsonElement e:read("models/entity/harrower.json").getAsJsonArray("bones")){
   JsonObject b=e.getAsJsonObject();String name=b.get("name").getAsString(),parent=b.get("parent").isJsonNull()?null:b.get("parent").getAsString();JsonArray pivot=b.getAsJsonArray("pivot"),rot=b.getAsJsonArray("rotation");float[] p={n(pivot,0),n(pivot,1),n(pivot,2)},pp=parent==null?new float[]{0,0,0}:pivots.get(parent);
   PartDefinition g=(parent==null?mesh.getRoot():defs.get(parent)).addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offsetAndRotation(p[0]-pp[0],(parent==null?24:0)-p[1]+pp[1],p[2]-pp[2],-n(rot,0)*Mth.DEG_TO_RAD,n(rot,1)*Mth.DEG_TO_RAD,-n(rot,2)*Mth.DEG_TO_RAD));defs.put(name,g);pivots.put(name,p);int i=0;
   for(JsonElement ce:b.getAsJsonArray("cubes")){
    JsonObject c=ce.getAsJsonObject();JsonArray f=c.getAsJsonArray("from"),t=c.getAsJsonArray("to"),o=c.getAsJsonArray("origin"),r=c.getAsJsonArray("rotation"),uv=c.getAsJsonArray("uv_offset");CubeListBuilder cube=CubeListBuilder.create().texOffs((int)n(uv,0),(int)n(uv,1)).addBox(n(f,0)-n(o,0),n(o,1)-n(t,1),n(f,2)-n(o,2),n(t,0)-n(f,0),n(t,1)-n(f,1),n(t,2)-n(f,2));
    g.addOrReplaceChild("cube_"+(i++),cube,PartPose.offsetAndRotation(n(o,0)-p[0],p[1]-n(o,1),n(o,2)-p[2],-n(r,0)*Mth.DEG_TO_RAD,n(r,1)*Mth.DEG_TO_RAD,-n(r,2)*Mth.DEG_TO_RAD));
   }
  }JsonArray size=read("models/entity/harrower.json").getAsJsonArray("texture_size");return LayerDefinition.create(mesh,size.get(0).getAsInt(),size.get(1).getAsInt());
 }
 private static final class Transition {
  final Map<String,float[]> previous=new HashMap<>(),last=new HashMap<>();
  final Map<String,FragmentGround> ground=new HashMap<>();
  int action=-1,serial=-1;float start;
 }
 private final Map<Harrower,Transition> transitions=new WeakHashMap<>();
 @Override public void setupAnim(Harrower e,float swing,float amount,float age,float yaw,float pitch){
  float partial=Mth.clamp(age-e.tickCount,0,1);boolean dying=e.isDeadOrDying()||e.action()==Harrower.DEATH;settledCorpse=dying&&e.actionSeconds(partial)>.6F;
  crumbleTick=dying?e.actionSeconds(partial)*20:0;
  corpse=e;
  Transition state=transitions.computeIfAbsent(e,k->new Transition());corpseState=state;
  if(state.action!=e.action()||state.serial!=e.actionSequence()){
   state.ground.clear();state.previous.clear();state.previous.putAll(state.last);state.start=age;state.action=e.action();state.serial=e.actionSequence();
  }
  root.getAllParts().forEach(ModelPart::resetPose);
  if(dying)apply("death",e.actionSeconds(partial),1);
  else {
   apply("idle",age/20,e.action()==Harrower.NONE?1:.25F);
   float phase=e.gaitPhase(partial);
   float motion=Mth.clamp(amount*5,0,1);
   if(e.action()==Harrower.NONE){
    apply("walk",phase*4,motion);parts.get("pelvis").y+=4*motion;
    float turn=e.turnLean(partial);apply("turn",1,turn);
    ModelPart h=parts.get("head");h.yRot+=Mth.clamp(yaw,-20,20)*Mth.DEG_TO_RAD;h.xRot+=Mth.clamp(pitch,-12,15)*Mth.DEG_TO_RAD;
   } else apply(com.leyline.harrower.combat.CombatMove.byId(e.action()).clip,e.actionSeconds(partial),1);
   if(e.isCharging()){apply("walk",phase*4,.5F,true);parts.get("pelvis").y+=2;}
   float terrainDrop=Math.max(0,.625F-Math.min(e.ankle(0,1,partial),e.ankle(1,1,partial)));
   parts.get("pelvis").y+=Mth.clamp(terrainDrop*16,0,16);
   // Feet are solved after the authored upper-body motion. World stance anchors prevent sliding.

  }
  float transitionTicks=e.action()==Harrower.NONE?9:com.leyline.harrower.combat.CombatMove.byId(e.action()).attack()?3:6;
  float blend=dying?1:Mth.clamp((age-state.start)/transitionTicks,0,1);blend=blend*blend*(3-2*blend);
  if(blend<1)for(var p:state.previous.entrySet()){ModelPart m=parts.get(p.getKey());float[] v=p.getValue();m.x=Mth.lerp(blend,v[0],m.x);m.y=Mth.lerp(blend,v[1],m.y);m.z=Mth.lerp(blend,v[2],m.z);m.xRot=Mth.lerp(blend,v[3],m.xRot);m.yRot=Mth.lerp(blend,v[4],m.yRot);m.zRot=Mth.lerp(blend,v[5],m.zRot);}
  // Final contacts must be solved AFTER pose blending; blending solved joints unplants feet.
  if(!dying){
   fitPelvisToReach(e,partial);
   for(int i=0;i<2;i++)solveLeg(e,i,partial);
   ContactAudit.sample(e,parts,partial);
  }
  if(!dying&&e.contactReaction()>0){float recoil=e.contactReaction(partial);parts.get("torso").xRot+=recoil*.025F;parts.get("head").xRot-=recoil*.035F;}
  for(var p:parts.entrySet()){ModelPart m=p.getValue();state.last.put(p.getKey(),new float[]{m.x,m.y,m.z,m.xRot,m.yRot,m.zRot});}
 }
 private org.joml.Vector3f legTarget(Harrower e,int side,float partial){
  org.joml.Matrix4f parent=new org.joml.Matrix4f();
  for(ModelPart p:new ModelPart[]{parts.get("root"),parts.get("pelvis")})parent.translate(p.x,p.y,p.z).rotateZYX(p.zRot,p.yRot,p.xRot);
  return parent.invert().transformPosition(new org.joml.Vector3f(e.ankle(side,0,partial)*16,24-e.ankle(side,1,partial)*16,e.ankle(side,2,partial)*16));
 }
 private void fitPelvisToReach(Harrower e,float partial){
  ModelPart pelvis=parts.get("pelvis");float[] ankles=new float[6];for(int i=0;i<6;i++)ankles[i]=e.ankle(i/3,i%3,partial);
  pelvis.y=com.leyline.harrower.LegKinematics.fit(pelvis.x,pelvis.y,pelvis.z,pelvis.xRot,pelvis.yRot,pelvis.zRot,ankles);
 }
 private void solveLeg(Harrower e,int side,float partial){
  String prefix=side==0?"left":"right";
  ModelPart hip=parts.get(prefix+"_leg"),shin=parts.get(prefix+"_shin"),foot=parts.get(prefix+"_foot"),pelvis=parts.get("pelvis");
  hip.resetPose();shin.resetPose();foot.resetPose();
  // Targets live in world/model space; invert the animated pelvis before solving local joints.
  org.joml.Vector3f target=legTarget(e,side,partial);
  double dx=target.x-hip.x,dy=target.y-hip.y,dz=target.z-hip.z;
  // Solve lateral reach by rotating the hip, rather than translating the leg out of its socket.
  double sideOffset=shin.x;
  double planeY=Math.sqrt(Math.max(.01,dx*dx+dy*dy-sideOffset*sideOffset));
  hip.zRot=(float)(Math.atan2(dy,dx)-Math.atan2(planeY,sideOffset));
  double upper=Math.sqrt(33*33+5*5),lower=Math.sqrt(25*25+6*6);
  // Solve the actual contact target. Terrain may flex one knee deeply; the separate
  // pelvis constraint prevents a whole-body seated collapse without lifting a foot.
  double distance=Math.max(12,Math.min(upper+lower-.025,Math.sqrt(planeY*planeY+dz*dz)));
  double direction=Math.atan2(dz,planeY);
  double bend=Math.acos(Math.max(-1,Math.min(1,(upper*upper+distance*distance-lower*lower)/(2*upper*distance))));
  double a=direction-bend,b=direction+Math.acos(Math.max(-1,Math.min(1,(lower*lower+distance*distance-upper*upper)/(2*lower*distance))));
  hip.xRot=(float)(a-Math.atan2(-5,33));
  shin.xRot=(float)(b-a-(Math.atan2(-6,25)-Math.atan2(-5,33)));
  // Cancel every parent rotation, including the authored pelvis lean.
  org.joml.Matrix4f inverse=new org.joml.Matrix4f();
  for(ModelPart part:new ModelPart[]{parts.get("root"),pelvis,hip,shin})inverse.rotateZYX(part.zRot,part.yRot,part.xRot);
  inverse.invert();
  foot.xRot=(float)Math.atan2(inverse.m12(),inverse.m22());
  foot.yRot=(float)Math.asin(Mth.clamp(-inverse.m02(),-1,1));
  foot.zRot=(float)Math.atan2(inverse.m01(),inverse.m00());

 }
 private void apply(String name,float time,float weight){apply(name,time,weight,false);}
 private void apply(String name,float time,float weight,boolean pelvisOnly){
  JsonObject clip=clips.getAsJsonObject(name);float length=clip.get("length").getAsFloat();time=clip.get("loop").getAsBoolean()?Math.max(0,time)%length:Mth.clamp(time,0,length);
  for(var bone:clip.getAsJsonObject("tracks").entrySet()){if(pelvisOnly&&!bone.getKey().equals("pelvis"))continue;ModelPart p=parts.get(bone.getKey());if(p==null)continue;for(var channel:bone.getValue().getAsJsonObject().entrySet()){
   JsonArray keys=channel.getValue().getAsJsonArray(),a=keys.get(0).getAsJsonArray(),b=a;for(JsonElement k:keys){JsonArray f=k.getAsJsonArray();if(n(f,0)<=time)a=f;if(n(f,0)>=time){b=f;break;}b=f;}
   float span=n(b,0)-n(a,0),u=span<=0?0:(time-n(a,0))/span;u=Mth.clamp(u,0,1);float x=Mth.lerp(u,n(a,1),n(b,1))*weight,y=Mth.lerp(u,n(a,2),n(b,2))*weight,z=Mth.lerp(u,n(a,3),n(b,3))*weight;
   if(channel.getKey().equals("rotation")){p.xRot-=x*Mth.DEG_TO_RAD;p.yRot+=y*Mth.DEG_TO_RAD;p.zRot-=z*Mth.DEG_TO_RAD;}else{p.x+=x;p.y-=y;p.z+=z;}
  }}
 }
 @Override public void renderToBuffer(PoseStack p,VertexConsumer v,int light,int overlay,int color){
  int actualOverlay=settledCorpse?net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY:overlay;
  if(crumbleTick<340){root.render(p,v,light,actualOverlay,color);return;}
  // Remove the view matrix using the actual renderer placement. This gives each
  // cube a world-space center and ground contact, including corpse tilt and scale.
  org.joml.Matrix4f modelToWorld=new org.joml.Matrix4f(corpseWorld).mul(new org.joml.Matrix4f(p.last().pose()).invert());
  org.joml.Vector3f down=p.last().pose().transformDirection(new org.joml.Matrix4f(corpseWorld).invert().transformDirection(new org.joml.Vector3f(0,-1,0)));
  PoseStack fragment=new PoseStack();
  root.visit(p,(posed,path,index,cube)->{
   String key=path+":"+index;int hash=path.hashCode()*31+index;
   float age=CorpseBreakup.age(crumbleTick,hash),tumble=CorpseBreakup.tumble(age);
   float ax=(Math.floorMod(hash,51)-25)*Mth.DEG_TO_RAD,az=(Math.floorMod(hash/51,51)-25)*Mth.DEG_TO_RAD;
   float cx=(cube.minX+cube.maxX)/32,cy=(cube.minY+cube.maxY)/32,cz=(cube.minZ+cube.maxZ)/32;
   float hx=(cube.maxX-cube.minX)/32,hy=(cube.maxY-cube.minY)/32,hz=(cube.maxZ-cube.minZ)/32;
   org.joml.Matrix4f world=new org.joml.Matrix4f(modelToWorld).mul(posed.pose());
   org.joml.Vector3f center=world.transformPosition(new org.joml.Vector3f(cx,cy,cz));
   double initialExtent=RockGeometry.lowerExtent(cube,world);
   FragmentGround contact=corpseState.ground.computeIfAbsent(key,k->{
    double bottom=center.y-initialExtent;
    var from=new net.minecraft.world.phys.Vec3(center.x,bottom+.04,center.z);
    var hit=corpse.level().clip(new net.minecraft.world.level.ClipContext(from,
     new net.minecraft.world.phys.Vec3(center.x,corpse.level().getMinBuildHeight()-1,center.z),
     net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,corpse));
    return new FragmentGround(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?Double.NEGATIVE_INFINITY:Math.min(bottom,hit.getLocation().y+.015));
   });
   org.joml.Matrix4f finalRotation=new org.joml.Matrix4f(world).rotateX(ax).rotateZ(az);
   double finalExtent=RockGeometry.lowerExtent(cube,finalRotation);
   float size=Double.isFinite(contact.height)?CorpseBreakup.size(age,center.y-contact.height-finalExtent):1;
   if(size<=0)return;
   org.joml.Matrix4f rotation=new org.joml.Matrix4f(world).rotateX(ax*tumble).rotateZ(az*tumble);
   double extent=RockGeometry.lowerExtent(cube,rotation);
   double settled=CorpseBreakup.centerY(center.y,contact.height,extent,age,size);
   float fall=(float)(center.y-settled);
   fragment.last().pose().set(posed.pose());fragment.last().normal().set(posed.normal());
   fragment.last().pose().translateLocal(down.x*fall,down.y*fall,down.z*fall);
   fragment.translate(cx,cy,cz);
   fragment.mulPose(com.mojang.math.Axis.XP.rotation(ax*tumble));
   fragment.mulPose(com.mojang.math.Axis.ZP.rotation(az*tumble));
   fragment.scale(size,size,size);fragment.translate(-cx,-cy,-cz);
   cube.compile(fragment.last(),v,light,actualOverlay,color);
  });
 }
}
