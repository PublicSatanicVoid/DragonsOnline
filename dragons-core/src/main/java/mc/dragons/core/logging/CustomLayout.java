 package mc.dragons.core.logging;
 
 import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import net.md_5.bungee.api.ChatColor;
 
 public class CustomLayout extends AbstractStringLayout {
   private boolean hideEmpty = true;
   
   private boolean hideMinecraft = true;
   
   private boolean truncatePackageNames = true;
   
   private boolean specialPackageNames = true;
   
   private String formatWithLogger = "[%t %l] [%s] %m";
   
   private String formatWithoutLogger = "[%t %l] %m";
   
   private final String DAY_PATTERN = Pattern.quote("%d");
   
   private final String TIME_PATTERN = Pattern.quote("%t");
   
   private final String LEVEL_PATTERN = Pattern.quote("%l");
   
   private final String SOURCE_PATTERN = Pattern.quote("%s");
   
   private final String MESSAGE_PATTERN = Pattern.quote("%m");
   
   public CustomLayout(Charset charset) {
     super(charset);
   }
   
   private String format(String pattern, String date, String time, String level, String source, String message) {
     return pattern.replaceAll(this.DAY_PATTERN, date)
       .replaceAll(this.TIME_PATTERN, time)
       .replaceAll(this.LEVEL_PATTERN, level)
       .replaceAll(this.SOURCE_PATTERN, source)
       .replaceAll(this.MESSAGE_PATTERN, Matcher.quoteReplacement((message == null) ? "" : message));
   }
   
   public String toSerializable(LogEvent logEvent) {
     String loggerName = logEvent.getLoggerName();
     if (loggerName.contains(".")) {
       int lastIndex = loggerName.lastIndexOf(".");
       String mostSpecific = loggerName.substring(lastIndex + 1);
       boolean special = false;
       if (this.specialPackageNames)
         if (loggerName.contains("net.minecraft.server")) {
           loggerName = "NMS." + mostSpecific;
           special = true;
         } else if (loggerName.contains("org.bukkit.craftbukkit")) {
           loggerName = "OBC." + mostSpecific;
           special = true;
         }  
       if (!special && this.truncatePackageNames) {
         String truncatedLoggerName = String.valueOf(loggerName.charAt(0)) + ".";
         int dotIndex = loggerName.indexOf(".", 0);
         while (dotIndex != lastIndex) {
           truncatedLoggerName = String.valueOf(truncatedLoggerName) + loggerName.charAt(dotIndex + 1) + ".";
           dotIndex = loggerName.indexOf(".", dotIndex + 1);
         } 
         truncatedLoggerName = String.valueOf(truncatedLoggerName) + mostSpecific;
         loggerName = truncatedLoggerName;
       } 
     } 
     boolean includeLogger = true;
     if (this.hideMinecraft && (loggerName.equals("Minecraft") || loggerName.contains("MinecraftServer") || loggerName.contains("DedicatedServer")))
       includeLogger = false; 
     if (this.hideEmpty && loggerName.length() == 0)
       includeLogger = false; 
     String timestamp = (new SimpleDateFormat("HH:mm:ss")).format(Long.valueOf(logEvent.getTimeMillis()));
     String datestamp = (new SimpleDateFormat("yyyy-MM-dd")).format(Long.valueOf(logEvent.getTimeMillis()));
     String message = ChatColor.stripColor(logEvent.getMessage().getFormattedMessage());
     if (logEvent.getThrown() != null) {
       Throwable buf = logEvent.getThrown();
       message = String.valueOf(message) + "\n" + format(includeLogger ? this.formatWithLogger : this.formatWithoutLogger, datestamp, timestamp, logEvent.getLevel().toString(), loggerName, String.valueOf(buf.getClass().getName()) + ": " + buf.getMessage());
       while (buf != null) {
         byte b;
         int i;
         StackTraceElement[] arrayOfStackTraceElement;
         for (i = (arrayOfStackTraceElement = buf.getStackTrace()).length, b = 0; b < i; ) {
           StackTraceElement elem = arrayOfStackTraceElement[b];
           message = String.valueOf(message) + "\n" + format(includeLogger ? this.formatWithLogger : this.formatWithoutLogger, datestamp, timestamp, logEvent.getLevel().toString(), loggerName, "    " + elem.toString());
           b++;
         } 
         buf = buf.getCause();
         if (buf != null)
           message = String.valueOf(message) + "\n" + format(includeLogger ? this.formatWithLogger : this.formatWithoutLogger, datestamp, timestamp, logEvent.getLevel().toString(), loggerName, "Caused by:"); 
       } 
     } 
     if (includeLogger)
       return format(this.formatWithLogger, datestamp, timestamp, logEvent.getLevel().toString(), loggerName, message); 
     return format(this.formatWithoutLogger, datestamp, timestamp, logEvent.getLevel().toString(), loggerName, message);
   }
 }


