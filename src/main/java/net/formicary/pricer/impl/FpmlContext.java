package net.formicary.pricer.impl;

import net.formicary.pricer.model.CalculationPeriodDates;

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
