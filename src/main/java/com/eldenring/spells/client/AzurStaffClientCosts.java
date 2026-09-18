package com.eldenring.spells.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;

/**
 * 旧调用兼容入口；客户端蓝耗预览由 {@link SpellManaCostClientCosts} 统一处理。
 */
@Deprecated
public final class AzurStaffClientCosts {
    private AzurStaffClientCosts() {
    }

    public static int manaCost(int original, AbstractSpell spell, CastSource source) {
        return SpellManaCostClientCosts.manaCost(original, spell, source);
    }
}
