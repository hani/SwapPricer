package net.formicary.pricer.impl;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.model.Index;
import org.fpml.spec503wd3.Interval;
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
  public double getZeroRate(String currency, Interval interval, LocalDate date) {
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    query.field("tenorPeriod").equal(interval.getPeriod().value());
    query.field("tenorUnit").equal(interval.getPeriodMultiplier().toString());
    query.field("fixingDate").equal(date);
    query.field("name").equal("LIBOR");
    Index index = query.get();
    if(index == null) {
      throw new IllegalArgumentException("No rate found for " + currency + " " + interval.getPeriodMultiplier() + interval.getPeriod() + " on " + date);
    }
    return index.getRate();
  }

  @Override
  public double getDiscountFactor(String currency, Interval interval, LocalDate date, LocalDate valuationDate) {
    double zero = getZeroRate(currency, interval, date) / 100;
    double days = Days.daysBetween(date, valuationDate).getDays();
    return Math.exp(zero * -(days) / 365d);
  }
}
