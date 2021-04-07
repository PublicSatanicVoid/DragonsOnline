package mc.dragons.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.Floor.FloorStatus;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.res.ResPointLoader.ResPoint;

public class DragonsResPlugin extends JavaPlugin implements CommandExecutor {
	
	public static final int MAX_RES_PER_USER = 5;
	
	public void onEnable() {
		resetResWorld();
		
		ResLoader resLoader = new ResLoader(Dragons.getInstance().getMongoConfig());
		ResPointLoader resPointLoader = new ResPointLoader(Dragons.getInstance().getMongoConfig());
		
		Dragons.getInstance().getLightweightLoaderRegistry().register(resLoader);
		Dragons.getInstance().getLightweightLoaderRegistry().register(resPointLoader);
		
		Dragons.getInstance().getUserHookRegistry().registerHook(new ResUserHook());
		
		resPointLoader.loadAllResPoints();
		
		getServer().getPluginManager().registerEvents(new ResEvents(), this);
		
		ResCommands resCommands = new ResCommands();
		getCommand("res").setExecutor(resCommands);
		getCommand("resadmin").setExecutor(resCommands);
		getCommand("restest").setExecutor(resCommands);
		getCommand("testschematic").setExecutor(resCommands);
		getCommand("testcontextualholograms").setExecutor(resCommands);
		
		getLogger().info("Loading holograms for res points...");
		for(ResPoint resPoint : resPointLoader.getAllResPoints()) {
			resPointLoader.createResPointHologram(resPoint);
		}
	}
	
	private void resetResWorld_recursiveStep(File parent) {
		if(parent.isDirectory()) {
			boolean empty = true;
			for(File file : parent.listFiles()) {
				empty = false;
				resetResWorld_recursiveStep(file);
			}
			if(empty) {
				parent.delete();
			}
		}
		else {
			parent.delete();
		}
	}
	
	private void resetResWorld() {
		getLogger().info("Resetting residential world...");
		resetResWorld_recursiveStep(new File("res_temp"));
		WorldCreator creator = WorldCreator.name("res_temp");
		creator.type(WorldType.FLAT);
		Bukkit.createWorld(creator);
		StorageManager lsm = Dragons.getInstance().getLocalStorageManager();
		StorageAccess lsa = lsm.getNewStorageAccess(GameObjectType.FLOOR, 
				new Document("worldName", "res_temp")
				.append("floorName", "RES")
				.append("displayName", "Residence")
				.append("levelMin", 0)
				.append("volatile", true)
				.append("status", FloorStatus.LIVE.toString()));
		Floor resFloor = new Floor(lsm, lsa, true);
		FloorLoader.link(Bukkit.getWorld("res_temp"), resFloor);
		Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects().add(resFloor);
	}

	public static Clipboard loadSchematic(String fileName) {
		File file = new File("plugins/FastAsyncWorldEdit/schematics/" + fileName + ".schem");
		if(!file.exists()) {
			System.err.println("[DragonsRes] Couldn't find schematic file " + fileName);
		}
		ClipboardFormat format = ClipboardFormats.findByFile(file);
		try {
			ClipboardReader reader = format.getReader(new FileInputStream(file));
			Clipboard clipboard = reader.read();
			return clipboard;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	@SuppressWarnings("deprecation")
	public static EditSession getEditSession(World world) {
		return WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
	}
	
	public static void pasteSchematic(Clipboard clipboard, EditSession session, Location loc) {
		Operation operation = new ClipboardHolder(clipboard).createPaste(session)
				.to(BlockVector3.at(loc.getBlockX(), loc.getBlockY() + clipboard.getHeight(), loc.getBlockZ()))
				.ignoreAirBlocks(true).build();
		try {
			Operations.complete(operation);
		} catch (WorldEditException e) {
			e.printStackTrace();
		}
		session.close();
	}
}
