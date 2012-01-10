package net.formicary.pricer.model;

import net.formicary.pricer.util.FastDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:30 AM
 */
public class CurvePillarPoint implements Comparable<CurvePillarPoint> {
  private String curveName;
  private FastDate closeDate;
  private FastDate maturityDate;
  private double accrualFactor;
  private double zeroRate;
  private double discountFactor;

  public CurvePillarPoint() {
  }

  public CurvePillarPoint(String curveName, FastDate maturityDate) {
    this.curveName = curveName;
    this.maturityDate = maturityDate;
  }

  public String getCurveName() {
    return curveName;
  }

  public void setCurveName(String curveName) {
    this.curveName = curveName;
  }

  public FastDate getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(FastDate closeDate) {
    this.closeDate = closeDate;
  }

  public FastDate getMaturityDate() {
    return maturityDate;
  }

  public void setMaturityDate(FastDate maturityDate) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CurvePillarPoint)) return false;

    CurvePillarPoint that = (CurvePillarPoint) o;

    if (!closeDate.equals(that.closeDate)) return false;
    if (!curveName.equals(that.curveName)) return false;
    //if we ever start storing curves for different close dates, we'll need to take that into account here

    return true;
  }

  @Override
  public int hashCode() {
    int result = curveName.hashCode();
    result = 31 * result + maturityDate.hashCode();
    return result;
  }

  @Override
  public int compareTo(CurvePillarPoint o) {
    int c = maturityDate.compareTo(o.maturityDate);
    if(c != 0) return c;
    return curveName.compareTo(o.curveName);
  }

  @Override
  public String toString() {
    return "CurvePillarPoint{" +
        "maturityDate=" + maturityDate +
        ", zeroRate=" + zeroRate +
        ", discountFactor=" + discountFactor +
        ", accrualFactor=" + accrualFactor +
        ", curveName='" + curveName + '\'' +
        '}';
  }
}
