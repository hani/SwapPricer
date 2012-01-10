package net.formicary.pricer.impl;

import javax.inject.Inject;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.model.Index;
import net.formicary.pricer.util.FastDate;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:57 AM
 */
public class MongoRateManagerImpl extends AbstractRateManager {
  @Inject private Datastore ds;

  protected double getRate(String key, String indexName, String currency, String tenor, FastDate date) {
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    //hack, LCH rates are given to us in 12M vs 1Y
    if("1Y".equals(tenor)) {
      tenor = "12M";
    }
    query.field("tenor").equal(tenor);
    query.field("fixingDate").equal(date);
    query.field("name").equal(indexName);
    Index index = query.get();
    if(index == null) return 0;
    return index.getRate();
  }
}
