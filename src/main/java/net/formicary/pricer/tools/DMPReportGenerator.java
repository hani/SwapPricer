package net.formicary.pricer.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javolution.text.TextBuilder;
import javolution.text.TypeFormat;
import net.formicary.pricer.CashflowGenerator;
import net.formicary.pricer.PersistenceModule;
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

  private static final Logger log = LoggerFactory.getLogger(DMPReportGenerator.class);

  private DMPConfig DMPConfig;

  public DMPConfig getDMPConfig() {
    return DMPConfig;
  }

  public void setDMPConfig(DMPConfig DMPConfig) {
    this.DMPConfig = DMPConfig;
  }

  public void generateReport() throws IOException {
    final FastDate date = new FastDate(2011, 11, 4);
    List<String> files = new ArrayList<String>();
    File dir = new File(DMPConfig.getInputDir());
    if(!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Input dir " + dir.getAbsolutePath() +" does not exist or is not a directory");
    }
    Collections.addAll(files, dir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("LCH") && name.endsWith(".xml");
      }
    }));
    final BufferedWriter os = new BufferedWriter(new FileWriter(DMPConfig.getOutputFile(), false));
    os.write("LchTradeId,NpvAmount,CashflowDate,CashflowAmount\n");
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
              if(DMPConfig.isShowTraces())
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
          //help the gc
          cashflows.clear();
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

  private void writeCashflows(Writer os, String id, List<Cashflow> cashflows) throws IOException {
    TextBuilder sb = TextBuilder.newInstance();
    for (Cashflow cashflow : cashflows) {
      sb.clear();
      sb.append(id);
      sb.append(',');
      TypeFormat.format(cashflow.getNpv(), sb).append(",");
      FastDate d = cashflow.getDate();
      TypeFormat.format(d.getYear(), sb).append('-');
      TypeFormat.format(d.getMonth(), sb).append('-');
      TypeFormat.format(d.getDay(), sb);
      sb.append(",");
      TypeFormat.format(cashflow.getAmount(), sb);
      sb.println(os);
    }
  }

  public static void main(final String[] args) throws IOException {
    DMPConfig config = new DMPConfig();
    new JCommander(config,  args);
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule(config.getInputDir()));
    DMPReportGenerator reporter = injector.getInstance(DMPReportGenerator.class);
    reporter.setDMPConfig(config);
    reporter.generateReport();
  }
}
