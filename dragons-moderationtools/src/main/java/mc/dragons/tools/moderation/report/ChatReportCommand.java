package mc.dragons.tools.moderation.report;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChatMessageRegistry;
import mc.dragons.core.gameobject.user.chat.MessageData;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class ChatReportCommand extends DragonsCommandExecutor {
	private static String CONFIRMATION_FLAG = "--internal-confirm-and-submit";
	
	private ChatMessageRegistry chatMessageRegistry = dragons.getChatMessageRegistry();
	private ReportLoader reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/chatreport <player> or click on a chat message to report it.");
			return true;
		}
		
		User reporter = user(sender);
		boolean forceId = StringUtil.getFlagIndex(args, "--id", 1) != -1;
		
		/* /chatreport <player> */
		if(!forceId && Integer.getInteger(args[0], null) == null) {
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			int i = 0;
			for(MessageData message : chatMessageRegistry.getAllBy(target, reporter)) {
				sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + ChatColor.RESET + message.getMessage() + (message.isPrivate() ? ChatColor.GRAY + " (Private)" : ""), 
						"/chatreport " + message.getId() + " --id", "Click to report this message"));
				i++;
				if(i >= 10) break;
			}
			if(i == 0) {
				sender.sendMessage(ChatColor.RED + "There are no messages by that user to report!");
			}
			else {
				sender.sendMessage(ChatColor.YELLOW + "Click on a message above to report it.");
			}
			return true;
		}
		
		/* /chatreport <messageId> */
		Integer messageId = parseInt(sender, args[0]);
		if(messageId == null) return true;
		
		MessageData messageData = chatMessageRegistry.get(messageId);
		if(messageData == null || (messageData.isPrivate() && !messageData.getTo().equals(reporter))) {
			sender.sendMessage(ChatColor.RED + "No data found for the specified message!");
			return true;
		}
		
		if(messageData.getSender().equals(reporter) && !reporter.getLocalData().getBoolean("canSelfReport", false)) {
			sender.sendMessage(ChatColor.RED + "You can't report your own message!");
			return true;
		}
		
		if(args.length < 2 || !args[1].equals(CONFIRMATION_FLAG)) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please review your chat report before submitting.");
			sender.sendMessage(ChatColor.GREEN + "Reporting: " + ChatColor.GRAY + messageData.getSender().getName());
			sender.sendMessage(ChatColor.GREEN + "Message: " + ChatColor.GRAY + messageData.getMessage());
			sender.sendMessage(ChatColor.GREEN + "Type: " + ChatColor.GRAY + (messageData.isPrivate() ? "Private Message" : "Public Chat"));
			sender.sendMessage(" ");
			sender.spigot().sendMessage(
					StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Submit]", "/chatreport " + args[0] + " " + CONFIRMATION_FLAG,
						"By submitting, you confirm that this report is accurate to the best of your knowledge."), 
					new TextComponent(" "),
					StringUtil.hoverableText(ChatColor.GRAY + "" + ChatColor.BOLD + "[Cancel]", "You can always create a new chat report by clicking on a chat message."));
			sender.sendMessage(" ");
			return true;
		}
		
		reportLoader.fileChatReport(messageData.getSender(), reporter, messageData);
		sender.sendMessage(ChatColor.GREEN + "Chat report filed successfully. A staff member will review it as soon as possible.");
		sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " [Click to Block Player]", "/block " + messageData.getSender().getName(), "Click to block " + messageData.getSender().getName()));
		
		return true;
	}

}
