package com.Tropfen_33.Duall_Wheel_Mount.events;

import com.Tropfen_33.Duall_Wheel_Mount.content.DualWheelMountBlockEntity;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;

public final class DualWheelMountCommonEvents {
    private DualWheelMountCommonEvents() {}

    public static void physicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        DualWheelMountBlockEntity.applyAllBatchedForces(level, timeStep);
    }
}
