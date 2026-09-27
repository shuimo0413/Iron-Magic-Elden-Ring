package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

/**
 * 铁魔法 ScrollModel 用的独立卷轴外观。由 {@code EldenRingSpellsClient} 转发。
 * <p>
 * 1.20.1 Forge 只是把这批 {@link ResourceLocation} 直接塞进 {@code ModelBakery.topLevelModels}
 * 并把 「namespace:path」当烘焙后的键（Forge 的 {@code ModelBakery} 补丁里没有 standalone 变体概念），
 * 而 {@code NBTOverrideItemModel} / {@code ScrollModelMixin} 用同一条普通 ResourceLocation
 * （{@code iss_elden_ring:item/<spell>_scroll}）去 {@code ModelManager#getModel} 取模型，
 * 所以这里必须注册普通 ResourceLocation，不能用 1.21 才有的 {@code ModelResourceLocation.standalone}。
 */
public final class ClientItemModels {

    private ClientItemModels() {
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        for (var spellHolder : ModSpells.SPELLS.getEntries()) {
            event.register(new ResourceLocation(
                    EldenRingSpellsMod.MOD_ID,
                    "item/" + spellHolder.getId().getPath() + "_scroll"
            ));
        }
    }
}
