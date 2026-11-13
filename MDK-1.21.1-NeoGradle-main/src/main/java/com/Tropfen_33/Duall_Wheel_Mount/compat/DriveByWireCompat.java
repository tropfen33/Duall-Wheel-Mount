package com.Tropfen_33.Duall_Wheel_Mount.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Optional Drive-By-Wire support. There is NO compile-time or load-time dependency:
 * Drive-By-Wire is only touched through reflection (a cached MethodHandle) when the
 * mod is installed. Without it, this behaves exactly like {@link Level#getSignal}.
 *
 * <p>Drive-By-Wire does not place real redstone power next to blocks. Instead it patches
 * Offroad's own WheelMountBlockEntity with mixins that redirect every
 * {@code Level.getSignal(BlockPos, Direction)} call to
 * {@code WireRedstoneCompat.getSignalIncludingReverseWire(Level, BlockPos, Direction)}.
 * The dual mount is a separate class, so those mixins never reach it; this class applies
 * the same redirect by hand.
 */
public final class DriveByWireCompat {
    private static final String MOD_ID = "drivebywire";
    private static final String COMPAT_CLASS = "edn.stratodonut.drivebywire.compat.WireRedstoneCompat";
    private static final String COMPAT_METHOD = "getSignalIncludingReverseWire";

    /** Lazily resolved; null when Drive-By-Wire is absent or its API changed. */
    private static volatile MethodHandle handle;
    private static volatile boolean resolved;
    private static volatile boolean disabledAfterError;

    private DriveByWireCompat() {}

    // ---------------------------------------------------------------------
    // Optional debugging. Start the game with  -Ddualwheelmount.debugWire=true
    // to log (about once per second per mount) what each face of the mount sees.
    // ---------------------------------------------------------------------
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("dualwheelmount.debugWire");
    private static volatile long lastDebugLog;
    private static volatile Method managerGet;
    private static volatile Method managerGetSignalAt;

    /** Logs the vanilla, compat and raw wire-network signal for all 6 faces of a mount. */
    public static void debug(Level level, BlockPos mountPos) {
        if (!DEBUG || level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDebugLog < 1000L) {
            return;
        }
        lastDebugLog = now;
        StringBuilder sb = new StringBuilder();
        sb.append("[DualWheelMount wire debug] mount=").append(mountPos)
                .append(" clientSide=").append(level.isClientSide())
                .append(" dbwLoaded=").append(ModList.get() != null && ModList.get().isLoaded(MOD_ID))
                .append(" handle=").append(resolve() != null)
                .append(" disabledAfterError=").append(disabledAfterError);
        for (Direction d : Direction.values()) {
            BlockPos neighbour = mountPos.relative(d);
            int vanilla = level.getSignal(neighbour, d);
            int viaCompat = getSignal(level, neighbour, d);
            int raw = rawWireSignal(level, mountPos, d);
            sb.append("\n    ").append(d).append(": vanilla=").append(vanilla)
                    .append(" compat=").append(viaCompat)
                    .append(" rawWireSinkOnThisFace=").append(raw);
        }
        LOGGER.info(sb.toString());
    }

    /** Signal stored in Drive-By-Wire's network for a sink on face {@code face} of {@code pos}. */
    private static int rawWireSignal(Level level, BlockPos pos, Direction face) {
        try {
            if (managerGet == null) {
                Class<?> mgr = Class.forName("edn.stratodonut.drivebywire.wire.WireNetworkManager");
                managerGet = mgr.getMethod("get", Level.class);
                managerGetSignalAt = mgr.getMethod("getSignalAt", BlockPos.class, Direction.class);
            }
            Object manager = managerGet.invoke(null, level);
            return (int) managerGetSignalAt.invoke(manager, pos, face);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Drop-in replacement for {@code level.getSignal(pos, direction)}; same arguments as
     * Offroad's wheel mount uses (the neighbour position and the direction towards the mount).
     */
    public static int getSignal(Level level, BlockPos pos, Direction direction) {
        MethodHandle mh = resolve();
        if (mh != null && !disabledAfterError) {
            try {
                return (int) mh.invokeExact(level, pos, direction);
            } catch (Throwable t) {
                // Never let an optional integration crash physics/rendering.
                disabledAfterError = true;
            }
        }
        return level.getSignal(pos, direction);
    }

    private static MethodHandle resolve() {
        if (resolved) {
            return handle;
        }
        synchronized (DriveByWireCompat.class) {
            if (resolved) {
                return handle;
            }
            try {
                if (ModList.get() != null && ModList.get().isLoaded(MOD_ID)) {
                    Class<?> clazz = Class.forName(COMPAT_CLASS);
                    handle = MethodHandles.publicLookup().findStatic(
                            clazz,
                            COMPAT_METHOD,
                            MethodType.methodType(int.class, Level.class, BlockPos.class, Direction.class));
                }
            } catch (Throwable ignored) {
                handle = null;
            }
            resolved = true;
            return handle;
        }
    }
}
