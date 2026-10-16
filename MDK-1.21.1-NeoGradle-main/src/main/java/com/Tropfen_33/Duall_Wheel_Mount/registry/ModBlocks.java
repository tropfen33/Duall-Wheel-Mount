package com.Tropfen_33.Duall_Wheel_Mount.registry;

import com.Tropfen_33.Duall_Wheel_Mount.DuallWheelMount;
import com.Tropfen_33.Duall_Wheel_Mount.content.DualWheelMountBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(DuallWheelMount.MOD_ID);

    public static final DeferredHolder<Block, DualWheelMountBlock> DUAL_WHEEL_MOUNT =
            BLOCKS.register("dual_wheel_mount", () ->
                    new DualWheelMountBlock(
                            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
                    ));

    private ModBlocks() {}

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
