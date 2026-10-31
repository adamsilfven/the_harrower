package com.leyline.harrower;
import com.leyline.harrower.combat.*;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=HarrowerMod.ID)
public final class DevelopmentCommands {
 private static int summon(net.minecraft.commands.CommandSourceStack source){
  var level=source.getLevel();var origin=source.getPosition();double yaw=Math.toRadians(source.getRotation().y);
  for(int distance:new int[]{18,24,30})for(int offset:new int[]{0,-35,35}){
   double angle=yaw+Math.toRadians(offset),x=origin.x-Math.sin(angle)*distance,z=origin.z+Math.cos(angle)*distance;
   int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(x),(int)Math.floor(z));Harrower boss=HarrowerMod.HARROWER.get().create(level);if(boss==null)return 0;boss.moveTo(x,y,z,source.getRotation().y+180,0);
   if(!level.getWorldBorder().isWithinBounds(boss.getBoundingBox())||!level.noCollision(boss)||level.containsAnyLiquid(boss.getBoundingBox()))continue;
   if(!level.getBlockState(boss.blockPosition().below()).isSolidRender(level,boss.blockPosition().below()))continue;
   if(source.getEntity() instanceof ServerPlayer player)boss.combat().encounter.setSummoner(player.getUUID());level.addFreshEntity(boss);
   source.sendSuccess(()->Component.literal("The Harrower summoned at "+boss.blockPosition().toShortString()+". No structure or catalyst required. Nearby Survival/Adventure players are attacked immediately."),true);return 1;
  }source.sendFailure(Component.literal("No clear site nearby. Move to open, solid ground with 12 blocks of headroom and try again."));return 0;
 }
 private static Harrower boss(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c)throws com.mojang.brigadier.exceptions.CommandSyntaxException{return EntityArgument.getEntity(c,"boss") instanceof Harrower h?h:null;}
 @SubscribeEvent public static void register(RegisterCommandsEvent event){
  event.getDispatcher().register(Commands.literal("harrower")
   .then(Commands.literal("damagefeedback").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{var player=c.getSource().getPlayerOrException();boolean enabled=BoolArgumentType.getBool(c,"enabled");player.getPersistentData().putBoolean("HarrowerHideDamage",!enabled);c.getSource().sendSuccess(()->Component.literal("Harrower damage chat "+(enabled?"enabled":"disabled")),false);return 1;})))
   .then(Commands.literal("summon").requires(s->s.hasPermission(2)).executes(c->summon(c.getSource())))
   .then(Commands.literal("join").then(Commands.argument("boss",EntityArgument.entity()).executes(c->{Harrower h=boss(c);if(h!=null&&c.getSource().getEntity() instanceof ServerPlayer p&&h.combat().encounter.join(p)){c.getSource().sendSuccess(()->Component.literal("Joined the Harrower encounter. Health scaling locks when combat starts."),false);return 1;}c.getSource().sendFailure(Component.literal("Join before combat starts, in Survival or Adventure, within 64 blocks."));return 0;})))
   .then(Commands.literal("status").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).executes(c->{Harrower h=boss(c);if(h==null)return 0;c.getSource().sendSuccess(()->Component.literal(h.combat().status()),false);return 1;})))
   .then(Commands.literal("reset").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).executes(c->{Harrower h=boss(c);if(h==null||!h.isAlive())return 0;h.combat().reset();return 1;})))
   .then(Commands.literal("ai").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{Harrower h=boss(c);if(h==null)return 0;h.combat().autonomy(BoolArgumentType.getBool(c,"enabled"));return 1;}))))
   .then(Commands.literal("effects").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).executes(c->{var p=EntityArgument.getPlayer(c,"player");c.getSource().sendSuccess(()->Component.literal(HollowingEffects.status(p)),false);return 1;})))
   .then(Commands.literal("debug").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{Harrower h=boss(c);if(h==null)return 0;h.combat().debug(BoolArgumentType.getBool(c,"enabled"));return 1;}))))
   .then(Commands.literal("phase").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).then(Commands.argument("phase",IntegerArgumentType.integer(1,3)).executes(c->{Harrower h=boss(c);if(h==null)return 0;int phase=IntegerArgumentType.getInteger(c,"phase");h.combat().encounter.stage=phase;h.setHealth(h.getMaxHealth()*(phase==1?1:phase==2?.69F:.34F));h.combat().force(CombatMove.TRANSITION);return 1;}))))
   .then(Commands.literal("target").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).then(Commands.argument("target",EntityArgument.entity()).executes(c->{Harrower h=boss(c);if(h!=null&&EntityArgument.getEntity(c,"target") instanceof LivingEntity target){h.combat().forceTarget(target);return 1;}return 0;}))))
   .then(Commands.literal("animate").requires(s->s.hasPermission(2)).then(Commands.argument("boss",EntityArgument.entity()).then(Commands.argument("clip",StringArgumentType.word()).suggests((c,b)->{for(var move:CombatMove.values())b.suggest(move.clip);b.suggest("slam");return b.buildFuture();}).executes(c->{
    Harrower h=boss(c);if(h==null)return 0;String name=StringArgumentType.getString(c,"clip");CombatMove selected=null;for(var move:CombatMove.values())if(move.clip.equals(name))selected=move;if(name.equals("slam"))selected=CombatMove.RUPTURE;
    if(selected==null){c.getSource().sendFailure(Component.literal("Unknown clip. Use command suggestions."));return 0;}h.combat().force(selected);return 1;
   })))));
 }
}
