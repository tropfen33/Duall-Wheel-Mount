package com.Tropfen_33.Duall_Wheel_Mount.registry;

import com.Tropfen_33.Duall_Wheel_Mount.DuallWheelMount;
import com.Tropfen_33.Duall_Wheel_Mount.content.DualWheelMountBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DuallWheelMount.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DualWheelMountBlockEntity>>
            DUAL_WHEEL_MOUNT = BLOCK_ENTITIES.register("dual_wheel_mount", () ->
                    BlockEntityType.Builder.of(
                            DualWheelMountBlockEntity::new,
                            ModBlocks.DUAL_WHEEL_MOUNT.get()
                    ).build(null));

    private ModBlockEntities() {}

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
