package io.github.waqfs.lib;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;

public class ScoreboardL {
    private static @Nullable ScoreboardObjective getHeader(MinecraftClient client) {
        if (client.world == null) return null;
        return client.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private static String unformat(String text) {
        return text.replaceAll("§.", "");
    }

    public static String getUnformattedHeader(MinecraftClient client) {
        ScoreboardObjective header = ScoreboardL.getHeader(client);
        if (header == null) return "";
        return header.getDisplayName().getString();
    }

    public static String[] getUnformattedRows(MinecraftClient client) {
        ScoreboardObjective header = ScoreboardL.getHeader(client);
        if (header == null || client.world == null || client.player == null) return new String[]{};

        Collection<ScoreboardEntry> entries = client.world.getScoreboard().getScoreboardEntries(header);
        HashMap<Integer, String> rows = new HashMap<>();
        for (ScoreboardEntry entry : entries) {
            Team team = client.world.getScoreboard().getScoreHolderTeam(entry.owner());
            if (team == null) continue;
            MutableText text = team.decorateName(entry.name());
            String displayText = text.getString();
            rows.put(entry.value(), ScoreboardL.unformat(displayText));
        }
        return rows.values().toArray(new String[0]);
    }
}
