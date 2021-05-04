package mc.dragons.social.friend;

import static mc.dragons.core.gameobject.user.User.asUsers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.StringUtil;

public class FriendUtil {
	public static List<UUID> getFriendsUUID(User user) {
		return user.getData().getList("friends", UUID.class, new ArrayList<UUID>());
	}
	
	public static List<UUID> getFriendsOutgoingUUID(User user) {
		return user.getData().getList("friendsOutgoing", UUID.class, new ArrayList<UUID>());
	}
	
	public static List<UUID> getFriendsIncomingUUID(User user) {
		return user.getData().getList("friendsIncoming", UUID.class, new ArrayList<UUID>());
	}
	
	public static List<User> getFriends(User user) {
		return asUsers(getFriendsUUID(user));
	}
	
	public static List<User> getOutgoing(User user) {
		return asUsers(getFriendsOutgoingUUID(user));
	}
	
	public static List<User> getIncoming(User user) {
		return asUsers(getFriendsIncomingUUID(user));
	}
	
	/**
	 * User sends request to target
	 * @param user
	 * @param target
	 * @return
	 */
	public static boolean sendRequest(User user, User target) {
		List<UUID> out = getFriendsOutgoingUUID(user);
		if(out.contains(target.getUUID())) return false;
		out.add(target.getUUID());
		
		List<UUID> in = getFriendsIncomingUUID(target);
		if(in.contains(user.getUUID())) return false;
		in.add(user.getUUID());
		
		user.getStorageAccess().set("friendsOutgoing", out);
		target.getStorageAccess().set("friendsIncoming", in);
		return true;
	}
	
	/**
	 * User accepts target's request
	 * @param user
	 * @param target
	 * @return
	 */
	public static boolean acceptRequest(User user, User target) {
		List<UUID> in = getFriendsIncomingUUID(user);
		if(!in.contains(target.getUUID())) return false;
		
		List<UUID> out = getFriendsOutgoingUUID(target);
		if(!out.contains(user.getUUID())) return false;

		user.debug("Accepting request. User in=" + StringUtil.parseList(in) + "; Target out=" + StringUtil.parseList(out));
		
		boolean success = in.remove(target.getUUID());
		success &= out.remove(user.getUUID());
		
		if(!success) return false;
		
		List<UUID> friendsUser = getFriendsUUID(user);
		friendsUser.add(target.getUUID());
		List<UUID> friendsTarget = getFriendsUUID(target);
		friendsTarget.add(user.getUUID());

		user.debug("Accepted request. User in=" + StringUtil.parseList(in) + "; Target out=" + StringUtil.parseList(out));
		
		user.getStorageAccess().set("friendsIncoming", in);
		user.getStorageAccess().set("friends", friendsUser);
		target.getStorageAccess().set("friendsOutgoing", out);
		target.getStorageAccess().set("friends", friendsTarget);
		
		return true;
	}
	
	/**
	 * User denies target's request
	 * @param user
	 * @param target
	 * @return
	 */
	public static boolean denyRequest(User user, User target) {
		List<UUID> in = getFriendsIncomingUUID(user);
		if(!in.contains(target.getUUID())) return false;
		
		List<UUID> out = getFriendsOutgoingUUID(target);
		if(!out.contains(user.getUUID())) return false;
		
		user.debug("Denying request. User in=" + StringUtil.parseList(in) + "; Target out=" + StringUtil.parseList(out));
		
		boolean success = in.remove(target.getUUID());
		success &= out.remove(user.getUUID());
		
		user.debug("Denied request. User in=" + StringUtil.parseList(in) + "; Target out=" + StringUtil.parseList(out));
		
		if(!success) return false;
		
		user.getStorageAccess().set("friendsIncoming", in);
		target.getStorageAccess().set("friendsOutgoing", out);
		
		return true;
	}
	
	/**
	 * User removes target from friend list
	 * @param user
	 * @param target
	 * @return
	 */
	public static boolean removeFriend(User user, User target) {
		List<UUID> friendsUser = getFriendsUUID(user);
		if(!friendsUser.contains(target.getUUID())) return false;
		
		List<UUID> friendsTarget = getFriendsUUID(target);
		if(!friendsTarget.contains(user.getUUID())) return false;
		
		boolean success = friendsUser.remove(target.getUUID());
		success &= friendsTarget.remove(user.getUUID());
		
		if(!success) return false;
		
		user.getStorageAccess().set("friends", friendsUser);
		target.getStorageAccess().set("friends", friendsTarget);
		
		return true;
	}
	
	public static boolean allowsRequests(User user) {
		return user.getData().getBoolean("acceptsIncomingFriendRequests", true);
	}
	
	public static void toggleRequests(User user) {
		user.getStorageAccess().set("acceptsIncomingFriendRequests", !allowsRequests(user));
	}
}
