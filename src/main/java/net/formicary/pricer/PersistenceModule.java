package net.formicary.pricer;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.converters.SimpleValueConverter;
import com.google.code.morphia.converters.TypeConverter;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.inject.AbstractModule;
import com.mongodb.Mongo;
import net.formicary.pricer.impl.FpmlTradeStore;
import net.formicary.pricer.impl.RateManagerImpl;
import net.formicary.pricer.model.Index;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.net.UnknownHostException;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 8:02 AM
 */
public class PersistenceModule extends AbstractModule {
  static {
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);
  }

  @Override
  protected void configure() {
    try {
      Morphia morphia = new Morphia();
      morphia.map(Index.class);
      morphia.getMapper().getConverters().addConverter(new LocalDateConverter());
      Datastore datastore = morphia.createDatastore(new Mongo(), "swappricer");
      datastore.ensureIndexes();
      bind(Datastore.class).toInstance(datastore);
    } catch(UnknownHostException e) {
      addError(e);
    }
    bind(RateManager.class).to(RateManagerImpl.class);
    bind(TradeStore.class).to(FpmlTradeStore.class);
  }

  public class LocalDateConverter  extends TypeConverter implements SimpleValueConverter {

    public LocalDateConverter() {
      super(LocalDate.class);
    }

    @Override
    public Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
      if (val == null) return null;

      if (val instanceof LocalDate)
        return val;
      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
      return formatter.parseLocalDate(val.toString());
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
      return formatter.print((LocalDate)value);
    }
  }
}
