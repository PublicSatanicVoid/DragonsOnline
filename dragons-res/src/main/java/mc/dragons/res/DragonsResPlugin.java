package mc.dragons.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.schematic.MCEditSchematicFormat;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.res.ResLoader.ResPoint;

@SuppressWarnings("deprecation")
public class DragonsResPlugin extends JavaPlugin implements CommandExecutor {
	
	public static final int MAX_RES_PER_USER = 2;
	
	public void onEnable() {
		
		resetResWorld();
		
		Dragons.getInstance().getUserHookRegistry().registerHook(new ResUserHook());
		
		if(Dragons.getInstance().isDebug()) {
			getLogger().info("Setting logger to trace mode because of DragonsCore setting");
			getLogger().setLevel(Level.FINEST);
		}
		
		getServer().getPluginManager().registerEvents(new ResEvents(), this);
		
		ResCommands resCommands = new ResCommands();
		getCommand("res").setExecutor(resCommands);
		getCommand("resadmin").setExecutor(resCommands);
		getCommand("restest").setExecutor(resCommands);
		getCommand("testdoorplacement").setExecutor(resCommands);
		getCommand("testschematic").setExecutor(resCommands);
		
		getLogger().info("Loading holograms for res points...");
		for(ResPoint resPoint : ResLoader.getAllResPoints()) {
			ResLoader.createResPointHologram(resPoint);
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
				.append("levelMin", 0));
		Floor resFloor = new Floor(lsm, lsa, true);
		FloorLoader.link(Bukkit.getWorld("res_temp"), resFloor);
		Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects().add(resFloor);
	}

	public static Clipboard loadSchematic(String fileName, World forWorld) {
		File file = new File("plugins/WorldEdit/schematics/" + fileName + ".schematic");
		if(!file.exists()) {
			System.err.println("[DragonsRes] Couldn't find schematic file " + fileName);
		}
		ClipboardFormat format = ClipboardFormat.findByFile(file);
		try {
			ClipboardReader reader = format.getReader(new FileInputStream(file));
		   Clipboard clipboard = reader.read(forWorld.getWorldData());
		   return clipboard;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
	
	public static EditSession getEditSession(World world) {
		return WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
	}
	
	public static void pasteSchematic(String schematic, EditSession session, Location loc) {
		session.enableQueue();
		try {
			MCEditSchematicFormat.getFormat(new File("")).load(new File("")).paste(session, new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), false);
		} catch (MaxChangedBlocksException | DataException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		session.enableQueue();
	}

	public static void pasteSchematic(Clipboard schematic, EditSession session, Location loc) {
		session.enableQueue();
		WorldData worldData = new BukkitWorld(loc.getWorld()).getWorldData();
		try {
			Operation operation = new ClipboardHolder(schematic, worldData)
					.createPaste(session, worldData)
					.to(BlockVector.toBlockPoint(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
					.ignoreAirBlocks(false)
					.build();
			Operations.complete(operation);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		session.disableQueue();
	}
}
