package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.CalculationPeriodAmount;
import net.formicary.pricer.model.DayCountFraction;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:00 PM
 */
public class CalculationPeriodAmountParser implements NodeParser<CalculationPeriodAmount> {

  enum Element {
    calculationPeriodAmount,
    calculation,
    notionalSchedule,
    notionalStepSchedule,
    initialValue,
    currency,
    floatingRateCalculation,
    floatingRateIndex,
    indexTenor,
    periodMultiplier,
    period,
    spreadSchedule,
    dayCountFraction,
    fixedRateSchedule
  }

  @Override
  public CalculationPeriodAmount parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodAmount amount = new CalculationPeriodAmount();

    double initialValue = 0;

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case dayCountFraction:
            String text = reader.getElementText().replace('/', '_');
            text = text.replace("30", "THIRTY");
            amount.setDayCountFraction(DayCountFraction.valueOf(text));
            break;
          case currency:
            amount.setCurrency(reader.getElementText());
            break;
          case initialValue:
            initialValue = Double.valueOf(reader.getElementText());
            break;
          case floatingRateIndex:
            amount.setFloatingRateIndex(reader.getElementText());
            break;
          case periodMultiplier:
            amount.setPeriodMultiplier(Integer.parseInt(reader.getElementText()));
            break;
          case period:
            amount.setPeriod(reader.getElementText());
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case calculationPeriodAmount:
            return amount;
          case spreadSchedule:
            amount.setSpreadSchedule(initialValue);
            break;
          case notionalStepSchedule:
            amount.setNotional(initialValue);
            break;
          case fixedRateSchedule:
            amount.setFixedRate(initialValue);
        }
      }
    }
    return amount;
  }
}
