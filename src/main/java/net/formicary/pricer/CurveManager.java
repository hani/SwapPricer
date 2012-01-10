package net.formicary.pricer;

import net.formicary.pricer.util.FastDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:29 AM
 */
public interface CurveManager {
  public String getForwardCurve(String ccy, String tenor);
  public String getDiscountCurve(String cc, String tenor);
  double getInterpolatedDiscountRate(FastDate date, String ccy, String tenor);
  double getInterpolatedForwardRate(FastDate date, String ccy, String tenor);
  double getDiscountFactor(FastDate flowDate, FastDate valuationDate, String ccy, String tenor, boolean isFixed);
  double getImpliedForwardRate(FastDate start, FastDate end, FastDate valuationDate, String ccy, String tenor);
}
