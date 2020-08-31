package mc.dragons.res;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Door;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.util.StringUtil;
import mc.dragons.res.ResLoader.ResPoint;
import mc.dragons.res.ResLoader.Residence;
import mc.dragons.res.ResLoader.Residence.ResAccess;

public class ResEvents implements Listener {

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if(event.getClickedBlock() == null) return;
		if(!(event.getClickedBlock().getState().getData() instanceof Door)) {
			return;
		}
		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);
		Door door = (Door) event.getClickedBlock().getState().getData();
		if(player.getWorld().getName().equals("res_temp")) {
			player.performCommand("res exit");
			event.setCancelled(true);
			return;
		}
		if(!door.isOpen()) {
			user.debug("R.Click on door (opening it)");
			Location realLocation = door.isTopHalf() ? event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation() 
					: event.getClickedBlock().getLocation();
			user.debug("-Door base=" + StringUtil.locToString(realLocation));
			ResPoint resPoint = ResLoader.getResPointByDoorLocation(realLocation);
			if(resPoint == null) return;
			user.debug("-Res point: " + resPoint.getName());
			//((Door) realLocation.getBlock().getState().getData()).setOpen(false);
			event.setCancelled(true);
			List<Residence> ownedHere = ResLoader.getAllResidencesOf(user, resPoint);
			if(ownedHere.size() == 0) {
				user.debug("-Doesn't have any residences here");
				if(resPoint.getPrice() > user.getGold()) {
					player.sendMessage(ChatColor.RED + "You cannot afford this residence! (Costs " + resPoint.getPrice() + " gold)");
					return;
				}
				if(ResLoader.getAllResidencesOf(user).size() >= DragonsResPlugin.MAX_RES_PER_USER) {
					player.sendMessage(ChatColor.RED + "You have reached the maximum number of residences per user! (" + DragonsResPlugin.MAX_RES_PER_USER + ")");
					return;
				}
				GUI gui = new GUI(1, "Purchase a residence here?");
				gui.add(new GUIElement(0, Material.EMERALD_BLOCK, ChatColor.GREEN + "✓ " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "YES", u -> {
					player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1f);
					u.takeGold(resPoint.getPrice());
					Residence purchased = ResLoader.addResidence(user, resPoint, ResAccess.PRIVATE);
					ResLoader.goToResidence(user, purchased.getId(), false);
					u.closeGUI(true);
				}));
				gui.add(new GUIElement(4, Material.PAPER, ChatColor.YELLOW + "Price: " + ChatColor.GOLD + resPoint.getPrice() + " Gold"));
				gui.add(new GUIElement(8, Material.REDSTONE_BLOCK, ChatColor.RED + "✘ " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "NO", 
						u -> u.closeGUI(true)));
				gui.open(user);
			}
			else {
				user.debug("-Has a residence here, going there now");
				player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1f);
				ResLoader.goToResidence(user, ownedHere.get(0).getId(), false);
			}
		}
	}
}
