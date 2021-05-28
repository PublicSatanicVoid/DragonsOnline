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
	
	private void toggleSelfParty(CommandSender sender) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		User user = user(sender);
		user.getLocalData().append("canSelfParty", !user.getLocalData().getBoolean("canSelfParty", false));
		sender.sendMessage(ChatColor.GREEN + "Toggled self-party ability.");
	}
	
	private void dumpPartyData(CommandSender sender) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		User user = user(sender);
		Party party = partyLoader.of(user);
		List<Party> invites = partyLoader.invitesWith(user);
		if(party == null) {
			sender.sendMessage("No party");
		}
		else {
			sender.sendMessage("Your Party: " + party.getId() + " (" + party.getOwner().getName() + " - " + party.getOwner().getServerName() + ")");
		}
		for(Party p : invites) {
			sender.sendMessage("Invite: " + p.getId() + " (" + p.getOwner().getName() + " - " + p.getOwner().getServerName() + ")");
		}
		for(Party p : partyLoader.all()) {
			sender.sendMessage("Active: " + p.getId() + " (" + p.getOwner().getName() + " - " + p.getOwner().getServerName() + ")");
		}
	}
	
	private void showHelp(CommandSender sender) {
		User user = user(sender);
		Party party = partyLoader.of(user);
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
	
	private void partyList(CommandSender sender) {	
		User user = user(sender);
		Party party = partyLoader.of(user);	
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
	
	private void partyKick(CommandSender sender, String[] args) {
		User user = user(sender);
		Party party = partyLoader.of(user);	
		if(party == null) {
			sender.sendMessage(ChatColor.RED + "You're not in a party!");
		}
		else if(!party.getOwner().equals(user)) {
			sender.sendMessage(ChatColor.RED + "You're not the host of this party!");
		}
		else {
			boolean found = false;
			for(User member : party.getMembers()) {
				if(member.getName().equalsIgnoreCase(args[1])) {
					partyLoader.kick(party, member, true);
					found = true;
					break;
				}
			}
			if(!found) {
				sender.sendMessage(ChatColor.RED + "Could not kick " + args[1] + " from the party");
			}
		}
	}
	
	private void partyDisband(CommandSender sender) {
		User user = user(sender);
		Party party = partyLoader.of(user);	
		if(party == null) {
			sender.sendMessage(ChatColor.RED + "You're not in a party!");
		}
		else if(!party.getOwner().equals(user)) {
			sender.sendMessage(ChatColor.RED + "You're not the host of this party!");
		}
		else {
			partyLoader.disband(party, true);
		}
	}
	
	private void partyJoin(CommandSender sender, String[] args) {
		User user = user(sender);
		Party party = partyLoader.of(user);	
		User target = lookupUser(sender, args[0]);
		if(target == null) return;
		
		// Check if we're accepting an invitation
		List<Party> invites = partyLoader.invitesWith(user);
		for(Party p : invites) {
			if(p.getInvites().contains(user) && (p.getOwner().equals(target) || p.getMembers().contains(target))) {
				partyLoader.join(p, user, true);
				sender.sendMessage(ChatColor.GREEN + "Joined " + p.getOwner().getName() + "'s party successfully! To speak in party chat, do /csp");
				return;
			}
		}
		
		// Otherwise, we're sending an invitation
		Party otherParty = partyLoader.of(target);
		if(otherParty != null) {
			sender.sendMessage(ChatColor.RED + target.getName() + " is already in a party!");
			return;
		}
		if(target.equals(user) && !user.getLocalData().getBoolean("canSelfParty", false)) {
			sender.sendMessage(ChatColor.RED + "You can't invite yourself!");
			return;
		}
		if(party == null) {
			party = partyLoader.newParty(user);
			sender.sendMessage(ChatColor.DARK_GREEN + "Created a new party! Invite others with /party <Player>");
		}
		if(party.getInvites().contains(target)) {
			sender.sendMessage(ChatColor.RED + "You've already invited " + target.getName() + "!");
		}
		else {
			partyLoader.invite(party, target, true);
			sender.sendMessage(ChatColor.GREEN + "Invitation sent to " + target.getName() + " successfully");
		}
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		if(label.equalsIgnoreCase("toggleselfparty")) {
			toggleSelfParty(sender);
		}
		
		else if(label.equalsIgnoreCase("dumpParties")) {
			dumpPartyData(sender);
		}
		
		else if(args.length == 0) {
			showHelp(sender);
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			partyList(sender);
		}
		
		else if(args[0].equalsIgnoreCase("kick")) {
			partyKick(sender, args);
		}
		
		else if(args[0].equalsIgnoreCase("disband")) {
			partyDisband(sender);
		}
		
		else {
			partyJoin(sender, args);
		}
		
		return true;
	}

}
