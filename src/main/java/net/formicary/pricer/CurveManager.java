package net.formicary.pricer;

import hirondelle.date4j.DateTime;
import org.fpml.spec503wd3.Interval;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:29 AM
 */
public interface CurveManager {
  public String getForwardCurve(String ccy, String tenor);
  public String getDiscountCurve(String cc, String tenor);
  double getInterpolatedDiscountRate(DateTime date, String ccy, String tenor);
  double getInterpolatedForwardRate(DateTime date, String ccy, String tenor);
  double getDiscountFactor(DateTime flowDate, DateTime valuationDate, String ccy, Interval tenor, boolean isFixed);
  double getImpliedForwardRate(DateTime start, DateTime end, DateTime valuationDate, String ccy, Interval tenor);
}
