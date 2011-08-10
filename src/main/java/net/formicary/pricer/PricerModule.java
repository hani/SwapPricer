package net.formicary.pricer;

import java.net.UnknownHostException;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.mongodb.Mongo;
import net.formicary.pricer.impl.MarketDataManagerImpl;
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
    try {
      binder.bind(Mongo.class).toInstance(new Mongo());
    } catch(UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
}
