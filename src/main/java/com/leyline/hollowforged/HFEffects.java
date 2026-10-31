package com.leyline.hollowforged;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
/** Presentation events only. The existing combat code remains authoritative. */
public final class HFEffects {
 public static final int SIGHT=0,WITNESS=1,UNBROKEN=2,SOVEREIGN=3,LAUNCH=4,FRACTURE=5,ARGUMENT=6,MELEE=7,REFUSAL=8,ANCHOR=9,FALL=10,EXPOSED=11,SHATTERED=12,STUN=13,SIGHT_CHARGE=14,HAMMER_CHARGE=15,GROUND_CHARGE=16,PUSH=17,READY=18,REJECT=19;
 public static Consumer<Effect> receive=e->{};
 public record Effect(int kind,int entity,int mode,Vec3 position,Vec3 direction,float radius,int duration) implements CustomPacketPayload {
  public static final Type<Effect> TYPE=new Type<>(Hollowforged.id("effect"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Effect> CODEC=new StreamCodec<>(){
   public Effect decode(RegistryFriendlyByteBuf b){int k=b.readVarInt(),e=b.readVarInt(),m=b.readVarInt();Vec3 p=new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),d=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());float r=b.readFloat();int t=b.readVarInt();if(k<0||k>19||m<0||m>2||!Double.isFinite(p.lengthSqr())||!Double.isFinite(d.lengthSqr())||!Float.isFinite(r)||r<0||r>40||t<0||t>1200)throw new IllegalArgumentException("Invalid effect");return new Effect(k,e,m,p,d,r,t);}
   public void encode(RegistryFriendlyByteBuf b,Effect e){b.writeVarInt(e.kind);b.writeVarInt(e.entity);b.writeVarInt(e.mode);b.writeDouble(e.position.x);b.writeDouble(e.position.y);b.writeDouble(e.position.z);b.writeDouble(e.direction.x);b.writeDouble(e.direction.y);b.writeDouble(e.direction.z);b.writeFloat(e.radius);b.writeVarInt(e.duration);}
  };
  public Type<Effect> type(){return TYPE;}
 }
 public record Charge(boolean active) implements CustomPacketPayload {
  public static final Type<Charge> TYPE=new Type<>(Hollowforged.id("sight_charge"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Charge> CODEC=new StreamCodec<>(){public Charge decode(RegistryFriendlyByteBuf b){return new Charge(b.readBoolean());}public void encode(RegistryFriendlyByteBuf b,Charge v){b.writeBoolean(v.active);}};
  public Type<Charge> type(){return TYPE;}
 }
 private static Effect event(LivingEntity e,int k,int mode,Vec3 pos,double radius,int ticks){return new Effect(k,e.getId(),mode,pos,e.getLookAngle().multiply(1,0,1).normalize(),(float)radius,ticks);}
 private static void nearby(LivingEntity e,Effect fx){if(e.level() instanceof ServerLevel level)for(var viewer:level.players())if(viewer.distanceToSqr(fx.position)<96*96)PacketDistributor.sendToPlayer(viewer,fx);}
 public static void burst(LivingEntity e,int k,Vec3 at,double radius,int ticks){nearby(e,event(e,k,0,at,radius,ticks));}
 public static void target(ServerPlayer owner,LivingEntity target,int kind,int duration){
  var d=HFState.data(target);String key="fx_mark_"+kind+"_"+owner.getUUID();long now=HFState.now(target);if(d.getLong(key)>now)return;d.putLong(key,now+20);
  PacketDistributor.sendToPlayer(owner,event(target,kind,1,target.position(),1,duration));
 }
 public static void mark(LivingEntity target,int kind,int duration){nearby(target,event(target,kind,1,target.position(),1,duration));}
 public static void charge(ServerPlayer p,boolean active){
  if(!HFState.crown(p)||!p.isAlive()||p.isSpectator())return;
  var d=HFState.data(p);long now=HFState.now(p);if(active&&now<d.getLong("fx_sight_after"))return;
  if(active)d.putLong("fx_sight_after",now+4);
  nearby(p,event(p,SIGHT_CHARGE,active?1:2,p.position(),1,24));
 }
 private static void state(ServerPlayer p,int kind,boolean active,double radius){
  var d=HFState.data(p);String key="fx_state_"+kind;boolean before=d.getBoolean(key);
  if(active&&(!before||HFState.now(p)%20==0))nearby(p,event(p,kind,1,p.position(),radius,40));
  else if(!active&&before)nearby(p,event(p,kind,2,p.position(),radius,12));
  d.putBoolean(key,active);
 }
 public static void preview(ServerPlayer p,int kind,boolean stop){
  if(!Boolean.getBoolean("lc_hollowforged.review"))return;
  LivingEntity source=p.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,p.getBoundingBox().inflate(32)).stream().findFirst().map(x->(LivingEntity)x).orElse(p);
  Vec3 center=new Vec3(0,65,0);double radius=kind==FRACTURE?24:kind==ARGUMENT?20:kind==WITNESS?32:kind==LAUNCH||kind==SOVEREIGN||kind==PUSH?16:kind==FALL?6:kind==MELEE?4:1;
  boolean state=kind==SIGHT||kind==UNBROKEN||kind==SOVEREIGN||kind==ANCHOR||kind==REFUSAL||kind>=EXPOSED&&kind<=GROUND_CHARGE;
  nearby(source,new Effect(kind,source.getId(),stop?2:state?1:0,center,new Vec3(0,0,1),(float)radius,state?100:kind==ARGUMENT?32:24));
 }
 public static void tick(ServerPlayer p){
  state(p,SIGHT,HFState.crown(p)&&HFState.data(p).getBoolean("awareness"),1);
  state(p,UNBROKEN,HFState.chest(p)&&HFState.active(p,"unbroken"),1);
  state(p,SOVEREIGN,HFState.legs(p)&&HFState.active(p,"sovereign"),16);
  state(p,ANCHOR,HFState.legs(p)&&HFState.data(p).getBoolean("anchor"),1.1);
  state(p,REFUSAL,HFState.chest(p)&&HFState.active(p,"refusal_protection"),1);
  state(p,LAUNCH,HFState.boots(p)&&HFState.active(p,"cataclysm")&&!p.onGround(),1);
  state(p,HAMMER_CHARGE,HFState.hammer(p)&&(HFState.active(p,"windup")||p.isUsingItem()&&p.getUsedItemHand()==net.minecraft.world.InteractionHand.MAIN_HAND),1);
  state(p,GROUND_CHARGE,HFState.legs(p)&&p.isShiftKeyDown()&&CombatAbilities.supported(p)&&p.getDeltaMovement().horizontalDistanceSqr()<.0025&&!HFState.active(p,"sovereign")&&HFState.data(p).getLong("cd_3")<=HFState.now(p),1.5);
 }
}
