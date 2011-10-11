package net.formicary.pricer.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.formicary.pricer.CurveManager;
import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:36 AM
 */
public class CurveManagerImpl implements CurveManager {
  //a map of currency -> (tenor -> forward,discount) curves
  private Map<String, Map<String, String[]>> mapping = new HashMap<String, Map<String, String[]>>();

  public CurveManagerImpl() throws IOException {
    List<String> lines = IOUtils.readLines(getClass().getResourceAsStream("/curvemapping.csv"));
    for(String line : lines) {
      String[] data = line.split(",");
      String ccy = data[0];
      String tenor = data[1];
      String forwardCurve = data[2];
      String discountCurve = data[3];
      Map<String, String[]> tenorMap = mapping.get(ccy);
      if(tenorMap == null) {
        tenorMap = new HashMap<String, String[]>();
        mapping.put(ccy, tenorMap);
      }
      tenorMap.put(tenor, new String[]{forwardCurve, discountCurve});
    }
  }

  @Override
  public double getInterpolatedRate(LocalDate date, String ccy, String tenor) {
    return 0;
  }

  @Override
  public String getDiscountCurve(String ccy, String tenor) {
    return mapping.get(ccy).get(tenor)[1];
  }

  @Override
  public String getForwardCurve(String ccy, String tenor) {
    return mapping.get(ccy).get(tenor)[2];
  }
}
