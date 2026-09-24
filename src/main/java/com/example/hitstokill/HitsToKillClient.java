package com.example.hitstokill;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class HitsToKillClient implements ClientModInitializer {

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(HitsToKillClient::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player me = mc.player;
        if (me == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        if (!(mc.crosshairPickEntity instanceof LivingEntity target) || !target.isAlive()) return;

        HolderLookup.RegistryLookup<Enchantment> ench =
                mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // --- attacker damage (full charge) ---
        double base = me.getAttributeValue(Attributes.ATTACK_DAMAGE); // weapon + Strength/Weakness
        int sharp = EnchantmentHelper.getItemEnchantmentLevel(
                ench.getOrThrow(Enchantments.SHARPNESS), me.getMainHandItem());
        double sharpBonus = sharp > 0 ? 0.5 * sharp + 0.5 : 0.0;

        double normal = afterDefense(base + sharpBonus, target, ench);
        double crit = afterDefense(base * 1.5 + sharpBonus, target, ench);

        double hp = target.getHealth() + target.getAbsorptionAmount();
        int hitsNormal = normal > 0.01 ? (int) Math.ceil(hp / normal) : 999;
        int hitsCrit = crit > 0.01 ? (int) Math.ceil(hp / crit) : 999;

        String text = hitsNormal + " (крит " + hitsCrit + ")";
        int color = hitsNormal <= 1 ? 0xFFFF5555 : hitsNormal <= 3 ? 0xFFFFAA00 : 0xFFFFFFFF;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        g.drawCenteredString(mc.font, text, cx, cy + 12, color);
    }

    /** Vanilla order: armor -> resistance -> protection enchants. */
    private static double afterDefense(double dmg, LivingEntity t, HolderLookup.RegistryLookup<Enchantment> ench) {
        double armor = t.getAttributeValue(Attributes.ARMOR);
        double tough = t.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double reduction = Math.min(20.0, Math.max(armor / 5.0, armor - dmg / (2.0 + tough / 4.0)));
        dmg *= 1.0 - reduction / 25.0;

        MobEffectInstance res = t.getEffect(MobEffects.RESISTANCE);
        if (res != null) {
            dmg *= Math.max(0.0, 1.0 - 0.2 * (res.getAmplifier() + 1));
        }

        Holder<Enchantment> prot = ench.getOrThrow(Enchantments.PROTECTION);
        int epf = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = t.getItemBySlot(slot);
            if (!s.isEmpty()) epf += EnchantmentHelper.getItemEnchantmentLevel(prot, s);
        }
        dmg *= 1.0 - Math.min(20, epf) / 25.0;
        return dmg;
    }
}
