 package mc.dragons.core.logging;
 
 import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;

import mc.dragons.core.Dragons;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
 
 public class CustomLoggingProvider {
   public static void enableCustomLogging() {
     Dragons.getInstance().getLogger().info("Switching to custom logging system...");
     Logger logger = (Logger)LogManager.getRootLogger();
     for (Appender appender1 : logger.getAppenders().values()) {
       if (appender1.getName().equals("TerminalConsole"))
         logger.removeAppender(appender1); 
     } 
     TerminalConsoleAppender appender = TerminalConsoleAppender.createAppender("TerminalConsole", new LogFilter(), new CustomLayout(Charset.defaultCharset()), false);
     appender.initialize();
     appender.start();
     logger.addAppender((Appender)appender);
   }
 }


