package mc.dragons.tools.dev.debug;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.StorageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class StateCommands extends DragonsCommandExecutor {

	private StateLoader stateLoader = instance.getLightweightLoaderRegistry().getLoader(StateLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		Player player = player(sender);
		User user = user(sender);
		
		if(label.equalsIgnoreCase("getstate")) {
			Player target = player;
			User targetUser = user;
			if(args.length > 0) {
				targetUser = lookupUser(sender, args[0]);
				if(targetUser == null) return true;
				target = targetUser.getPlayer();
			}
			
			Document state = new Document("loc", StorageUtil.locToDoc(target.getLocation()))
					.append("health", target.getHealth())
					.append("gamemode", target.getGameMode().toString())
					.append("quests", targetUser.getData().get("quests", Document.class))
					.append("deathCountdown", targetUser.getDeathCountdownRemaining())
					.append("speaking", targetUser.getSpeakingChannel().toString())
					.append("listening", targetUser.getActiveChatChannels().stream().map(ch -> ch.toString()).collect(Collectors.toList()))
					.append("inventory", targetUser.getInventoryAsDocument())
					.append("xp", targetUser.getXP())
					.append("gold", targetUser.getGold())
					.append("godMode", targetUser.isGodMode())
					.append("skills", targetUser.getData().get("skills", Document.class))
					.append("skillProgress", targetUser.getData().get("skillProgress", Document.class))
					.append("lastRes", targetUser.getData().getInteger("lastResId"))
					.append("resExitTo", targetUser.getData().get("resExitTo", Document.class))
					.append("originalUser", targetUser.getUUID().toString())
					.append("originalTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(Date.from(Instant.now())));
			UUID stateToken = stateLoader.registerStateToken(state);
			TextComponent clickable = new TextComponent(ChatColor.GREEN + "Your current state token is " + ChatColor.GRAY + stateToken);
			clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to get copy-able state token")));
			clickable.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/setstate " + stateToken));
			player.spigot().sendMessage(clickable);
		}
		
		else if(label.equalsIgnoreCase("setstate")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/setstate <StateToken> [TargetPlayer]");
				return true;
			}
			
			Player target = player;
			User targetUser = user;
			if(args.length > 1) {
				target = lookupPlayer(sender, args[1]);
				if(target == null) return true;
				targetUser = UserLoader.fromPlayer(target);
			}
			
			Document state = lookup(sender, () -> stateLoader.getState(UUID.fromString(args[0])), ChatColor.RED + "Invalid state token!");
			if(state == null || state.isEmpty()) return true;
			sender.sendMessage(ChatColor.GRAY + "Taking snapshot of current state for backup...");
			player.performCommand("getstate " + targetUser.getName());
			sender.sendMessage(ChatColor.GRAY + "Applying state data of user " + userLoader.loadObject(UUID.fromString(state.getString("originalUser"))).getName()
					+ " at " + state.getString("originalTime"));
			target.teleport(StorageUtil.docToLoc(state.get("loc", Document.class)));
			target.setHealth(state.getDouble("health"));
			target.setGameMode(GameMode.valueOf(state.getString("gamemode")));
			targetUser.loadQuests(null, state.get("quests", Document.class));
			if(state.getInteger("deathCountdown", 0) != 0) {
				targetUser.setDeathCountdown(state.getInteger("deathCountdown"));
			}
			targetUser.setSpeakingChannel(ChatChannel.valueOf(state.getString("speaking")));
			List<ChatChannel> ch = new ArrayList<>(targetUser.getActiveChatChannels());
			for(ChatChannel c : ch) {
				targetUser.removeActiveChatChannel(c);
			}
			for(String c : state.getList("listening", String.class)) {
				targetUser.addActiveChatChannel(ChatChannel.valueOf(c));
			}
			targetUser.clearInventory();
			targetUser.loadInventory(null, state.get("inventory", Document.class));
			targetUser.setXP(state.getInteger("xp"));
			targetUser.setGold(state.getDouble("gold"), false);
			targetUser.getData().append("skills", state.get("skills", Document.class));
			targetUser.getData().append("skillProgress", state.get("skillProgress", Document.class));
			targetUser.getData().append("lastResId", state.getInteger("lastRes"));
			if(state.getBoolean("resExitTo") != null) {
				targetUser.getData().append("resExitTo", StorageUtil.docToLoc(state.get("resExitTo", Document.class)));
			}
			final User fTargetUser = targetUser;
			Bukkit.getScheduler().runTaskLater(instance, () -> {
				fTargetUser.sendActionBar(ChatColor.GRAY + "Your state was updated (" + args[0] + ")");
			}, 5L);
			sender.sendMessage(ChatColor.GREEN + "State data applied successfully. New token: " + UUID.fromString(args[0]));
		}
		
		return true;
	}

}
