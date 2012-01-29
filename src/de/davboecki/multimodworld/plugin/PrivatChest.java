package de.davboecki.multimodworld.plugin;

import net.minecraft.server.BaseMod;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ModLoaderMp;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.generator.*;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.davboecki.multimodworld.plugin.commandhandler.CommandHandler;
import de.davboecki.multimodworld.plugin.commandhandler.ConfirmListener;
import de.davboecki.multimodworld.plugin.handitemhelper.HandItemThread;
import de.davboecki.multimodworld.plugin.listener.*;
import de.davboecki.multimodworld.plugin.reteleport.ReTeleportThread;
import de.davboecki.multimodworld.plugin.reteleport.TeleportHandler;
import de.davboecki.multimodworld.plugin.yaml.MyYamlConstructor;
import de.davboecki.multimodworld.server.plugin.IModWorldHandlePlugin;
import de.davboecki.multimodworld.plugin.settings.ExchangeWorldSetting;
import de.davboecki.multimodworld.plugin.settings.Settings;
import de.davboecki.multimodworld.plugin.settings.WorldSetting;


public class PrivatChest extends JavaPlugin {
	
    public FlatWorldGenerator Worldgen = new FlatWorldGenerator();
    public PlayerPositionCheck PlayerPositionCheck = new PlayerPositionCheck(this);
    
    public Logger log = Logger.getLogger("Minecraft");
    public final PrivatChestPlayerListener PrivatChestPlayerListener = new PrivatChestPlayerListener(this);
    private final CreatueSpawnListener CreatueSpawnListener = new CreatueSpawnListener(this);
    private final PrivatChestBlockListener PrivatChestBlockListener = new PrivatChestBlockListener(this);
    private final WorldLoadListener WorldLoadListener = new WorldLoadListener(this);
    public final PlayerModPacketListener PlayerModPacketListener = new PlayerModPacketListener(this);
    private final FurnaceListener FurnaceListener = new FurnaceListener(this);
    private final PlayerPreCommandListener PlayerPreCommandListener = new PlayerPreCommandListener(this);
    public final ConfirmListener confirmlistener = new ConfirmListener(this);
    public RoomControler RoomControl = new RoomControler(this);
    public ArrayList<String> ModPacketOK = new ArrayList<String>();
    public Settings Settings = new Settings(this);
    private Yaml yaml;
    private CommandHandler commandhandler = null;
    public MultiModWorld MultiModWorld = null;
    public TeleportHandler teleporthandler = new TeleportHandler(this);
    public PacketListener PacketListener = new PacketListener(this);
    
    
    //Debug Mode
    private static boolean debug = false;
    public static boolean debug(){
    	return debug;
    }
    
    public static void setdebug(Boolean flag){
    	debug = flag;
    }
    //Debug Mode
    
    public Settings getSettings(){
    	return Settings;
    }
    
    public void onEnable() {
    	
    	boolean flag = false;
		try {
			Class HandleInterface = Class.forName("de.davboecki.multimodworld.server.plugin.IModWorldHandlePlugin");
			if(HandleInterface != null){
				flag = true;
			}
		} catch (Exception e) {}
		if(flag) {
	        log.info("[PrivatChest] IModWorldHandlePlugin class found!");
			MultiModWorld = new MultiModWorld(this);
		}
    	
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_JOIN, PrivatChestPlayerListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_RESPAWN, PrivatChestPlayerListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_MOVE, PrivatChestPlayerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, PrivatChestPlayerListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, PrivatChestPlayerListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, PrivatChestPlayerListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, PrivatChestPlayerListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.CREATURE_SPAWN, CreatueSpawnListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_PLACE, PrivatChestBlockListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, PrivatChestBlockListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.WORLD_LOAD, WorldLoadListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.CUSTOM_EVENT, PlayerModPacketListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.FURNACE_BURN, FurnaceListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.FURNACE_SMELT, FurnaceListener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, PlayerPreCommandListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_CHAT, confirmlistener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, confirmlistener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.CUSTOM_EVENT, PacketListener, Event.Priority.Normal, this);
        Settings.load();

        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new ReTeleportThread(this), 1, 1);
        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new HandItemThread(this), 1, 1);
        
        commandhandler = new CommandHandler(this);
        
        log.info("[PrivatChest] Plugin v"+this.getDescription().getVersion()+" has been enabled!");
        if(MultiModWorld != null) {
        	String ModVersion = de.davboecki.multimodworld.server.ModChecker.getVerion();
        	String MultiModWorldVersion = "v1.1.0";
        	if(!ModVersion.equalsIgnoreCase(MultiModWorldVersion)) {
        		this.getServer().getPluginManager().disablePlugin(this);
                log.info("[MultiModWorld] Mod "+ModVersion+" has been found. But "+MultiModWorldVersion+" is required.");
        	} else {
        		log.info("[MultiModWorld] Mod "+ModVersion+" has been enabled.");
        	}
        }
    }

    public void save(){Settings.save();}
    public void load(){Settings.load();}
    
    public void onDisable() {
    	Settings.save();
        log.info("[PrivatChest] Plugin v"+this.getDescription().getVersion()+" has been disabled.");
    }
    
    public void sendModLoaderPacket(Player player){
    	PacketListener.AllowModLoaderPacket = true;
    	ModLoaderMp.HandleAllLogins(((CraftPlayer) player).getHandle());
    	PacketListener.AllowModLoaderPacket = false;
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("multimodworld") || label.equalsIgnoreCase("mmw") || label.equalsIgnoreCase("privatchest")) {

        	//Op-only
        	if(!sender.isOp()) {
        		sender.sendMessage(ChatColor.RED+"You don't have permission to do that.");
        		return true;
        	}
        	
        	boolean flag = commandhandler.HandleMMWCommand(sender, args);
        	MultiModWorld.cachereset(); //Reset Cache
        	if(!flag && sender instanceof Player){
        		commandhandler.displayhelp(sender,true);
        		return true;
        	}
        	return flag;
        } else {
        	return false;
        }
    }
    
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
	log.info("[PrivatChest] WorldName: "+worldName);
    	if(!Settings.ExchangeWorlds.containsKey(worldName)){
    		log.info("[PrivatChest] New WorldName: "+worldName);
    		Settings.ExchangeWorlds.put(worldName,new ExchangeWorldSetting());
    		Settings.ExchangeWorlds.get(worldName).WorldType = "Creative";
    		Settings.save();
    	}
        return Worldgen;
    }

}
