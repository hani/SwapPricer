package net.formicary.pricer.impl;

import javax.inject.Inject;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.model.Index;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:30 PM
 */
public class RateManagerImpl implements RateManager {
  @Inject Datastore ds;

  @Override
  public double lookup(String currency, String indexName, String tenor, LocalDate date) {
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    query.field("name").equal(indexName);
    query.field("tenorUnit").equal(tenor.substring(0, tenor.length() - 1));
    query.field("tenorPeriod").equal(tenor.substring(tenor.length() - 1));
    query.field("fixingDate").equal(date);
    Index i = query.get();
    if(i == null) {
      throw new IllegalArgumentException("No rate for specified criteria");
    }
    return i.getRate();
  }
}
