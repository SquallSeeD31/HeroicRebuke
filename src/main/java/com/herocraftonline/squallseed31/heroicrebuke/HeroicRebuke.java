package com.herocraftonline.squallseed31.heroicrebuke;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HeroicRebuke extends JavaPlugin {
    //Data!

    public static HashMap<String, Warning> warnings;
    public static HashMap<String, ArrayList<String>> lists;
    //Holder for incrementing index number when not using a database
    public int noDatabaseIndex = 1;
    //Plugin variables
    private final HeroicRebukeListener listener = new HeroicRebukeListener(this);
    private HeroicRebukeDatasource database;
    public PluginDescriptionFile pdfFile;
    public String name;
    public String version;
    public File dataFolder;
    public static final Logger log = Logger.getLogger("Minecraft");
    //Configuration variables
    //public Configuration config;
    private static String maindir = "plugins/HeroicRebuke/";
    private final File configfile = new File(maindir + "config.yml");
    public RandomString codeGen;
    public String timestampFormat;
    public String consoleSender = "SERVER";
    public String messageColor = ChatColor.RED.toString();
    public String nameColor = ChatColor.DARK_AQUA.toString();
    public String infoColor = ChatColor.GOLD.toString();
    public boolean blockMove;
    public static boolean useDB;
    public boolean useCode;
    public boolean onlyWarnOnline;
    public boolean canAcknowledge;
    public String permissionSystem;
    public List<String> rebukeAdmins;
    public int maxPerPage;
    public int codeLength;
    public String mySqlDir;
    public String mySqlUser;
    public String mySqlPass;
    public String dbType;
    public boolean useBan;
    public int banThreshold;
    public String banMessage;
    //Set debugging true to see debug messages
    public static final Boolean debugging = false;

    public void onEnable() {
        //this.config = getConfiguration();
        // Load config
        new File(maindir).mkdir();
        if (!configfile.exists()) {
            try {
                configfile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(HeroicRebuke.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            this.getConfig().load(configfile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HeroicRebuke.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HeroicRebuke.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidConfigurationException ex) {
            Logger.getLogger(HeroicRebuke.class.getName()).log(Level.SEVERE, null, ex);
        }


        this.blockMove = true;
        warnings = new HashMap<String, Warning>();
        lists = new HashMap<String, ArrayList<String>>();
        pdfFile = getDescription();
        name = pdfFile.getName();
        version = pdfFile.getVersion();
        dataFolder = getDataFolder();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_TELEPORT, this.listener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_MOVE, this.listener, Event.Priority.Highest, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, this.listener, Event.Priority.Monitor, this);

        //Start config
        messageColor = getConfigColor("colors.message", "RED");
        nameColor = getConfigColor("colors.name", "DARK_AQUA");
        infoColor = getConfigColor("colors.info", "GOLD");
        timestampFormat = this.getConfig().getString("options.timeformat", "MM/dd/yyyy HH:mm:ss z");
        permissionSystem = this.getConfig().getString("options.permissions", "Permissions");
// Doesn't work with new config system
//        if (permissionSystem.equalsIgnoreCase("config")) {
//            rebukeAdmins = this.getConfig().getStringList("admins", null);
//        }
        useCode = this.getConfig().getBoolean("options.code.use", true);
        useCode = this.getConfig().getBoolean("options.code.use", true);
        codeLength = this.getConfig().getInt("options.code.length", 6);
        canAcknowledge = this.getConfig().getBoolean("options.canAcknowledge", true);
        consoleSender = this.getConfig().getString("options.server_name", "SERVER");
        blockMove = this.getConfig().getBoolean("options.block_move", true);
        onlyWarnOnline = this.getConfig().getBoolean("options.only_warn_online", true);
        maxPerPage = this.getConfig().getInt("options.lines_per_page", 5);
        mySqlDir = this.getConfig().getString("options.mysql.location", "localhost:3306/HeroicRebuke");
        mySqlUser = this.getConfig().getString("options.mysql.username", "root");
        mySqlPass = this.getConfig().getString("options.mysql.password", "");
        useBan = this.getConfig().getBoolean("options.ban.enable", false);
        banThreshold = this.getConfig().getInt("options.ban.threshold", 5);
        banMessage = this.getConfig().getString("options.ban.message", "[HeroicRebuke] Banned for cumulative violations!");
        //End config

        dbType = this.getConfig().getString("options.database", "sqlite");
        if (dbType.equalsIgnoreCase("sqlite") || dbType.equalsIgnoreCase("true")) {
            useDB = true;
            database = new HeroicRebukeSQLite(this);
        } else if (dbType.equalsIgnoreCase("mysql")) {
            useDB = true;
            database = new HeroicRebukeMySQL(this);
        } else {
            useDB = false;
        }

        codeGen = new RandomString(codeLength);
        if (permissionSystem.equalsIgnoreCase("permissions")) {
            setupPermissions();
        }
        if (useDB) {
            database.initDB();
        } else {
            log.info("[" + name + "] No database enabled, warnings will not persist.");
        }

        saveConfig();
        String strEnable = "[" + name + "] " + version + " enabled.";
        log.info(strEnable);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = null;
        String senderName = null;
        Warning isWarned = null;

        //onCommand supports console sender, so we have to cast player
        if (sender instanceof Player) {
            player = (Player) sender;
            senderName = player.getName();
            isWarned = warnings.get(senderName.toLowerCase());
        } else {
            senderName = consoleSender;
        }

        //If someone is warned and they type /warn, show them their warning
        if (args.length < 1) {
            if (isWarned != null) {
                sendWarning(player, isWarned);
                return true;
            }
            return false;
        }

        //Begin command handlers

        //Add Command
        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.add")) {
                return false;
            }
            if (args.length < 3) {
                sender.sendMessage(messageColor + "Usage: " + infoColor + "/warn add <name> <reason>");
                return true;
            }
            if (warnings.containsKey(args[1].toLowerCase())) {
                sender.sendMessage(nameColor + args[1] + messageColor + " is already being warned by " + nameColor + warnings.get(args[1].toLowerCase()).getSender() + messageColor + ".");
                return true;
            }
            String message = null;
            StringBuffer result = new StringBuffer();
            result.append(args[2]);
            if (args.length > 3) {
                for (int i = 3; i < args.length; i++) {
                    result.append(" ");
                    result.append(args[i]);
                }
            }
            message = result.toString();
            Player p = null;
            String target = args[1];
            Warning w;
            List<Player> pList = getServer().matchPlayer(args[1]);
            if (!pList.isEmpty()) {
                if (pList.size() > 1) {
                    String buildMessage = infoColor + "Error: " + messageColor + "Found multiple players matching " + nameColor + args[1] + messageColor + ": ";
                    Iterator<Player> it = pList.iterator();
                    while (it.hasNext()) {
                        buildMessage += nameColor + it.next().getName() + messageColor + ", ";
                    }
                    buildMessage += "please be more specific.";
                    sender.sendMessage(buildMessage);
                    return true;
                } else {
                    p = pList.get(0);
                    target = p.getName();
                }
            }

            int curWarnings = database.countWarnings(target);
            if (useBan && curWarnings + 1 >= banThreshold) {
                //getServer().getHandle().a(target);

                if (p != null && p.isOnline()) {
                    p.kickPlayer(banMessage);
                }
                sender.sendMessage(nameColor + target + messageColor + " has been banned for cumulative violations.");
                return true;
            }
            if (p == null || !p.isOnline()) {
                if (!onlyWarnOnline) {
                    w = makeWarning(target, senderName, message);
                    sender.sendMessage(nameColor + target + messageColor + " is either offline or not a player, but has been warned.");
                } else {
                    sender.sendMessage(infoColor + "Error: " + nameColor + target + messageColor + " is either offline or not a player!");
                }
                return true;
            }

            w = makeWarning(p.getName(), senderName, message);
            sendWarning(p, w);
            listener.rootPlayer(p);
            sender.sendMessage(nameColor + p.getName() + messageColor + " is online and has been warned.");

            return true;
        }

        //Clear Command
        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.clear")) {
                return false;
            }
            if (args.length < 2) {
                sender.sendMessage(messageColor + "Usage: " + infoColor + "/warn clear <name>");
                return true;
            }
            List<String> matchList = new ArrayList<String>();
            String matchName = null;
            for (String warnKey : warnings.keySet()) {
                if (args[1].equalsIgnoreCase(warnKey)) {
                    matchList.clear();
                    matchList.add(args[1]);
                    break;
                }
                if (warnKey.toLowerCase().indexOf(args[1].toLowerCase()) != -1) {
                    matchList.add(warnKey);
                }
            }
            if (!matchList.isEmpty()) {
                if (matchList.size() > 1) {
                    String buildMessage = infoColor + "Error: " + messageColor + "Found multiple warned players matching " + nameColor + args[1] + messageColor + ": ";
                    Iterator<String> it = matchList.iterator();
                    while (it.hasNext()) {
                        buildMessage += nameColor + warnings.get(it.next().toLowerCase()).getTarget() + messageColor + ", ";
                    }
                    buildMessage += "please be more specific.";
                    sender.sendMessage(buildMessage);
                    return true;
                } else {
                    matchName = matchList.get(0);
                }
            }
            if (matchName == null || !warnings.containsKey(matchName.toLowerCase())) {
                sender.sendMessage(nameColor + args[1] + messageColor + " not found or has no active warnings.");
                return true;
            }


            if (useDB) {
                database.clearWarning(matchName);
            }

            sender.sendMessage(messageColor + "Removed active warning from " + nameColor + warnings.get(matchName.toLowerCase()).getTarget());
            warnRemoval(matchName, senderName);

            return true;
        }

        //Delete command
        if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.delete")) {
                return false;
            }
            if (args.length < 2) {
                sender.sendMessage(messageColor + "Usage: " + infoColor + "/warn delete <index>");
                return true;
            }
            if (!useDB) {
                sender.sendMessage(messageColor + "The delete command is only available when using a database.");
                return true;
            }
            int index = -1;
            try {
                index = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException e) {
                sender.sendMessage(infoColor + "Error: " + messageColor + "Bad number format. " + infoColor + "<index>" + messageColor + " must represent a valid index number.");
                return true;
            }
            Warning w = getFromId(index);
            if (w != null) {
                warnRemoval(w.getTarget(), senderName);
            }

            String result = database.delWarning(index);
            if (result != null) {
                sender.sendMessage(messageColor + "Deleted warning with index [" + infoColor + index + messageColor + "] on player [" + nameColor + result + messageColor + "]");
            } else {
                sender.sendMessage(messageColor + "No warning found with index [" + infoColor + index + messageColor + "]");
            }
            return true;
        }


        //Acknowledge command
        if (args[0].equalsIgnoreCase("ack") || args[0].equalsIgnoreCase("acknowledge")) {
            if (!canAcknowledge) {
                sender.sendMessage(infoColor + "Error: " + messageColor + "You may not acknowledge this warning");
                return true;
            }
            String code = null;
            if (useCode) {
                if (args.length < 2) {
                    sender.sendMessage(messageColor + "Usage: " + infoColor + "/warn acknowledge <code>");
                    return true;
                }
                code = args[1].trim();
            }
            if (player != null) {
                ackWarning(player, code);
            } else {
                sender.sendMessage("The server is above the law.");
            }
            return true;
        }

        //List command
        if (args[0].equalsIgnoreCase("list")) {
            if (isWarned != null) {
                sendWarning(player, isWarned);
                return true;
            }
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.list")) {
                return false;
            }
            String target;
            int page = 1;
            int i = 0;
            if (sender.isOp() || hasPermission(player, "heroicrebuke.list.others")) {
                if (args.length < 2) {
                    target = senderName;
                } else {
                    try {
                        page = Integer.parseInt(args[1].trim());
                        target = senderName;
                    } catch (NumberFormatException e) {
                        target = args[1].trim();
                        if (args.length > 2) {
                            i = 2;
                        }
                    }
                }
            } else {
                target = senderName;
                if (args.length > 1) {
                    i = 1;
                }
            }
            if (i > 0) {
                try {
                    page = Integer.parseInt(args[i].trim());
                } catch (NumberFormatException e) {
                    sender.sendMessage(infoColor + "Error: " + messageColor + "Bad number format. Type " + infoColor + "/warn list" + messageColor + " without a page number to get acceptable range.");
                    return true;
                }
            } else {
                lists.remove(target.toLowerCase());
            }
            if (!useDB) {
                sender.sendMessage(messageColor + "The list command is only available when using a database.");
                return true;
            }
            sendWarningList(target, sender, senderName, page);
            return true;
        }

        //Active command
        if (args[0].equalsIgnoreCase("active")) {
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.active")) {
                return false;
            }
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1].trim());
                } catch (NumberFormatException e) {
                    sender.sendMessage(infoColor + "Error: " + messageColor + "Bad number format. Type " + infoColor + "/warn active" + messageColor + " without a page number to get acceptable range.");
                    return true;
                }
            } else {
                lists.remove(senderName.toLowerCase());
            }
            sendActiveList(sender, senderName, page);
            return true;
        }

        //Info command
        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.isOp() && !hasPermission(player, "heroicrebuke.info")) {
                return false;
            }
            if (args.length < 2) {
                sender.sendMessage(messageColor + "Usage: " + infoColor + "/warn info <index>");
                return true;
            }
            int index = -1;
            try {
                index = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException e) {
                sender.sendMessage(infoColor + "Error: " + messageColor + "Bad number format.");
                return true;
            } finally {
                if (index < 1) {
                    return false;
                }
            }
            Warning w = getFromId(index);
            if (w == null && !useDB) {
                sender.sendMessage(messageColor + "No warning found with index [" + infoColor + index + messageColor + "]");
                return true;
            }
            if (useDB) {
                w = database.getWarning(index);
            }
            if (w == null) {
                sender.sendMessage(messageColor + "No warning found with index [" + infoColor + index + messageColor + "]");
                return true;
            }
            String send_time = getFormatTime(w.getSendTime());
            String ack_time = getFormatTime(w.getAckTime());
            String buildLine = messageColor + "[" + infoColor + w.getId() + messageColor + "] " + infoColor + send_time + messageColor + " From: " + nameColor + w.getSender()
                    + messageColor + " To: " + nameColor + w.getTarget();
            if (useCode) {
                buildLine += messageColor + " Code: " + infoColor + w.getCode();
            }
            if (w.isAcknowledged()) {
                buildLine += infoColor + " *ACK* " + messageColor + "At: " + infoColor + ack_time;
            }
            sender.sendMessage(buildLine);
            sender.sendMessage(messageColor + "Message: " + w.getMessage());
            return true;
        }

        //Help command
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(infoColor + "===HeroicRebuke Commands===");
            if (!sender.isOp()) {
                sender.sendMessage(infoColor + "/warn acknowledge " + (useCode ? "(code) " : " ") + messageColor + "- Clears your active warning" + (useCode ? ". Requires " + infoColor + "(code)" + messageColor + " from the warning" : ""));
            }
            if (hasPermission(player, "heroicrebuke.add")) {
                sender.sendMessage(infoColor + "/warn add <name> <reason>" + messageColor + " - Warn " + infoColor + "<name> " + messageColor + "for " + infoColor + "<reason>");
            }
            if (hasPermission(player, "heroicrebuke.clear")) {
                sender.sendMessage(infoColor + "/warn clear <name>" + messageColor + " - Clear active warning of " + infoColor + "<name>");
            }
            if (hasPermission(player, "heroicrebuke.active")) {
                sender.sendMessage(infoColor + "/warn active (page)" + messageColor + " - Show all unacknowledged warnings");
            }
            if (hasPermission(player, "heroicrebuke.info")) {
                sender.sendMessage(infoColor + "/warn info <index>" + messageColor + " - Display extended information about the given warning");
            }
            if (useDB) {
                if (hasPermission(player, "heroicrebuke.list")) {
                    sender.sendMessage(infoColor + "/warn list " + ((hasPermission(player, "heroicrebuke.list.others")) ? "<name> (page)" : " (page)") + messageColor + " - List previous warnings" + ((hasPermission(player, "heroicrebuke.list.others")) ? " for " + infoColor + "<name>" : ""));
                }
                if (hasPermission(player, "heroicrebuke.delete")) {
                    sender.sendMessage(infoColor + "/warn delete <index>" + messageColor + " - Permanently delete a warning; requires index number displayed by " + infoColor + "list" + messageColor + " or " + infoColor + "active");
                }
            }
            if (isWarned != null) {
                sendWarning(player, isWarned);
                return true;
            }
            return true;
        }

        //If no valid command is provided and player is warned, re-send warning
        if (isWarned != null) {
            sendWarning(player, isWarned);
            return true;
        }
        return false;
    }

    public void warnRemoval(String target, String senderName) {
        warnings.remove(target.toLowerCase());
        Player p = getServer().getPlayer(target);
        if (p != null && p.isOnline()) {
            p.sendMessage(messageColor + "Your warning was removed by " + nameColor + senderName);
            HeroicRebukeListener.rootLocations.remove(p);
        }
    }

    public Warning getFromId(int id) {
        for (Warning w : warnings.values()) {
            if (w.getId() == id) {
                return w;
            }
        }
        return null;
    }

    public void sendWarningList(String target, CommandSender sender, String senderName, int page) {
        ArrayList<String> curList = lists.get(target.toLowerCase());
        if (curList == null) {
            curList = database.listWarnings(target);
            lists.put(target.toLowerCase(), curList);
        }
        if (curList.isEmpty()) {
            sender.sendMessage(nameColor + target + messageColor + " has received no warnings.");
            return;
        }
        if (page < 1) {
            page = 1;
        }
        int numPages = (int) Math.ceil(curList.size() / maxPerPage);
        if (curList.size() % maxPerPage > 0) {
            numPages++;
        }
        if (page > numPages) {
            sender.sendMessage(messageColor + "Bad page number, please issue " + infoColor + "/warn list" + messageColor + " command again without a page number to get acceptable range.");
            return;
        }
        int startOfPage = (page - 1) * maxPerPage;
        int endOfPage = maxPerPage + (page - 1) * maxPerPage - 1;
        if (endOfPage >= curList.size()) {
            endOfPage = curList.size() - 1;
        }
        sender.sendMessage(messageColor + "Warnings Matching [" + nameColor + target + messageColor + "] (Page " + infoColor + page + messageColor + "/" + infoColor + numPages + messageColor + ") - Type " + infoColor + "/warn info #" + messageColor + " for details of a given warning.");
        for (int i = startOfPage; i <= endOfPage; i++) {
            String msg = curList.get(i);
            if (msg != null) {
                sender.sendMessage(msg);
            }
        }
    }

    public void sendActiveList(CommandSender sender, String senderName, int page) {
        ArrayList<String> curList = lists.get(senderName.toLowerCase());
        if (curList == null) {
            curList = new ArrayList<String>();
            for (Warning w : warnings.values()) {
                String send_time = getFormatTime(w.getSendTime());
                String buildLine = messageColor + "[" + infoColor + w.getId() + messageColor + "] " + infoColor + send_time + messageColor + " From: " + nameColor + w.getSender() + messageColor
                        + " To: " + nameColor + w.getTarget();
                curList.add(buildLine);
            }
            lists.put(senderName.toLowerCase(), curList);
        }
        if (curList.isEmpty()) {
            sender.sendMessage(messageColor + "There are no active warnings!");
            return;
        }
        if (page < 1) {
            page = 1;
        }
        int numPages = (int) Math.ceil(curList.size() / maxPerPage);
        if (curList.size() % maxPerPage > 0) {
            numPages++;
        }
        debug("List Size: " + curList.size() + " Pages: " + numPages + " Max: " + maxPerPage);
        if (page > numPages) {
            sender.sendMessage(messageColor + "Bad page number, please type " + infoColor + "/warn active" + messageColor + " without a page number to get acceptable range.");
            return;
        }
        int startOfPage = (page - 1) * maxPerPage;
        int endOfPage = maxPerPage + (page - 1) * maxPerPage - 1;
        if (endOfPage >= curList.size()) {
            endOfPage = curList.size() - 1;
        }
        debug("Start: " + startOfPage + " End: " + endOfPage);
        sender.sendMessage(messageColor + "Active Warnings (Page " + infoColor + page + messageColor + "/" + infoColor + numPages + messageColor + ") - Type " + infoColor + "/warn info #" + messageColor + " for details of a given warning.");
        for (int i = startOfPage; i <= endOfPage; i++) {
            String msg = curList.get(i);
            if (msg != null) {
                sender.sendMessage(msg);
            }
        }
    }

    public Warning makeWarning(String to, String from, String message) {
        Warning w = new Warning(to, from, message);
        if (useCode) {
            w.setCode(codeGen.nextString());
        }
        int index = noDatabaseIndex++;
        if (useDB) {
            index = database.newWarning(w);
        }
        w.setId(index);
        warnings.put(to.toLowerCase(), w);
        return w;
    }

    public void sendWarning(Player p, Warning w) {
        if (w == null) {
            return;
        }
        String warnHeader = messageColor + "[Warned by: " + nameColor + w.getSender() + messageColor + "] " + w.getMessage();
        p.sendMessage(warnHeader);
        if (canAcknowledge) {
            String warnFooter = "Type " + infoColor + "/warn acknowledge " + ((w.getCode() != null) ? w.getCode() : "") + messageColor + " to clear it.";
            if (blockMove) {
                warnFooter = messageColor + "Movement disabled; " + warnFooter;
            }
            p.sendMessage(warnFooter);
        }
    }

    private void ackWarning(Player p, String code) {
        Warning w = warnings.get(p.getName().toLowerCase());
        if (w == null) {
            p.sendMessage(messageColor + "You are not being warned for anything.");
            return;
        }
        if ((useCode && (w.getCode() != null)) && !w.getCode().equalsIgnoreCase(code)) {
            p.sendMessage(infoColor + "Error:" + messageColor + " You must enter the correct code to acknowledge your warning.");
            return;
        }
        p.sendMessage(messageColor + "You have acknowledged your warning.");
        String message = nameColor + p.getName() + messageColor + " acknowledged your warning.";
        if (w.getSender() != consoleSender) {
            try {
                Player sender = getServer().getPlayer(w.getSender());
                sender.sendMessage(message);
            } catch (Exception e) {
            }
        } else {
            System.out.println(message.replaceAll("(?i)\u00A7[0-F]", ""));
        }
        warnings.remove(p.getName().toLowerCase());
        HeroicRebukeListener.rootLocations.remove(p);
        if (useDB) {
            database.ackWarning(p.getName());
        }
    }

    //Method returns a formatted timestamp, defaults if format/time are unusable
    public String getFormatTime(Long time) {
        if (time == null) {
            time = System.currentTimeMillis();
        }
        Date timestamp = new Date(time);
        try {
            SimpleDateFormat format = new SimpleDateFormat(timestampFormat);
            return format.format(timestamp);
        } catch (IllegalArgumentException e) {
            log.severe("[HeroicRebuke] Couldn't use provided timestamp format, using default.");
            e.printStackTrace();
            timestampFormat = "MM/dd/yyyy HH:mm:ss z";
            SimpleDateFormat format = new SimpleDateFormat(timestampFormat);
            return format.format(timestamp);
        }
    }

    //Method validates color constants defined in a config.yml
    public String getConfigColor(String property, String def) {
        String propColor = this.getConfig().getString(property, def);
        ChatColor returnColor = null;
        try {
            returnColor = ChatColor.valueOf(propColor);
        } catch (Exception e) {
            log.info("[" + name + "] Improper color definition in config.yml, using default.");
            returnColor = ChatColor.valueOf(def);
        }
        return returnColor.toString();
    }

    public String getColorName(String colorCode) {
        try {
            colorCode = colorCode.replace("\u00A7", "0x");
            Byte b = Byte.decode(colorCode);
            return ChatColor.getByCode(Integer.valueOf(b.intValue())).name();
        } catch (NumberFormatException e) {
            log.severe("[" + name + "] Unexpected error parsing color code: " + colorCode + ", using default of WHITE");
            return "WHITE";
        }
    }

    //This method is the default API hook for Permissions
    public void setupPermissions() {
        log.info("[" + this.name + "]" + " Permission system possibly not present.");
        //permissionSystem = "Ops";
    }

    //Permissions system check
    public boolean hasPermission(Player p, String permission) {
        //Ops always win
        if (p.isOp()) {
            return true;
        }
        //If using Nijikokun's Permissions, do a Permissions check
        if (p.hasPermission(permission)) {
            return true;
        }
        //If using config.yml admins definition, iterate over the list (for case insensitivity)
        if (permissionSystem.equalsIgnoreCase("config")) {
            if (rebukeAdmins != null) {
                Iterator<String> it = rebukeAdmins.iterator();
                while (it.hasNext()) {
                    if (it.next().equalsIgnoreCase(p.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void saveConfig() {
        this.getConfig().set("colors.message", getColorName(messageColor));
        this.getConfig().set("colors.name", getColorName(nameColor));
        this.getConfig().set("colors.info", getColorName(infoColor));
        this.getConfig().set("options.timeformat", timestampFormat);
        this.getConfig().set("admins", rebukeAdmins);
        this.getConfig().set("options.code.use", useCode);
        this.getConfig().set("options.code.length", codeLength);
        this.getConfig().set("options.canAcknowledge", canAcknowledge);
        this.getConfig().set("options.server_name", consoleSender);
        this.getConfig().set("options.block_move", blockMove);
        this.getConfig().set("options.only_warn_online", onlyWarnOnline);
        this.getConfig().set("options.lines_per_page", maxPerPage);
        this.getConfig().set("options.mysql.location", mySqlDir);
        this.getConfig().set("options.mysql.username", mySqlUser);
        this.getConfig().set("options.mysql.password", mySqlPass);
        this.getConfig().set("options.database", dbType);
        this.getConfig().set("options.ban.enable", useBan);
        this.getConfig().set("options.ban.threshold", banThreshold);
        this.getConfig().set("options.ban.message", banMessage);
        try {
            this.getConfig().save(configfile);
        } catch (IOException ex) {
            Logger.getLogger(HeroicRebuke.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void debug(String message) {
        if (debugging) {
            log.info(message);
        }
    }

    public void onDisable() {
        if (useDB) {
            try {
                database.getConnection().close();
            } catch (SQLException e) {
                log.severe("[" + name + "] Error closing database: ");
                e.printStackTrace();
            }
        }
        String strDisable = "[" + name + "] " + version + " disabled.";
        log.info(strDisable);
    }
}