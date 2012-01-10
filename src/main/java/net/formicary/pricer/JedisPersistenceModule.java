package net.formicary.pricer;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.formicary.pricer.impl.JedisRateManagerImpl;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 8:02 AM
 */
public class JedisPersistenceModule extends AbstractModule {
  private String fpmlDir;

  public JedisPersistenceModule(String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("fpmlDir")).toInstance(fpmlDir);
    bind(RateManager.class).to(JedisRateManagerImpl.class);
    //bind(TradeStore.class).to(FpmlJAXBTradeStore.class);
  }


}
