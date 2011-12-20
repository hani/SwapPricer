package net.formicary.pricer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:22 PM
 */
public class PaymentDates {
  private CalculationPeriodDates calculationPeriodDates;
  private int periodMultiplier;
  private String period;
  private BusinessDayConvention businessDayConvention;
  private List<String> businessCenters = new ArrayList<String>();
  private PayRelativeTo payRelativeTo;

  public CalculationPeriodDates getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(CalculationPeriodDates calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
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

  public BusinessDayConvention getBusinessDayConvention() {
    return businessDayConvention;
  }

  public void setBusinessDayConvention(BusinessDayConvention businessDayConvention) {
    this.businessDayConvention = businessDayConvention;
  }

  public List<String> getBusinessCenters() {
    return businessCenters;
  }

  public void setBusinessCenters(List<String> businessCenters) {
    this.businessCenters = businessCenters;
  }

  public String getPaymentFrequency() {
    return periodMultiplier + period;
  }

  public void setPayRelativeTo(PayRelativeTo payRelativeTo) {
    this.payRelativeTo = payRelativeTo;
  }

  public PayRelativeTo getPayRelativeTo() {
    return payRelativeTo;
  }
}
