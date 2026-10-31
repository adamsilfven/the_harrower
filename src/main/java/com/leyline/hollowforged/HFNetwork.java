package com.leyline.hollowforged;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class HFNetwork {
 public static Consumer<View> receive=view->{};
 public record Action(int ability) implements CustomPacketPayload {
  public static final Type<Action> TYPE=new Type<>(Hollowforged.id("action"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=new StreamCodec<>(){
   public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,Action v){b.writeVarInt(v.ability());}
  };
  @Override public Type<Action> type(){return TYPE;}
 }
 public record View(String dimension,int[] reveals,int[] cooldowns,int equipment,int flags,String targetStatus) implements CustomPacketPayload {
  public static final Type<View> TYPE=new Type<>(Hollowforged.id("view"));
  public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=new StreamCodec<>(){
   public View decode(RegistryFriendlyByteBuf b){String dimension=b.readUtf(128);int count=b.readVarInt();if(count<0||count>256)throw new IllegalArgumentException("Invalid reveal count");int[] ids=new int[count];for(int i=0;i<count;i++)ids[i]=b.readVarInt();int[] cd=new int[7];for(int i=0;i<7;i++)cd[i]=b.readVarInt();return new View(dimension,ids,cd,b.readVarInt(),b.readVarInt(),b.readUtf(256));}
   public void encode(RegistryFriendlyByteBuf b,View v){b.writeUtf(v.dimension(),128);b.writeVarInt(v.reveals().length);for(int id:v.reveals())b.writeVarInt(id);for(int i=0;i<7;i++)b.writeVarInt(v.cooldowns()[i]);b.writeVarInt(v.equipment());b.writeVarInt(v.flags());b.writeUtf(v.targetStatus(),256);}
  };
  @Override public Type<View> type(){return TYPE;}
 }
 public static void register(RegisterPayloadHandlersEvent event){
  var registrar=event.registrar("2");
  registrar.playToClient(HFEffects.Effect.TYPE,HFEffects.Effect.CODEC,(payload,ctx)->ctx.enqueueWork(()->HFEffects.receive.accept(payload)));
  registrar.playToServer(HFEffects.Charge.TYPE,HFEffects.Charge.CODEC,(payload,ctx)->ctx.enqueueWork(()->{if(ctx.player() instanceof ServerPlayer p)HFEffects.charge(p,payload.active());}));
  registrar.playToServer(Action.TYPE,Action.CODEC,(payload,ctx)->ctx.enqueueWork(()->{
   if(ctx.player() instanceof ServerPlayer p){var d=HFState.data(p);long now=HFState.now(p);if(now<d.getLong("request_after"))return;d.putLong("request_after",now+4);CombatAbilities.activate(p,payload.ability());}
  }));
  registrar.playToClient(View.TYPE,View.CODEC,(payload,ctx)->ctx.enqueueWork(()->receive.accept(payload)));
 }
 public static void sync(ServerPlayer p){
  int mask=0;int[] cooldowns=new int[7];long now=HFState.now(p);var d=HFState.data(p);
  for(int i=0;i<7;i++){if(HFState.equipped(p,i))mask|=1<<i;cooldowns[i]=(int)Math.clamp(d.getLong("cd_"+i)-now,0,6000);}
  if(mask==0&&!d.getBoolean("synced_equipped"))return;d.putBoolean("synced_equipped",mask!=0);
  int flags=(d.getBoolean("awareness")?1:0)|(d.getBoolean("anchor")?2:0)|(HFState.active(p,"unbroken")?4:0)|(HFState.active(p,"sovereign")?8:0)|(HFState.active(p,"cataclysm")?16:0)|(HFState.active(p,"windup")?32:0)|(HFState.active(p,"refusal_protection")?64:0);
  PacketDistributor.sendToPlayer(p,new View(p.level().dimension().location().toString(),CombatAbilities.reveals(p),cooldowns,mask,flags,CombatAbilities.targetStatus(p)));
 }
}
