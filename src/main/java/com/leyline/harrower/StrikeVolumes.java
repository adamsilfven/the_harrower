package com.leyline.harrower;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hand envelopes exported from the exact rig and editable animation keys. X=model-left, Z=forward. */
public final class StrikeVolumes {
 private static final JsonObject DATA=load();
 private static JsonObject load(){try(var in=StrikeVolumes.class.getResourceAsStream("/assets/lc_harrower/animations/strike_volumes.json")){if(in==null)throw new IOException("Missing strike envelopes");return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}}
 private static double[] frame(String clip,float tick,String name){
  var frames=DATA.getAsJsonObject(clip).getAsJsonArray("frames");float clamped=Math.max(0,Math.min(frames.size()-1,tick));int index=(int)clamped;float u=clamped-index;
  var a=frames.get(index).getAsJsonObject().getAsJsonArray(name);var b=frames.get(Math.min(frames.size()-1,index+1)).getAsJsonObject().getAsJsonArray(name);double[] out=new double[a.size()];for(int i=0;i<out.length;i++)out[i]=a.get(i).getAsDouble()*(1-u)+b.get(i).getAsDouble()*u;return out;
 }
 public static float aimOffset(boolean left){return aimOffset(left?"attack_left":"attack",left?"left_hand":"right_hand",18);}
 public static float aimOffset(String clip,String hand,float tick){double[] a=frame(clip,tick,hand);double x=(a[0]+a[3])/2,z=(a[2]+a[5])/2;return(float)Math.toDegrees(Math.atan2(-x,z));}
 public static Vec3 grip(Harrower boss,String clip,float tick,String hand){
  double[] a=frame(clip,tick,hand+"_grip");double yaw=Math.toRadians(boss.getYRot()),c=Math.cos(yaw),s=Math.sin(yaw),scale=boss.getScale();
  return boss.position().add((a[0]*c-a[2]*s)*scale,(a[1]-additionalDrop(boss,clip,tick))*scale,(a[0]*s+a[2]*c)*scale);
 }
 private static double additionalDrop(Harrower boss,String clip,float tick){
  double[] pelvis=frame(clip,tick,"pelvis"),rotation=frame(clip,tick,"pelvis_rotation");
  double terrain=Math.min(1,Math.max(0,.625-Math.min(boss.serverAnkle(0,1),boss.serverAnkle(1,1))));
  float[] ankles=new float[6];for(int i=0;i<6;i++)ankles[i]=boss.serverAnkle(i/3,i%3);
  float y=LegKinematics.fit((float)pelvis[0],(float)(pelvis[1]+terrain*16),(float)pelvis[2],(float)Math.toRadians(rotation[0]),(float)Math.toRadians(rotation[1]),(float)Math.toRadians(rotation[2]),ankles);
  return Math.max(0,(y-pelvis[1])/16);

 }
 public static AABB hand(Harrower boss,String clip,float tick,String hand){
  double[] a=frame(clip,tick,hand);double yaw=Math.toRadians(boss.getYRot()),c=Math.cos(yaw),s=Math.sin(yaw),scale=boss.getScale(),drop=additionalDrop(boss,clip,tick);
  double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
  for(int i=0;i<8;i++){
   double x=a[(i&1)==0?0:3],y=a[(i&2)==0?1:4]-drop,z=a[(i&4)==0?2:5];double wx=boss.getX()+(x*c-z*s)*scale,wy=boss.getY()+y*scale,wz=boss.getZ()+(x*s+z*c)*scale;
   minX=Math.min(minX,wx);minY=Math.min(minY,wy);minZ=Math.min(minZ,wz);maxX=Math.max(maxX,wx);maxY=Math.max(maxY,wy);maxZ=Math.max(maxZ,wz);
  }return new AABB(minX,minY,minZ,maxX,maxY,maxZ).inflate(.20*scale);
 }
 public static Vec3 groundPoint(Harrower boss,String clip,float tick){Vec3 right=hand(boss,clip,tick,clip.equals("attack_left")||clip.endsWith("_left")?"left_hand":"right_hand").getCenter();if(clip.equals("rupture")||clip.equals("slam"))right=right.add(hand(boss,clip,tick,"left_hand").getCenter()).scale(.5);return new Vec3(right.x,boss.getY(),right.z);}
}
