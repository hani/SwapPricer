package net.formicary.pricer;

import com.google.inject.ImplementedBy;
import net.formicary.pricer.impl.FpmlSTAXTradeStore;
import org.fpml.spec503wd3.Product;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:24 PM
 */
@ImplementedBy(FpmlSTAXTradeStore.class)
public interface TradeStore {
  Product getTrade(String id);
}
