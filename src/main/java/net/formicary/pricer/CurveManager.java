package net.formicary.pricer;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:29 AM
 */
public interface CurveManager {
  public String getForwardCurve(String ccy, String tenor);
  public String getDiscountCurve(String cc, String tenor);
  double getInterpolatedRate(LocalDate date, String ccy, String tenor);
  double getDiscountFactor(LocalDate flowDate, LocalDate valuationDate, String ccy, String tenor);
}
