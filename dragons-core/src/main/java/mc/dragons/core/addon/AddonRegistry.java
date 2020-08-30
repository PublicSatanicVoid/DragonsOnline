 package mc.dragons.core.addon;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.logging.Logger;
 import mc.dragons.core.Dragons;
 
 public class AddonRegistry {
   private Logger LOGGER;
   
   private List<Addon> addons;
   
   public AddonRegistry(Dragons plugin) {
     this.addons = new ArrayList<>();
     this.LOGGER = plugin.getLogger();
   }
   
   public void register(Addon addon) {
     this.LOGGER.info("Registering addon " + addon.getName() + " of type " + addon.getType());
     this.addons.add(addon);
   }
   
   public void enableAll() {
     this.addons.forEach(addon -> addon.onEnable());
   }
   
   public Addon getAddonByName(String name) {
     for (Addon addon : this.addons) {
       if (addon.getName().equalsIgnoreCase(name))
         return addon; 
     } 
     return null;
   }
   
   public List<Addon> getAllAddons() {
     return this.addons;
   }
 }


