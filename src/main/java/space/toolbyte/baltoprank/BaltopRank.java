package space.toolbyte.baltoprank;

import com.earth2me.essentials.Essentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.*;

public class BaltopRank extends JavaPlugin {

    private LuckPerms luckPerms;
    private Essentials essentials;
    private final String RANK_SUFFIX = "&e🔪&c&k/&k&#36454F";
    private UUID currentTopPlayer = null;

    @Override
    public void onEnable() {
        // Initialize LuckPerms
        this.luckPerms = LuckPermsProvider.get();

        // Initialize Essentials
        this.essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        if (essentials == null) {
            getLogger().severe("Essentials not found! Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("BaltopRank enabled!");

        // Register command
        if (getCommand("baltoprank") != null) {
            getCommand("baltoprank").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("baltoprank.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                sender.sendMessage("§aManually checking Baltop...");
                Bukkit.getScheduler().runTaskAsynchronously(this, this::checkBaltop);
                return true;
            });
        }

        // Initial check
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::checkBaltop, 100L); // Delay a bit to ensure all
                                                                                         // plugins are ready

        // Schedule check every 6 hours (6 hours * 60 minutes * 60 seconds * 20 ticks)
        long intervalTicks = 6L * 60 * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::checkBaltop, intervalTicks, intervalTicks);
    }

    private void checkBaltop() {
        getLogger().info("Checking Baltop #1 player...");

        try {
            UUID topPlayer = getTopBalancePlayer();

            if (topPlayer == null) {
                getLogger().warning("Could not determine top player.");
                return;
            }

            if (currentTopPlayer == null || !currentTopPlayer.equals(topPlayer)) {
                String topName = Bukkit.getOfflinePlayer(topPlayer).getName();
                getLogger().info("New Top #1 detected: " + topName);
                updateSuffix(currentTopPlayer, topPlayer);
                currentTopPlayer = topPlayer;

                // Broadcast the change as requested ("ping every 6 hours")
                Component message = LegacyComponentSerializer.legacySection()
                        .deserialize(
                                "§e§lBaltop Update §8» §fNew Top #1: §e" + topName + " §f- They have received the ")
                        .append(LegacyComponentSerializer.legacyAmpersand().deserialize(RANK_SUFFIX))
                        .append(LegacyComponentSerializer.legacySection().deserialize(" §fsuffix!"));
                Bukkit.broadcast(message);
            } else {
                getLogger().info("Top #1 player remains unchanged.");
            }
        } catch (Exception e) {
            getLogger().severe("Error checking baltop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private UUID getTopBalancePlayer() {
        UUID top = null;
        BigDecimal maxBalance = BigDecimal.valueOf(-1);

        // Essentials maintains its own list of users
        Set<UUID> allUsers = essentials.getUserMap().getAllUniqueUsers();

        for (UUID uuid : allUsers) {
            try {
                // Essentials money is stored in user data
                BigDecimal balance = essentials.getUser(uuid).getMoney();
                if (balance != null && (top == null || balance.compareTo(maxBalance) > 0)) {
                    maxBalance = balance;
                    top = uuid;
                }
            } catch (Exception ignored) {
            }
        }

        return top;
    }

    private void updateSuffix(UUID oldPlayerId, UUID newPlayerId) {
        // Remove from old
        if (oldPlayerId != null) {
            removeSuffix(oldPlayerId);
        }

        // Add to new
        if (newPlayerId != null) {
            addSuffix(newPlayerId);
        }
    }

    private void removeSuffix(UUID uuid) {
        luckPerms.getUserManager().modifyUser(uuid, user -> {
            user.data().clear(node -> {
                if (!(node instanceof SuffixNode))
                    return false;
                return ((SuffixNode) node).getMetaValue().equals(RANK_SUFFIX);
            });
            getLogger().info("Removed suffix from " + uuid);
        });
    }

    private void addSuffix(UUID uuid) {
        luckPerms.getUserManager().modifyUser(uuid, user -> {
            SuffixNode node = SuffixNode.builder(RANK_SUFFIX, 100).build();
            user.data().add(node);
            getLogger().info("Added suffix to " + uuid);
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("BaltopRank disabled!");
    }
}
