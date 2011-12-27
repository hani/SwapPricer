package net.formicary.pricer.impl;

import net.formicary.pricer.TradeStore;
import org.fpml.spec503wd3.Swap;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:26 PM
 */
@Singleton
public class SimpleTradeStore implements TradeStore {
  private Map<String, Swap> map = new HashMap<String, Swap>();

  @Override
  public Swap getTrade(String id) {
    return map.get(id);
  }

  public void addTrade(Swap trade) {
    map.put(trade.getId(), trade);
  }
}
