package dev.cigarette.helper;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Optional;

/**
 * Helper class for handling annoying {@link Text} stuff.
 */
public class TextHelper {
    private static String styleToCodes(Style style) {
        StringBuilder builder = new StringBuilder();
        TextColor color = style.getColor();
        builder.append("§r");
        if (color != null) {
            builder.append('§');
            switch (color.toString()) {
                case "black" -> builder.append('0');
                case "dark_blue" -> builder.append('1');
                case "dark_green" -> builder.append('2');
                case "dark_aqua" -> builder.append('3');
                case "dark_red" -> builder.append('4');
                case "dark_purple" -> builder.append('5');
                case "gold" -> builder.append('6');
                case "gray" -> builder.append('7');
                case "dark_gray" -> builder.append('8');
                case "blue" -> builder.append('9');
                case "green" -> builder.append('a');
                case "aqua" -> builder.append('b');
                case "red" -> builder.append('c');
                case "light_purple" -> builder.append('d');
                case "yellow" -> builder.append('e');
                case "white" -> builder.append('f');
                default -> builder.append(color);
            }
        }
        if (style.isBold()) builder.append("§l");
        if (style.isItalic()) builder.append("§o");
        if (style.isStrikethrough()) builder.append("§m");
        if (style.isUnderlined()) builder.append("§n");
        if (style.isObfuscated()) builder.append("§k");
        return builder.toString();
    }

    /**
     * Converts a {@link Text} object to a 1.8.9-style string.
     *
     * @param text The text to convert.
     * @return The color-coded string. Note that each block of styles will start with a ({@code §r}) including the start of the string.
     */
    public static String toColorCodedString(Text text) {
        StringBuilder builder = new StringBuilder();
        text.visit((style, text2) -> {
            builder.append(TextHelper.styleToCodes(style));
            builder.append(text2);
            return Optional.empty();
        }, text.getStyle());
        return builder.toString();
    }
}
