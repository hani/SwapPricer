package net.formicary.pricer.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.model.VanillaSwap;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:26 PM
 */
@Singleton
public class SimpleTradeStore implements TradeStore {
  private Map<String, VanillaSwap> map = new HashMap<String, VanillaSwap>();

  @Override
  public VanillaSwap getTrade(String id) {
    return map.get(id);
  }

  public void addTrade(VanillaSwap trade) {
    map.put(trade.getId(), trade);
  }
}
