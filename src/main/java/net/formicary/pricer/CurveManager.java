package net.formicary.pricer;

import org.fpml.spec503wd3.Interval;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:29 AM
 */
public interface CurveManager {
  public String getForwardCurve(String ccy, String tenor);
  public String getDiscountCurve(String cc, String tenor);
  double getInterpolatedDiscountRate(LocalDate date, String ccy, String tenor);
  double getInterpolatedForwardRate(LocalDate date, String ccy, String tenor);
  double getDiscountFactor(LocalDate flowDate, LocalDate valuationDate, String ccy, Interval tenor, boolean isFixed);
  double getImpliedForwardRate(LocalDate start, LocalDate end, LocalDate valuationDate, String ccy, Interval tenor);
}
