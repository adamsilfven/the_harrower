package com.leyline.harrower.combat;

/** Base animation ticks. Blue changes the rate of windup, never the committed active path. */
public enum CombatMove {
 NONE(0,"idle",0,0,0,0,0,"none",-1),
 FIST_RIGHT(1,"attack",18,4,48,28,0,"fist",1),
 RUPTURE(2,"rupture",40,35,105,20,360,"rupture",-1),
 HURT(3,"hurt",0,0,20,0,0,"hurt",-1),
 DEATH(4,"death",0,0,360,0,0,"death",-1),
 FIST_LEFT(5,"attack_left",18,4,48,28,0,"fist",0),
 SWEEP_RIGHT(6,"sweep_right",24,12,74,22,200,"sweep",1),
 SWEEP_LEFT(7,"sweep_left",24,12,74,22,200,"sweep",0),
 HEEL_RIGHT(8,"heel_right",30,1,76,20,280,"heel",1),
 HEEL_LEFT(9,"heel_left",30,1,76,20,280,"heel",0),
 STOMP_RIGHT(10,"stomp_right",26,1,72,24,240,"stomp",1),
 STOMP_LEFT(11,"stomp_left",26,1,72,24,240,"stomp",0),
 CHARGE(12,"charge",40,134,214,36,500,"charge",-1),
 GRASP_RIGHT(13,"grasp_right",36,270,346,8,700,"grasp",1),
 GRASP_LEFT(14,"grasp_left",36,270,346,8,700,"grasp",0),
 HOLLOWING(17,"hollowing",80,48,168,45,1200,"hollowing",-1),
 TRANSITION(18,"phase",60,0,60,0,0,"phase",-1),
 STAGGER(19,"stagger",0,0,20,0,0,"stagger",-1);
 public final int id,windup,active,duration,cooldown,side;public final float damage;public final String clip,group;
 CombatMove(int id,String clip,int windup,int active,int duration,float damage,int cooldown,String group,int side){this.id=id;this.clip=clip;this.windup=windup;this.active=active;this.duration=duration;this.damage=damage;this.cooldown=cooldown;this.group=group;this.side=side;}
 public static CombatMove byId(int id){for(var move:values())if(move.id==id)return move;return NONE;}
 public boolean handStrike(){return group.equals("fist")||group.equals("sweep");}
 public boolean grasp(){return group.equals("grasp");}
 public boolean footStrike(){return group.equals("heel")||group.equals("stomp");}
 public boolean attack(){return damage>0;}
 public boolean wave(){return this==RUPTURE||this==HOLLOWING;}
 public boolean shockDamage(){return wave()||group.equals("stomp");}
 public String hand(){return side==0?"left_hand":"right_hand";}
 public boolean recovery(float tick){return tick>=windup+active;}
}
