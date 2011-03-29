package com.herocraftonline.squallseed31.heroicrebuke;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.bukkit.entity.Player;

public class HeroicRebukeSQLite extends HeroicRebukeDatasource
{

  public HeroicRebukeSQLite(HeroicRebuke instance) { 
		 plugin = instance; 
		 connection = getConnection();
	 }

  protected Connection createConnection()
  {
    try
    {
      Class.forName("org.sqlite.JDBC");
      Connection conn = DriverManager.getConnection("jdbc:sqlite:heroicRebuke.db");
      return conn;
    } catch (ClassNotFoundException e) {
      HeroicRebuke.log.severe("[HeroicRebuke] SQLite connector not found! Is 'sqlitejdbc-v056.jar' in /lib?");
    } catch (SQLException e) {
      HeroicRebuke.log.severe("[HeroicRebuke] Error connecting to SQLite Database: " + e.getMessage());
    }
    return null;
  }

  public void initDB() {
	try {
      Connection conn = getConnection();
      conn.setAutoCommit(false);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `warnings` (`id` INTEGER PRIMARY KEY,`to` VARCHAR(32) NOT NULL,`from` VARCHAR(32) NOT NULL,`message` VARCHAR(255) NOT NULL,`ack` BOOLEAN NOT NULL DEFAULT '0',`send_time` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,`ack_time` INTEGER,`code` VARCHAR(6))");
      stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `warned` ON `warnings` (`to`)");
      conn.commit();
      loadWarnings();
    } catch (SQLException e) {
      log.severe("[" + plugin.name + "] Table creation error: " + e);
    }
  }

  public int newWarning(Warning w) {
	int index = -1;
	try {
      Connection conn = getConnection();
      conn.setAutoCommit(false);
      PreparedStatement ps = conn.prepareStatement("INSERT INTO `warnings` (`to`, `from`, `message`, `code`) VALUES (?,?,?,?)");
      ps.setString(1, w.getTarget());
      ps.setString(2, w.getSender());
      ps.setString(3, w.getMessage());
      ps.setString(4, w.getCode());
      ps.executeUpdate();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
      if (rs.next())
          index = rs.getInt(1);
      conn.commit();
    } catch (SQLException e) {
      log.severe("[" + plugin.name + "] Warning creation error: " + e);
    }
    return index;
  }
  
  public void loadWarnings() {
	  try {
		  Connection conn = getConnection();
		  Statement stmt = conn.createStatement();
		  ResultSet rs = stmt.executeQuery("SELECT `id`,`to`,`from`,`message`,`ack`,strftime('%s',`send_time`),strftime('%s',`ack_time`),`code` FROM `warnings` WHERE `ack` = '0'");
		  int i = 0;
		  while (rs.next()) {
			  Warning w = new Warning(rs.getInt("id"),rs.getString("to"),rs.getString("from"),rs.getString("message"),rs.getBoolean("ack"),rs.getLong(6)*1000,rs.getLong(7)*1000,rs.getString("code"));
			  HeroicRebuke.warnings.put(rs.getString("to").toLowerCase(), w);
			  Player p = plugin.getServer().getPlayer(rs.getString("to"));
			  if (p != null && !HeroicRebukeListener.rootLocations.containsKey(p))
				  HeroicRebukeListener.rootLocations.put(p, p.getLocation());
			  HeroicRebuke.debug("Loaded Warning: " + w.toString());
			  i++;
		  }
		  conn.commit();
		  log.info("[" + plugin.name + "] Loaded " + i + " active warning" + (i==1 ? "." : "s."));
	  } catch (SQLException e) {
	      log.severe("[" + plugin.name + "] Warning load error: " + e);
	  }
  }
  
  public Warning getWarning(int index) {
	  Warning w = null;
	  try {
		  Connection conn = getConnection();
		  PreparedStatement ps = conn.prepareStatement("SELECT `id`,`to`,`from`,`message`,`ack`,strftime('%s',`send_time`),strftime('%s',`ack_time`),`code` FROM `warnings` WHERE `id` = ?");
		  ps.setInt(1, index);
		  ResultSet rs = ps.executeQuery();
		  while (rs.next()) {
			  w = new Warning(rs.getInt("id"),rs.getString("to"),rs.getString("from"),rs.getString("message"),rs.getBoolean("ack"),rs.getLong(6)*1000,rs.getLong(7)*1000,rs.getString("code"));
		  }
		  conn.commit();
	  } catch (SQLException e) {
	      log.severe("[" + plugin.name + "] Warning get error: " + e);
	  }
	  return w;
  }
  
  public ArrayList<String> listWarnings(String to) {
	  ArrayList<String> output = new ArrayList<String>();
	  try {
		  Connection conn = getConnection();
		  if (to.indexOf(";") != -1)
			  return null;
		  PreparedStatement ps = conn.prepareStatement("SELECT `id`,`to`,`from`,`message`,`ack`,strftime('%s',`send_time`),strftime('%s',`ack_time`),`code` FROM `warnings` WHERE `to` LIKE '%"+to+"%' ORDER BY `id` ASC");
		  ResultSet rs = ps.executeQuery();
		  while (rs.next()) {
			  String send_time = plugin.getFormatTime(rs.getLong(6) * 1000);
			  String ack_time = plugin.getFormatTime(rs.getLong(7) * 1000);
			  String buildLine = plugin.messageColor + "[" + plugin.infoColor + rs.getInt("id") + plugin.messageColor + "] " + plugin.infoColor + send_time + plugin.messageColor + " From: " + plugin.nameColor + rs.getString("from") + plugin.messageColor + 
			  			   " To: " + plugin.nameColor + rs.getString("to");
			  if (rs.getBoolean("ack"))
				  buildLine += plugin.infoColor + " *ACK* " + plugin.messageColor + "At: " + plugin.infoColor + ack_time;
			  output.add(buildLine);
		  }
		  conn.commit();
	  } catch (SQLException e) {
	      log.severe("[" + plugin.name + "] Warning load error: " + e);
	  }
	  return output;
  }

	public int countWarnings(String player) {
		  int result = -1;
		  try {
			  Connection conn = getConnection();
			  PreparedStatement ps = conn.prepareStatement("SELECT count(`id`) FROM `warnings` WHERE `to` LIKE ?");
			  ps.setString(1, player);
			  ResultSet rs = ps.executeQuery();
			  if (rs.next()) {
				  result = rs.getInt(1);
			  }
			  conn.commit();
		  } catch (SQLException e) {
		      log.severe("[" + plugin.name + "] Warning count error: " + e);
		  }
		  return result;
	}
}