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

  public Cashflow() {
  }

  public Cashflow(double npv, LocalDate date) {
    this.npv = npv;
    this.date = date;
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
      '}';
  }
}
