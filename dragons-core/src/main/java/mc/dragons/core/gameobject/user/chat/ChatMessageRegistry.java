package mc.dragons.core.gameobject.user.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mc.dragons.core.gameobject.user.User;

/**
 * Registry of all chat messages since the server was started
 * 
 * @author Adam
 *
 */
public class ChatMessageRegistry {
	private Map<Integer, MessageData> history;
	
	public ChatMessageRegistry() {
		history = new HashMap<>();
	}
	
	public void register(MessageData message) {
		history.put(message.getId(), message);
	}
	
	public MessageData get(int id) {
		return history.get(id);
	}

	public List<MessageData> getAllBy(User target, User issuer) {
		return history.values().stream()
				.filter(m -> m.getSender().equals(target))
				.filter(m -> m.isPrivate() || (m.isPrivate() == issuer.equals(m.getTo())))
				.sorted((m1,m2) -> m2.getId() - m1.getId())
				.collect(Collectors.toList());
	}
}
