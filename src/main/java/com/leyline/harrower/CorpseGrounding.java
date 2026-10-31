package com.leyline.harrower;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.phys.Vec3;

/** Settle the rigid corpse in both pitch and roll, using its whole support footprint. */
final class CorpseGrounding {
 private static final List<Vec3> SUPPORT=load();
 private boolean fitted;private double restY;private float tilt,roll;
 private static List<Vec3> load(){
  try(var in=CorpseGrounding.class.getResourceAsStream("/assets/lc_harrower/animations/death_support.json")){
   var data=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();List<Vec3> points=new ArrayList<>();
   for(var v:data.getAsJsonArray("points")){var p=v.getAsJsonArray();points.add(new Vec3(p.get(0).getAsDouble(),p.get(1).getAsDouble(),p.get(2).getAsDouble()));}return List.copyOf(points);
  }catch(IOException ex){throw new IllegalStateException(ex);}
 }
 private static double floor(Harrower boss,double x,double z,double reference){
  BlockPos.MutableBlockPos at=new BlockPos.MutableBlockPos();
  for(int y=Mth.floor(reference+2);y>=Mth.floor(reference-32);y--){
   at.set(Mth.floor(x),y,Mth.floor(z));if(!boss.level().hasChunkAt(at))return boss.getY();
   var state=boss.level().getBlockState(at);if(state.getBlock() instanceof BushBlock||state.is(BlockTags.LEAVES))continue;
   double top=-1;for(var box:state.getCollisionShape(boss.level(),at).toAabbs())if(x-at.getX()>=box.minX&&x-at.getX()<=box.maxX&&z-at.getZ()>=box.minZ&&z-at.getZ()<=box.maxZ)top=Math.max(top,box.maxY);
   if(top>=0)return y+top;
  }return boss.getY();
 }
 private record Fit(float pitch,float roll,double root,double score){}
 private Fit evaluate(Harrower b,double reference,float pitch,float roll){
  double a=Math.toRadians(pitch),r=Math.toRadians(roll),c=Math.cos(a),s=Math.sin(a),cr=Math.cos(r),sr=Math.sin(r),yaw=Math.toRadians(b.getYRot());
  double root=-Double.MAX_VALUE,total=0,scale=b.getScale();int count=0;
  for(Vec3 p:SUPPORT){
   // Matches renderer Ry * Rx(-pitch) * Rz(-roll), after native model axis conversion.
   double x=p.x*cr-p.y*sr,yr=p.x*sr+p.y*cr,height=yr*c-p.z*s,depth=p.z*c+yr*s;
   double wx=b.getX()+(x*Math.cos(yaw)-depth*Math.sin(yaw))*scale,wz=b.getZ()+(x*Math.sin(yaw)+depth*Math.cos(yaw))*scale;
   double required=floor(b,wx,wz,reference)-height*scale;root=Math.max(root,required);total+=required;count++;
  }
  // Minimize average clearance, with a small preference for a level, stable resting pose.
  return new Fit(pitch,roll,root+.02,root-total/count+.00008*(pitch*pitch+roll*roll));
 }
 void update(Harrower boss,double startingY){
  if(boss.deathTime<100)return;
  if(!fitted){
   Fit best=evaluate(boss,startingY,0,0);
   for(int p=-40;p<=40;p+=10)for(int r=-30;r<=30;r+=10){Fit f=evaluate(boss,startingY,p,r);if(f.score<best.score)best=f;}
   float p0=best.pitch,r0=best.roll;
   for(int p=-8;p<=8;p+=2)for(int r=-8;r<=8;r+=2){Fit f=evaluate(boss,startingY,Mth.clamp(p0+p,-45,45),Mth.clamp(r0+r,-35,35));if(f.score<best.score)best=f;}
   restY=best.root;tilt=best.pitch;roll=best.roll;fitted=true;
  }
  float u=Mth.clamp((boss.deathTime-100)/60F,0,1);u=u*u*(3-2*u);
  boss.syncCorpseGrounding((float)((restY-boss.getY())/boss.getScale())*u,tilt*u,roll*u);
 }
}
