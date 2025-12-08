package dev.cigarette.helper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;

/**
 * Helper class for retrieving data from the scoreboard.
 */
public class ScoreboardHelper {
    private static @Nullable ScoreboardObjective getHeader(MinecraftClient client) {
        if (client.world == null) return null;
        return client.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private static String unformat(String text) {
        return text.replaceAll("§.", "");
    }

    /**
     * {@return the unformatted header of the scoreboard}
     *
     * @param client The Minecraft client instance.
     */
    public static String getUnformattedHeader(MinecraftClient client) {
        ScoreboardObjective header = ScoreboardHelper.getHeader(client);
        if (header == null) return "";
        return header.getDisplayName().getString();
    }

    /**
     * {@return an array of unformatted rows from the scoreboard}
     *
     * @param client The Minecraft client instance.
     */
    public static String[] getUnformattedRows(MinecraftClient client) {
        ScoreboardObjective header = ScoreboardHelper.getHeader(client);
        if (header == null || client.world == null || client.player == null) return new String[]{};

        Collection<ScoreboardEntry> entries = client.world.getScoreboard().getScoreboardEntries(header);
        HashMap<Integer, String> rows = new HashMap<>();
        for (ScoreboardEntry entry : entries) {
            Team team = client.world.getScoreboard().getScoreHolderTeam(entry.owner());
            if (team == null) continue;
            MutableText text = team.decorateName(entry.name());
            String displayText = text.getString();
            rows.put(entry.value(), ScoreboardHelper.unformat(displayText));
        }
        return rows.values().toArray(new String[0]);
    }
}
