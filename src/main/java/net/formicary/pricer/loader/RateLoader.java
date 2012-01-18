package net.formicary.pricer.loader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javolution.text.TypeFormat;
import net.formicary.pricer.PersistenceModule;
import net.formicary.pricer.model.Index;
import net.formicary.pricer.util.FastDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 7:34 AM
 */
public abstract class RateLoader {
  private static final Logger log = LoggerFactory.getLogger(RateLoader.class);

  public void importHistoricRates(String file) throws IOException {
    List<File> files = new ArrayList<File>();
    File root = new File(file);
    if(!root.isDirectory()) {
      files.add(root);
    } else {
      files = list(root);
    }
    long now = System.currentTimeMillis();
    int count = 0;
    for(File f : files) {
      log.info("Parsing " + f);
      BufferedReader is = new BufferedReader(new FileReader(f));
      is.readLine();
      String line;
      while((line = is.readLine()) != null) {
        String[] items = line.split("\t");
        if(items.length < 7) continue;
        Index index = new Index();
        index.setCurrency(items[0]);
        index.setName(items[1]);
        index.setTenor(items[2] + items[3]);
        FastDate fixingDate = new FastDate(Integer.parseInt(items[4].substring(6, 10)), Integer.parseInt(items[4].substring(3, 5)), Integer.parseInt(items[4].substring(0, 2)));
        index.setFixingDate(fixingDate);
        index.setEffectiveDate(new FastDate(Integer.parseInt(items[5].substring(6, 10)), Integer.parseInt(items[5].substring(3, 5)), Integer.parseInt(items[5].substring(0, 2))));
        try {
          index.setRate(TypeFormat.parseDouble(items[6]));
          index.setRegulatoryBody(items[7]);
          save(index);
          if(++count % 20000 == 0) {
            log.info("Saved " + count  + " rates");
          }
        } catch(NumberFormatException e) {
          log.info("Invalid rate found for index " + index + ":" + items[6]);
        }
      }
      is.close();
    }
    log.info("Initialised historic rates in " + (System.currentTimeMillis() - now) + "ms with " + count + " rates");
  }

  private List<File> list(final File root) {
    final List<File> files = new ArrayList<File>();
    files.addAll(Arrays.asList(root.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        if(file.getName().contains(("00003"))) {
          return true;
        }
        if(file.isDirectory()) {
          files.addAll(list(file));
        }
        return false;
      }
    })));
    return files;
  }

  protected abstract void save(Index index);

  public static void main(String[] args) throws IOException {
    Injector i = Guice.createInjector(new PersistenceModule("src/test/resources/fpml"));
    i.getInstance(MongoRateLoader.class).importHistoricRates(args[0]);
  }
}
