package com.leyline.harrower.client;
import org.joml.Matrix3f;
import org.joml.Vector3f;
/** Timing and gravity shared by the terrain-aware corpse fragments and checks. */
public final class CorpseBreakup {
 public static final float GRAVITY=.06F;
 public static float age(float clock,int pieceHash){return Math.max(0,clock-340-Math.floorMod(pieceHash,34));}
 public static float fall(float age){return .5F*GRAVITY*age*age;}
 public static float tumble(float age){return smooth(age/12F);}
 public static float size(float age,double distance){
  // Finish the small tumble before dissolving a grounded stone into the pile.
  double land=Math.max(12,Math.sqrt(2*Math.max(0,distance)/GRAVITY));
  return 1-smooth((float)((age-land-4)/18));
 }
 public static double centerY(double original,double ground,double extent,float age,float size){
  // Shrinking is anchored to the floor, rather than pulling the bottom upward.
  return Math.min(original,Math.max(ground+extent*size,original-fall(age)));
 }
 private static float smooth(float u){u=Math.clamp(u,0,1);return u*u*(3-2*u);}
 // Retained for coordinate regression checks and source compatibility.
 public static Vector3f gravityInModel(float pitch,float roll){
  float radians=(float)(Math.PI/180);
  return new Matrix3f().rotationX(-pitch*radians).rotateZ(-roll*radians).scale(-1,-1,1).invert().transform(new Vector3f(0,-1,0));
 }
 private CorpseBreakup(){}
}
