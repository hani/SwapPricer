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
import com.google.inject.name.Names;
import com.mongodb.Mongo;
import hirondelle.date4j.DateTime;
import net.formicary.pricer.impl.MongoRateManagerImpl;
import net.formicary.pricer.model.Index;

import java.net.UnknownHostException;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 8:02 AM
 */
public class PersistenceModule extends AbstractModule {
  private String fpmlDir;
  static {
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);
  }

  public PersistenceModule(String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  @Override
  protected void configure() {
    try {
      bind(String.class).annotatedWith(Names.named("fpmlDir")).toInstance(fpmlDir);
      Morphia morphia = new Morphia();
      morphia.map(Index.class);
      morphia.getMapper().getConverters().addConverter(new LocalDateConverter());
      Datastore datastore = morphia.createDatastore(new Mongo(), "swappricer");
      datastore.ensureIndexes();
      bind(Datastore.class).toInstance(datastore);
    } catch(UnknownHostException e) {
      addError(e);
    }
    bind(RateManager.class).to(MongoRateManagerImpl.class);
    //bind(TradeStore.class).to(FpmlJAXBTradeStore.class);
  }

  public class LocalDateConverter  extends TypeConverter implements SimpleValueConverter {

    public LocalDateConverter() {
      super(DateTime.class);
    }

    @Override
    public Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
      if (val == null) return null;

      if (val instanceof DateTime)
        return val;
      String s = (String)val;
      return DateTime.forDateOnly(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(4, 6)), Integer.parseInt(s.substring(6, 8)));
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      DateTime dt = (DateTime)value;
      return dt.format("YYYYMMDD");
    }
  }
}
