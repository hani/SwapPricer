package net.formicary.pricer.loader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.inject.Inject;

import com.google.code.morphia.Datastore;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.PersistenceModule;
import net.formicary.pricer.model.Index;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 7:34 AM
 */
public class RateLoader {
  private static final Logger log = LoggerFactory.getLogger(RateLoader.class);

  @Inject Datastore ds;

  public void importHistoricRates() throws IOException {
    long now = System.currentTimeMillis();
    BufferedReader is = new BufferedReader(new FileReader("data/rep00003.txt"));
    is.readLine();
    String line;
    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    int count = 0;
    while((line = is.readLine()) != null) {
      count++;
      String[] items = line.split("\t");
      Index index = new Index();
      index.setCurrency(items[0]);
      index.setName(items[1]);
      index.setTenorUnit(items[2]);
      index.setTenorPeriod(items[3]);
      index.setFixingDate(formatter.parseLocalDate(items[4]));
      index.setEffectiveDate(formatter.parseLocalDate(items[5]));
      index.setRate(Double.parseDouble(items[6]));
      index.setRegulatoryBody(items[7]);
      ds.save(index);
      if(count % 20000 == 0) {
        log.info("Saved " + count  + " rates");
      }
    }
    log.info("Initialised historic rates in " + (System.currentTimeMillis() - now) + "ms with " + count + " rates");
  }

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new PersistenceModule());
    i.getInstance(RateLoader.class).importHistoricRates();
  }
}
