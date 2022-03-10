package mc.dragons.tools.content.command.builder;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class SignCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.BUILDER) || !requirePlayer(sender)) return true;
		
		if(args.length <= 1) {
			sender.sendMessage(ChatColor.RED + "/sign <1-4> <text ...>");
			return true;
		}
		
		Player player = (Player) sender;
		Block target = player.getTargetBlockExact(10);
		if(target == null || !(target.getState() instanceof Sign)) {
			sender.sendMessage(ChatColor.RED + "Look at a sign to modify its text!");
			return true;
		}
		
		Integer index = parseInt(sender, args[0]);
		if(index == null) return true;
		
		Sign sign = (Sign) target.getState();
		sign.setLine(index - 1, StringUtil.colorize(StringUtil.concatArgs(args, 1)));
		sign.update();
		
		return true;
	}

}
