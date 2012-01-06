package net.formicary.pricer.tools;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;

/**
 * Class to suck out the relevant cashflows for a specified list of LCH trades from report 72
 * @author hani
 *         Date: 1/5/12
 *         Time: 7:41 PM
 */
public class CashflowExtractor {
  private File reportFile;
  private File fpmlDir;

  public CashflowExtractor(File reportFile, File fpmlDir) {
    this.reportFile = reportFile;
    this.fpmlDir = fpmlDir;
  }

  private void extractFlows() throws IOException {
    //read in list of trade IDs
    String[] files = fpmlDir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".xml");
      }
    });
    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String header = reader.readLine();
    int count = 1;
    String line;
    Map<String, List<String>> tradeIds = new HashMap<String, List<String>>(files.length);
    for(String file : files) {
      ArrayList<String> list = new ArrayList<String>();
      tradeIds.put(file.substring(0, file.lastIndexOf('.')), list);
    }
    while((line = reader.readLine()) != null) {
      for(Map.Entry<String, List<String>> entry : tradeIds.entrySet()) {
        if(line.contains(entry.getKey())) {
          line = line.replace(" 00:00:00", "");
          String[] items = line.split("\t");
          StringBuilder sb = new StringBuilder();
          sb.append(items[6]).append("\t\t");
          sb.append(items[8]).append("\t\t");
          sb.append(items[10]).append("\t\t");
          sb.append(items[12]).append("\t\t");
          sb.append(items[13]);
          entry.getValue().add(sb.toString());
        }
      }
      if(++count % 20000 == 0) {
        System.out.println("Processed " + count + " lines.");
      }
    }
    reader.close();
    for(Map.Entry<String, List<String>> entry : tradeIds.entrySet()) {
      if(entry.getValue().size() > 0) {
        File file = new File(fpmlDir, entry.getKey() + ".csv");
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        IOUtils.writeLines(entry.getValue(), "\n", output);
        output.close();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    CashflowExtractor extractor = new CashflowExtractor(new File(args[0]), new File(args[1]));
    extractor.extractFlows();
  }

}
