package com.herocraftonline.squallseed31.heroicrebuke;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class HeroicRebukeDatasource {

    protected static Connection connection;
    protected static HeroicRebuke plugin;
    protected static final Logger log = Logger.getLogger("Minecraft");

    public synchronized Connection getConnection() {
        if (connection == null) {
            connection = createConnection();
            if (connection == null) {
                HeroicRebuke.useDB = false;
            }
        }
        return connection;
    }

    protected abstract Connection createConnection();

    public abstract void initDB();

    public abstract int newWarning(Warning w);

    public String delWarning(Integer id) {
        String result = null;
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps1 = conn.prepareStatement("SELECT `to` FROM `warnings` WHERE `id` = ?");
            ps1.setInt(1, id);
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `warnings` WHERE `id` = ?");
                ps2.setInt(1, id);
                ps2.executeUpdate();
                result = rs.getString("to");
            }
            conn.commit();
        } catch (SQLException e) {
            log.log(Level.SEVERE, MessageFormat.format("[HeroicRebuke] Warning deletion error: {1}", e));
        }
        return result;
    }

    public void ackWarning(String to) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("UPDATE `warnings` SET `ack` = '1', `ack_time` = CURRENT_TIMESTAMP WHERE `ack` = '0' AND `to` LIKE ?");
            ps.setString(1, to);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.log(Level.SEVERE, MessageFormat.format("[HeroicRebuke] Warning acknowledge error: {0}", e));
        }
    }

    public void clearWarning(String to) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `warnings` WHERE `ack` = '0' AND `to` LIKE ?");
            ps2.setString(1, to);
            ps2.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            log.log(Level.SEVERE, MessageFormat.format("[HeroicRebuke] Warning clear error: {0}", e));
        }
    }

    public abstract int countWarnings(String player);

    public abstract void loadWarnings();

    public abstract Warning getWarning(int index);

    public abstract ArrayList<String> listWarnings(String to);
}