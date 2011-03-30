package com.herocraftonline.squallseed31.herosneak;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class HeroSneakListener extends PlayerListener {
    private final HeroSneak plugin; 

    public HeroSneakListener(HeroSneak instance) { 
    	plugin = instance;
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
		if (plugin.hasPermission(event.getPlayer(), "herosneak.auto")) {
			event.getPlayer().setSneaking(true);
			plugin.sneakingPlayers.add(event.getPlayer());
		}
    }

	public void onPlayerQuit(PlayerQuitEvent event){
        if(plugin.sneakingPlayers.contains(event.getPlayer()))
        	plugin.sneakingPlayers.remove(event.getPlayer());
	}

	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!plugin.sneakingPlayers.isEmpty() && plugin.sneakingPlayers.contains(event.getPlayer())) {
	        if(plugin.sneakingPlayers.contains(event.getPlayer()))
	        	plugin.sneakingPlayers.remove(event.getPlayer());
		}
	}

	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if(!plugin.sneakingPlayers.isEmpty() && plugin.sneakingPlayers.contains(event.getPlayer())) {
        	event.getPlayer().setSneaking(true);
        	event.setCancelled(true);
       }
    }
}