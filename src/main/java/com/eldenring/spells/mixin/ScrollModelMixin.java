package com.eldenring.spells.mixin;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.render.ScrollModel;
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
@Mixin(ScrollModel.class)
public abstract class ScrollModelMixin {

    @Inject(method = "getModelFromStack", at = @At("HEAD"), cancellable = true)
    private void eldenRingSpells$usePerSpellScrollModel(
            ItemStack itemStack,
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
                ResourceLocation.fromNamespaceAndPath(
                        EldenRingSpellsMod.MOD_ID,
                        "item/" + spellId.getPath() + "_scroll"
                )
        ));
    }
}
