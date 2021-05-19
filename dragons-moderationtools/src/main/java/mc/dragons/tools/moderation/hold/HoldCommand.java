package mc.dragons.tools.moderation.hold;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;

public class HoldCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/hold <user1,user2,...> <reason>");
			return true;
		}
		
		List<User> targets = Arrays.stream(args[0].split(Pattern.quote(","))).map(username -> userLoader.loadObject(username)).filter(Objects::nonNull).collect(Collectors.toList());
		String reason = StringUtil.concatArgs(args, 1);
		
		HoldEntry hold = holdLoader.newHold(targets, user(sender), reason);
		if(hold == null) {
			sender.sendMessage(ChatColor.RED + "Could not place hold: An internal error occurred.");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Hold placed successfully. ID: " + hold.getId());
		}
		
		return true;
	}
}
