package mc.dragons.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatOptionsCommand extends DragonsCommandExecutor {

	private Map<User, MessageData> messageTxn = new HashMap<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User user = user(sender);
		
		if(label.equals("chatoptions")) {
			if(args.length == 0) return true;
			Integer id = parseInt(sender, args[0]);
			if(id == null) return true;
			
			MessageData msg = dragons.getChatMessageRegistry().get(id);
			if(!user.getSeenMessages().contains(msg)) {
				sender.sendMessage(ChatColor.RED + "Nope!");
				return true;
			}
			
			messageTxn.put(user, msg);
			
			sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + msg.getSender().getName() + " - " + msg.getMessage());
			TextComponent replyText = StringUtil.plainText("");
			if(!msg.isPrivate()) {
				replyText = StringUtil.clickableHoverableText(ChatColor.GREEN + " [Reply]", "/chatreply ", true, "Reply to this message");
			}
			sender.spigot().sendMessage(replyText,
					StringUtil.clickableHoverableText(ChatColor.YELLOW + " [Report]", "/chatreport " + id + " --id", "Report this message"));
		}
		
		else if(label.equals("chatreply")) {
			if(args.length == 0 || !messageTxn.containsKey(user)) {
				sender.sendMessage(ChatColor.RED + "Click on a chat message and select [Reply] to reply to it");
				sender.sendMessage(ChatColor.GRAY + " This will move you to the channel the message was sent on");
				return true;
			}
			
			MessageData msg = messageTxn.get(user);
			if(user.getSpeakingChannel() != msg.getChannel()) {
				sender.sendMessage(ChatColor.GREEN + "Now speaking on " + msg.getChannel());
				user.setSpeakingChannel(msg.getChannel());
			}
			sender.sendMessage(" ");
			user.chat(StringUtil.concatArgs(args, 0), msg.getId());
		}
		
		return true;
	}

}
