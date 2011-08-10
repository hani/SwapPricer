package net.formicary.pricer.loader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.*;
import net.formicary.pricer.PricerModule;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.formicary.pricer.Calendars.*;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:53 AM
 */
public class MongoDBLoader {

  private static final Logger log = LoggerFactory.getLogger(MongoDBLoader.class);
  private DB db;

  public void setMongo(Mongo mongo) {
    db = mongo.getDB("pricer");
  }

  private DBCollection createCollection(String name) {
    if(db.collectionExists(name)) {
      DBCollection c = db.getCollection(name);
      log.info("Deleting old calendars, of size " + c.getCount());
      c.drop();
    }
    return db.getCollection(name);
  }

  private void loadCalendar() throws IOException {
    DBCollection coll = createCollection("calendar");
    long now = System.currentTimeMillis();
    BufferedReader is = new BufferedReader(new FileReader("data/rep00006.txt"));
    is.readLine();
    String line;
    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    List<DBObject> list = new ArrayList<DBObject>();
    while((line = is.readLine()) != null) {
      BasicDBObject row = new BasicDBObject();
      String[] items = line.split("\t");
      row.put(Financialcentrename.toString(), items[0]);
      row.put(Financialcentrecode.toString(), items[1]);
      row.put(Holidaydate.toString(), LocalDate.parse(items[2], formatter).toString());
      row.put(Holidayname.toString(), items[2]);
      list.add(row);
    }
    log.info("Read " + list.size() + " items in " + (System.currentTimeMillis() - now) + "ms");
    now = System.currentTimeMillis();
    coll.insert(list);
    coll.createIndex(new BasicDBObject("Financialcentrecode", 1));
    coll.createIndex(new BasicDBObject("Holidaydate", 1));
    log.info("Inserted into mongodb in " + (System.currentTimeMillis() - now) + "ms");
  }

  public static void main(String[] args) throws IOException {
    MongoDBLoader loader = new MongoDBLoader();
    loader.setMongo(new Mongo());
    loader.loadCalendar();
  }
}
