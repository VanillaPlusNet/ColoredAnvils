package me.flamingkatana.mc.plugins.coloredanvils.item;

import me.flamingkatana.mc.plugins.coloredanvils.ColoredAnvils;
import me.flamingkatana.mc.plugins.coloredanvils.constant.AnvilConstants;
import me.flamingkatana.mc.plugins.coloredanvils.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.flamingkatana.mc.plugins.coloredanvils.util.Util.getServerVersionAsInt;
import static me.flamingkatana.mc.plugins.coloredanvils.util.Util.stripColorsAndFormatting;

public class ItemColorTranslator {

    private final boolean useFullColors;

    public ItemColorTranslator() {
        useFullColors = canUseFullColors();
    }

    public void updateColorTranslationForAnvilOutput(AnvilInventory anvilInventory, HumanEntity humanEntity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack inputItem = anvilInventory.getItem(AnvilConstants.FIRST_INPUT_SLOT);
                if (inputItem == null) {
                    return;
                }
                ItemStack outputItem = anvilInventory.getItem(AnvilConstants.OUTPUT_SLOT);
                if (outputItem == null) {
                    return;
                }
                translateOutputItemNameColorBasedOnInputItem(outputItem, inputItem, humanEntity);
            }
        }.runTaskLater(ColoredAnvils.getPlugin(), 0L);
    }

    public ItemStack translateOutputItemNameColorBasedOnInputItem(ItemStack outputItem, ItemStack inputItem, HumanEntity humanEntity) {
        ItemMeta outputItemMeta = outputItem.getItemMeta();
        if (outputItemMeta == null || !outputItemMeta.hasDisplayName()) {
            return outputItem;
        }
        String outputName = outputItemMeta.getDisplayName();
        ItemMeta inputItemMeta = inputItem.getItemMeta();
        if (inputItemMeta == null || !inputItemMeta.hasDisplayName()) {
            return translateNameColorWithPermissions(outputItem, humanEntity);
        }
        String inputName = inputItemMeta.getDisplayName();
        if (doesOutputNameMatchInputName(outputName, inputName)) {
            outputItemMeta.setDisplayName(inputName);
            outputItem.setItemMeta(outputItemMeta);
            if (!ColoredAnvils.permissionValidator().arePermissionsEnabledForNoChange()) {
                return outputItem;
            }
        }
        return translateNameColorWithPermissions(outputItem, humanEntity);
    }

    public ItemStack translateNameColorWithPermissions(ItemStack itemStack, HumanEntity humanEntity) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }
        String untranslatedName = itemMeta.getDisplayName();
        String translatedName = translateColorCodes(untranslatedName);
        String permissionEnforcedTranslatedName = ColoredAnvils.permissionValidator().enforcePermissionsOnName(humanEntity, translatedName);
        itemMeta.setDisplayName(permissionEnforcedTranslatedName);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private String translateColorCodes(String input) {
        if (!useFullColors) {
            return org.bukkit.ChatColor.translateAlternateColorCodes(AnvilConstants.UNTRANSLATED_COLOR_CHAR, input);
        }
        // This is a modified version of Kikisito's hex color translation implementation. Full credits to them.
        Pattern pattern = Pattern.compile("&#([0-9a-fA-F]){6}|&#([0-9a-fA-F]){3}");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group();
            if (hex.length() == 5) {
                // Convert 3 character hex to 6 characters
                hex = hex.substring(0, 2) + Util.doubleCharacters(hex.substring(2));
            }
            matcher.appendReplacement(sb, ChatColor.of(hex.substring(1)).toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes(AnvilConstants.UNTRANSLATED_COLOR_CHAR, sb.toString());
    }

    private boolean canUseFullColors() {
        Integer serverVersion = getServerVersionAsInt();
        return serverVersion == null || serverVersion >= 16;
    }

    private boolean doesOutputNameMatchInputName(String outputName, String inputName) {
        return stripColorsAndFormatting(outputName).equals(stripColorsAndFormatting(inputName));
    }

}
