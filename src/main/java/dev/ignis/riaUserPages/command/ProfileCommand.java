package dev.ignis.riaUserPages.command;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.localization.LocalizationManager;
import dev.ignis.riaUserPages.model.Comment;
import dev.ignis.riaUserPages.model.PendingAction;
import dev.ignis.riaUserPages.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Main command executor for /ria-player-profile
 */
public class ProfileCommand implements TabExecutor {

    private final RiaUserPages plugin;
    private final LocalizationManager lang;

    public ProfileCommand(RiaUserPages plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLocalizationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Default: open own profile
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile <view|setbio|comment|admin>"));
                return true;
            }
            if (!player.hasPermission("ria.profile.use")) {
                player.sendMessage(lang.getPrefixed("no-permission"));
                return true;
            }
            plugin.getProfileGUI().openProfile(player, player.getUniqueId(), 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "view":
                return handleView(sender, args);
            case "setbio":
                return handleSetBio(sender, args);
            case "comment":
                return handleComment(sender, args);
            case "admin":
                return handleAdmin(sender, args);
            case "confirm":
                return handleConfirm(sender);
            case "cancel":
                return handleCancel(sender);
            default:
                sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile <view|setbio|comment|admin>"));
                return true;
        }
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile view <player>"));
            return true;
        }

        if (!player.hasPermission("ria.profile.use")) {
            player.sendMessage(lang.getPrefixed("no-permission"));
            return true;
        }

        UUID targetUuid;
        if (args.length < 2) {
            targetUuid = player.getUniqueId();
        } else {
            String targetName = args[1];
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null) {
                targetUuid = targetPlayer.getUniqueId();
            } else {
                // Try to find offline player by username
                PlayerProfile profile = plugin.getDatabaseManager().getProfileDAO().getProfileByUsername(targetName).orElse(null);
                if (profile == null) {
                    // Player not found in database - they may have never joined the server
                    player.sendMessage(lang.getPrefixed("player-not-found"));
                    return true;
                }
                targetUuid = profile.getUuid();
            }
        }

        plugin.getProfileGUI().openProfile(player, targetUuid, 1);
        return true;
    }

    private boolean handleSetBio(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("ria.profile.edit")) {
            player.sendMessage(lang.getPrefixed("no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile setbio <bio>"));
            return true;
        }

        // Join all remaining arguments as bio content (supports spaces in bio)
        String bio = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Check bio length (reasonable limit)
        if (bio.length() > 100) {
            player.sendMessage(lang.getPrefixed("bio-too-long", "%max%", "100"));
            return true;
        }

        plugin.getProfileService().setBio(player.getUniqueId(), bio);
        player.sendMessage(lang.getPrefixed("bio-set", "%bio%", bio));
        return true;
    }

    private boolean handleComment(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("delete")) {
            return handleCommentDelete(player, args);
        }

        if (!player.hasPermission("ria.profile.comment")) {
            player.sendMessage(lang.getPrefixed("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile comment <player> <message>"));
            return true;
        }

        String targetName = args[1];
        String content = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (content.isEmpty()) {
            player.sendMessage(lang.getPrefixed("comment-empty"));
            return true;
        }

        if (content.length() > plugin.getConfigManager().getMaxCommentLength()) {
            player.sendMessage(lang.getPrefixed("comment-too-long", "%max%", String.valueOf(plugin.getConfigManager().getMaxCommentLength())));
            return true;
        }

        PlayerProfile targetProfile = plugin.getProfileService().getProfileByName(targetName);
        if (targetProfile == null) {
            player.sendMessage(lang.getPrefixed("player-not-found"));
            return true;
        }

        plugin.getCommentService().addComment(player, targetProfile.getUuid(), content).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(lang.getPrefixed("comment-success"));
                } else {
                    player.sendMessage(lang.getPrefixed("database-error"));
                }
            });
        });

        return true;
    }

    private boolean handleCommentDelete(Player player, String[] args) {
        if (!player.hasPermission("ria.profile.comment.delete")) {
            player.sendMessage(lang.getPrefixed("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile comment delete <id>"));
            return true;
        }

        long commentId;
        try {
            commentId = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getPrefixed("comment-not-found"));
            return true;
        }

        Comment comment = plugin.getDatabaseManager().getCommentDAO().getComment(commentId).orElse(null);
        if (comment == null) {
            player.sendMessage(lang.getPrefixed("comment-not-found"));
            return true;
        }

        // Check permissions: can delete if it's on their profile or they have admin permission
        boolean isAdmin = player.hasPermission("ria.admin.*");
        boolean isTarget = comment.getTargetUuid().equals(player.getUniqueId());

        if (!isAdmin && !isTarget) {
            player.sendMessage(lang.getPrefixed("comment-delete-no-permission"));
            return true;
        }

        boolean deleted = plugin.getDatabaseManager().getCommentDAO().deleteComment(commentId);
        if (deleted) {
            player.sendMessage(lang.getPrefixed("comment-delete-success"));
            // Refresh GUI if open
            plugin.getProfileGUI().refreshGUI(player);
        } else {
            player.sendMessage(lang.getPrefixed("comment-not-found"));
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ria.admin.*")) {
            sender.sendMessage(lang.getPrefixed("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin <reload|clear|resetlikes|unlock|migrate>"));
            return true;
        }

        String adminSub = args[1].toLowerCase();

        switch (adminSub) {
            case "reload":
                return handleAdminReload(sender);
            case "clear":
                return handleAdminClear(sender, args);
            case "resetlikes":
                return handleAdminResetLikes(sender, args);
            case "unlock":
                return handleAdminUnlock(sender, args);
            case "lock":
                return handleAdminLock(sender, args);
            case "migrate":
                return handleAdminMigrate(sender, args);
            default:
                sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin <reload|clear|resetlikes|unlock|lock|migrate>"));
                return true;
        }
    }

    private boolean handleAdminReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getLocalizationManager().reload();
        sender.sendMessage(lang.getPrefixed("reload-success"));
        return true;
    }

    private boolean handleAdminClear(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin clear <player> [page]"));
            return true;
        }

        String targetName = args[2];
        int page = 1;
        if (args.length >= 4) {
            try {
                page = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number.");
                return true;
            }
        }

        PlayerProfile profile = plugin.getProfileService().getProfileByName(targetName);
        if (profile == null) {
            sender.sendMessage(lang.getPrefixed("player-not-found"));
            return true;
        }

        int cleared = plugin.getDatabaseManager().getInventoryDAO().clearPage(profile.getUuid(), page);
        sender.sendMessage(lang.getPrefixed("clear-success", 
                "%player%", profile.getUsername(),
                "%page%", String.valueOf(page),
                "%count%", String.valueOf(cleared)));
        return true;
    }

    private boolean handleAdminResetLikes(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin resetlikes <player>"));
            return true;
        }

        String targetName = args[2];
        PlayerProfile profile = plugin.getProfileService().getProfileByName(targetName);
        if (profile == null) {
            sender.sendMessage(lang.getPrefixed("player-not-found"));
            return true;
        }

        plugin.getLikeService().resetLikes(profile.getUuid());
        sender.sendMessage(lang.getPrefixed("reset-likes-success", "%player%", profile.getUsername()));
        return true;
    }

    private boolean handleAdminUnlock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin unlock <player>"));
            return true;
        }

        String targetName = args[2];
        PlayerProfile profile = plugin.getProfileService().getProfileByName(targetName);
        if (profile == null) {
            sender.sendMessage(lang.getPrefixed("player-not-found"));
            return true;
        }

        plugin.getProfileService().unlockPages(profile.getUuid(), 1);
        sender.sendMessage(lang.getPrefixed("unlock-success", 
                "%player%", profile.getUsername(),
                "%pages%", "1"));
        return true;
    }

    private boolean handleAdminLock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin lock <player>"));
            return true;
        }

        String targetName = args[2];
        PlayerProfile profile = plugin.getProfileService().getProfileByName(targetName);
        if (profile == null) {
            sender.sendMessage(lang.getPrefixed("player-not-found"));
            return true;
        }

        int currentPages = profile.getUnlockedPages();
        int newPages = Math.max(1, currentPages - 1);

        plugin.getProfileService().setUnlockedPages(profile.getUuid(), newPages);
        sender.sendMessage(lang.getPrefixed("lock-success", 
                "%player%", profile.getUsername(),
                "%pages%", String.valueOf(currentPages - newPages),
                "%remaining%", String.valueOf(newPages)));
        return true;
    }

    private boolean handleAdminMigrate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(lang.getPrefixed("invalid-usage", "%usage%", "/ria-player-profile admin migrate <source> <target>"));
            return true;
        }

        String source = args[2];
        String target = args[3];

        plugin.getMigrationService().migrateData(source, target).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS:
                        sender.sendMessage(lang.getPrefixed("migrate-success", 
                                "%source%", source, "%target%", target));
                        break;
                    case SOURCE_NOT_FOUND:
                        sender.sendMessage(lang.getPrefixed("player-not-found"));
                        break;
                    case TARGET_EXISTS:
                        sender.sendMessage(lang.getPrefixed("migrate-error-target-exists"));
                        break;
                    case SAME_PLAYER:
                        sender.sendMessage(lang.getPrefixed("migrate-error-same-player"));
                        break;
                    case ERROR:
                        sender.sendMessage(lang.getPrefixed("database-error"));
                        break;
                }
            });
        });

        return true;
    }

    private boolean handleConfirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PendingAction action = plugin.getPendingActionManager().getAndRemovePendingAction(player.getUniqueId());
        if (action == null) {
            player.sendMessage(lang.getPrefixed("no-pending-action"));
            return true;
        }

        switch (action.getType()) {
            case DELETE_COMMENT:
                boolean deleted = plugin.getDatabaseManager().getCommentDAO().deleteComment(action.getCommentId());
                if (deleted) {
                    player.sendMessage(lang.getPrefixed("comment-delete-success"));
                } else {
                    player.sendMessage(lang.getPrefixed("comment-not-found"));
                }
                break;

            case DELETE_DISPLAY_ITEM:
                plugin.getProfileGUI().confirmItemDeletion(player, action);
                break;
        }

        return true;
    }

    private boolean handleCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PendingAction action = plugin.getPendingActionManager().getAndRemovePendingAction(player.getUniqueId());
        if (action == null) {
            player.sendMessage(lang.getPrefixed("no-pending-action"));
            return true;
        }

        player.sendMessage(lang.getPrefixed("action-cancelled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("view");
            completions.add("setbio");
            completions.add("comment");
            if (sender.hasPermission("ria.admin.*")) {
                completions.add("admin");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "view":
                case "setbio":
                case "comment":
                    // Return online player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "admin":
                    if (sender.hasPermission("ria.admin.*")) {
                        completions.add("reload");
                        completions.add("clear");
                        completions.add("resetlikes");
                        completions.add("unlock");
                        completions.add("lock");
                        completions.add("migrate");
                    }
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            switch (args[1].toLowerCase()) {
                case "clear":
                case "resetlikes":
                case "unlock":
                case "lock":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "migrate":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("clear")) {
                completions.add("1");
                completions.add("2");
                completions.add("3");
            } else if (args[1].equalsIgnoreCase("migrate")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}
