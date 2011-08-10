package net.formicary.pricer;

import java.io.IOException;
import java.net.UnknownHostException;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.mongodb.Mongo;
import net.formicary.pricer.impl.MarketDataManagerImpl;
import net.formicary.pricer.loader.HolidayLoader;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:31 AM
 */
public class PricerModule implements Module {
  private static final Logger log = LoggerFactory.getLogger(PricerModule.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(MarketDataManager.class).to(MarketDataManagerImpl.class);
    LocalDateKitCalculatorsFactory calculatorsFactory = LocalDateKitCalculatorsFactory.getDefaultInstance();
    try {
      HolidayLoader loader = new HolidayLoader(calculatorsFactory);
      binder.bind(Mongo.class).toInstance(new Mongo());
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    binder.bind(LocalDateKitCalculatorsFactory.class).toInstance(calculatorsFactory);
  }
}
