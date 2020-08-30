 package mc.dragons.core.tasks;
 
 import mc.dragons.core.Dragons;
 import org.bukkit.Bukkit;
 import org.bukkit.entity.Player;
 import org.bukkit.scheduler.BukkitRunnable;
 
 public class UpdateScoreboardTask extends BukkitRunnable {
   private Dragons plugin;
   
   public UpdateScoreboardTask(Dragons instance) {
     this.plugin = instance;
   }
   
   public void run() {
     for (Player player : Bukkit.getOnlinePlayers())
       this.plugin.getSidebarManager().updateScoreboard(player); 
   }
 }


