package net.formicary.pricer.model;

import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 11:45 AM
 */
public class CalculationPeriodDates {
  private PeriodDate effectiveDate;
  private PeriodDate terminationDate;

  private BusinessDayConvention periodConvention;
  private List<String> periodBusinessCenters;
  private int periodMultiplier;
  private String period;
  private int rollConvention;

  public PeriodDate getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(PeriodDate effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public PeriodDate getTerminationDate() {
    return terminationDate;
  }

  public void setTerminationDate(PeriodDate terminationDate) {
    this.terminationDate = terminationDate;
  }

  public BusinessDayConvention getPeriodConvention() {
    return periodConvention;
  }

  public void setPeriodConvention(BusinessDayConvention periodConvention) {
    this.periodConvention = periodConvention;
  }

  public List<String> getPeriodBusinessCenters() {
    return periodBusinessCenters;
  }

  public void setPeriodBusinessCenters(List<String> periodBusinessCenters) {
    this.periodBusinessCenters = periodBusinessCenters;
  }

  public int getPeriodMultiplier() {
    return periodMultiplier;
  }

  public void setPeriodMultiplier(int periodMultiplier) {
    this.periodMultiplier = periodMultiplier;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public int getRollConvention() {
    return rollConvention;
  }

  public void setRollConvention(int i) {
    this.rollConvention = i;
  }
}
