 package mc.dragons.core.storage.impl.loader;
 
 import java.util.HashMap;
 import java.util.Map;
 
 public class LightweightLoaderRegistry {
   @SuppressWarnings("rawtypes")
   private Map<Class<? extends AbstractLightweightLoader>, AbstractLightweightLoader<?>> loaders = new HashMap<>();
   
   public void register(AbstractLightweightLoader<?> loader) {
     this.loaders.put(loader.getClass(), loader);
   }
   
   @SuppressWarnings("unchecked")
   public <C extends AbstractLightweightLoader<?>> C getLoader(Class<C> clazz) {
     return (C)this.loaders.get(clazz);
   }
 }


