package net.formicary.pricer.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.model.Index;
import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.PeriodEnum;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:57 AM
 */
@Singleton
public class MongoRateManagerImpl extends AbstractRateManager {
  @Inject private Datastore ds;

  protected double getRate(String key, String indexName, String currency, Interval interval, FastDate date) {
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
    query.field("name").equal(indexName);
    Index index = query.get();
    if(index == null) return 0;
    return index.getRate();
  }
}
