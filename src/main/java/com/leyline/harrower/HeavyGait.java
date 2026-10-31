package com.leyline.harrower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Server-owned planted feet. Stance anchors remain in world space, including during turns.
 * Each advance cycle travels 3.0 blocks. 62% stance gives two short double-support intervals.
 * Six local ankle coordinates plus distance phase are synchronized by Harrower.
 */
final class HeavyGait {
    static final float STRIDE = 3.0F;
    private final Harrower boss;
    private final Foot[] feet = {new Foot(), new Foot()};
    private Vec3 previous;
    private float phase,previousYaw;
    private int footfalls,lastSerial=-1;
    private boolean wasGrounded=true;
    private boolean previousLocomotion=true;private float actionEntryClock;
    private static final class Foot { Vec3 anchor, from, to, position,actionFrom; boolean swing,settling; int recovery; Vec3 recoveryFrom,recoveryTo; }
    HeavyGait(Harrower boss) { this.boss = boss; }
    int footfalls() { return footfalls; }
    private Vec3 world(double x, double y, double z) {
        double a = Math.toRadians(boss.getYRot()), c = Math.cos(a), s = Math.sin(a);
        return boss.position().add(x*c + z*s, y, x*s-z*c);
    }
    private double ground(Vec3 at) {
        // Sample toe, ankle and heel. Respect collision shapes (slabs/stairs), never excavate.
        java.util.ArrayList<Double> supports=new java.util.ArrayList<>();
        double yaw=Math.toRadians(boss.getYRot()),c=Math.cos(yaw),s=Math.sin(yaw);
        for(double x:new double[]{-.55,0,.55})for(double z:new double[]{.08,.55,1.05}) {
            Vec3 p=at.add(x*c+z*s,0,x*s-z*c);int bx=Mth.floor(p.x),bz=Mth.floor(p.z);
            for(int y=Mth.floor(boss.getY()+1.2);y>=Mth.floor(boss.getY()-8);y--){
                BlockPos b=new BlockPos(bx,y,bz);var state=boss.level().getBlockState(b);
                if(state.getBlock() instanceof net.minecraft.world.level.block.BushBlock||state.is(net.minecraft.tags.BlockTags.LEAVES))continue;
                var shape=state.getCollisionShape(boss.level(),b);double top=-1;
                for(var box:shape.toAabbs())if(p.x-bx>=box.minX&&p.x-bx<=box.maxX&&p.z-bz>=box.minZ&&p.z-bz<=box.maxZ)top=Math.max(top,box.maxY);
                if(top>=0){supports.add(y+top);break;}
            }
        }
        if(supports.isEmpty())return boss.getY()-1.5;
        supports.sort(Double::compare);return Mth.clamp(supports.get(supports.size()-1),boss.getY()-8,boss.getY()+1.2);
    }

