 package mc.dragons.core.storage.impl.loader;
 
 import com.mongodb.client.FindIterable;
 import java.time.Instant;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import org.bson.Document;
 import org.bson.conversions.Bson;
 
 public class ChangeLogLoader extends AbstractLightweightLoader<ChangeLogLoader.ChangeLogEntry> {
   public static class ChangeLogEntry {
     private int id;
     
     private String date;
     
     private String by;
     
     private String title;
     
     private List<String> changelog;
     
     public ChangeLogEntry(int id, String date, String by, String title, List<String> changelog) {
       this.date = date;
       this.by = by;
       this.title = title;
       this.changelog = changelog;
     }
     
     public int getId() {
       return this.id;
     }
     
     public String getDate() {
       return this.date;
     }
     
     public String getBy() {
       return this.by;
     }
     
     public String getTitle() {
       return this.title;
     }
     
     public List<String> getChangeLog() {
       return this.changelog;
     }
   }
   
   public ChangeLogLoader() {
     super("changelogs", "changelogs");
   }
   
   public List<ChangeLogEntry> getUnreadChangelogs(int lastReadChangelog) {
     List<ChangeLogEntry> result = new ArrayList<>();
     FindIterable<Document> dbResults = this.collection.find((Bson)new Document("_id", new Document("$gt", Integer.valueOf(lastReadChangelog))));
     for (Document d : dbResults)
       result.add(new ChangeLogEntry(d.getInteger("_id").intValue(), d.getString("date"), d.getString("by"), d.getString("title"), d.getList("changelog", String.class))); 
     return result;
   }
   
   public void deleteChangeLog(int id) {
     this.collection.deleteOne((Bson)new Document("_id", Integer.valueOf(id)));
   }
   
   public void addChangeLog(String by, String title, List<String> changelog) {
     String date = Date.from(Instant.now()).toString();
     this.collection.insertOne((new Document("_id", Integer.valueOf(reserveNextId()))).append("date", date)
         .append("by", by).append("title", title).append("changelog", changelog));
   }
 }


