package net.captersers.stattweaks.mixin;

import net.captersers.stattweaks.manager.STBalanceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void stattweaks$cancelAttributeTooltips(Consumer<Component> consumer, Player player, CallbackInfo ci) {
        if ("base".equals(STBalanceManager.getTooltipMode())) {
            ci.cancel();
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void stattweaks$modifyTooltip(Item.TooltipContext context, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        if (!"base".equals(STBalanceManager.getTooltipMode())) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        
        if (modifiers == null || modifiers.modifiers().isEmpty()) return;

        List<Component> tooltip = cir.getReturnValue();
        
        // Remove standard attribute tooltips if they were somehow added
        // (In base mode we want full control)
        // Note: This is a bit risky but usually the attribute lines are at the end 
        // before the "Hidden" or "NBT" tags.
        
        Set<Holder<Attribute>> handled = new HashSet<>();

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            Holder<Attribute> attr = entry.attribute();
            if (handled.contains(attr)) continue;
            
            if (!stattweaks$isDisplayableAttribute(attr)) continue;

            double baseValue = 0;
            if (player != null) {
                baseValue = player.getAttributeBaseValue(attr);
            } else {
                if (attr.is(Attributes.ATTACK_DAMAGE)) baseValue = 1.0;
                else if (attr.is(Attributes.ATTACK_SPEED)) baseValue = 4.0;
            }

            double amount = 0;
            for (ItemAttributeModifiers.Entry e2 : modifiers.modifiers()) {
                if (e2.attribute().equals(attr)) {
                    if (e2.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                        amount += e2.modifier().amount();
                    } else if (e2.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                        amount += (baseValue * e2.modifier().amount());
                    }
                }
            }

            double total = baseValue + amount;
            handled.add(attr);

            String translationKey = attr.value().getDescriptionId();
            
            // Use a simple format to match the user request "10 Damage"
            // We use DARK_GREEN for the value to make it stand out as a "tweaked" stat
            MutableComponent line = Component.literal(" ")
                    .append(Component.literal(stattweaks$formatValue(total)).withStyle(ChatFormatting.DARK_GREEN))
                    .append(" ")
                    .append(Component.translatable(translationKey)).withStyle(ChatFormatting.GRAY);
            
            tooltip.add(line);
        }
    }

    private boolean stattweaks$isDisplayableAttribute(Holder<Attribute> attr) {
        return attr.is(Attributes.ATTACK_DAMAGE) || 
               attr.is(Attributes.ATTACK_SPEED) || 
               attr.is(Attributes.ARMOR) || 
               attr.is(Attributes.ARMOR_TOUGHNESS) || 
               attr.is(Attributes.KNOCKBACK_RESISTANCE) ||
               attr.is(Attributes.MAX_HEALTH);
    }

    private String stattweaks$formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }
}