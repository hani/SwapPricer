package net.formicary.pricer.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javolution.text.TextBuilder;
import javolution.text.TypeFormat;
import net.formicary.pricer.CashflowGenerator;
import net.formicary.pricer.JedisPersistenceModule;
import net.formicary.pricer.PricerModule;
import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.util.FastDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hsuleiman
 *         Date: 12/28/11
 *         Time: 9:32 AM
 */
public class DMPReportGenerator {
  @Inject private CashflowGenerator generator;
  @Inject private Executor executor;
  private boolean showTraces = false;

  private static final Logger log = LoggerFactory.getLogger(DMPReportGenerator.class);

  public void generateReport(String inputDir, String outputFile) throws IOException {
    final FastDate date = new FastDate(2011, 11, 4);
    List<String> files = new ArrayList<String>();
    File dir = new File(inputDir);
    if(!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Input dir " + dir.getAbsolutePath() +" does not exist or is not a directory");
    }
    Collections.addAll(files, dir.list(new FilenameFilter() {
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
              if(showTraces)
                log.error("Error calculating cashflows for trade " + id, e);
              else
                log.error("Error calculating cashflows for trade " + id + ": " + e.getMessage());
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
        log.error("Error writing cashflow", e);
      }
    }
    long timeTaken = System.currentTimeMillis() - now;
    log.info("Priced {} trades with {} failures", total, failures);
    log.info("Total time: {}ms. Average time: {}ms", timeTaken, timeTaken/files.size());
    os.close();
  }

  private void writeCashflows(BufferedOutputStream os, String id, List<Cashflow> cashflows) throws IOException {
    TextBuilder sb = TextBuilder.newInstance();
    for (Cashflow cashflow : cashflows) {
      sb.clear();
      sb.append(id);
      sb.append(',');
      TypeFormat.format(cashflow.getNpv(), sb).append(",");
      FastDate d = cashflow.getDate();
      TypeFormat.format(d.getYear(), sb).append('/');
      TypeFormat.format(d.getMonth(), sb).append('/');
      TypeFormat.format(d.getDay(), sb).append('/');
      sb.append(",");
      TypeFormat.format(cashflow.getAmount(), sb);
      sb.append('\n');
      os.write(sb.toString().getBytes());
    }
  }

  public static void main(final String[] args) throws IOException {
    Injector injector = Guice.createInjector(new PricerModule(), new JedisPersistenceModule(args[0]));
    DMPReportGenerator reporter = injector.getInstance(DMPReportGenerator.class);
    reporter.generateReport(args[0], args[1]);
  }
}
