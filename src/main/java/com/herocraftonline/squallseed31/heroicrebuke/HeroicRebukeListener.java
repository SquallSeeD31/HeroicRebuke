package com.herocraftonline.squallseed31.heroicrebuke;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Location;
import java.util.HashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class HeroicRebukeListener implements Listener {

    private final HeroicRebuke plugin;
    public static HashMap<Player, Location> rootLocations = new HashMap<Player, Location>();
    private static HashMap<Player, Long> nextinform = new HashMap<Player, Long>();

    public HeroicRebukeListener(HeroicRebuke instance) {
        plugin = instance;
    }

    public void rootPlayer(Player p) {
        rootLocations.put(p, p.getLocation());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.blockMove) {
            if (rootLocations.containsKey(event.getPlayer())) {
                Location from = rootLocations.get(event.getPlayer());
                if (event.getTo() != from) {
                    event.setTo(from);
                    warnMove(event.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.blockMove) {
            if (rootLocations.containsKey(event.getPlayer())) {
                Location from = rootLocations.get(event.getPlayer());
                double deltaX = Math.abs(from.getX() - event.getTo().getX());
                double deltaY = Math.abs(from.getY() - event.getTo().getY());
                double deltaZ = Math.abs(from.getZ() - event.getTo().getZ());
                if (deltaX > 1.5 || deltaY > 1.5 || deltaZ > 1.5) {
                    HeroicRebuke.debug("From: " + from.toString() + " To: " + event.getTo().toString());
                    event.setTo(from);
                    warnMove(event.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (HeroicRebuke.warnings.containsKey(p.getName().toLowerCase())) {
            if (!rootLocations.containsKey(p)) {
                rootPlayer(p);
            }
            plugin.sendWarning(p, HeroicRebuke.warnings.get(p.getName().toLowerCase()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.blockMove) {
            if (rootLocations.containsKey(event.getPlayer())) {
                event.setCancelled(true);
                warnMove(event.getPlayer());
            }
        }
    }

    private void warnMove(Player player) {
        if (nextinform.containsKey(player)) {
            if (System.currentTimeMillis() < nextinform.get(player) * 1000) { // not yet
                return;
            } else { // need to notify
                nextinform.remove(player);
            }
        }
        nextinform.put(player, (System.currentTimeMillis() / 1000) + 15);
        player.sendMessage("Movement disabled! Say /warn list");
    }
}