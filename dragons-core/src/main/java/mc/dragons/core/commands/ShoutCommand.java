 package mc.dragons.core.commands;
 
 import mc.dragons.core.gameobject.loader.UserLoader;
 import mc.dragons.core.gameobject.user.PermissionLevel;
 import mc.dragons.core.gameobject.user.User;
 import mc.dragons.core.util.PermissionUtil;
 import mc.dragons.core.util.StringUtil;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandExecutor;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.Player;
 
 public class ShoutCommand implements CommandExecutor {
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
     if (sender instanceof Player) {
       Player player = (Player)sender;
       User user = UserLoader.fromPlayer(player);
       if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true))
         return true; 
     } 
     if (args.length == 0) {
       if (!label.equalsIgnoreCase("shout"))
         sender.sendMessage(ChatColor.RED + "Alias for /shout."); 
       sender.sendMessage(ChatColor.RED + "/shout <message>");
       return true;
     } 
     String message = StringUtil.concatArgs(args, 0);
     Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + sender.getName() + " " + ChatColor.AQUA + message);
     return true;
   }
 }


