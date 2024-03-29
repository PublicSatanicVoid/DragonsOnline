package mc.dragons.dev.notifier;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.gameobject.user.UserLoader;

public class AdviceBroadcaster extends BukkitRunnable {
	private int adviceNo = 0;
	private String[] advice = {
		ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/task " + ChatColor.YELLOW + "to create a new task! An admin can approve it and assign people to it.",
		ChatColor.LIGHT_PURPLE + "Make sure to report bugs or leave suggestions on our Discord!",
		ChatColor.RED + "Are you experiencing lag or performance drops? See if it's you or the server with " + ChatColor.DARK_RED + "/lag",
		ChatColor.AQUA + "To communicate globally with other staff, do " + ChatColor.DARK_AQUA + "/c speak staff",
		ChatColor.YELLOW + "When you've completed a task, do " + ChatColor.GOLD + "/done <task#> " + ChatColor.YELLOW + "to mark it as completed!",
		ChatColor.GREEN + "Don't know what to do? Find tasks with " + ChatColor.DARK_GREEN + "/findtasks" 
				+ ChatColor.GREEN + ", check the quest or build planning docs at " + ChatColor.DARK_GREEN + "bit.ly/dragons-quests " 
				+ ChatColor.GREEN + "or " + ChatColor.DARK_GREEN + "bit.ly/dragons-builds" + ChatColor.GREEN + ", or ask an admin!",
		ChatColor.BLUE + "Thank you for contributing your time to make DragonsOnline a reality. We appreciate you.",
		ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/setwarp " + ChatColor.YELLOW + "to mark an important spot!",
		ChatColor.LIGHT_PURPLE + "To make a vanilla mob permanent and fixed in place, look at it and type " + ChatColor.DARK_PURPLE + "/fixed",
		ChatColor.RED + "Redeem your stars for exclusive staff items! " + ChatColor.DARK_RED + "/staffshop",
		ChatColor.AQUA + "You can now use &9color &3codes in &1signs!",
		ChatColor.GREEN + "It's more fun together! Add friends with " + ChatColor.DARK_GREEN + "/friend <Player>"
	};
	
	@Override
	public void run() {
		String msg = ChatColor.GRAY + "" + ChatColor.BOLD + " * " + advice[adviceNo++];
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(UserLoader.fromPlayer(p).getData().getBoolean(TipsCommand.KEY_DISABLE_TIPS, false)) continue;
			p.sendMessage(msg);
		}
		if(adviceNo == advice.length) adviceNo = 0;
	}
}
