package net.formicary.pricer.model;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:07 PM
 */
public class CalculationPeriodAmount {
  private double notional;
  private String currency;
  private DayCountFraction dayCountFraction;

  public double getNotional() {
    return notional;
  }

  public void setNotional(double notional) {
    this.notional = notional;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public DayCountFraction getDayCountFraction() {
    return dayCountFraction;
  }

  public void setDayCountFraction(DayCountFraction dayCountFraction) {
    this.dayCountFraction = dayCountFraction;
  }
}
