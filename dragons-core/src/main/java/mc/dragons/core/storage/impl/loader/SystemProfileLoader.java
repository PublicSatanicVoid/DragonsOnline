 package mc.dragons.core.storage.impl.loader;
 
 import java.math.BigInteger;
 import java.nio.charset.StandardCharsets;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.logging.Logger;
 import mc.dragons.core.Dragons;
 import mc.dragons.core.gameobject.user.PermissionLevel;
 import mc.dragons.core.gameobject.user.User;
 import mc.dragons.core.storage.impl.SystemProfile;
 import org.bson.Document;
 import org.bson.conversions.Bson;
 import org.bukkit.Bukkit;
 import org.bukkit.entity.Player;
 
 public class SystemProfileLoader extends AbstractLightweightLoader<SystemProfile> {
   private Set<SystemProfile> profiles;
   
   private Logger LOGGER;
   
   public SystemProfileLoader(Dragons instance) {
     super("#unused#", "sysprofiles");
     this.LOGGER = instance.getLogger();
     this.profiles = new HashSet<>();
   }
   
   public static String passwordHash(String password) {
     try {
       return (new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(("DragonsOnline System Logon b091283a#1*&AJK@83" + password).getBytes(StandardCharsets.UTF_8)))).toString(16);
     } catch (NoSuchAlgorithmException e) {
       e.printStackTrace();
       return "SHA256HashFailedNoSuchAlgorithmException";
     } 
   }
   
   public SystemProfile authenticateProfile(User user, String profileName, String profilePassword) {
     if (!isAvailable(profileName, user.getName()))
       return null; 
     SystemProfile systemProfile = loadProfile(profileName);
     if (systemProfile == null)
       return null; 
     if (!systemProfile.isActive())
       return null; 
     if (!systemProfile.getPasswordHash().equals(passwordHash(profilePassword)))
       return null; 
     systemProfile.setLocalCurrentUser(user);
     this.LOGGER.info(String.valueOf(user.getName()) + " logged into system profile " + profileName);
     return systemProfile;
   }
   
   public SystemProfile loadProfile(String profileName) {
     for (SystemProfile systemProfile1 : this.profiles) {
       if (systemProfile1.getProfileName().equalsIgnoreCase(profileName))
         return systemProfile1; 
     } 
     Document profile = (Document)this.collection.find((Bson)new Document("profileName", profileName)).first();
     if (profile == null)
       return null; 
     Document flags = (Document)profile.get("flags", Document.class);
     SystemProfile systemProfile = new SystemProfile(null, profileName, profile.getString("profilePasswordHash"), PermissionLevel.valueOf(profile.getString("maxPermissionLevel")), 
         new SystemProfile.SystemProfileFlags(flags), profile.getBoolean("active").booleanValue());
     this.profiles.add(systemProfile);
     return systemProfile;
   }
   
   private void kickProfileLocally(String profileName) {
     String currentUser = getCurrentUser(profileName);
     if (currentUser.equals(""))
       return; 
     Player player = Bukkit.getPlayerExact(currentUser);
     player.kickPlayer("Your system profile changed, relog for updated permissions.");
     logoutProfile(profileName);
   }
   
   public String getCurrentUser(String profileName) {
     Document profile = (Document)this.collection.find((Bson)new Document("profileName", profileName)).first();
     if (profile == null)
       return ""; 
     return profile.getString("currentUser");
   }
   
   public boolean isAvailable(String profileName, String testUser) {
     String currentUser = getCurrentUser(profileName);
     if (!currentUser.equals(testUser) && !currentUser.equals(""))
       return false; 
     return true;
   }
   
   public void setActive(String profileName, boolean active) {
     this.collection.updateOne((Bson)new Document("profileName", profileName), (Bson)new Document("$set", new Document("active", Boolean.valueOf(active))));
     loadProfile(profileName).setLocalActive(active);
     if (!active)
       kickProfileLocally(profileName); 
   }
   
   public void createProfile(String profileName, String profilePassword, PermissionLevel permissionLevel) {
     this.collection.insertOne((new Document("profileName", profileName))
         .append("profilePasswordHash", passwordHash(profilePassword))
         .append("maxPermissionLevel", permissionLevel.toString())
         .append("flags", SystemProfile.SystemProfileFlags.emptyFlagsDocument())
         .append("currentUser", "")
         .append("active", Boolean.valueOf(true)));
   }
   
   public void setProfileMaxPermissionLevel(String profileName, PermissionLevel newMaxPermissionLevel) {
     this.collection.updateOne((Bson)new Document("profileName", profileName), (Bson)new Document("$set", new Document("maxPermissionLevel", newMaxPermissionLevel.toString())));
     loadProfile(profileName).setLocalMaxPermissionLevel(newMaxPermissionLevel);
     kickProfileLocally(profileName);
     this.LOGGER.info("Max permission level of " + profileName + " changed to " + newMaxPermissionLevel);
   }
   
   public void setProfileFlag(String profileName, String flagName, boolean flagValue) {
     this.collection.updateOne((Bson)new Document("profileName", profileName), (Bson)new Document("$set", new Document("flags." + flagName, Boolean.valueOf(flagValue))));
     SystemProfile.SystemProfileFlags flags = loadProfile(profileName).getFlags();
     flags.setLocalFlag(SystemProfile.SystemProfileFlags.SystemProfileFlag.valueOf(flagName), flagValue);
     kickProfileLocally(profileName);
     this.LOGGER.info("Profile flag " + flagName + " of " + profileName + " set to " + flagValue);
   }
   
   public void setProfilePassword(String profileName, String newPassword) {
     String hash = passwordHash(newPassword);
     this.collection.updateOne((Bson)new Document("profileName", profileName), (Bson)new Document("$set", new Document("profilePasswordHash", hash)));
     loadProfile(profileName).setLocalPasswordHash(hash);
     kickProfileLocally(profileName);
     this.LOGGER.info("Profile " + profileName + " password has changed");
   }
   
   public void logoutProfile(String profileName) {
     this.collection.updateOne((Bson)new Document("profileName", profileName), (Bson)new Document("$set", new Document("currentUser", "")));
     loadProfile(profileName).setLocalCurrentUser(null);
     this.LOGGER.info("Profile " + profileName + " was logged out");
   }
 }


