package net.formicary.pricer.impl;

import net.formicary.pricer.CurveManager;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:36 AM
 */
public class CurveManagerImpl implements CurveManager {
  @Override
  public double getInterpolatedRate(LocalDate date, String ccy, String tenor) {
    return 0;
  }

  @Override
  public String getDiscountCurve(String cc, String tenor) {
    return null;
  }

  @Override
  public String getForwardCurve(String ccy, String tenor) {
    return null;
  }
}
