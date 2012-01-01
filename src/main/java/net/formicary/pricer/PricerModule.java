package net.formicary.pricer;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import net.formicary.pricer.impl.CalendarManagerImpl;
import net.formicary.pricer.impl.CurveManagerImpl;
import net.formicary.pricer.loader.HolidayLoader;
import org.fpml.spec503wd3.DataDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
          long now = System.currentTimeMillis();
          JAXBContext context = JAXBContext.newInstance(DataDocument.class);
          log.info("Time taken to initialise jaxb context: {}ms", System.currentTimeMillis() - now);
          return context;
        } catch (JAXBException e) {
          throw new RuntimeException("Could not create the JAXBContext object", e);
        }
      }
    }).in(SINGLETON);
    bind(CalendarManager.class).to(CalendarManagerImpl.class);
    bind(CurveManager.class).to(CurveManagerImpl.class);
    int availableCores = Runtime.getRuntime().availableProcessors();
    int coresToUse = availableCores;
    if(availableCores > 2) {
      coresToUse = coresToUse / 2;
    }
    if(availableCores > 8) {
      coresToUse += -2;
    }
    log.info("Detected {} cores. Using thread pool of size {}", availableCores, coresToUse);
    bind(Executor.class).toInstance(Executors.newFixedThreadPool(coresToUse, new ThreadFactory() {
      private AtomicInteger threadNumber = new AtomicInteger(1);

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "pricer-" + threadNumber.getAndIncrement());
        t.setDaemon(true);
        if(t.getPriority() != Thread.NORM_PRIORITY)
          t.setPriority(Thread.NORM_PRIORITY);
        return t;
      }
    }));
    HolidayManager holidayManager = new HolidayManager();
    bind(HolidayManager.class).toInstance(holidayManager);
    try {
      HolidayLoader loader = new HolidayLoader(holidayManager);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
