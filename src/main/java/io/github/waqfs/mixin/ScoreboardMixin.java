package io.github.waqfs.mixin;

import io.github.waqfs.GameDetector;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {
    @Inject(method = "updateScoreboardTeam", at = @At("HEAD"))
    private void updateScoreboardTeam(Team team, CallbackInfo info) {
        GameDetector.detect();
    }

    @Inject(method = "updateExistingObjective", at = @At("HEAD"))
    private void updateExistingObjective(ScoreboardObjective objective, CallbackInfo info) {
        GameDetector.detect();
    }
}
