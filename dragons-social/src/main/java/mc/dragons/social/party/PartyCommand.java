package mc.dragons.social.party;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.party.PartyLoader.Party;
import net.md_5.bungee.api.ChatColor;

public class PartyCommand extends DragonsCommandExecutor {
	private PartyLoader partyLoader;
	
	public PartyCommand(Dragons instance) {
		partyLoader = instance.getLightweightLoaderRegistry().getLoader(PartyLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		User user = user(sender);
		Party party = partyLoader.of(user);
		
		if(label.equalsIgnoreCase("toggleselfparty")) {
			if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
			user.getLocalData().append("canSelfParty", !user.getLocalData().getBoolean("canSelfParty", false));
			sender.sendMessage(ChatColor.GREEN + "Toggled self-party ability.");
		}
		
		else if(args.length == 0) {
			if(party == null) {
				sender.sendMessage(ChatColor.GRAY + "You're not currently in a party");
			}
			else if(party.getOwner().equals(user)) {
				sender.sendMessage(ChatColor.GREEN + "You're currently hosting a party");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "You're currently in " + party.getOwner().getName() + "'s party");
			}
			sender.sendMessage(ChatColor.YELLOW + "/party <Player>" + ChatColor.GRAY + " invites a player, or accept an invitation");
			sender.sendMessage(ChatColor.YELLOW + "/party list" + ChatColor.GRAY + " lists all players in your party");
			sender.sendMessage(ChatColor.YELLOW + "/party kick <Player>" + ChatColor.GRAY + " removes a player from your party");
			sender.sendMessage(ChatColor.YELLOW + "/party disband" + ChatColor.GRAY + " disbands your party");
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			if(party == null) {
				sender.sendMessage(ChatColor.RED + "You're not in a party!");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "Party Host: " + ChatColor.GRAY + party.getOwner().getName());
				sender.sendMessage(ChatColor.GREEN + "Party Members: (" + party.getMembers().size() + ") " + ChatColor.GRAY
					+ StringUtil.parseList(party.getMembers().stream().map(u -> u.getName()).collect(Collectors.toList())));
				sender.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "Invite more with /party <Player>");
			}
		}
		
		else if(args[0].equalsIgnoreCase("kick")) {
			if(party == null) {
				sender.sendMessage(ChatColor.RED + "You're not in a party!");
			}
			else if(!party.getOwner().equals(user)) {
				sender.sendMessage(ChatColor.RED + "You're not the host of this party!");
			}
			else {
				for(User member : party.getMembers()) {
					if(member.getName().equalsIgnoreCase(args[1])) {
						partyLoader.kick(party, member);
						break;
					}
				}
			}
		}
		
		else if(args[0].equalsIgnoreCase("disband")) {
			if(party == null) {
				sender.sendMessage(ChatColor.RED + "You're not in a party!");
			}
			else if(!party.getOwner().equals(user)) {
				sender.sendMessage(ChatColor.RED + "You're not the host of this party!");
			}
			else {
				partyLoader.disband(party);
			}
		}
		
		else {
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			
			// Check if we're accepting an invitation
			List<Party> invites = partyLoader.invitesWith(user);
			for(Party p : invites) {
				if(p.getInvites().contains(user) && (p.getOwner().equals(target) || p.getMembers().contains(target))) {
					p.join(user);
					sender.sendMessage(ChatColor.GREEN + "Joined " + p.getOwner().getName() + "'s party successfully!");
					return true;
				}
			}
			
			// Otherwise, we're sending an invitation
			Party otherParty = partyLoader.of(target);
			if(otherParty != null) {
				sender.sendMessage(ChatColor.RED + target.getName() + " is already in a party!");
				return true;
			}
			if(target.equals(user) && !user.getLocalData().getBoolean("canSelfParty", false)) {
				sender.sendMessage(ChatColor.RED + "You can't invite yourself!");
				return true;
			}
			if(party == null) {
					party = partyLoader.newParty(user);
					sender.sendMessage(ChatColor.DARK_GREEN + "Created a new party! Invite others with /party <Player>");
			}
			if(party.getInvites().contains(target)) {
				sender.sendMessage(ChatColor.RED + "You've already invited " + target.getName() + "!");
			}
			else {
				partyLoader.invite(party, target);
				sender.sendMessage(ChatColor.GREEN + "Invitation sent to " + target.getName() + " successfully");
			}
		}
		
		return true;
	}

}
