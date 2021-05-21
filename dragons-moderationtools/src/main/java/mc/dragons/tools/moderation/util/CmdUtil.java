package mc.dragons.tools.moderation.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.punishment.PunishmentCode;

public class CmdUtil {
	private static UserLoader userLoader = GameObjectType.USER.getLoader();
	
	public static class CmdData {
		public List<User> targets;
		public PunishmentCode code;
		public String extraInfo;
		
		public String formatInternalReason() {
			return code.getCode() + ": " + code.getName() + (extraInfo.isBlank() ? "" : " (" + extraInfo + ")");
		}
	}
	
	public static CmdData parse(CommandSender sender, String suggestCommand, String[] args, String... flags) {
		CmdData data = new CmdData();
		int uuidFlagIndex = StringUtil.getFlagIndex(args, "-uuid", 0);
		int reasonIndex = 0;
		data.targets = new ArrayList<>();
		for(int i = 0; i < args.length; i++) {
			PunishmentCode test = PunishmentCode.getByCode(args[i]);
			if(test == null) {
				User target = uuidFlagIndex == -1 ? userLoader.loadObject(args[i]) : userLoader.loadObject(UUID.fromString(args[i]));
				if(target == null) {
					sender.sendMessage(DragonsCommandExecutor.ERR_USER_NOT_FOUND + " (\"" + args[i] + "\")");
					return null;
				}
				data.targets.add(target);
				reasonIndex = i + 1;
			}
			else {
				if(i == 0) {
					sender.sendMessage(ChatColor.RED + "Specify a player! " + suggestCommand);
					return null;
				}
				data.code = test;
				reasonIndex = i + 1;
				break;
			}
		}
		if(data.code == null) {
			sender.sendMessage(ChatColor.RED + "Invalid punishment code! Valid codes are:");
			PunishmentCode.showPunishmentCodes(sender, suggestCommand.replaceAll(Pattern.quote("<players>"), StringUtil.parseList(data.targets.stream().map(u -> u.getName()).collect(Collectors.toList()), " ")));
			return null;
		}
		int minFlagIndex = uuidFlagIndex;
		for(String flag : flags) {
			int index = StringUtil.getFlagIndex(args, flag, 0);
			if(minFlagIndex == -1 || index < minFlagIndex) minFlagIndex = index;
		}
		data.extraInfo = StringUtil.concatArgs(args, reasonIndex, minFlagIndex);
		return data;
 	}
}
