package com.leyline.harrower.combat;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import com.leyline.harrower.Harrower;

/** Explicit integration seam. No guessed LC classes, reflection, saved-data keys or block IDs.
 * The actual LC adapter must be audited against the installed Core before installation.
 */
public final class LeylineBridge {
 public enum Color { WHITE,BLUE,BLACK,RED,GREEN }
 public record OwnedLeyline(String identity,UUID owner,Color color,Vec3 center,boolean active){}
 public record DeathContext(UUID summoner,Map<Color,Integer> suppressionTicks,int eligibleColors){
  public double additionalCoreChance(){return Math.min(5,eligibleColors)*.05;}
 }
 public interface Access {
  Collection<OwnedLeyline> ownedActive(ServerLevel level,UUID summoner);
  /** Atomically replace this boss's leases, suspending normal effects while any live lease remains. */
  void updateSuppression(ServerLevel level,UUID boss,Collection<OwnedLeyline> eligible,long expiresAt);
  /** Remove only this boss's lease; do not cancel another encounter's suppression. */
  void release(ServerLevel level,UUID boss);
  default void onDeath(ServerLevel level,Harrower boss,DeathContext context){}
  String auditedVersion();
 }
 private static Access access;
 public static synchronized void install(Access adapter){Objects.requireNonNull(adapter);if(access!=null&&access!=adapter)throw new IllegalStateException("An audited Leyline adapter is already installed");access=adapter;}
 public static synchronized void uninstall(Access expected){if(access==expected)access=null;}
 public static Access access(){return access;}
 public static String status(){return access==null?"NO_AUDITED_CORE_ADAPTER":access.auditedVersion();}
 private LeylineBridge(){}
}
