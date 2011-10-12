package net.formicary.pricer.impl;

import javax.inject.Singleton;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.model.VanillaSwap;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlTradeStore implements TradeStore {
  @Override
  public VanillaSwap getTrade(String id) {
    return null;
  }
}
