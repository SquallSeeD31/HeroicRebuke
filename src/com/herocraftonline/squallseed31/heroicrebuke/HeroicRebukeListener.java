package com.herocraftonline.squallseed31.heroicrebuke;

import org.bukkit.entity.Player; 
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener; 
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import java.util.HashMap;

public class HeroicRebukeListener extends PlayerListener { 
 private final HeroicRebuke plugin; 
 public static HashMap<Player, Location> rootLocations = new HashMap<Player, Location>();

 public HeroicRebukeListener(HeroicRebuke instance) { 
	 plugin = instance; 
 }
 
 public void rootPlayer(Player p) {
	 rootLocations.put(p, p.getLocation());
 }
 
 public void onPlayerMove(PlayerMoveEvent event) {
	 if (plugin.blockMove) {
		 if (rootLocations.containsKey(event.getPlayer())) {
			 Location from = rootLocations.get(event.getPlayer());
			 if (event.getTo() != from) {
			     event.setCancelled(true);
			     event.getPlayer().teleportTo(from);				 
			 }
		 }
	 }
 }
 
 public void onPlayerTeleport(PlayerMoveEvent event) {
	 if (plugin.blockMove) {
	     if (rootLocations.containsKey(event.getPlayer())) {
	    	 Location from = rootLocations.get(event.getPlayer());
			 double deltaX = Math.abs(from.getX() - event.getTo().getX());
			 double deltaY = Math.abs(from.getY() - event.getTo().getY());
			 double deltaZ = Math.abs(from.getZ() - event.getTo().getZ());
			 if (deltaX > 1.5 || deltaY > 1.5 || deltaZ > 1.5) {
	    		 HeroicRebuke.debug("From: " + from.toString() + " To: " + event.getTo().toString());
				 event.getPlayer().teleportTo(from);
				 event.setCancelled(true);
		     }
		 }
	 }
 }
 
 public void onPlayerJoin(PlayerEvent event) {
	 Player p = event.getPlayer();
	 if (HeroicRebuke.warnings.containsKey(p.getName().toLowerCase())) {
		 if (!rootLocations.containsKey(p))
			 rootPlayer(p);
		 plugin.sendWarning(p, HeroicRebuke.warnings.get(p.getName().toLowerCase()));
	 }
 }
}