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
  private double fixedRate;
  private double spreadSchedule;
  private String floatingRateIndex;
  private int periodMultiplier;
  private String period;
  private CompoundingMethod compoundingMethod;

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

  public void setFixedRate(double fixedRate) {
    this.fixedRate = fixedRate;
  }

  public void setSpreadSchedule(double spreadSchedule) {
    this.spreadSchedule = spreadSchedule;
  }

  public double getFixedRate() {
    return fixedRate;
  }

  public double getSpreadSchedule() {
    return spreadSchedule;
  }

  public void setFloatingRateIndex(String floatingRateIndex) {
    this.floatingRateIndex = floatingRateIndex;
  }

  public void setPeriodMultiplier(int periodMultiplier) {
    this.periodMultiplier = periodMultiplier;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public String getFloatingRateIndex() {
    return floatingRateIndex;
  }

  public int getPeriodMultiplier() {
    return periodMultiplier;
  }

  public String getPeriod() {
    return period;
  }

  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod;
  }

  public void setCompoundingMethod(CompoundingMethod compoundingMethod) {
    this.compoundingMethod = compoundingMethod;
  }
}
