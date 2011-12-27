package net.formicary.pricer;

import org.fpml.spec503wd3.Swap;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:24 PM
 */
public interface TradeStore {
  Swap getTrade(String id);
}
