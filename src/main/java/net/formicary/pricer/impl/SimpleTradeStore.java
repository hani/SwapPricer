package net.formicary.pricer.impl;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

import net.formicary.pricer.TradeStore;
import org.fpml.spec503wd3.Product;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:26 PM
 */
@Singleton
public class SimpleTradeStore implements TradeStore {
  private Map<String, Product> map = new HashMap<String, Product>();

  @Override
  public Product getTrade(String id) {
    return map.get(id);
  }

  public void addTrade(Product trade) {
    map.put(trade.getId(), trade);
  }
}
