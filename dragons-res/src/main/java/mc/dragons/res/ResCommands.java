package mc.dragons.res;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitWorld;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.res.ResLoader.Residence;
import mc.dragons.res.ResLoader.Residence.ResAccess;
import mc.dragons.res.ResPointLoader.ResPoint;
public class ResCommands extends DragonsCommandExecutor {
	private ResLoader resLoader;
	private ResPointLoader resPointLoader;
	
	public ResCommands(Dragons instance) {
		resLoader = instance.getLightweightLoaderRegistry().getLoader(ResLoader.class);
		resPointLoader = instance.getLightweightLoaderRegistry().getLoader(ResPointLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		Player player = player(sender);
		User user = user(sender);
		
		if(label.equalsIgnoreCase("res")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GRAY + "To create a residence, open the door");
				sender.sendMessage(ChatColor.GRAY + "of the residence you would like to claim.");
				sender.sendMessage(ChatColor.GRAY + "You have claimed " + ChatColor.RESET + resLoader.getAllResidencesOf(user).size()
						+ "/" + DragonsResidences.MAX_RES_PER_USER + " allowed residences.");
				sender.sendMessage(ChatColor.YELLOW + "/res mine");
				sender.sendMessage(ChatColor.YELLOW + "/res exit");
				sender.sendMessage(ChatColor.YELLOW + "/res delete <ID>");
			}
			else if(args[0].equalsIgnoreCase("mine")) {
				sender.sendMessage(ChatColor.GREEN + "Listing your residences:");
				for(Residence res : resLoader.getAllResidencesOf(user)) {
					Location door = res.getResPoint().getDoorLocation();
					sender.sendMessage(ChatColor.DARK_GREEN + "#" + res.getId() + ChatColor.GRAY + " (" + res.getAccessLevel() + ")"
							+ " (" + FloorLoader.fromWorld(door.getWorld()).getDisplayName() + " " + door.getBlockX() + ", " + door.getBlockZ() + ")");
				}
			}
			else if(args[0].equalsIgnoreCase("exit")) {
				Document saved = user.getData().get("resExitTo", Document.class);
				if(saved == null) {
					sender.sendMessage(ChatColor.RED + "You're not in a residence right now!");
					return true;
				}
				Location to = StorageUtil.docToLoc(saved);
				player.teleport(to);
				sender.sendMessage(ChatColor.GREEN + "Exited residence successfully.");
				user.getStorageAccess().set("resExitTo", null);
			}
			
			else if(args[0].equalsIgnoreCase("delete")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/res delete <ID>");
					return true;
				}
				Integer id = parseInt(sender, args[1]);
				if(id == null) return true;
				
				Residence res = resLoader.getResidenceById(id);
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID! You can see your residences with /res mine");
					return true;
				}
				if(res.isLocked()) {
					sender.sendMessage(ChatColor.RED + "This residence has been locked by an administrator.");
					return true;
				}
				if(!res.getOwner().getIdentifier().equals(user.getIdentifier())) {
					sender.sendMessage(ChatColor.RED + "You do not own this residence! You can see your residences with /res mine");
					return true;
				}
				resLoader.deleteResidence(id);
				resPointLoader.updateResHologramOn(user, res.getResPoint());
				if(player.getWorld().getName().equals("res_temp") && user.getData().getInteger("lastResId") == id) {
					Document saved = user.getData().get("resExitTo", Document.class);
					Location to = StorageUtil.docToLoc(saved);
					player.teleport(to);
					user.getStorageAccess().set("resExitTo", null);
				}
				sender.sendMessage(ChatColor.GREEN + "Deleted residence #" + id + " successfully.");
			}
			return true;
		}
		
		
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
	
		if(label.equalsIgnoreCase("resadmin")) {
			if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
			// Res commands for admins
			if(args.length == 0) {
				sender.sendMessage(ChatColor.YELLOW + "/resadmin linkpoint <Name> <Price> <Display Name...>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin setprice <Name> <NewPrice>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin displayname <Name> <New Display Name...>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin delpoint <Name>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin listpoints");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin listowned <Player>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin [un]lock <ID>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin delete <ID>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin flag <ID> <Flag> <Value>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin goto <ID>");
				sender.sendMessage(ChatColor.YELLOW + "/resadmin rebuild <ID>");
			}
			else if(args[0].equalsIgnoreCase("linkpoint")) {
				if(args.length < 3) {
					sender.sendMessage(ChatColor.RED + "/resadmin linkpoint <Name> <Price> <Display Name...>");
					return true;
				}
				Block target = player.getTargetBlock(null, 5);
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "Please look at the door you want to link!");
					return true;
				}
				if(!(target.getBlockData() instanceof Door)) {
					sender.sendMessage(ChatColor.RED + "Please look at the " + ChatColor.ITALIC + "door " + ChatColor.RED + "you want to link!");
					return true;
				}
				Door door = (Door) target.getBlockData();
				if(door.getHalf() == Half.TOP) {
					target = target.getRelative(BlockFace.DOWN);
				}
				resPointLoader.addResPoint(args[1], StringUtil.concatArgs(args, 3), Double.valueOf(args[2]), target.getLocation());
				sender.sendMessage(ChatColor.GREEN + "Added res point successfully.");
			}
			else if(args[0].equalsIgnoreCase("setprice")) {
				if(args.length < 2) {
					sender.sendMessage(ChatColor.RED + "/resadmin setprice <Name> <NewPrice>");
					return true;
				}
				ResPoint resPoint = resPointLoader.getResPointByName(args[1]);
				if(resPoint == null) {
					sender.sendMessage(ChatColor.RED + "Invalid res point! /respoint listpoints");
					return true;
				}
				resPoint.setPrice(Double.valueOf(args[2]));
				sender.sendMessage(ChatColor.GREEN + "Updated res point price successfully.");
			}
			else if(args[0].equalsIgnoreCase("displayname")) {
				if(args.length < 2) {
					sender.sendMessage(ChatColor.RED + "/resadmin displayname <Name> <New Display Name...>");
					return true;
				}
				ResPoint resPoint = resPointLoader.getResPointByName(args[1]);
				if(resPoint == null) {
					sender.sendMessage(ChatColor.RED + "Invalid res point! /respoint listpoints");
					return true;
				}
				resPoint.setDisplayName(StringUtil.concatArgs(args, 2));
				sender.sendMessage(ChatColor.GREEN + "Updated res point display name successfully.");
			}
			else if(args[0].equalsIgnoreCase("delpoint")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/reasdmin delpoint <Name>");
					return true;
				}
				resPointLoader.deleteResPoint(args[1]);
				sender.sendMessage(ChatColor.GREEN + "If a res point by this name existed, it has been deleted successfully.");
				return true;
			}
			else if(args[0].equalsIgnoreCase("listpoints")) {
				sender.sendMessage(ChatColor.GREEN + "Listing all res points:");
				for(ResPoint resPoint : resPointLoader.getAllResPoints()) {
					sender.sendMessage(ChatColor.GRAY + "- " + resPoint.getName() + " - " + resPoint.getPrice() + " gold"
							+ " (" + StringUtil.locToString(resPoint.getDoorLocation()) + ")");
					if(!(resPoint.getDoorLocation().getBlock().getBlockData() instanceof Door)) {
						sender.sendMessage(ChatColor.RED + "   (Invalid: No door at that location!)");
					}
				}
			}
			else if(args[0].equalsIgnoreCase("listowned")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/resadmin listowned <Player>");
					return true;
				}
				sender.sendMessage(ChatColor.GREEN + "Listing residences owned by " + args[1] + ":");
				for(Residence res : resLoader.getAllResidencesOf(GameObjectType.USER.<User, UserLoader>getLoader().loadObject(args[1]))) {
					sender.sendMessage(ChatColor.DARK_GREEN + "#" + res.getId() + ChatColor.GRAY + ": " + res.getAccessLevel() + (res.isLocked() ? " (Locked)" : ""));
				}
			}
			else if(args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("unlock")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/resadmin " + args[0].toLowerCase() + " <ID>");
					return true;
				}
				Residence res = resLoader.getResidenceById(Integer.valueOf(args[1]));
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID!");
					return true;
				}
				res.setLocked(args[0].equalsIgnoreCase("lock"));
				sender.sendMessage(ChatColor.GREEN + (args[0].equalsIgnoreCase("lock") ? "Locked" : "Unlocked") + " residence #" + args[1]);
			}
			else if(args[0].equalsIgnoreCase("delete")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/resadmin delete <ID>");
					return true;
				}
				Residence res = resLoader.getResidenceById(Integer.valueOf(args[1]));
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID!");
					return true;
				}
				resLoader.deleteResidence(res.getId());
				sender.sendMessage(ChatColor.GREEN + "Deleted residence #" + args[1]);
			}
			else if(args[0].equalsIgnoreCase("flag")) {
				if(args.length < 3) {
					sender.sendMessage(ChatColor.RED + "/resadmin flag <ID> <Flag> <Value>");
					return true;
				}
				Residence res = resLoader.getResidenceById(Integer.valueOf(args[1]));
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID!");
					return true;
				}
				res.getProperties().append(args[2], args[3]);
				res.save();
				sender.sendMessage(ChatColor.GREEN + "Set flag " + args[2] + " to " + args[3] + " for residence #" + args[1]);
			}
			else if(args[0].equalsIgnoreCase("goto")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/resadmin goto <ID>");
					return true;
				}
				Residence res = resLoader.getResidenceById(Integer.valueOf(args[1]));
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID!");
					return true;
				}
				resLoader.goToResidence(user, res.getId(), true);
			}
			else if(args[0].equalsIgnoreCase("rebuild")) {
				if(args.length == 1) {
					sender.sendMessage(ChatColor.RED + "/resadmin rebuild <ID>");
					return true;
				}
				int id = Integer.valueOf(args[1]);
				Residence res = resLoader.getResidenceById(id);
				if(res == null) {
					sender.sendMessage(ChatColor.RED + "Invalid residence ID!");
					return true;
				}
				resLoader.removeResidenceLocally(id);
				resLoader.generateResidence(id);
				sender.sendMessage(ChatColor.GREEN + "Rebuilt residence #" + id + " successfully.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "/resadmin");
			}
		}
		
		else if(label.equalsIgnoreCase("testcontextualholograms")) {
			for(ResPoint resPoint : resPointLoader.getAllResPoints()) {
				resPointLoader.updateResHologramOn(user, resPoint);
			}
		}
		
		else if(label.equalsIgnoreCase("testschematic")) {
			if(args[1].equalsIgnoreCase("good")) {
				DragonsResidences.pasteSchematic(DragonsResidences.loadSchematic(args[0]), 
						DragonsResidences.getEditSession(new BukkitWorld(player.getWorld())), player.getLocation());
			}
			else if(args[1].equalsIgnoreCase("bad")) {
				//DragonsResPlugin.pasteSchematic(args[0], DragonsResPlugin.getEditSession(new BukkitWorld(player.getWorld())), player.getLocation());
				sender.sendMessage(ChatColor.RED + "No! Don't be bad");
				sender.sendMessage(ChatColor.GREEN + "Be good instead ;)");
			}
		}
		
		else if(label.equalsIgnoreCase("restest")) {
			// Test commands for devs
			if(args.length == 0) {
				sender.sendMessage("/restest new <ResPoint>");
				sender.sendMessage("/restest go <ID>");
				sender.sendMessage("/restest my");
				sender.sendMessage("/restest whatsthatrespoint");
			}
			else if(args[0].equalsIgnoreCase("new")) {
				Residence res = resLoader.addResidence(user, resPointLoader.getResPointByName(args[1]), ResAccess.PRIVATE);
				sender.sendMessage("Created new residence #" + res.getId());
			}
			else if(args[0].equalsIgnoreCase("go")) {
				Residence res = resLoader.getResidenceById(Integer.valueOf(args[1]));
				if(res == null) {
					sender.sendMessage("No residence by that ID exists! (highest ID=" + resPointLoader.getCurrentMaxId() + ")");
					return true;
				}
				resLoader.goToResidence(user, res.getId(), true);
			}
			else if(args[0].equalsIgnoreCase("my")) {
				for(Residence res : resLoader.getAllResidencesOf(user)) {
					sender.sendMessage("#" + res.getId() + " (owner=" + res.getOwner().getName() + ", access=" + res.getAccessLevel() + ")");
				}
			}
			else if(args[0].equalsIgnoreCase("whatsthatrespoint")) { // bottom half only
				Block target = player.getTargetBlock(null, 5);
				if(target == null) {
					sender.sendMessage("No target block");
					return true;
				}
				sender.sendMessage("Target block is " + target.getType());
				sender.sendMessage("Target block location is " + StringUtil.locToString(target.getLocation()));
				ResPoint resPoint = resPointLoader.getResPointByDoorLocation(target.getLocation());
				if(resPoint == null) {
					sender.sendMessage("No res point here");
					return true;
				}
				sender.sendMessage("Res point is " + resPoint.getName());
			}
		}
		
		
		
		return true;
	}
	
}
