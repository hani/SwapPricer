package net.formicary.pricer.impl;

import javax.inject.Inject;

import com.mongodb.*;
import net.formicary.pricer.MarketDataManager;
import org.joda.time.LocalDate;

import static net.formicary.pricer.Calendars.*;
/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:27 AM
 */
public class MarketDataManagerImpl implements MarketDataManager {

  private DB db;

  @Inject
  public void setMongo(Mongo mongo) {
    this.db = mongo.getDB("pricer");
  }

  @Override
  public boolean isHoliday(String businessCenter, LocalDate date) {
    DBCollection calendar = db.getCollection("calendar");
    BasicDBObject query = new BasicDBObject();
    query.put(Financialcentrecode.toString(), businessCenter);
    query.put(Holidaydate.toString(), date.toString());
    if(calendar.findOne(query) != null) {
      return true;
    }
    return false;
  }
}
