package net.formicary.pricer.loader;

import com.google.code.morphia.Datastore;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.MongoPersistenceModule;
import net.formicary.pricer.model.Index;

import javax.inject.Inject;
import java.io.IOException;

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

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new MongoPersistenceModule("src/test/resources/fpml"));
    i.getInstance(MongoRateLoader.class).importHistoricRates(args[0]);
  }
}
