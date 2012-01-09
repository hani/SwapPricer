package net.formicary.pricer.loader;

import com.google.inject.Guice;
import net.formicary.pricer.HolidayManager;
import net.formicary.pricer.PricerModule;
import net.formicary.pricer.util.FastDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:53 AM
 */
public class HolidayLoader {

  private static final Logger log = LoggerFactory.getLogger(HolidayLoader.class);

  @Inject
  public HolidayLoader(HolidayManager manager) throws IOException {
    long now = System.currentTimeMillis();
    BufferedReader is = new BufferedReader(new FileReader("staticdata/rep00006.txt"));
    is.readLine();
    String line;
    Map<String, Set<FastDate>> allDates = new HashMap<String, Set<FastDate>>();

    int count = 0;
    while((line = is.readLine()) != null) {
      count++;
      String[] items = line.split("\t");
      Set<FastDate> dates = allDates.get(items[1]);
      if(dates == null) {
        dates = new HashSet<FastDate>();
        allDates.put(items[1], dates);
      }
      String d = items[2];
      dates.add(new FastDate(Integer.parseInt(d.substring(6, 10)), Integer.parseInt(d.substring(3, 5)), Integer.parseInt(d.substring(0, 2))));
    }

    for(Map.Entry<String, Set<FastDate>> entry : allDates.entrySet()) {
      manager.registerHolidays(entry.getKey(), entry.getValue());
    }

    log.info("Initialised datecalc manager in " + (System.currentTimeMillis() - now) + "ms with " + count + " dates");
  }

  public static void main(String[] args) throws IOException {
    Guice.createInjector(new PricerModule());
  }
}
