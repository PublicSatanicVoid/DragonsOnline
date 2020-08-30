package mc.dragons.core.storage;

public interface Counter {
  int getCurrentId(String paramString);
  
  int reserveNextId(String paramString);
}


