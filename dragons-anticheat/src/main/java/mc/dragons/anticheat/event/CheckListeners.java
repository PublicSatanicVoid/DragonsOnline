package mc.dragons.anticheat.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.CheckRegistry;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.move.MoveData;
import mc.dragons.anticheat.check.move.MoveData.MoveEntry;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.DragonsLogger;

public class CheckListeners implements Listener {
	private DragonsLogger LOGGER;
	private CheckRegistry checkRegistry;
	
	public CheckListeners(DragonsAntiCheat plugin) {
		LOGGER = plugin.getLogger();
		checkRegistry = plugin.getCheckRegistry();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);
		MoveData moveData = MoveData.of(user);
		moveData.lastValidLocation = player.getLocation();
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);
		MoveData moveData = MoveData.of(user);
		
		moveData.moveHistory.add(new MoveEntry(event.getFrom(), event.getTo()));
		moveData.trimMoveHistory();
		
		boolean okay = checkRegistry.runChecks(CheckType.MOVING, user);
		if(okay) {
			moveData.validMove();
			LOGGER.trace("Valid move - " + moveData.validMoves);
		}
		else {
			moveData.invalidMove();
			LOGGER.trace("Invalid move - " + moveData.validMoves);
		}
	}
}
