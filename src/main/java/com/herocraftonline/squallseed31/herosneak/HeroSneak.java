package com.herocraftonline.squallseed31.herosneak;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class HeroSneak extends JavaPlugin
{
	protected ArrayList<Player> sneakingPlayers = new ArrayList<Player>();
	private static Timer refreshTimer = null;
	
	//Plugin variables
	private final HeroSneakListener listener = new HeroSneakListener(this);
	private PluginDescriptionFile pdfFile;
	private String name;
	private String version;
	
	private static final Logger log = Logger.getLogger("Minecraft");
    private static PermissionHandler Permissions = null;
	
	//Configuration variables
	private Configuration config;

	private String sneakOnMessage;
	private String sneakOffMessage;
	private String permissionSystem;
	private int refreshInterval;
	private boolean opsAutoSneak;
	private List<String> canAutosneak;
	private List<String> canSneak;
	
	//Set debugging true to see debug messages
	public static final Boolean debugging = false;
  
  public void onEnable()
  {
	    this.config = getConfiguration();
		pdfFile = getDescription();
		name = pdfFile.getName();
		version = pdfFile.getVersion();
		
	    PluginManager pm = getServer().getPluginManager();
	    pm.registerEvent(Type.PLAYER_JOIN, this.listener, Priority.Monitor, this);
	    pm.registerEvent(Type.PLAYER_QUIT, this.listener, Priority.Monitor, this);
	    try {
	        pm.registerEvent(Type.PLAYER_TOGGLE_SNEAK, this.listener, Priority.Highest, this);
	        pm.registerEvent(Type.PLAYER_RESPAWN, this.listener, Priority.Highest, this);
	    } catch (NoSuchFieldError e) {
	    	log.severe("[" + name + "] PLAYER_TOGGLE_SNEAK unsupported in this version of CraftBukkit! Disabling plugin.");
	    	if (debugging) e.printStackTrace();
	    	pm.disablePlugin(this);
	    }

    //Start config
    sneakOnMessage = this.config.getString("messages.sneakOn", "&7You are now sneaking.").replace("&", "\u00A7");
    sneakOffMessage = this.config.getString("messages.sneakOff", "&7You are no longer sneaking.").replace("&", "\u00A7");
    permissionSystem = this.config.getString("options.permissions", "permissions");
    opsAutoSneak = this.config.getBoolean("options.opsAutoSneak", false);
    refreshInterval = this.config.getInt("options.refreshInterval", 5);
    if (permissionSystem.equalsIgnoreCase("config")) {
    	canAutosneak = config.getStringList("permissions.autosneak", null);
    	canSneak = config.getStringList("permissions.sneak", null);
    } else if (permissionSystem.equalsIgnoreCase("permissions"))
    	setupPermissions();
    else if (permissionSystem.equalsIgnoreCase("none")) {
    	//Do nothing; this value is acceptable now.
    }
    else
    	permissionSystem = "Ops";
    //End config
    
	saveConfig();
    setupAutosneak();
    setupRefresh();

    String strEnable = "[" + name + "] " + version + " enabled.";
    log.info(strEnable);
 }
  
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
	  if (!(sender instanceof Player)) return false;
	  Player player = (Player)sender;

	  if (!hasPermission(player, "herosneak.sneak"))
		  return true;
	  if (args.length > 1)
		  return false;
      if(args.length == 0)
          toggleSneak(player);
      else {
    	  if (args[0].equalsIgnoreCase("on"))
    		  setSneak(player, true);
    	  else if (args[0].equalsIgnoreCase("off"))
    		  setSneak(player, false);
    	  else
    		  return false;
      }
      return true;
  }
  
  private void setupAutosneak() {
	  for (Player p : getServer().getOnlinePlayers()) {
			if (hasPermission(p, "herosneak.auto")) {
				p.setSneaking(true);
				sneakingPlayers.add(p);
			}
	  }
  }
  
  private void toggleSneak(Player player){
      if (sneakingPlayers.contains(player))
          setSneak(player, false);
      else
          setSneak(player, true);
  }
  
  private void setSneak(Player player, boolean sneak){
      if (sneak) {
          player.setSneaking(true);
    	  player.sendMessage(sneakOnMessage);
          if(!sneakingPlayers.contains(player))
        	  sneakingPlayers.add(player);
      } else{
          player.setSneaking(false);
          player.sendMessage(sneakOffMessage);
          if(sneakingPlayers.contains(player))
        	  sneakingPlayers.remove(player);
      }
  }
  
  private void setupRefresh() {
	    if (refreshInterval != 0) {
	      refreshTimer = new Timer();
	      refreshTimer.scheduleAtFixedRate(new TimerTask() {
	        public void run() {
	        	if (!sneakingPlayers.isEmpty()) {
	        		for (Player p : sneakingPlayers) {
	        			p.setSneaking(false);
	        			p.setSneaking(true);
	        		}
	        	}
	        }
	      }
	      , 500L, refreshInterval * 1000L);
	    }
	  }
  
  //This method is the default API hook for Permissions
  public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
		if(Permissions == null) {
		    if(test != null) {
		    	if (!test.isEnabled())
		    		this.getServer().getPluginManager().enablePlugin(test);
				Permissions = ((Permissions)test).getHandler();
			} else {
				log.info("[" + this.name + "]" + " Permission system not enabled. Defaulting to ops only.");
				permissionSystem = "Ops";
		    }
		}
  }

  //Permissions system check
  public boolean hasPermission(Player p, String permission) {
	  debug(p.getName() + " requested permission " + permission);
	  //Added support for "none" permission system to grant access to all
	  if (permissionSystem.equalsIgnoreCase("none") && (permission.equalsIgnoreCase("herosneak.sneak") || opsAutoSneak))
		  return true;
	  //Ops always win
	  if (p.isOp() && (permission.equalsIgnoreCase("herosneak.sneak") || opsAutoSneak))
		  return true;
	  //If using Nijikokun's Permissions, do a Permissions check
	  if (permissionSystem.equalsIgnoreCase("permissions") && Permissions.has(p, permission)) {
		  if (permission.equalsIgnoreCase("herosneak.auto") && Permissions.has(p, "*") && !opsAutoSneak)
			  return false;
		  else
			  return true;
	  }
	  //If using config.yml definitions, iterate over the list (for case insensitivity)
	  if (permissionSystem.equalsIgnoreCase("config")) {
		  if (permission.equalsIgnoreCase("herosneak.auto")) {
			  if (canAutosneak != null) {
				  Iterator<String> it = canAutosneak.iterator();
				  while (it.hasNext()) {
					  if (it.next().equalsIgnoreCase(p.getName()))
						  return true;
				  }
			  }
		  } else if (permission.equalsIgnoreCase("herosneak.sneak")) {
			  if (canSneak != null) {
				  Iterator<String> it = canSneak.iterator();
				  while (it.hasNext()) {
					  if (it.next().equalsIgnoreCase(p.getName()))
						  return true;
				  }
			  }
		  }
	  }
	  return false;
  }
  
  //Creates config.yml if it doesn't exist, initializes with default data
  //If config.yml does exist, writes back data that was read
  public void saveConfig() {
	  this.config.setProperty("messages.sneakOn", sneakOnMessage.replace("\u00A7", "&"));
	  this.config.setProperty("messages.sneakOff", sneakOffMessage.replace("\u00A7", "&"));
	  this.config.setProperty("options.permissions", permissionSystem);
	  this.config.setProperty("options.opsAutoSneak", opsAutoSneak);
	  this.config.setProperty("options.refreshInterval", refreshInterval);
	  this.config.setProperty("permissions.sneak", canSneak);
	  this.config.setProperty("permissions.autosneak", canAutosneak);
	  this.config.save();
  }
  
  public static void debug(String message) {
	  if (debugging) {
		  log.info(message);
	  }
  }
  
  public void onDisable()
  {
    String strDisable = "[" + name + "] " + version + " disabled.";
	    log.info(strDisable);
  }
  }