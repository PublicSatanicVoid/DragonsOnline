package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.chat.MessageData;

public class ChatReportCommand implements CommandExecutor {
	private ChatMessageRegistry chatMessageRegistry;
	private ReportLoader reportLoader;
	
	public ChatReportCommand(Dragons instance) {
		chatMessageRegistry = instance.getChatMessageRegistry();
		reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Click on a chat message to report it.");
			return true;
		}
		
		User reporter = UserLoader.fromPlayer((Player) sender);
		MessageData messageData = chatMessageRegistry.get(Integer.valueOf(args[0]));
		if(messageData == null) {
			sender.sendMessage(ChatColor.RED + "No data found for the specified message!");
			return true;
		}
		
		reportLoader.fileChatReport(messageData.getSender(), reporter, messageData);
		sender.sendMessage(ChatColor.GREEN + "Chat report filed successfully. A staff member will review it as soon as possible.");
		
		return true;
	}

}
