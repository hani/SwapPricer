package net.formicary.pricer.loader;

import java.io.*;
import java.util.*;

import javax.inject.Inject;

import com.google.inject.Guice;
import net.formicary.pricer.PricerModule;
import net.objectlab.kit.datecalc.common.DefaultHolidayCalendar;
import net.objectlab.kit.datecalc.common.HolidayCalendar;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:53 AM
 */
public class HolidayLoader {

  private static final Logger log = LoggerFactory.getLogger(HolidayLoader.class);

  @Inject
  public HolidayLoader(LocalDateKitCalculatorsFactory factory) throws IOException {
    long now = System.currentTimeMillis();
    BufferedReader is = new BufferedReader(new FileReader("data/rep00006.txt"));
    is.readLine();
    String line;
    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    Map<String, Set<LocalDate>> allDates = new HashMap<String, Set<LocalDate>>();

    int count = 0;
    while((line = is.readLine()) != null) {
      count++;
      String[] items = line.split("\t");
      Set<LocalDate> dates = allDates.get(items[1]);
      if(dates == null) {
        dates = new HashSet<LocalDate>();
        allDates.put(items[1], dates);
      }
      dates.add(LocalDate.parse(items[2], formatter));
    }

    for(Map.Entry<String, Set<LocalDate>> entry : allDates.entrySet()) {
      HolidayCalendar<LocalDate> calendar = new DefaultHolidayCalendar<LocalDate>(entry.getValue());
      factory.registerHolidays(entry.getKey(), calendar);
    }

    log.info("Initialised datecalc factory in " + (System.currentTimeMillis() - now) + "ms with " + count + " dates");
  }

  public static void main(String[] args) throws IOException {
    Guice.createInjector(new PricerModule());
  }
}
