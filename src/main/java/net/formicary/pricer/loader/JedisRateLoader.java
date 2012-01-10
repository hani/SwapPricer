package net.formicary.pricer.loader;

import java.io.IOException;
import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.JedisPersistenceModule;
import net.formicary.pricer.model.Index;
import net.formicary.pricer.util.FastDate;
import redis.clients.jedis.Jedis;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:48 AM
 */
public class JedisRateLoader extends RateLoader {
  @Inject private Jedis ds;

  protected void save(Index index) {
    FastDate date = index.getFixingDate();
    String key = index.getName() + "-" + index.getCurrency() + "-" + date.getYear()
        + '-' + date.getDayOfYear() + '-' + date.getDay() + "-" + index.getTenorUnit() + index.getTenorPeriod();
    ds.set(key, Double.toString(index.getRate()));
  }

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new JedisPersistenceModule("src/test/resources/fpml"));
    i.getInstance(JedisRateLoader.class).importHistoricRates(args[0]);
  }
}
