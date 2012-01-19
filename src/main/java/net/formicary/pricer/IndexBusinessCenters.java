package net.formicary.pricer;

import java.util.HashMap;
import java.util.Map;

import org.fpml.spec503wd3.BusinessCenter;
import org.fpml.spec503wd3.BusinessCenters;

/**
 * @author hani
 *         Date: 1/18/12
 *         Time: 10:07 PM
 */
public class IndexBusinessCenters {
  private static Map<String, BusinessCenter> bcByCcy = new HashMap<String, BusinessCenter>();
  private static final BusinessCenter EUTA = getBusinessCenter("EUTA");
  private static final BusinessCenter GBLO = getBusinessCenter("GBLO");
  private static final BusinessCenter USNY = getBusinessCenter("USNY");
  private static final BusinessCenter CATO = getBusinessCenter("CATO");

  static {
    bcByCcy.put("EUR", EUTA);
    bcByCcy.put("GBP", getBusinessCenter("GBLO"));
    bcByCcy.put("USD", getBusinessCenter("USNY"));
  }

  private static BusinessCenter getBusinessCenter(String name) {
    BusinessCenter bc = new BusinessCenter();
    bc.setId(name);
    bc.setValue(name);
    return bc;
  }

  public static BusinessCenters getCenters(String indexName, String ccy) {
    BusinessCenters centers = new BusinessCenters();
    boolean hasEUTA = false;
    boolean hasGBLO = false;
    if("LIBOR".equals(indexName) && !"EUR".equals(ccy)) {
      centers.getBusinessCenter().add(GBLO);
      hasGBLO = true;
    } else if ("EURIBOR".equals(indexName)) {
      centers.getBusinessCenter().add(EUTA);
      hasEUTA = true;
    }
    if("EUR".equals(ccy) && !hasEUTA) {
      centers.getBusinessCenter().add(EUTA);
    }
    if("USD".equals(ccy)) {
      centers.getBusinessCenter().add(USNY);
    }
    if("CAD".equals(ccy)) {
      centers.getBusinessCenter().add(CATO);
    }
    if("GBP".equals(ccy) && !hasGBLO) {
      centers.getBusinessCenter().add(GBLO);
    }
    return centers;
  }
}
