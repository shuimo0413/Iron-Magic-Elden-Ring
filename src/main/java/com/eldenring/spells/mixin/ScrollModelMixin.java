package com.eldenring.spells.mixin;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.render.ScrollModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 铁魔法通用卷轴按学派切模型（{@code item/scroll_<school>}）。
 * 辉石法术改用每道咒自己的 {@code item/<spell>_scroll}，创造栏里的通用卷轴才能显示 Wiki 图标。
 */
// remap = false：目标是铁魔法自己的类与方法，成品 jar 里不混淆，refmap 里查不到映射
@Mixin(value = ScrollModel.class, remap = false)
public abstract class ScrollModelMixin {

    /**
     * 注入铁魔法 1.20.1 的 {@code ScrollModel#getModelFromTag}（包私有）。
     * <p>
     * 1.21.1 分支注入的是 {@code getModelFromStack(ItemStack)}；1.20.1 的方法名与形参都不同，
     * 是「按 NBT 抠模型」而非「按 item stack 抠」，多一个 {@link CompoundTag}（物品 NBT，可能为 null）。
     * 目标名写错时 mixin 在 {@code required: true} + {@code defaultRequire: 1} 下会直接让游戏启动崩溃，
     * 且编译期完全看不出来，所以这里以 1.20.1 参考 jar 的 javap 结果为准。
     */
    @Inject(method = "getModelFromTag", at = @At("HEAD"), cancellable = true)
    private void eldenRingSpells$usePerSpellScrollModel(
            ItemStack itemStack,
            CompoundTag tag,
            CallbackInfoReturnable<Optional<ResourceLocation>> cir
    ) {
        if (!ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }
        ResourceLocation spellId = ISpellContainer.get(itemStack).getSpellAtIndex(0).getSpell().getSpellResource();
        if (!EldenRingSpellsMod.MOD_ID.equals(spellId.getNamespace())) {
            return;
        }
        cir.setReturnValue(Optional.of(
                new ResourceLocation(
                        EldenRingSpellsMod.MOD_ID,
                        "item/" + spellId.getPath() + "_scroll"
                )
        ));
    }
}
