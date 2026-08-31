package com.eldenring.spells.client;

import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 卡利亚迅剑是否该出现在手上：本地玩家跟 {@link CarianSlicerClientHold} 的斩击片长，
 * 别人只看铁魔法同步的施法 id。剑本身是假物品，给 {@code ItemRenderer} 挤成立体薄片。
 */
public final class CarianSlicerHand {

    /** 延迟到第一次渲染再 new，避免 mixin 加载时物品还没注册。 */
    private static ItemStack swordStack;

    private CarianSlicerHand() {
    }

    /**
     * 挥砍过程中（含松手后收完当前刀）为 true。
     */
    public static boolean shouldShowSword(LivingEntity livingEntity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (livingEntity instanceof LocalPlayer && livingEntity == minecraft.player) {
            return CarianSlicerClientHold.isSlashPlaybackActive();
        }
        SyncedSpellData syncedSpellData = ClientMagicData.getSyncedSpellData(livingEntity);
        if (syncedSpellData == null || !syncedSpellData.isCasting()) {
            return false;
        }
        String castingSpellId = syncedSpellData.getCastingSpellId();
        return castingSpellId != null
                && castingSpellId.equals(ModSpells.CARIAN_SLICER.get().getSpellId());
    }

    /**
     * 手里那把像素剑。缓存同一份 stack，避免每帧 new。
     */
    public static ItemStack swordStack() {
        if (swordStack == null) {
            swordStack = new ItemStack(ModItems.CARIAN_SLICER_SWORD.get());
        }
        return swordStack;
    }
}
