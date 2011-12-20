package net.formicary.pricer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:22 PM
 */
public class ResetDates {
  private ResetRelativeTo resetRelativeTo;
  private int fixingMultiplier;
  private String fixingPeriod;
  private BusinessDayConvention businessDayConvention;
  private List<String> businessCenters = new ArrayList<String>();
  private int resetMultiplier;
  private String resetPeriod;
  private CalculationPeriodDates calculationPeriodDates;
  private DayType dayType;

  public int getFixingMultiplier() {
    return fixingMultiplier;
  }

  public void setFixingMultiplier(int fixingMultiplier) {
    this.fixingMultiplier = fixingMultiplier;
  }

  public String getFixingPeriod() {
    return fixingPeriod;
  }

  public void setFixingPeriod(String fixingPeriod) {
    this.fixingPeriod = fixingPeriod;
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

  public int getResetMultiplier() {
    return resetMultiplier;
  }

  public void setResetMultiplier(int resetMultiplier) {
    this.resetMultiplier = resetMultiplier;
  }

  public String getResetPeriod() {
    return resetPeriod;
  }

  public void setResetPeriod(String resetPeriod) {
    this.resetPeriod = resetPeriod;
  }

  public ResetRelativeTo getResetRelativeTo() {
    return resetRelativeTo;
  }

  public void setResetRelativeTo(ResetRelativeTo resetRelativeTo) {
    this.resetRelativeTo = resetRelativeTo;
  }

  public CalculationPeriodDates getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(CalculationPeriodDates calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
  }

  public DayType getDayType() {
    return dayType;
  }

  public void setDayType(DayType dayType) {
    this.dayType = dayType;
  }
}
