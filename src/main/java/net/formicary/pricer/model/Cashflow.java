package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:22 PM
 */
public class Cashflow implements Comparable<Cashflow>{
  private String tradeId;
  private double npv;
  private LocalDate date;
  private FlowType type;
  private double discountFactor;
  private double rate;
  private double amount;
  private double dayCountFraction;

  public Cashflow() {
  }

  public Cashflow(double npv, LocalDate date) {
    this.npv = npv;
    this.date = date;
  }

  public String getTradeId() {
    return tradeId;
  }

  public void setTradeId(String tradeId) {
    this.tradeId = tradeId;
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

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public double getDayCountFraction() {
    return dayCountFraction;
  }

  public void setDayCountFraction(double dayCountFraction) {
    this.dayCountFraction = dayCountFraction;
  }

  @Override
  public String toString() {
    return "Cashflow{" +
        "npv=" + npv +
        ", amount=" + amount +
        ", date=" + date +
        ", type=" + type +
        ", discountFactor=" + discountFactor +
        ", rate=" + rate +
        "}\n";
  }

  public void reverse() {
    this.npv = -npv;
    this.amount = -amount;
  }
}
