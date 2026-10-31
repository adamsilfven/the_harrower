package com.leyline.harrower.client;

import com.leyline.harrower.Harrower;
import net.minecraft.client.model.geom.ModelPart;
import java.util.*;
import java.nio.file.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Optional development evidence from the final posed render hierarchy, not server anchors.
 * Enable with -Dharrower.contactProbe=true. No file I/O during normal play.
 */
final class ContactAudit {
 private static final boolean ENABLED=Boolean.getBoolean("harrower.contactProbe");
 private static String worst="";
 private static long samples;private static double maxError,maxTilt,maxPelvisDrop,maxNormalKnee,maxExtraDrop;
 static void sample(Harrower entity,Map<String,ModelPart> parts,float partial){
  if(!ENABLED)return;
  var pelvis=parts.get("pelvis");maxPelvisDrop=Math.max(maxPelvisDrop,(pelvis.y-pelvis.getInitialPose().y)/16.0);
  double allowance=Math.max(0,Math.min(1,.625-Math.min(entity.ankle(0,1,partial),entity.ankle(1,1,partial))));maxExtraDrop=Math.max(maxExtraDrop,(pelvis.y-pelvis.getInitialPose().y)/16.0-allowance);
  if(entity.action()==Harrower.NONE)for(String side:new String[]{"left","right"})maxNormalKnee=Math.max(maxNormalKnee,Math.toDegrees(Math.abs(parts.get(side+"_shin").xRot)));
  for(String side:new String[]{"left","right"}){
   int i=side.equals("left")?0:1;Matrix4f transform=new Matrix4f();
   for(String bone:new String[]{"root","pelvis",side+"_leg",side+"_shin",side+"_foot"}){
    ModelPart p=parts.get(bone);transform.translate(p.x,p.y,p.z).rotateZYX(p.zRot,p.yRot,p.xRot);
   }
   Vector3f actual=transform.transformPosition(new Vector3f());
   Vector3f wanted=new Vector3f(entity.ankle(i,0,partial)*16,24-entity.ankle(i,1,partial)*16,entity.ankle(i,2,partial)*16);
   double error=actual.distance(wanted)/16;
   if(error>maxError){
    maxError=error;StringBuilder detail=new StringBuilder("entity="+entity.getId()+", action="+entity.action()+", side="+side+", actual="+actual+", wanted="+wanted);
    for(String name:new String[]{"root","pelvis",side+"_leg",side+"_shin",side+"_foot"}){var p=parts.get(name);detail.append(" | "+name+" pos="+p.x+","+p.y+","+p.z+" rot="+p.xRot+","+p.yRot+","+p.zRot);}
    worst=detail.toString();
   }
   maxTilt=Math.max(maxTilt,Math.sqrt(transform.m10()*transform.m10()+transform.m12()*transform.m12()));samples++;
  }
  if(samples%240==0)try{
   String json="{\n  \"posed_leg_samples\": "+samples+",\n  \"max_actual_ankle_target_error_blocks\": "+maxError+",\n  \"max_sole_normal_horizontal_component\": "+maxTilt+",\n  \"scope\": \"Final client bone transforms after transition blending and IK; not GPU readback or arbitrary-terrain certification\",\n  \"posture_max_extra_drop_beyond_terrain_blocks\": "+maxExtraDrop+",\n  \"posture_max_pelvis_drop_blocks\": "+maxPelvisDrop+",\n  \"posture_max_normal_knee_degrees\": "+maxNormalKnee+"\n}\n";
   Files.writeString(Path.of("harrower-render-contact-audit.json"),json);
   Files.writeString(Path.of("harrower-render-contact-worst.txt"),worst);
  }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
 }
}
