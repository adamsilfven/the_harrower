package com.leyline.hollowforged.client;
/** A heavy overhead swing, anchored to the same resting end grip. Purely visual. */
public final class HammerSwingCurve {
 public record Sample(float pitch,float yaw,float roll,float twist,float lift,float shaftAngle,float bearing){}
 public static final float DURATION=18;
 private static float ease(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
 private static float lerp(float a,float b,float x){return a+(b-a)*ease(x);}
 public static Sample sample(float t){
  if(t<=0||t>=1)return new Sample(0,0,0,0,0,42.5F,90);
  if(t<.34F){float q=t/.34F;return new Sample(lerp(0,-1.55F,q),lerp(0,-.10F,q),0,lerp(0,-.12F,q),lerp(0,1,q),lerp(42.5F,-16,q),90);}
  if(t<.64F){float q=(t-.34F)/.30F;return new Sample(lerp(-1.55F,-.35F,q),lerp(-.10F,.06F,q),0,lerp(-.12F,.16F,q),lerp(1,-.24F,q),lerp(-16,122,q),90);}
  float q=(t-.64F)/.36F;return new Sample(lerp(-.35F,0,q),lerp(.06F,0,q),0,lerp(.16F,0,q),lerp(-.24F,0,q),lerp(122,42.5F,q),90);
 }
}
