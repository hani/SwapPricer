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
public class CalculationPeriodAmountParser implements NodeParser {

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
            amount.setNotional(Double.valueOf(reader.getElementText()));
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case calculationPeriodAmount:
            return amount;
        }
      }
    }
    return amount;
  }
}
