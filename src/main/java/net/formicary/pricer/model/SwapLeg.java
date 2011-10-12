package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:07 AM
 */
public class SwapLeg {
  private LocalDate startDate;
  private LegType type;
  private boolean isPay;
  private BusinessDayConvention startBusinessDatConvention;
  private BusinessDayConvention endBusinessDatConvention;
  private BusinessDayConvention periodBusinessDatConvention;
  private LocalDate endDate;
  private String periodMultiplier;
  private int rollConvention;
  private int notional;
  private String floatingRateIndex;
  private DayCount dayCount;
  private int fixingDateOffset;
  private boolean fixingRelativeToStart;
  private String fixingCalendar;
  private String businessCentre;
  private double fixedRate;
  private String currency;

  public BusinessDayConvention[] getBusinessDayConventions() {
    return new BusinessDayConvention[] {startBusinessDatConvention, periodBusinessDatConvention, endBusinessDatConvention};
  }

  public BusinessDayConvention getPeriodBusinessDatConvention() {
    return periodBusinessDatConvention;
  }

  public void setPeriodBusinessDatConvention(BusinessDayConvention periodBusinessDatConvention) {
    this.periodBusinessDatConvention = periodBusinessDatConvention;
  }

  public String getBusinessCentre() {
    return businessCentre;
  }

  public void setBusinessCentre(String businessCentre) {
    this.businessCentre = businessCentre;
  }

  public double getFixedRate() {
    return fixedRate;
  }

  public void setFixedRate(double fixedRate) {
    this.fixedRate = fixedRate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LegType getType() {
    return type;
  }

  public void setType(LegType type) {
    this.type = type;
  }

  public boolean isPay() {
    return isPay;
  }

  public void setPay(boolean pay) {
    isPay = pay;
  }

  public BusinessDayConvention getStartBusinessDatConvention() {
    return startBusinessDatConvention;
  }

  public void setStartBusinessDatConvention(BusinessDayConvention startBusinessDatConvention) {
    this.startBusinessDatConvention = startBusinessDatConvention;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public BusinessDayConvention getEndBusinessDatConvention() {
    return endBusinessDatConvention;
  }

  public void setEndBusinessDatConvention(BusinessDayConvention endBusinessDatConvention) {
    this.endBusinessDatConvention = endBusinessDatConvention;
  }

  public String getPeriodMultiplier() {
    return periodMultiplier;
  }

  public void setPeriodMultiplier(String periodMultiplier) {
    this.periodMultiplier = periodMultiplier;
  }

  public int getRollConvention() {
    return rollConvention;
  }

  public void setRollConvention(int rollConvention) {
    this.rollConvention = rollConvention;
  }

  public int getNotional() {
    return notional;
  }

  public void setNotional(int notional) {
    this.notional = notional;
  }

  public String getFloatingRateIndex() {
    return floatingRateIndex;
  }

  public void setFloatingRateIndex(String floatingRateIndex) {
    this.floatingRateIndex = floatingRateIndex;
  }

  public DayCount getDayCount() {
    return dayCount;
  }

  public void setDayCount(DayCount dayCount) {
    this.dayCount = dayCount;
  }

  public int getFixingDateOffset() {
    return fixingDateOffset;
  }

  public void setFixingDateOffset(int fixingDateOffset) {
    this.fixingDateOffset = fixingDateOffset;
  }

  public boolean isFixingRelativeToStart() {
    return fixingRelativeToStart;
  }

  public void setFixingRelativeToStart(boolean fixingRelativeToStart) {
    this.fixingRelativeToStart = fixingRelativeToStart;
  }

  public String getFixingCalendar() {
    return fixingCalendar;
  }

  public void setFixingCalendar(String fixingCalendar) {
    this.fixingCalendar = fixingCalendar;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }
}
