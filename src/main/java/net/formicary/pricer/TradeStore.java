package net.formicary.pricer;

import net.formicary.pricer.model.VanillaSwap;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:24 PM
 */
public interface TradeStore {
  VanillaSwap getTrade(String id);
}
