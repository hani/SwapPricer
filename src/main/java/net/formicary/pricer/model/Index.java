package net.formicary.pricer.model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexes;
import hirondelle.date4j.DateTime;
import org.bson.types.ObjectId;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 7:57 AM
 */
@Entity
@Indexes({
    @com.google.code.morphia.annotations.Index("name, currency, tenorUnit, tenorPeriod, fixingDate")
})
public class Index {
  @Id private ObjectId id;
  private String currency;
  private String name;
  private String tenorUnit;
  private String tenorPeriod;
  private DateTime fixingDate;
  private DateTime effectiveDate;
  private double rate;
  private String regulatoryBody;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTenorUnit() {
    return tenorUnit;
  }

  public void setTenorUnit(String tenorUnit) {
    this.tenorUnit = tenorUnit;
  }

  public String getTenorPeriod() {
    return tenorPeriod;
  }

  public void setTenorPeriod(String tenorPeriod) {
    this.tenorPeriod = tenorPeriod;
  }

  public DateTime getFixingDate() {
    return fixingDate;
  }

  public void setFixingDate(DateTime fixingDate) {
    this.fixingDate = fixingDate;
  }

  public DateTime getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(DateTime effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public double getRate() {
    return rate;
  }

  public void setRate(double rate) {
    this.rate = rate;
  }

  public String getRegulatoryBody() {
    return regulatoryBody;
  }

  public void setRegulatoryBody(String regulatoryBody) {
    this.regulatoryBody = regulatoryBody;
  }

  @Override
  public String toString() {
    return "Index{" +
      "id=" + id +
      ", currency='" + currency + '\'' +
      ", name='" + name + '\'' +
      ", tenorUnit='" + tenorUnit + '\'' +
      ", tenorPeriod='" + tenorPeriod + '\'' +
      ", fixingDate=" + fixingDate +
      '}';
  }
}
