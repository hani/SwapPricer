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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hsuleiman
 *         Date: 12/28/11
 *         Time: 9:32 AM
 */
public class DMPReportGenerator {
  @Inject private CashflowGenerator generator;
  @Inject private Executor executor;
  private static final Logger log = LoggerFactory.getLogger(DMPReportGenerator.class);

  public void generateReport(String inputDir, String outputFile) throws IOException {
    final LocalDate date = new LocalDate(2011, 11, 4);
    List<String> files = new ArrayList<String>();
    Collections.addAll(files, new File(inputDir).list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("LCH") && name.endsWith(".xml");
      }
    }));
    final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile, false));
    os.write("LchTradeId,NpvAmount,CashflowDate,CashflowAmount\n".getBytes());
    final AtomicInteger failures = new AtomicInteger(0);
    CompletionService<List<Cashflow>> service = new ExecutorCompletionService<List<Cashflow>>(executor);
    long now  = System.currentTimeMillis();
    for (String file : files) {
      final String id = file.substring(0, file.indexOf('.'));
        service.submit(new Callable<List<Cashflow>>() {
          @Override
          public List<Cashflow> call() throws Exception {
            try {
              return generator.generateCashflows(date, id);
            } catch(Exception e) {
              failures.incrementAndGet();
              log.error("Error calculating cashflows for trade " + id, e);
              return null;
            }
          }
        });
    }
    int total = files.size();
    for(int i = 0; i < total; i++) {
      try {
        List<Cashflow> cashflows = service.take().get();
        if(cashflows != null) {
          writeCashflows(os, cashflows.get(0).getTradeId(), cashflows);
        }
      } catch(Exception e) {
        log.error("");
      }
    }
    long timeTaken = System.currentTimeMillis() - now;
    log.info("Priced {} trades with {} failures", total, failures);
    log.info("Total time: {}ms. Average time: {}ms", timeTaken, timeTaken/files.size());
    os.close();
  }

  private void writeCashflows(BufferedOutputStream os, String id, List<Cashflow> cashflows) throws IOException {
    for (Cashflow cashflow : cashflows) {
      StringBuilder sb = new StringBuilder();
      sb.append(id).append(",");
      sb.append(cashflow.getNpv()).append(",");
      sb.append(cashflow.getDate()).append(",");
      sb.append(cashflow.getAmount());
      sb.append('\n');
      os.write(sb.toString().getBytes());
    }
  }

  public static void main(final String[] args) throws IOException {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule(args[0]));
    DMPReportGenerator reporter = injector.getInstance(DMPReportGenerator.class);
    reporter.generateReport(args[0], args[1]);
  }
}
