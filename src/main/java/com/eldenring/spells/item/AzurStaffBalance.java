package com.eldenring.spells.item;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.network.AzurStaffSettingsPayload;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class AzurStaffBalance {
    public static final double DEFAULT_CAST_TIME_REDUCTION = 0.15D;
    public static final double DEFAULT_MANA_COST_MULTIPLIER = 1.20D;
    public static final ResourceLocation CAST_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "azur_cast_speed");

    private static volatile Values values =
            new Values(DEFAULT_CAST_TIME_REDUCTION, DEFAULT_MANA_COST_MULTIPLIER);

    private AzurStaffBalance() {
    }

    public static double manaCostMultiplier() {
        return values.manaCostMultiplier();
    }

    /** 世界配置卸载后恢复本地预览默认值，不再向正在停止的服务器安排工作。 */
    public static void resetDefaults() {
        values = new Values(DEFAULT_CAST_TIME_REDUCTION, DEFAULT_MANA_COST_MULTIPLIER);
    }

    public static boolean isMainhandAzur(Player player) {
        return player != null && player.getMainHandItem().getItem() instanceof AzurGlintstoneStaffItem;
    }

    public static double currentMultiplier(Player player) {
        return isMainhandAzur(player) ? manaCostMultiplier() : 1.0D;
    }

    public static boolean chargesMana(Player player, CastSource source, AbstractSpell spell, MagicData data) {
        return AzurManaCostPolicy.chargesMana(source.consumesMana(),
                data.getPlayerRecasts().hasRecastForSpell(spell.getSpellId()), player.isCreative(),
                ServerConfigs.CREATIVE_MANA_COST.get());
    }

    public static double multiplier(Player player, MagicData data, AbstractSpell spell, boolean activeCast) {
        double started = activeCast && data.isCasting() && spell.getSpellId().equals(data.getCastingSpellId())
                ? ((AzurCastCostData) data).eldenRingSpells$getAzurMultiplier() : 1.0D;
        return AzurManaCostPolicy.effectiveMultiplier(isMainhandAzur(player), manaCostMultiplier(), started);
    }

    public static int manaCost(int original, Player player, CastSource source, AbstractSpell spell,
                               MagicData data, boolean activeCast) {
        return AzurManaCostPolicy.apply(original, multiplier(player, data, spell, activeCast),
                chargesMana(player, source, spell, data));
    }

    public static AttributeModifier castSpeedModifier() {
        return new AttributeModifier(CAST_SPEED_ID, values.castTimeReduction(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @SubscribeEvent
    public static void addConfiguredAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof AzurGlintstoneStaffItem) {
            event.replaceModifier(AttributeRegistry.CAST_TIME_REDUCTION, castSpeedModifier(),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    public static void configure(double castTimeReduction, double manaCostMultiplier) {
        Values updated = new Values(castTimeReduction, manaCostMultiplier);
        if (values.equals(updated)) {
            return;
        }
        values = updated;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Equipment attributes otherwise refresh only when the equipped stack changes.
            server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> {
                syncTo(player);
                if (isMainhandAzur(player)) {
                    AttributeInstance attribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION);
                    if (attribute != null) {
                        attribute.removeModifier(CAST_SPEED_ID);
                        attribute.addTransientModifier(castSpeedModifier());
                    }
                }
            }));
        }
    }

    public static void acceptServerSettings(double castTimeReduction, double manaCostMultiplier) {
        values = new Values(castTimeReduction, manaCostMultiplier);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    private static void syncTo(ServerPlayer player) {
        // Fake players and unnegotiated test connections do not support mod payloads.
        if (player.connection.hasChannel(AzurStaffSettingsPayload.TYPE)) {
            Values current = values;
            PacketDistributor.sendToPlayer(player,
                    new AzurStaffSettingsPayload(current.castTimeReduction(), current.manaCostMultiplier()));
        }
    }

    private record Values(double castTimeReduction, double manaCostMultiplier) {
    }
}
