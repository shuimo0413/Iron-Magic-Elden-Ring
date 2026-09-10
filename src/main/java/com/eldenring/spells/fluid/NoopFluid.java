package com.eldenring.spells.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 炼药锅用的「只存在于罐内」流体：无方块形态、无桶。
 * <p>
 * 对照铁魔法 {@code io.redspace.ironsspellbooks.fluids.NoopFluid}，本模组自持一份以免依赖非 api 包路径变动。
 */
public class NoopFluid extends BaseFlowingFluid {
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
