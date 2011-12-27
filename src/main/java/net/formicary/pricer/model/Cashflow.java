package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:22 PM
 */
public class Cashflow implements Comparable<Cashflow>{
  private double npv;
  private LocalDate date;
  private FlowType type;
  private double discountFactor;
  private double rate;

  public Cashflow() {
  }

  public Cashflow(double npv, LocalDate date) {
    this.npv = npv;
    this.date = date;
  }

  public double getDiscountFactor() {
    return discountFactor;
  }

  public void setDiscountFactor(double discountFactor) {
    this.discountFactor = discountFactor;
  }

  public FlowType getType() {
    return type;
  }

  public void setType(FlowType type) {
    this.type = type;
  }

  public double getNpv() {
    return npv;
  }

  public void setNpv(double npv) {
    this.npv = npv;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public double getRate() {
    return rate;
  }

  public void setRate(double rate) {
    this.rate = rate;
  }

  @Override
  public int compareTo(Cashflow o) {
    if(o == null) return -1;
    return getDate().compareTo(o.getDate());
  }

  @Override
  public String toString() {
    return "Cashflow{" +
        "npv=" + npv +
        ", date=" + date +
        ", type=" + type +
        ", discountFactor=" + discountFactor +
        ", rate=" + rate +
        '}';
  }
}
