package net.formicary.pricer.loader;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javolution.text.TypeFormat;
import net.formicary.pricer.PersistenceModule;
import net.formicary.pricer.model.Index;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 7:34 AM
 */
public abstract class RateLoader {
  private static final Logger log = LoggerFactory.getLogger(RateLoader.class);

  public void importHistoricRates(String fileName) throws IOException {
    long now = System.currentTimeMillis();
    BufferedReader is = new BufferedReader(new FileReader(fileName));
    is.readLine();
    String line;
    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    int count = 0;
    while((line = is.readLine()) != null) {
      String[] items = line.split("\t");
      if(items.length < 7) continue;
      Index index = new Index();
      index.setCurrency(items[0]);
      index.setName(items[1]);
      index.setTenorUnit(items[2]);
      index.setTenorPeriod(items[3]);
      LocalDate fixingDate = formatter.parseLocalDate(items[4]);
      index.setFixingDate(fixingDate);
      index.setEffectiveDate(formatter.parseLocalDate(items[5]));
      try {
        index.setRate(TypeFormat.parseDouble(items[6]));
        index.setRegulatoryBody(items[7]);
        save(index);
        if(++count % 20000 == 0) {
          log.info("Saved " + count  + " rates");
        }
      } catch(NumberFormatException e) {
        log.info("Invalid rate found for index " + index + ":" + items[5]);
      }
    }
    log.info("Initialised historic rates in " + (System.currentTimeMillis() - now) + "ms with " + count + " rates");
  }

  protected abstract void save(Index index);

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new PersistenceModule("src/test/resources/fpml"));
    i.getInstance(RateLoader.class).importHistoricRates(args[0]);
  }
}
