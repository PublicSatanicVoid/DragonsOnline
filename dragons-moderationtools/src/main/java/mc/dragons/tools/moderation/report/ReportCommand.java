package mc.dragons.tools.moderation.report;

import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ReportCommand extends DragonsCommandExecutor {

	public static String[][] GENERIC_REASONS = {
			{ "Suspected Hacking or Cheating", "Modded game client or other non-Vanilla behavior" },
			{ "Residence Content Violation", "A residence owned by this user does not follow community standards" },
			{ "Guild Content Violation", "A guild owned by this user does not follow community standards" },
			{ "Trolling or Abuse", "Misusing game features to harass others or ruin the gameplay experience" },
			{ "Language or Spamming", ChatColor.RED + "Please click on the message that is in violation instead!" },
			{ "Other Violation", ChatColor.RED + "Please use /report <player> <reason for reporting> instead!" }
	};
	
	private static String CONFIRMATION_FLAG = " --internal-confirm-and-submit";
	
	private ReportLoader reportLoader = instance.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(args.length < 1) {
			sender.sendMessage(ChatColor.RED + "/report <player> [reason for reporting]");
			return true;
		}
		
		User reporter = user(sender);
		User target = lookupUser(sender, args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "No player by the name of \"" + args[0] + "\" was found in our records!");
			return true;
		}
		
		if(target.equals(reporter) && !reporter.getLocalData().getBoolean("canSelfReport", false)) {
			sender.sendMessage(ChatColor.RED + "You can't report yourself!");
			return true;
		}
		
		if(args.length == 1) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "What are you reporting " + target.getName() + " for?");
			for(String[] reason : GENERIC_REASONS) {
				TextComponent option = new TextComponent(ChatColor.GRAY + " • " + ChatColor.AQUA + reason[0]);
				option.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						new Text(ChatColor.GRAY + reason[1])));
				option.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report " + args[0] + " " + reason[0]));
				reporter.getPlayer().spigot().sendMessage(option);
			}
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.GRAY + "Click on one of the reasons above to continue with the report.");
			sender.sendMessage(" ");
			return true;
		}
		
		String reason = StringUtil.concatArgs(args, 1);
		
		if(reason.equalsIgnoreCase("Language or Spamming")) {
			sender.sendMessage(ChatColor.RED + "To report chat abuse, please click on the message that is in violation!");
			return true;
		}
		
		if(reason.equalsIgnoreCase("Other Violation")) {
			sender.sendMessage(ChatColor.RED + "To report a different violation, use /report <player> <reason for reporting>");
			return true;
		}
		
		if(!reason.contains(CONFIRMATION_FLAG)) {
			sender.sendMessage(" ");
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please review your report before submitting.");
			sender.sendMessage(ChatColor.GREEN + "Reporting: " + ChatColor.GRAY + target.getName());
			sender.sendMessage(ChatColor.GREEN + "Reason: " + ChatColor.GRAY + reason);
			sender.sendMessage(" ");
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[Submit]", "/report " + args[0] + " " + reason + CONFIRMATION_FLAG, 
					"By submitting, you confirm that this report is accurate to the best of your knowledge.\n" +
					"You will not be able to modify this report once it has been submitted."),
				new TextComponent(" "),
				StringUtil.hoverableText(ChatColor.GRAY + "" + ChatColor.BOLD + "[Cancel]", "You can always create a new report with " + ChatColor.YELLOW + "/report <player>" + ChatColor.GRAY + "."));
			sender.sendMessage(" ");
			return true;
		}
		
		reason = reason.replaceAll(Pattern.quote(CONFIRMATION_FLAG), "");
		
		reportLoader.fileUserReport(target, reporter, reason);
		sender.sendMessage(ChatColor.GREEN + "Your report against " + target.getName() + " was filed successfully. We will review it as soon as possible." + 
				(reporter.isVerified() ? " As a verified user, your reports are prioritized for review." : ""));
		
		return true;
	}

}
