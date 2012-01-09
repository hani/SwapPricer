package net.formicary.pricer.loader;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.JedisPersistenceModule;
import net.formicary.pricer.model.Index;
import org.joda.time.LocalDate;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import java.io.IOException;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:48 AM
 */
public class JedisRateLoader extends RateLoader {
  @Inject private Jedis ds;

  protected void save(Index index) {
    LocalDate date = index.getFixingDate();
    String key = index.getName() + "-" + index.getCurrency() + "-" + date.getYear()
        + '-' + date.getDayOfYear() + '-' + date.getDayOfMonth() + "-" + index.getTenorUnit() + index.getTenorPeriod();
    ds.set(key, Double.toString(index.getRate()));
  }

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new JedisPersistenceModule("src/test/resources/fpml"));
    i.getInstance(JedisRateLoader.class).importHistoricRates(args[0]);
  }
}
