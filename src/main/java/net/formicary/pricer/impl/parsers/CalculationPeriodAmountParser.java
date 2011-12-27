package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.*;

import javax.xml.bind.JAXBElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.math.BigDecimal;
import java.math.BigInteger;

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
    fixedRateSchedule,
    compoundingMethod
  }

  @Override
  public CalculationPeriodAmount parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodAmount cpa = new CalculationPeriodAmount();
    Calculation calculation = new Calculation();
    Notional notional = new Notional();
    calculation.setNotionalSchedule(notional);
    cpa.setCalculation(calculation);
    AmountSchedule amount = new AmountSchedule();
    notional.setNotionalStepSchedule(amount);

    String initialValue = "0";

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case dayCountFraction:
            DayCountFraction dayCountFraction = new DayCountFraction();
            dayCountFraction.setValue(reader.getElementText());
            calculation.setDayCountFraction(dayCountFraction);
            break;
          case currency:
            Currency currency = new Currency();
            currency.setValue(reader.getElementText());
            amount.setCurrency(currency);
            break;
          case initialValue:
            initialValue = reader.getElementText();
            break;
          case floatingRateIndex:
            FloatingRateCalculation c = getFloatingCalculation(calculation);
            FloatingRateIndex i = new FloatingRateIndex();
            i.setValue(reader.getElementText());
            c.setFloatingRateIndex(i);
            break;
          case periodMultiplier:
            getFloatingCalculation(calculation).getIndexTenor().setPeriodMultiplier(new BigInteger(reader.getElementText()));
            break;
          case period:
            getFloatingCalculation(calculation).getIndexTenor().setPeriod(PeriodEnum.valueOf(reader.getElementText()));
            break;
          case compoundingMethod:
            calculation.setCompoundingMethod(CompoundingMethodEnum.fromValue(reader.getElementText()));
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case calculationPeriodAmount:
            return cpa;
          case spreadSchedule:
            //TODO where do we stuff this?
            break;
          case notionalStepSchedule:
            amount.setInitialValue(new BigDecimal(initialValue));
            break;
          case fixedRateSchedule:
            Schedule fr = new Schedule();
            fr.setInitialValue(new BigDecimal(initialValue));
            calculation.setFixedRateSchedule(fr);
            break;
        }
      }
    }
    return cpa;
  }

  private FloatingRateCalculation getFloatingCalculation(Calculation calculation) {
    JAXBElement<FloatingRateCalculation> fc = (JAXBElement<FloatingRateCalculation>) calculation.getRateCalculation();
    if(fc != null) {
      return fc.getValue();
    }
    FloatingRateCalculation floating = new FloatingRateCalculation();
    floating.setIndexTenor(new Interval());
    ObjectFactory f = new ObjectFactory();
    calculation.setRateCalculation((f.createFloatingRateCalculation(floating)));
    return floating;
  }
}
