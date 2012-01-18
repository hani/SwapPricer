package net.formicary.pricer;

import net.formicary.pricer.util.FastDate;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:28 PM
 */
public interface RateManager {
  double getZeroRate(String indexName, String currency, String tenor, FastDate date);
  double getDiscountFactor(String indexName, String currency, String tenor, FastDate date, FastDate valuationDate);
}
