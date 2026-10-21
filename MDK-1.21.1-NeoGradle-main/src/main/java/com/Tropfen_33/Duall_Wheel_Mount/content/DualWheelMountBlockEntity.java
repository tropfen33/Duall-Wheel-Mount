package com.Tropfen_33.Duall_Wheel_Mount.content;

import com.Tropfen_33.Duall_Wheel_Mount.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collection;
import java.util.List;

public class DualWheelMountBlockEntity extends KineticBlockEntity
        implements BlockEntitySubLevelActor, Clearable {

    private static final String INVENTORY_TAG = "Wheels";

    private static final double MAX_ALLOWED_EXTENSION = 0.65;
    private static final double NO_WHEEL_EXTENSION = 0.5;

    private static final Collection<DualWheelMountBlockEntity> QUEUED_MOUNTS = new ObjectOpenHashSet<>();

    private final DualWheelMountInventory inventory;
    private SuspensionStrengthValueBehaviour strength;

    private double leftExtension = NO_WHEEL_EXTENSION;
    private double lastLeftExtension = NO_WHEEL_EXTENSION;
    private double rightExtension = NO_WHEEL_EXTENSION;
    private double lastRightExtension = NO_WHEEL_EXTENSION;

    private double leftAngle;
    private double lastLeftAngle;
    private double rightAngle;
    private double lastRightAngle;

    private double leftAngularVelocity;
    private double rightAngularVelocity;

    private double leftFriction = 1.0;
    private double rightFriction = 1.0;

    private boolean leftLiftedUp;
    private boolean rightLiftedUp;

    private final Vector3d leftForcePos = new Vector3d();
    private final Vector3d rightForcePos = new Vector3d();
    private final Vector3d leftForce = new Vector3d();
    private final Vector3d rightForce = new Vector3d();

    private final ForceTotal forceTotal = new ForceTotal();

    public DualWheelMountBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUAL_WHEEL_MOUNT.get(), pos, state);
        this.inventory = new DualWheelMountInventory(this);
    }

    public static void applyAllBatchedForces(ServerLevel level, double timeStep) {
        for (DualWheelMountBlockEntity blockEntity : QUEUED_MOUNTS) {
            if (!blockEntity.isRemoved()) {
                blockEntity.applyBatchedForces();
            }
        }
        QUEUED_MOUNTS.clear();
    }

    private void applyBatchedForces() {
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel == null) {
            return;
        }

        RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel) subLevel);
        handle.applyForcesAndReset(forceTotal);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(this.strength = new SuspensionStrengthValueBehaviour(
                Component.literal("Suspension Strength"), this, new SuspensionStrengthValueBox(0)));
        this.strength.value = 10;
    }

    public ItemStack getLeftWheel() {
        return inventory.getLeftWheel();
    }

    public ItemStack getRightWheel() {
        return inventory.getRightWheel();
    }

    public boolean hasLeftWheel() {
        return !getLeftWheel().isEmpty() && getLeftWheel().get(OffroadDataComponents.TIRE) != null;
    }

    public boolean hasRightWheel() {
        return !getRightWheel().isEmpty() && getRightWheel().get(OffroadDataComponents.TIRE) != null;
    }

    public DualWheelMountInventory getInventory() {
        return inventory;
    }

    public void onStackChanged() {
        invalidateRenderBoundingBox();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        boolean left = hasLeftWheel();
        boolean right = hasRightWheel();

        if (!left && !right) {
            return;
        }

        MassData massData = subLevel.getMassTracker();
        Pose3d pose = subLevel.logicalPose();
        Direction facing = getBlockState().getValue(DualWheelMountBlock.HORIZONTAL_FACING);

        if (left) {
            applyWheelPhysics(subLevel, massData, pose, facing, getLeftWheel(), true, timeStep);
        }
        if (right) {
            applyWheelPhysics(subLevel, massData, pose, facing, getRightWheel(), false, timeStep);
        }

        QUEUED_MOUNTS.add(this);
    }

    private void applyWheelPhysics(
            ServerSubLevel subLevel,
            MassData massData,
            Pose3d pose,
            Direction facing,
            ItemStack wheelStack,
            boolean leftSide,
            double timeStep) {

        TireLike tire = wheelStack.get(OffroadDataComponents.TIRE);
        if (tire == null) {
            return;
        }

        float radius = tire.radius();
        Vec3 wheelPos = getWheelPosition(facing, leftSide);
        Vector3d forcePos = leftSide ? leftForcePos : rightForcePos;
        Vector3d force = leftSide ? leftForce : rightForce;

        forcePos.set(wheelPos.x, wheelPos.y, wheelPos.z);

        double normalMass = 1.0 / massData.getInverseNormalMass(forcePos, OrientedBoundingBox3d.UP);
        double effectiveStrength = this.strength == null ? 10.0 : this.strength.getValue();
        double normalMassScaling = Math.min(normalMass / effectiveStrength, 1.0) * 10.0;
        double strengthMul = effectiveStrength * normalMassScaling * 2.0;
        double springStrength = effectiveStrength * normalMassScaling * 40.0;
        double dampingStrength = effectiveStrength * normalMassScaling;

        // Front redstone steers right; back redstone steers left.
        double steeringYaw = getSteeringYaw(facing);

        // The wheel axle runs left/right. Steering rotates the rolling and lateral
        // axes around Y together so both wheels follow the same steering angle.
        Vec3i sideAxis = facing.getCounterClockWise().getNormal();
        Vector3dc sideD = getRotatedWheelAxis(sideAxis, steeringYaw);
        Vec3i rollingAxis = facing.getNormal();
        Vector3dc normalD = getRotatedWheelAxis(rollingAxis, steeringYaw);

        TerrainCastResult terrain = computeMaxExtensionToTerrain(facing, wheelPos, sideD, pose);
        double maxExtension = terrain.maxExtension();

        if (leftSide) {
            leftExtension = Mth.lerp(1.0, leftExtension, maxExtension);
        } else {
            rightExtension = Mth.lerp(1.0, rightExtension, maxExtension);
        }

        if (maxExtension > MAX_ALLOWED_EXTENSION + radius + 0.25) {
            if (leftSide) {
                leftExtension = MAX_ALLOWED_EXTENSION;
                leftLiftedUp = true;
            } else {
                rightExtension = MAX_ALLOWED_EXTENSION;
                rightLiftedUp = true;
            }
            return;
        }

        if (leftSide) {
            leftLiftedUp = false;
        } else {
            rightLiftedUp = false;
        }

        double extension = leftSide ? leftExtension : rightExtension;
        double distance = (MAX_ALLOWED_EXTENSION / 6.0) + extension;
        double springLength = Mth.clamp(distance - radius, 0.0, MAX_ALLOWED_EXTENSION);

        Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.toJOML(wheelPos));
        Vector3d localVelocity = pose.transformNormalInverse(velocity);

        double dampingForce = -localVelocity.y * dampingStrength;
        double springForce = ((MAX_ALLOWED_EXTENSION - springLength) * springStrength + dampingForce) * timeStep;

        Vec3i rayHitNormal = terrain.normal().getNormal();
        Vec3 localForce = new Vec3(
                springForce * rayHitNormal.getX(),
                springForce * rayHitNormal.getY(),
                springForce * rayHitNormal.getZ());

        if (terrain.subLevel() != null) {
            localForce = terrain.subLevel().logicalPose().transformNormal(localForce);
        }
        localForce = pose.transformNormalInverse(localForce);

        force.set(localForce.x, localForce.y, localForce.z);

        double friction = terrain.minInteractingBlock() == null
                ? 1.0
                : fudgeFriction(PhysicsBlockPropertyHelper.getFriction(
                        this.level.getBlockState(terrain.minInteractingBlock())));
        friction = Math.max(friction, tire.minimumFriction());

        if (leftSide) {
            leftFriction = friction;
        } else {
            rightFriction = friction;
        }

        // Bottom redstone controls braking. A full signal gives the strongest
        // braking, while the wheel still retains a small amount of base friction.
        double brakeStrength = getBrakeStrength();
        double surfaceBraking = Math.min(friction, 1.0);
        double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;
        float kineticSpeed = facing.getAxis() == Direction.Axis.X ? getSpeed() : -getSpeed();

        // Keep the original Offroad drive-force multiplier, but do not let the
        // motor keep accelerating the sublevel past the rolling speed implied by
        // the kinetic RPM and this tire's circumference. Both wheel contacts can
        // still contribute suspension, braking, and lateral friction separately.
        double currentRollingSpeed = localVelocity.dot(normalD);
        double targetRollingSpeed = kineticSpeed * (Math.PI * 2.0 * radius / 60.0);
        double driveForce = kineticSpeed * (1.0 - brakeStrength) * surfaceBraking * 1.75 * timeStep;
        if (kineticSpeed * currentRollingSpeed > 0.0
                && Math.abs(currentRollingSpeed) >= Math.abs(targetRollingSpeed)) {
            driveForce = 0.0;
        }

        force.fma(
                (localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep)
                        + driveForce,
                normalD);
        force.fma(
                localVelocity.dot(sideD) * -0.6 * friction * strengthMul * timeStep,
                sideD);

        forceTotal.applyImpulseAtPoint(subLevel, forcePos, force);
    }

    private Vec3 getWheelPosition(Direction facing, boolean leftSide) {
        Direction side = leftSide ? facing.getCounterClockWise() : facing.getClockWise();
        return getBlockPos().relative(side).getCenter();
    }

    private static double fudgeFriction(double realValue) {
        return realValue < 1.0 ? 0.1 + 0.9 * realValue : realValue;
    }

    private TerrainCastResult computeMaxExtensionToTerrain(
            Direction facing, Vec3 wheelPosCenter, Vector3dc normalD, Pose3dc pose) {

        double minExtension = 5.0;
        Direction minNormal = Direction.UP;
        SubLevel minHitSubLevel = null;
        BlockPos minInteractingBlock = null;

        for (int i = -1; i <= 1; i++) {
            Vec3 localPosO = wheelPosCenter.add(JOMLConversion.toMojang(normalD).scale(i));
            ClipContext clipContext = new ClipContext(
                    localPosO,
                    localPosO.subtract(0.0, 5.0, 0.0),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty());

            ((ClipContextExtension) clipContext).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
            BlockHitResult clipResult = this.level.clip(clipContext);

            if (clipResult.getType() == HitResult.Type.MISS) {
                continue;
            }

            SubLevel hitSubLevel = Sable.HELPER.getContaining(this.level, clipResult.getLocation());
            Vec3 localHitPos = pose.transformPositionInverse(
                    hitSubLevel == null
                            ? clipResult.getLocation()
                            : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation()));

            if (localHitPos.y > wheelPosCenter.y || localPosO.distanceTo(localHitPos) < 0.05) {
                continue;
            }

            double dist = wheelPosCenter.y - localHitPos.y;
            if (dist <= 1e-5) {
                continue;
            }

            Direction dir = clipResult.getDirection();
            Vector3d hitNormal = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            if (hitSubLevel != null) {
                hitSubLevel.logicalPose().transformNormal(hitNormal);
            }
            pose.transformNormalInverse(hitNormal);

            if (hitNormal.dot(0.0, 1.0, 0.0) < 0.5) {
                continue;
            }

            if (dist < minExtension) {
                minExtension = dist;
                minNormal = dir;
                minHitSubLevel = hitSubLevel;
                minInteractingBlock = clipResult.getBlockPos();
            }
        }

        return new TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
    }

    public double getSteeringYawForRendering() {
        Direction facing = getBlockState().getValue(DualWheelMountBlock.HORIZONTAL_FACING);
        return getSteeringYaw(facing);
    }

    private double getSteeringYaw(Direction facing) {
        int frontSignal = this.level.getSignal(this.getBlockPos().relative(facing), facing);
        int backSignal = this.level.getSignal(this.getBlockPos().relative(facing.getOpposite()), facing.getOpposite());

        // Front redstone must always steer to the mount's right, regardless
        // of which horizontal direction the mount faces. The local steering
        // frame is reversed for NORTH and WEST, so compensate for those two
        // facings while leaving EAST and SOUTH unchanged.
        double steering = (frontSignal - backSignal) / 15.0;
        return -steering * (Math.PI / 6.0);
    }

    private double getBrakeStrength() {
        return this.level.getSignal(this.getBlockPos().below(), Direction.DOWN) / 15.0;
    }

    private Vector3dc getRotatedWheelAxis(Vec3i axis, double steeringYaw) {
        Vector3d result = new Vector3d(axis.getX(), axis.getY(), axis.getZ());
        result.rotateY(steeringYaw);
        return result;
    }

    @Override
    public void tick() {
        super.tick();

        lastLeftExtension = leftExtension;
        lastRightExtension = rightExtension;
        lastLeftAngle = leftAngle;
        lastRightAngle = rightAngle;

        if (!level.isClientSide) {
            return;
        }

        updateClientWheel(true);
        updateClientWheel(false);
    }

    private void updateClientWheel(boolean leftSide) {
        ItemStack stack = leftSide ? getLeftWheel() : getRightWheel();
        TireLike tire = stack.get(OffroadDataComponents.TIRE);

        if (tire == null) {
            if (leftSide) {
                leftAngle = 0.0;
                leftExtension = Mth.lerp(0.6, leftExtension, NO_WHEEL_EXTENSION);
            } else {
                rightAngle = 0.0;
                rightExtension = Mth.lerp(0.6, rightExtension, NO_WHEEL_EXTENSION);
            }
            return;
        }

        Direction facing = getBlockState().getValue(DualWheelMountBlock.HORIZONTAL_FACING);
        updateClientSuspensionExtension(facing, leftSide, tire.radius());

        float speed = facing.getAxis() == Direction.Axis.X ? -getSpeed() : getSpeed();
        double attemptedAngularVelocity = Mth.lerp(
                0.2,
                leftSide ? leftAngularVelocity : rightAngularVelocity,
                speed * Math.PI * 2.0 / 60.0 / 20.0);

        SubLevel subLevel = Sable.HELPER.getContaining(this);

        // This is client-side visual state, so the server-only lifted flags
        // cannot be used here. Check actual terrain contact on the client.
        boolean grounded = isWheelGroundedClient(facing, leftSide, tire.radius());
        if (subLevel == null || !grounded) {
            // Match the original Offroad Wheel Mount: when this wheel is not
            // being driven by contact with the physics sublevel, animate it
            // from the kinetic input instead of merely coasting to a stop.
            if (leftSide) {
                leftAngularVelocity = attemptedAngularVelocity;
                leftAngle += leftAngularVelocity;
            } else {
                rightAngularVelocity = attemptedAngularVelocity;
                rightAngle += rightAngularVelocity;
            }
            return;
        }

        Vec3 wheelPos = getWheelPosition(facing, leftSide);
        Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.toJOML(wheelPos));
        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity).div(20.0);

        Vec3i rollingAxis = facing.getNormal();
        Vector3dc normalD = getRotatedWheelAxis(rollingAxis, getSteeringYaw(facing));
        double translation = localVelocity.dot(normalD);
        double circumference = Math.PI * tire.radius() * 2.0;
        double angularDelta = -translation / circumference * Math.PI * 2.0;

        double friction = leftSide ? leftFriction : rightFriction;
        if (friction < 1.0) {
            angularDelta = Mth.lerp(friction, attemptedAngularVelocity, angularDelta);
        }

        if (leftSide) {
            leftAngularVelocity = angularDelta;
            leftAngle += angularDelta;
        } else {
            rightAngularVelocity = angularDelta;
            rightAngle += angularDelta;
        }
    }

    /**
     * Recreate the original Offroad Wheel Mount's client-side suspension extension.
     * Use Sable's sublevel-aware terrain cast and subtract the tire radius, just like
     * the original computeMaxExtension() path. A plain level.clip() misses terrain
     * transforms on moving sublevels and makes the visual suspension appear static.
     */
    private void updateClientSuspensionExtension(Direction facing, boolean leftSide, float radius) {
        if (this.level == null) {
            return;
        }

        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel == null) {
            setVisualExtension(leftSide, Mth.lerp(0.7,
                    leftSide ? leftExtension : rightExtension, MAX_ALLOWED_EXTENSION));
            return;
        }

        Pose3dc pose = subLevel.logicalPose();
        Vec3 wheelPos = getWheelPosition(facing, leftSide);
        Vec3i axleAxis = facing.getCounterClockWise().getNormal();
        Vector3dc sideD = getRotatedWheelAxis(axleAxis, getSteeringYaw(facing));
        TerrainCastResult terrain = computeMaxExtensionToTerrain(facing, wheelPos, sideD, pose);

        // Match the original Offroad calculation: extension is measured from the
        // tire's surface, not its center, and clamped to the original travel range.
        double targetExtension = Mth.clamp(terrain.maxExtension() - radius, -0.45, MAX_ALLOWED_EXTENSION);
        double currentExtension = leftSide ? leftExtension : rightExtension;
        setVisualExtension(leftSide, Mth.lerp(0.7, currentExtension, targetExtension));
    }

    private void setVisualExtension(boolean leftSide, double extension) {
        if (leftSide) {
            leftExtension = extension;
        } else {
            rightExtension = extension;
        }
    }

    private boolean isWheelGroundedClient(Direction facing, boolean leftSide, float radius) {
        if (this.level == null) {
            return false;
        }

        Vec3 wheelPos = getWheelPosition(facing, leftSide);
        Vec3 start = wheelPos.add(0.0, 0.15, 0.0);
        Vec3 end = wheelPos.add(0.0, -(radius + MAX_ALLOWED_EXTENSION + 0.35), 0.0);
        ClipContext clip = new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        BlockHitResult hit = this.level.clip(clip);
        return hit.getType() != HitResult.Type.MISS
                && hit.getDirection() == Direction.UP
                && hit.getLocation().y <= wheelPos.y + radius + 0.2;
    }

    public float getLerpedLeftAngle(float partialTick) {
        return (float) Mth.lerp(partialTick, lastLeftAngle, leftAngle);
    }

    public float getLerpedRightAngle(float partialTick) {
        return (float) Mth.lerp(partialTick, lastRightAngle, rightAngle);
    }

    public double getLerpedLeftExtension(float partialTick) {
        return Mth.lerp(partialTick, lastLeftExtension, leftExtension);
    }

    public double getLerpedRightExtension(float partialTick) {
        return Mth.lerp(partialTick, lastRightExtension, rightExtension);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        onStackChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.put(INVENTORY_TAG, inventory.write(registries));
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        inventory.suppressUpdate = true;
        if (tag.contains(INVENTORY_TAG)) {
            inventory.read(registries, tag.getCompound(INVENTORY_TAG));
        }
        inventory.suppressUpdate = false;
        onStackChanged();
        super.read(tag, registries, clientPacket);
    }

    private static class SuspensionStrengthValueBehaviour extends ScrollValueBehaviour {
        private static final int MAX_SUSPENSION_STRENGTH = 180;

        public SuspensionStrengthValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
            super(label, be, slot);
            this.between(5, MAX_SUSPENSION_STRENGTH);
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            return new ValueSettingsBoard(
                    this.label,
                    MAX_SUSPENSION_STRENGTH,
                    20,
                    List.of(Component.literal("Suspension Strength")),
                    new ValueSettingsFormatter(ValueSettings::format));
        }
    }

    private static final class SuspensionStrengthValueBox extends ValueBoxTransform {
        private final int hOffset;

        public SuspensionStrengthValueBox(int hOffset) {
            this.hOffset = hOffset;
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(DualWheelMountBlock.HORIZONTAL_FACING);
            // The position is already moved to the back face in getLocalOffset;
            // orient the value box outward instead of rotating it into the block.
            float yRot = AngleHelper.horizontalAngle(facing);
            TransformStack.of(ms)
                    .rotateYDegrees(yRot);
        }

        @Override
        public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
            Vec3 offset = this.getLocalOffset(level, pos, state);
            if (offset == null)
                return false;
            return localHit.distanceTo(offset) < this.scale / 3;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(DualWheelMountBlock.HORIZONTAL_FACING);
            float stateAngle = AngleHelper.horizontalAngle(facing) + 180;
            return VecHelper.rotateCentered(
                    VecHelper.voxelSpace(8 + this.hOffset, 8, 15.5f),
                    stateAngle,
                    Direction.Axis.Y);
        }
    }

    private record TerrainCastResult(
            double maxExtension,
            @NotNull Direction normal,
            @Nullable SubLevel subLevel,
            @Nullable BlockPos minInteractingBlock) {
    }
}
