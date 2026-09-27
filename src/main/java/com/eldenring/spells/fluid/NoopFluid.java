package com.eldenring.spells.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * 炼药锅用的「只存在于罐内」流体：无方块形态、无桶。
 * <p>
 * 对照铁魔法 1.20.1 {@code io.redspace.ironsspellbooks.fluids.NoopFluid}：
 * Forge 的 {@link ForgeFlowingFluid} 已实现 {@code getFlowing/getSource/getBucket/createLegacyBlock}，
 * 这里只需覆写抽象来的 {@code isSource/getAmount}，并保留 getBucket/createLegacyBlock 的无害空实现。
 */
public class NoopFluid extends ForgeFlowingFluid {
    public NoopFluid(Properties properties) {
        super(properties);
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
    }

    @Override
    public int getAmount(FluidState state) {
        return 0;
    }
}
