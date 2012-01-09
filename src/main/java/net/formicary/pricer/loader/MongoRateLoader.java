package net.formicary.pricer.loader;

import com.google.code.morphia.Datastore;
import net.formicary.pricer.model.Index;

import javax.inject.Inject;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:48 AM
 */
public class MongoRateLoader extends RateLoader {
  @Inject private Datastore ds;

  protected void save(Index index) {
    ds.save(index);
  }
}
