package net.formicary.pricer.impl;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.model.Index;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import javax.inject.Inject;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:30 PM
 */
public class RateManagerImpl implements RateManager {
  @Inject Datastore ds;

  @Override
  public double getZeroRate(String currency, String tenor, LocalDate date) {
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    query.field("tenorUnit").equal(tenor.substring(0, tenor.length() - 1));
    query.field("tenorPeriod").equal(tenor.substring(tenor.length() - 1));
    query.field("fixingDate").equal(date);
    query.field("name").equal("LIBOR");
    Index index = query.get();
    if(index == null) {
      throw new IllegalArgumentException("No rate found for " + currency + " " + tenor + " on " + date);
    }
    return index.getRate();
  }

  @Override
  public double getDiscountFactor(String currency, String tenorPeriod, LocalDate date, LocalDate valuationDate) {
    double zero = getZeroRate(currency, tenorPeriod, date) / 100;
    double days = Days.daysBetween(date, valuationDate).getDays();
    return Math.exp(zero * -(days) / 365d);
  }
}
