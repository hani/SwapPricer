package net.formicary.pricer;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import net.formicary.pricer.impl.CalendarManagerImpl;
import net.formicary.pricer.impl.CurveManagerImpl;
import net.formicary.pricer.impl.FpmlJAXBTradeStore;
import net.formicary.pricer.loader.HolidayLoader;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.fpml.spec503wd3.DataDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static com.google.inject.Scopes.SINGLETON;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:31 AM
 */
public class PricerModule extends AbstractModule {
  private static final Logger log = LoggerFactory.getLogger(PricerModule.class);

  @Override
  public void configure() {
    bind(JAXBContext.class).toProvider(new Provider<JAXBContext>() {
      public JAXBContext get() {
        try {
          return JAXBContext.newInstance(DataDocument.class);
        } catch (JAXBException e) {
          throw new RuntimeException("Could not create the JAXBContext object", e);
        }
      }
    }).in(SINGLETON);
    bind(CalendarManager.class).to(CalendarManagerImpl.class);
    bind(CurveManager.class).to(CurveManagerImpl.class);
    bind(TradeStore.class).to(FpmlJAXBTradeStore.class);
    LocalDateKitCalculatorsFactory calculatorsFactory = LocalDateKitCalculatorsFactory.getDefaultInstance();
    try {
      HolidayLoader loader = new HolidayLoader(calculatorsFactory);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    bind(LocalDateKitCalculatorsFactory.class).toInstance(calculatorsFactory);
  }
}
