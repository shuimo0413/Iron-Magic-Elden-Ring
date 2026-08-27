package com.eldenring.spells.client;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModSpells;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 铁魔法 ScrollModel 用的 standalone 卷轴外观。由 {@code EldenRingSpellsClient} 转发。
 */
public final class ClientItemModels {

    private ClientItemModels() {
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        for (var spellHolder : ModSpells.SPELLS.getEntries()) {
            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            EldenRingSpellsMod.MOD_ID,
                            "item/" + spellHolder.getId().getPath() + "_scroll"
                    )
            ));
        }
    }
}
