package net.formicary.pricer.impl.parsers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.util.DateUtil;
import org.fpml.spec503wd3.*;

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
    compoundingMethod,
    initialRate,
    knownAmountSchedule,
    step,
    stepDate,
    stepValue
  }

  @Override
  public CalculationPeriodAmount parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodAmount cpa = new CalculationPeriodAmount();

    String initialValue = null;
    Currency currency = null;
    List<Step> steps = null;
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case dayCountFraction:
            DayCountFraction dayCountFraction = new DayCountFraction();
            dayCountFraction.setValue(reader.getElementText());
            cpa.getCalculation().setDayCountFraction(dayCountFraction);
            break;
          case currency:
            currency = new Currency();
            currency.setValue(reader.getElementText());
            break;
          case calculation:
            cpa.setCalculation(new Calculation());
            break;
          case initialValue:
            initialValue = reader.getElementText();
            break;
          case floatingRateIndex:
            FloatingRateCalculation c = getFloatingCalculation(cpa.getCalculation());
            FloatingRateIndex i = new FloatingRateIndex();
            i.setValue(reader.getElementText());
            c.setFloatingRateIndex(i);
            break;
          case knownAmountSchedule:
            cpa.setKnownAmountSchedule(new AmountSchedule());
            break;
          case periodMultiplier:
            getFloatingCalculation(cpa.getCalculation()).getIndexTenor().setPeriodMultiplier(new BigInteger(reader.getElementText()));
            break;
          case period:
            getFloatingCalculation(cpa.getCalculation()).getIndexTenor().setPeriod(PeriodEnum.valueOf(reader.getElementText()));
            break;
          case spreadSchedule:
            SpreadSchedule spread = new SpreadSchedule();
            getFloatingCalculation(cpa.getCalculation()).getSpreadSchedule().add(spread);
            break;
          case notionalSchedule:
            cpa.getCalculation().setNotionalSchedule(new Notional());
            break;
          case compoundingMethod:
            cpa.getCalculation().setCompoundingMethod(CompoundingMethodEnum.fromValue(reader.getElementText()));
            break;
          case initialRate:
            getFloatingCalculation(cpa.getCalculation()).setInitialRate(new BigDecimal(reader.getElementText()));
            break;
          case notionalStepSchedule:
            AmountSchedule schedule = new AmountSchedule();
            cpa.getCalculation().getNotionalSchedule().setNotionalStepSchedule(schedule);
            break;
          case step:
            if(steps == null) {
              steps = new ArrayList<Step>(20);
            }
            steps.add(new Step());
            break;
          case stepDate:
            Step step = steps.get(steps.size() - 1);
            step.setStepDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case stepValue:
            step = steps.get(steps.size() - 1);
            step.setStepValue(new BigDecimal(reader.getElementText()));
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case calculationPeriodAmount:
            return cpa;
          case spreadSchedule:
            getFloatingCalculation(cpa.getCalculation()).getSpreadSchedule().get(0).setInitialValue(new BigDecimal(initialValue));
            initialValue = null;
            break;
          case notionalStepSchedule:
            Notional notionalSchedule = cpa.getCalculation().getNotionalSchedule();
            notionalSchedule.getNotionalStepSchedule().setInitialValue(new BigDecimal(initialValue));
            notionalSchedule.getNotionalStepSchedule().setCurrency(currency);
            if(steps != null) {
              notionalSchedule.getNotionalStepSchedule().getStep().addAll(steps);
              steps = null;
            }
            initialValue = null;
            currency = null;
            break;
          case knownAmountSchedule:
            cpa.getKnownAmountSchedule().setInitialValue(new BigDecimal(initialValue));
            cpa.getKnownAmountSchedule().setCurrency(currency);
            initialValue = null;
            currency = null;
            break;
          case fixedRateSchedule:
            Schedule fr = new Schedule();
            fr.setInitialValue(new BigDecimal(initialValue));
            cpa.getCalculation().setFixedRateSchedule(fr);
            if(steps != null) {
              fr.getStep().addAll(steps);
              steps = null;
            }
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
