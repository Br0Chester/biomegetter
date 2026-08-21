package com.idk.biomegetter.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Кодек для цвета в человекочитаемом hex-формате "#AARRGGBB" (или "#RRGGBB" — тогда альфа
 * считается непрозрачной FF). Используется вместо знаковых int-значений вида -15886 в json.
 */
public final class HexColorCodec {

    public static final Codec<Integer> CODEC = Codec.STRING.comapFlatMap(
            HexColorCodec::parse,
            HexColorCodec::toHexString
    );

    private static DataResult<Integer> parse(String raw) {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.length() != 6 && hex.length() != 8) {
            return DataResult.error(() -> "Color must be #RRGGBB or #AARRGGBB, got: " + raw);
        }
        try {
            long parsed = Long.parseLong(hex, 16);
            int argb = hex.length() == 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
            return DataResult.success(argb);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid hex color: " + raw);
        }
    }

    private static String toHexString(int argb) {
        return String.format("#%08X", argb);
    }

    private HexColorCodec() {
    }
}