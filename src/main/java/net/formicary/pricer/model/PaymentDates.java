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
  private int paymentFrequencyPeriodMultiplier;
  private String paymentFrequencyPeriod;
  private int paymentDaysOffsetPeriodMultiplier;
  private String paymentDaysOffsetPeriod;
  private BusinessDayConvention businessDayConvention;
  private List<String> businessCenters = new ArrayList<String>();
  private PayRelativeTo payRelativeTo;
  private DayType dayType;

  public CalculationPeriodDates getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(CalculationPeriodDates calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
  }

  public int getPaymentFrequencyPeriodMultiplier() {
    return paymentFrequencyPeriodMultiplier;
  }

  public void setPaymentFrequencyPeriodMultiplier(int periodMultiplier) {
    this.paymentFrequencyPeriodMultiplier = periodMultiplier;
  }

  public String getPaymentFrequencyPeriod() {
    return paymentFrequencyPeriod;
  }

  public void setPaymentFrequencyPeriod(String paymentFrequencyPeriod) {
    this.paymentFrequencyPeriod = paymentFrequencyPeriod;
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
    return paymentFrequencyPeriodMultiplier + paymentFrequencyPeriod;
  }

  public void setPayRelativeTo(PayRelativeTo payRelativeTo) {
    this.payRelativeTo = payRelativeTo;
  }

  public PayRelativeTo getPayRelativeTo() {
    return payRelativeTo;
  }

  public int getPaymentDaysOffsetPeriodMultiplier() {
    return paymentDaysOffsetPeriodMultiplier;
  }

  public void setPaymentDaysOffsetPeriodMultiplier(int paymentDaysOffsetPeriodMultiplier) {
    this.paymentDaysOffsetPeriodMultiplier = paymentDaysOffsetPeriodMultiplier;
  }

  public String getPaymentDaysOffsetPeriod() {
    return paymentDaysOffsetPeriod;
  }

  public void setPaymentDaysOffsetPeriod(String paymentDaysOffsetPeriod) {
    this.paymentDaysOffsetPeriod = paymentDaysOffsetPeriod;
  }

  public DayType getDayType() {
    return dayType;
  }

  public void setDayType(DayType dayType) {
    this.dayType = dayType;
  }
}
