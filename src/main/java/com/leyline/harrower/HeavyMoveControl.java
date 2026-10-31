package com.leyline.harrower;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Grounded steering: build momentum, brake deliberately and turn at most 1.5 degrees/tick. */
final class HeavyMoveControl extends MoveControl {
    private final Harrower boss;
    private float speed;
    HeavyMoveControl(Harrower boss) { super(boss); this.boss = boss; }
    @Override public void tick() {
        float wantedSpeed = 0;
        boolean backward=boss.combat().backingUp();
        if (operation == Operation.MOVE_TO && boss.action() == Harrower.NONE) {
            double dx = wantedX - boss.getX(), dz = wantedZ - boss.getZ();
            if (dx * dx + dz * dz > .04) {
                float desired = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90 + (backward?180:0);
                boss.setYRot(Mth.approachDegrees(boss.getYRot(), desired, 1.5F));
                float error = Math.abs(Mth.wrapDegrees(desired - boss.getYRot()));
                // A tight turn costs forward speed; no sideways skating toward a waypoint.
                wantedSpeed = (float)(speedModifier * boss.getAttributeValue(Attributes.MOVEMENT_SPEED) * (.028/.26))
                        * Mth.clamp(1 - error / 70, 0, 1);
            }
        }
        if(boss.action()==Harrower.NONE&&(operation!=Operation.MOVE_TO||wantedSpeed==0)){speed=0;boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);}
        var target=boss.getTarget();
        if(boss.action()==Harrower.NONE&&!backward&&target!=null&&boss.combat().active()&&boss.combat().canEngage(target)&&target.position().subtract(boss.position()).horizontalDistance()<5.5){wantedSpeed=0;speed=0;boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);}
        if(boss.action()==Harrower.NONE&&wantedSpeed>0){var direction=net.minecraft.world.phys.Vec3.directionFromRotation(0,boss.getYRot());if(!boss.combat().safeAdvance(boss.position().add(direction.scale(backward?-1.5:1.5)))){wantedSpeed=0;speed=0;boss.setDeltaMovement(0,boss.getDeltaMovement().y,0);}}
        operation = Operation.WAIT;
        if (boss.action() != Harrower.NONE) speed = 0;
        else speed = Mth.approach(speed, wantedSpeed, wantedSpeed > speed ? .0025F : .0045F);
        boss.setSpeed(speed);
        boss.setZza(speed > .001 ? (backward?-1:1) : 0);
        boss.setXxa(0);
    }
}