    void tick() {
        Vec3 now=boss.position();
        if(previous==null || now.distanceToSqr(previous)>16) reset();
        // Airborne feet follow the body, never obsolete world-space ground anchors.
        if(!boss.onGround()){
            previous=now;previousYaw=boss.getYRot();wasGrounded=false;
            for(int i=0;i<2;i++){Foot f=feet[i];f.position=world(i==0?1.6875:-1.6875,.625,-.3125);f.anchor=f.position;f.swing=false;f.recovery=0;boss.syncFoot(i,i==0?1.6875F:-1.6875F,.625F,-.3125F);}
            boss.syncPhase(phase);return;
        }
        if(!wasGrounded){reset();wasGrounded=true;}
        double travel=Math.min(.4,now.subtract(previous).horizontalDistance()); previous=now;
        double turnTravel=Math.abs(Mth.wrapDegrees(boss.getYRot()-previousYaw))*Math.PI/180*1.8;previousYaw=boss.getYRot();
        double stepTravel=Math.max(travel,turnTravel);
        if(boss.locomotion() && boss.onGround()) {
            phase += (float)stepTravel/(boss.isCharging()?2.8F:STRIDE);
            // Finish a suspended swing on braking; stop only with both feet supporting weight.
            float half=phase% .5F;
            if(stepTravel<.002 && half>.10F) phase+=.0125F;
        }
        if(!boss.locomotion()&&(lastSerial!=boss.actionSequence()||previousLocomotion)) {
            actionEntryClock=boss.combat().clock();phase=(float)Math.ceil(phase*2)/2;
            for(Foot foot:feet){foot.actionFrom=foot.position;foot.settling=foot.swing;
                if(foot.swing)foot.anchor=new Vec3(foot.position.x,ground(foot.position)+.625,foot.position.z);
                foot.swing=false;
            }
        }
        lastSerial=boss.actionSequence();previousLocomotion=boss.locomotion();
        for(int i=0;i<2;i++) {
            Foot foot=feet[i]; float q=(phase+i*.5F)%1;
            if(foot.recovery>0){
                double u=(9-foot.recovery)/8.0,ease=u*u*(3-2*u);
                foot.position=foot.recoveryFrom.lerp(foot.recoveryTo,ease).add(0,Math.sin(Math.PI*u)*.22,0);
                if(--foot.recovery==0){foot.anchor=foot.recoveryTo;foot.position=foot.anchor;footfalls++;stomp(foot.anchor,.8F);}
                sync(i,foot.position,now);continue;
            }
            boolean swing=q>=.62F && boss.locomotion() && boss.onGround();
            if(swing && !foot.swing) {
                foot.from=foot.anchor;
                Vec3 dest=world(i==0?1.6875:-1.6875,0,-.3125-(travel>.005?(boss.isCharging()?.85:.65):0));
                foot.to=new Vec3(dest.x,ground(dest)+.625,dest.z);
            }
            if(swing) {
                double u=(q-.62)/.38, eased=u*u*(3-2*u);
                foot.position=foot.from.lerp(foot.to,eased).add(0,Math.sin(Math.PI*u)*.23,0);
            } else {
                if(foot.swing) { foot.anchor=foot.to; footfalls++; stomp(foot.anchor,1); }
                foot.position=foot.anchor;
                if(!boss.locomotion()) {
                    var move=boss.combat().move();double clock=boss.combat().clock();
                    if(move.footStrike()&&move.side==i){
                        Vec3 neutral=world(i==0?1.6875:-1.6875,0,-.3125+boss.combat().footBack(i));
                        double u=Math.min(1,clock/(move.windup*.7)),ease=u*u*(3-2*u);
                        Vec3 origin=foot.actionFrom==null?foot.anchor:foot.actionFrom;
                        double x=origin.x+(neutral.x-origin.x)*ease,z=origin.z+(neutral.z-origin.z)*ease;
                        Vec3 at=new Vec3(x,0,z);foot.position=new Vec3(x,ground(at)+.625+boss.combat().footLift(i),z);foot.anchor=foot.position;
                    }else if(foot.settling&&foot.actionFrom!=null){
                        double u=Math.min(1,(clock-actionEntryClock)/6),ease=u*u*(3-2*u);foot.position=foot.actionFrom.lerp(foot.anchor,ease);
                        if(u>=1){foot.settling=false;footfalls++;stomp(foot.anchor,.8F);}
                    }
                }
            }
            foot.swing=swing;
            // Correct an overextended stance through a visible step, never a collapsing pelvis.
            Vec3 hip=world(i==0?1.5:-1.5,0,.375);
            double reach=Math.hypot(foot.position.x-hip.x,foot.position.z-hip.z);
            boolean elevated=foot.position.y>now.y+1.125;
            if(!swing&&!authoredFoot(i)){double floor=ground(foot.position)+.625;foot.position=new Vec3(foot.position.x,floor,foot.position.z);foot.anchor=foot.position;}
            boolean lostSupport=!swing&&Math.abs(foot.position.y-(ground(foot.position)+.625))>.18;
            boolean authoredLift=boss.combat().move().footStrike()&&boss.combat().move().side==i;
            if(!authoredLift&&!swing&&feet[1-i].recovery==0&&(reach>1.9||elevated||lostSupport)){
                Vec3 neutral=world(i==0?1.6875:-1.6875,0,-.3125);
                foot.recoveryFrom=foot.position;foot.recoveryTo=new Vec3(neutral.x,ground(neutral)+.625,neutral.z);foot.recovery=8;foot.swing=false;
            }
            sync(i,foot.position,now);
        }
        boss.syncPhase(phase);
    }
    private boolean authoredFoot(int i){return boss.combat().move().footStrike()&&boss.combat().move().side==i;}
    private void sync(int i,Vec3 position,Vec3 body){
        Vec3 d=position.subtract(body);double a=Math.toRadians(boss.getYRot()),c=Math.cos(a),s=Math.sin(a);
        boss.syncFoot(i,(float)(d.x*c+d.z*s),(float)d.y,(float)(d.x*s-d.z*c));
    }
    private void reset() {
        previous=boss.position();phase=0;previousYaw=boss.getYRot();
        for(int i=0;i<2;i++) {Vec3 p=world(i==0?1.6875:-1.6875,.625,-.3125);feet[i].anchor=new Vec3(p.x,ground(p)+.625,p.z);feet[i].position=feet[i].anchor;feet[i].swing=false;feet[i].recovery=0;}
    }
    void slamRing(double radius) {
        if(!(boss.level() instanceof ServerLevel level))return;
        // 48 ground samples / 96 particles once per slam; never alter the terrain.
        for(int i=0;i<48;i++){
            double a=i*Math.PI*2/48;
            Vec3 at=boss.position().add(Math.cos(a)*radius,0,Math.sin(a)*radius);
            double y=ground(at);BlockPos p=BlockPos.containing(at.x,y-.05,at.z);
            var state=level.getBlockState(p);
            if(!state.isAir())level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),at.x,y+.08,at.z,2,.12,.08,.12,.03);
        }
    }
    void stomp(Vec3 ankle,float scale) {
        if(!(boss.level() instanceof ServerLevel level))return;
        BlockPos p=BlockPos.containing(ankle.add(0,-.75,0));var state=level.getBlockState(p);
        if(state.isAir()) {p=p.below();state=level.getBlockState(p);}
        if(!state.isAir())level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),ankle.x,ankle.y-.5,ankle.z,(int)(10*scale),.7,.10,.7,.025);
        boss.playSound(HarrowerMod.STEP.get(),1.1F*scale,.92F);
        boss.playSound(SoundEvents.STONE_BREAK,1.2F*scale,.55F);
    }
}
