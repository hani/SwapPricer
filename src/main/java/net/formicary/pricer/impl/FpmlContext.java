package net.formicary.pricer.impl;

import net.formicary.pricer.model.CalculationPeriodDates;
import net.formicary.pricer.model.SwapStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 12:54 PM
 */
public class FpmlContext {
  private Map<String, List<String>> businessCenters = new HashMap<String, List<String>>();
  private Map<String, CalculationPeriodDates> calculationPeriodDates = new HashMap<String, CalculationPeriodDates>();
  private Map<String, NodeParser> parsers;
  private SwapStream stream1;
  private SwapStream stream2;

  public SwapStream getStream1() {
    return stream1;
  }

  public void setStream1(SwapStream stream1) {
    this.stream1 = stream1;
  }

  public SwapStream getStream2() {
    return stream2;
  }

  public void setStream2(SwapStream stream2) {
    this.stream2 = stream2;
  }

  public Map<String, List<String>> getBusinessCenters() {
    return businessCenters;
  }

  public void setBusinessCenters(Map<String, List<String>> businessCenters) {
    this.businessCenters = businessCenters;
  }

  public Map<String, CalculationPeriodDates> getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(Map<String, CalculationPeriodDates> calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
  }

  public void setParsers(Map<String, NodeParser> parsers) {
    this.parsers = parsers;
  }

  public Map<String, NodeParser> getParsers() {
    return parsers;
  }
}
