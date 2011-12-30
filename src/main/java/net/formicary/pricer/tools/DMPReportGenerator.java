package net.formicary.pricer.tools;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.CashflowGenerator;
import net.formicary.pricer.PersistenceModule;
import net.formicary.pricer.PricerModule;
import net.formicary.pricer.model.Cashflow;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/28/11
 *         Time: 9:32 AM
 */
public class DMPReportGenerator {
  @Inject private CashflowGenerator generator;
  private static final Logger log = LoggerFactory.getLogger(DMPReportGenerator.class);

  public void generateReport(String inputDir, String outputFile) throws IOException {
    LocalDate date = new LocalDate(2011, 11, 4);
    List<String> files = new ArrayList<String>();
    Collections.addAll(files, new File(inputDir).list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("LCH") && name.endsWith(".xml");
      }
    }));
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile, false));
    os.write("LchTradeId,NpvAmount,CashflowDate,CashflowAmount\n".getBytes());
    long now  = System.currentTimeMillis();
    int failures = 0;
    for (String file : files) {
      String id = file.substring(0, file.indexOf('.'));
      List<Cashflow> cashflows = null;
      try {
        cashflows = generator.generateCashflows(date, id);
        for (Cashflow cashflow : cashflows) {
          StringBuilder sb = new StringBuilder();
          sb.append(id).append(",");
          sb.append(cashflow.getNpv()).append(",");
          sb.append(cashflow.getDate()).append(",");
          sb.append(cashflow.getAmount());
          sb.append('\n');
          os.write(sb.toString().getBytes());
        }
      } catch (Exception e) {
        log.error("Error calculating cashflows for trade " + id, e);
        failures++;
      }
    }
    long timeTaken = System.currentTimeMillis() - now;
    log.info("Priced {} trades with {} failures", files.size(), failures);
    log.info("Total time: {}ms. Average time: {}ms", timeTaken, timeTaken/files.size());
    os.close();
  }

  public static void main(final String[] args) throws IOException {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule("src/test/resources/fpml"));
    DMPReportGenerator reporter = injector.getInstance(DMPReportGenerator.class);
    reporter.generateReport(args[0], args[1]);
  }
}
