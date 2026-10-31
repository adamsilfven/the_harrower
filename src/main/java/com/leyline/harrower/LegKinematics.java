package com.leyline.harrower;

/** Native model-space pelvis fit shared by rendering and server strike placement. */
public final class LegKinematics {
 public static float fit(float x,float y,float z,float rx,float ry,float rz,float[] ankles){
  double reach=Math.sqrt(33*33+5*5)+Math.sqrt(25*25+6*6)-.08;
  for(int pass=0;pass<40;pass++){
   var inverse=new org.joml.Matrix4f().translation(0,24,0).translate(x,y,z).rotateZYX(rz,ry,rx).invert();double excess=0;
   for(int side=0;side<2;side++){
    var target=inverse.transformPosition(new org.joml.Vector3f(ankles[side*3]*16,24-ankles[side*3+1]*16,ankles[side*3+2]*16));
    double dx=target.x-(side==0?24:-24),dy=target.y+2,dz=target.z-1;
    if(dy>0)excess=Math.max(excess,Math.sqrt(dx*dx+dy*dy+dz*dz)-reach);
   }
   if(excess<.01)break;y+=Math.min(2,(float)excess+.02F);
  }return y;
 }
 private LegKinematics(){}
}
