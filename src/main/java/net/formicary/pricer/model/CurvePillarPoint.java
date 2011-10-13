package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:30 AM
 */
public class CurvePillarPoint {
  private String curveName;
  private LocalDate closeDate;
  private LocalDate maturityDate;
  private double accrualFactor;
  private double zeroRate;
  private double discountFactor;

  public String getCurveName() {
    return curveName;
  }

  public void setCurveName(String curveName) {
    this.curveName = curveName;
  }

  public LocalDate getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(LocalDate closeDate) {
    this.closeDate = closeDate;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public void setMaturityDate(LocalDate maturityDate) {
    this.maturityDate = maturityDate;
  }

  public double getAccrualFactor() {
    return accrualFactor;
  }

  public void setAccrualFactor(double accrualFactor) {
    this.accrualFactor = accrualFactor;
  }

  public double getZeroRate() {
    return zeroRate;
  }

  public void setZeroRate(double zeroRate) {
    this.zeroRate = zeroRate;
  }

  public double getDiscountFactor() {
    return discountFactor;
  }

  public void setDiscountFactor(double discountFactor) {
    this.discountFactor = discountFactor;
  }
}
