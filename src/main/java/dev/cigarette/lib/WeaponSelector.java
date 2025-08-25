package dev.cigarette.lib;

import dev.cigarette.agent.ZombiesAgent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // Import ConcurrentHashMap
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeaponSelector {
    // Patterns to extract weapon stats from tooltips
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("Damage: ([0-9.]+) HP");
    private static final Pattern AMMO_PATTERN = Pattern.compile("Ammo: ([0-9]+)");
    private static final Pattern FIRE_RATE_PATTERN = Pattern.compile("Fire Rate: ([0-9.]+)s");
    private static final Pattern RELOAD_PATTERN = Pattern.compile("Reload: ([0-9.]+)s");

    // Use ConcurrentHashMap to prevent ConcurrentModificationException
    private static final Map<Integer, Long> weaponCooldown = new ConcurrentHashMap<>();

    public static class WeaponStats {
        public final ItemStack weapon;
        public final int slotIndex;
        public final double damage;
        public final int ammo;
        public final double fireRate; // Time between shots in seconds
        public final double reloadTime; // Reload time in seconds
        public final int count;
        public final double DPS;
        public final double burstDPS; // DPS without considering reloads

        public WeaponStats(ItemStack weapon, int slotIndex, double damage, int ammo,
                           double fireRate, double reloadTime, int count) {
            this.weapon = weapon;
            this.slotIndex = slotIndex;
            this.damage = damage;
            this.ammo = ammo;
            this.fireRate = fireRate;
            this.reloadTime = reloadTime;
            this.count = count;


            this.burstDPS = damage / fireRate;

            double timeToEmptyClip = ammo * fireRate;
            double totalCycleTime = timeToEmptyClip + reloadTime;
            double totalDamagePerCycle = damage * ammo;
            this.DPS = totalDamagePerCycle / totalCycleTime;

        }

        // TODO: This needs to be more reliable, item damage and count are okay
        //  but we need to figure out how to force the client to refresh those slots from the server?
        public boolean isValidWeapon() {
            return damage > 0 && ammo > 0 && fireRate > 0 && count > 0;
        }
    }

    /**
     * Analyzes all weapons in the player's hotbar and returns their stats
     */
    public static List<WeaponStats> analyzeWeapons(ClientPlayerEntity player) {
        List<WeaponStats> weapons = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || !ZombiesAgent.isGun(stack)) {
                continue;
            }

            WeaponStats stats = parseWeaponStats(stack, i);
            if (stats != null && stats.isValidWeapon()) {
                weapons.add(stats);
            }
        }

        return weapons;
    }

    /**
     * Parses weapon statistics from item tooltip
     */
    @Nullable
    private static WeaponStats parseWeaponStats(ItemStack weapon, int slotIndex) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            List<Text> tooltip = weapon.getTooltip(Item.TooltipContext.DEFAULT, client.player, client.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC);

            double damage = -1;
            int ammo = -1;
            double fireRate = -1;
            double reloadTime = -1;

            for (Text line : tooltip) {
                String text = TextL.toColorCodedString(line).replaceAll("§[a-zA-Z0-9]", "");

                Matcher damageMatcher = DAMAGE_PATTERN.matcher(text);
                if (damageMatcher.find()) {
                    damage = Double.parseDouble(damageMatcher.group(1));
                }

                Matcher ammoMatcher = AMMO_PATTERN.matcher(text);
                if (ammoMatcher.find()) {
                    ammo = Integer.parseInt(ammoMatcher.group(1));
                }

                Matcher fireRateMatcher = FIRE_RATE_PATTERN.matcher(text);
                if (fireRateMatcher.find()) {
                    fireRate = Double.parseDouble(fireRateMatcher.group(1));
                }

                Matcher reloadMatcher = RELOAD_PATTERN.matcher(text);
                if (reloadMatcher.find()) {
                    reloadTime = Double.parseDouble(reloadMatcher.group(1));
                }
            }

            if (damage > 0 && ammo > 0 && fireRate > 0) {
                return new WeaponStats(weapon, slotIndex, damage, ammo, fireRate, reloadTime, weapon.getCount());
            }

        } catch (Exception e) {
            // Failed to parse weapon stats
            return null;
        }

        return null;
    }

    /**
     * Selects the best weapon for auto-shooting based on situation
     */
    @Nullable
    public static WeaponStats selectBestWeapon(ClientPlayerEntity player, @Nullable ZombiesAgent.ZombieTarget target) {
        List<WeaponStats> weapons = analyzeWeapons(player);
        if (weapons.isEmpty()) {
            return null;
        }

        // If no target, we can assume a default distance and no headshot possibility for scoring.
        double distance = (target != null) ? target.getDistance() : 10.0; // Default distance
        boolean canHeadshot = (target != null) && target.canHeadshot();

        WeaponStats bestWeapon = null;
        double bestScore = -1;

        for (WeaponStats weapon : weapons) {
            double score = calculateWeaponScore(weapon, distance, canHeadshot, player);
            if (score > bestScore) {
                bestScore = score;
                bestWeapon = weapon;
            }
        }
        return bestWeapon;
    }

    /**
     * Calculates weapon score based on situation
     */
    private static double calculateWeaponScore(WeaponStats weapon, double distance, boolean canHeadshot, ClientPlayerEntity player) {
        // Disqualify weapon if it's on cooldown or needs reloading (isDamaged is used for empty clip).
        if (weaponCooldown.containsKey(weapon.slotIndex)) return -1.0;
        if (player.getInventory().getStack(weapon.slotIndex).isDamaged()) return -1.0;

        double score;
        int currentSlot = player.getInventory().getSelectedSlot();

        // If this is the currently held weapon, we can use the XP bar for a precise ammo count.
        if (weapon.slotIndex == currentSlot) {
            int currentAmmo = player.experienceLevel;
            if (currentAmmo <= 0) {
                // This should be caught by isDamaged(), but it's a good fallback.
                return -1.0;
            }
            // Recalculate DPS based on the remaining ammo in the clip.
            double timeToEmptyClip = currentAmmo * weapon.fireRate;
            double totalDamagePerCycle = weapon.damage * currentAmmo;
            score = totalDamagePerCycle / (timeToEmptyClip + weapon.reloadTime);
        } else {
            // For weapons not currently held, we assume they have a full clip.
            // The isDamaged() check above handles the empty case.
            score = weapon.DPS;
        }

        // Headshot bonus for high-damage weapons.
        if (canHeadshot && weapon.damage > 15) {
            score *= 1.2;
        }

        return score;
    }

    /**
     * Switches to the best weapon if it's not already selected
     */
    public static boolean switchToBestWeapon(ClientPlayerEntity player, @Nullable ZombiesAgent.ZombieTarget target) {
        WeaponStats bestWeapon = selectBestWeapon(player, target);

        if (bestWeapon == null) {
            return false;
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        if (currentSlot == bestWeapon.slotIndex) {
            return true;
        }

        player.getInventory().setSelectedSlot(bestWeapon.slotIndex);
        return true;
    }

    /**
     * Gets stats for currently held weapon
     */
    @Nullable
    public static WeaponStats getCurrentWeaponStats(ClientPlayerEntity player) {
        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack currentWeapon = player.getInventory().getStack(currentSlot);

        if (currentWeapon.isEmpty() || !ZombiesAgent.isGun(currentWeapon)) {
            return null;
        }

        return parseWeaponStats(currentWeapon, currentSlot);
    }

    public static void addCooldown(int slotIndex) {
        WeaponStats weaponStats = parseWeaponStats(MinecraftClient.getInstance().player.getInventory().getStack(slotIndex), slotIndex);

        // if weaponStats is null, return
        if (weaponStats == null) return;

        long firerateMs = (long) (weaponStats.fireRate * 1000);
        long expirationTime = System.currentTimeMillis() + firerateMs;
        weaponCooldown.put(slotIndex, expirationTime);

        // remove expired cooldowns - this is now safe
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : weaponCooldown.entrySet()) {
            if (currentTime > entry.getValue()) { // Corrected logic: check if current time is past the expiration time
                weaponCooldown.remove(entry.getKey());
            }
        }
    }
}