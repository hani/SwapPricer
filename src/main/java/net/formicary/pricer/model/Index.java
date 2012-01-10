package net.formicary.pricer.model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexes;
import net.formicary.pricer.util.FastDate;
import org.bson.types.ObjectId;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 7:57 AM
 */
@Entity
@Indexes({
    @com.google.code.morphia.annotations.Index("name, currency, tenor, fixingDate")
})
public class Index {
  @Id private ObjectId id;
  private String currency;
  private String name;
  private String tenor;
  private FastDate fixingDate;
  private FastDate effectiveDate;
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

  public String getTenor() {
    return tenor;
  }

  public void setTenor(String tenor) {
    this.tenor = tenor;
  }

  public FastDate getFixingDate() {
    return fixingDate;
  }

  public void setFixingDate(FastDate fixingDate) {
    this.fixingDate = fixingDate;
  }

  public FastDate getEffectiveDate() {
    return effectiveDate;
  }

  public void setEffectiveDate(FastDate effectiveDate) {
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
      ", tenor='" + tenor + '\'' +
      ", fixingDate=" + fixingDate +
      '}';
  }
}
