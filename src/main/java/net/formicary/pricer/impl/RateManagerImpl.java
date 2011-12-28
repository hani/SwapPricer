package net.formicary.pricer.impl;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.model.Index;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.PeriodEnum;
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
  public double getZeroRate(String indexName, String currency, Interval interval, LocalDate date) {
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    //hack, LCH rates are given to us in 12M vs 1Y
    if(interval.getPeriod() == PeriodEnum.Y && interval.getPeriodMultiplier().intValue() == 1) {
      query.field("tenorPeriod").equal("M");
      query.field("tenorUnit").equal("12");
    } else {
      query.field("tenorPeriod").equal(interval.getPeriod().value());
      query.field("tenorUnit").equal(interval.getPeriodMultiplier().toString());
    }
    query.field("fixingDate").equal(date);
    //TODO shouldn't hardcode
    query.field("name").equal(indexName);
    Index index = query.get();
    if(index == null) {
      throw new IllegalArgumentException("No rate found for " + currency + " " + interval.getPeriodMultiplier() + interval.getPeriod() + " on " + date);
    }
    return index.getRate();
  }

  @Override
  public double getDiscountFactor(String currency, Interval interval, LocalDate date, LocalDate valuationDate) {
    double zero = getZeroRate("LIBOR", currency, interval, date) / 100;
    double days = Days.daysBetween(date, valuationDate).getDays();
    return Math.exp(zero * -(days) / 365d);
  }
}
