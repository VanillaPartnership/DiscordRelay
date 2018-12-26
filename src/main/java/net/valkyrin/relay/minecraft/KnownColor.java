/*
 * discord-relay: A Minecraft <=> Discord relay, but for Vanilla servers.
 * Copyright (C) 2018 VanillaPartnership
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.valkyrin.relay.minecraft;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.text.format.TextColor;

import java.awt.*;

@RequiredArgsConstructor
public enum KnownColor {
    BLACK(Color.BLACK),
    DARK_BLUE(Color.decode("#0000AA")),
    DARK_GREEN(Color.decode("#00AA00")),
    DARK_AQUA(Color.decode("#00AAAA")),
    DARK_RED(Color.decode("#AA0000")),
    DARK_PURPLE(Color.decode("#AA00AA")),
    GOLD(Color.decode("#AAAA00")),
    GRAY(Color.decode("#AAAAAA")),
    DARK_GRAY(Color.decode("#555555")),
    BLUE(Color.decode("#5555FF")),
    GREEN(Color.decode("#55FF55")),
    AQUA(Color.decode("#55FFFF")),
    RED(Color.decode("#FF5555")),
    LIGHT_PURPLE(Color.decode("#FF55FF")),
    YELLOW(Color.decode("#FFFF55")),
    WHITE(Color.WHITE);
    private final Color color;

    public static KnownColor getClosest(final @NonNull Color color) {
        long diff = Long.MAX_VALUE;
        KnownColor closest = null;
        for (KnownColor knownColor : KnownColor.values()) {
            long currDiff = diff(knownColor, color);
            if (currDiff < diff) {
                diff = currDiff;
                closest = knownColor;
            }
        }
        return closest;
    }

    public TextColor getTextColor() {
        return TextColor.valueOf(this.name());
    }

    private static long diff(KnownColor knownColor, Color color) {
        long diffBlue = knownColor.color.getBlue() - color.getBlue();
        long diffGreen = knownColor.color.getGreen() - color.getGreen();
        long diffRed = knownColor.color.getRed() - color.getRed();
        return diffBlue * diffBlue + diffGreen * diffGreen + diffRed * diffRed;
    }
}
