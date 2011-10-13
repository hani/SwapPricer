package net.formicary.pricer;

import java.io.IOException;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import net.formicary.pricer.impl.*;
import net.formicary.pricer.loader.HolidayLoader;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:31 AM
 */
public class PricerModule extends AbstractModule {
  private static final Logger log = LoggerFactory.getLogger(PricerModule.class);

  @Override
  public void configure() {
    bind(CalendarManager.class).to(CalendarManagerImpl.class);
    bind(CurveManager.class).to(CurveManagerImpl.class);
    bind(TradeStore.class).to(SimpleTradeStore.class);
    LocalDateKitCalculatorsFactory calculatorsFactory = LocalDateKitCalculatorsFactory.getDefaultInstance();
    try {
      HolidayLoader loader = new HolidayLoader(calculatorsFactory);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    bind(LocalDateKitCalculatorsFactory.class).toInstance(calculatorsFactory);
  }
}
