/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.extra.repo.RepoTemplate;
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepo;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;

/**
 * A curve node whose instrument is a repo.
 * <p>
 * The trade produced by the node will be a BUY for a positive quantity and a SELL for a negative quantity.
 */
@BeanDefinition
public final class RepoCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the repo associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final RepoTemplate template;
  /**
   * The identifier of the market data value that provides the rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final QuoteId rateId;
  /**
   * The additional spread added to the rate.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the repo period if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The method by which the date of the node is calculated, defaulted to 'End'.
   */
  @PropertyDefinition
  private final CurveNodeDate date;
  /**
   * The date order rules, used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveNodeDateOrder dateOrder;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve node for a repo using the specified instrument template and rate key.
   * <p>
   * The additional spread will be set to be 0. 
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateId  the identifier of the market rate used when building the instrument for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static RepoCurveNode of(RepoTemplate template, QuoteId rateId) {
    return of(template, rateId, 0d);
  }

  /**
   * Returns a curve node for a repo using the specified instrument template, rate key and spread.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @return a node whose instrument is built from the template using a market rate
   */
  public static RepoCurveNode of(RepoTemplate template, QuoteId rateId, double additionalSpread) {
    return builder()
        .template(template)
        .rateId(rateId)
        .additionalSpread(additionalSpread)
        .build();
  }

  /**
   * Returns a curve node for a repo using the specified instrument template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @param label  the label to use for the node, if null or empty an appropriate default label will be used
   * @return a node whose instrument is built from the template using a market rate
   */
  public static RepoCurveNode of(
      RepoTemplate template,
      QuoteId rateId,
      double additionalSpread,
      String label) {

    return new RepoCurveNode(
        template, rateId, additionalSpread, label, CurveNodeDate.END, CurveNodeDateOrder.DEFAULT);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
    builder.dateOrder = CurveNodeDateOrder.DEFAULT;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = builder.template.getTenor().toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableId> requirements() {
    return ImmutableSet.of(rateId);
  }

  @Override
  public LocalDate date(LocalDate valuationDate, ReferenceData refData) {
    return date.calculate(
        () -> calculateEnd(valuationDate, refData),
        () -> calculateLastFixingDate(valuationDate));
  }

  @Override
  public DatedParameterMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    LocalDate nodeDate = date(valuationDate, refData);
    if (date.isFixed()) {
      return LabelDateParameterMetadata.of(nodeDate, label);
    }
    return TenorDateParameterMetadata.of(nodeDate, template.getTenor(), label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate valuationDate, ReferenceData refData) {
    RepoTrade trade = template.createTrade(valuationDate, BuySell.BUY, 0d, 0d, refData);
    ResolvedRepo repo = trade.getProduct().resolve(refData);
    return repo.getEndDate();
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate) {
    throw new UnsupportedOperationException("Node date of 'LastFixing' is not supported for Repo");
  }

  @Override
  public RepoTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    double fixedRate = marketData.getValue(rateId) + additionalSpread;
    BuySell buySell = quantity > 0d ? BuySell.BUY : BuySell.SELL;
    return template.createTrade(marketData.getValuationDate(), buySell, Math.abs(quantity), fixedRate, refData);
  }

  @Override
  public ResolvedRepoTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData) {
    return trade(quantity, marketData, refData).resolve(refData);
  }

  @Override
  public double initialGuess(MarketData marketData, ValueType valueType) {
    if (ValueType.ZERO_RATE.equals(valueType) || ValueType.FORWARD_RATE.equals(valueType)) {
      return marketData.getValue(rateId);
    }
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      Period tenorPeriod = template.getTenor().getPeriod();
      long totalMonths = tenorPeriod.toTotalMonths();
      double approximateMaturity = totalMonths > 0L ? totalMonths / 12d : tenorPeriod.getDays() / 365d;
      return Math.exp(-approximateMaturity * marketData.getValue(rateId));
    }
    return 0d;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this node with the specified date.
   * 
   * @param date  the date to use
   * @return the node based on this node with the specified date
   */
  public RepoCurveNode withDate(CurveNodeDate date) {
    return new RepoCurveNode(template, rateId, additionalSpread, label, date, dateOrder);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code RepoCurveNode}.
   * @return the meta-bean, not null
   */
  public static RepoCurveNode.Meta meta() {
    return RepoCurveNode.Meta.INSTANCE;
  }

  static {
    MetaBean.register(RepoCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static RepoCurveNode.Builder builder() {
    return new RepoCurveNode.Builder();
  }

  private RepoCurveNode(
      RepoTemplate template,
      QuoteId rateId,
      double additionalSpread,
      String label,
      CurveNodeDate date,
      CurveNodeDateOrder dateOrder) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateId, "rateId");
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(dateOrder, "dateOrder");
    this.template = template;
    this.rateId = rateId;
    this.additionalSpread = additionalSpread;
    this.label = label;
    this.date = date;
    this.dateOrder = dateOrder;
  }

  @Override
  public RepoCurveNode.Meta metaBean() {
    return RepoCurveNode.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the template for the repo associated with this node.
   * @return the value of the property, not null
   */
  public RepoTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value that provides the rate.
   * @return the value of the property, not null
   */
  public QuoteId getRateId() {
    return rateId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the rate.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the repo period if not specified.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method by which the date of the node is calculated, defaulted to 'End'.
   * @return the value of the property
   */
  public CurveNodeDate getDate() {
    return date;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date order rules, used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   * @return the value of the property, not null
   */
  @Override
  public CurveNodeDateOrder getDateOrder() {
    return dateOrder;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RepoCurveNode other = (RepoCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(rateId, other.rateId) &&
          JodaBeanUtils.equal(additionalSpread, other.additionalSpread) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(dateOrder, other.dateOrder);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateId);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionalSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateOrder);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("RepoCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("rateId").append('=').append(rateId).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(additionalSpread).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RepoCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<RepoTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", RepoCurveNode.class, RepoTemplate.class);
    /**
     * The meta-property for the {@code rateId} property.
     */
    private final MetaProperty<QuoteId> rateId = DirectMetaProperty.ofImmutable(
        this, "rateId", RepoCurveNode.class, QuoteId.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", RepoCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", RepoCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", RepoCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-property for the {@code dateOrder} property.
     */
    private final MetaProperty<CurveNodeDateOrder> dateOrder = DirectMetaProperty.ofImmutable(
        this, "dateOrder", RepoCurveNode.class, CurveNodeDateOrder.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "rateId",
        "additionalSpread",
        "label",
        "date",
        "dateOrder");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -938107365:  // rateId
          return rateId;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        case -263699392:  // dateOrder
          return dateOrder;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public RepoCurveNode.Builder builder() {
      return new RepoCurveNode.Builder();
    }

    @Override
    public Class<? extends RepoCurveNode> beanType() {
      return RepoCurveNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code template} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RepoTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code rateId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<QuoteId> rateId() {
      return rateId;
    }

    /**
     * The meta-property for the {@code additionalSpread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> additionalSpread() {
      return additionalSpread;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code dateOrder} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDateOrder> dateOrder() {
      return dateOrder;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((RepoCurveNode) bean).getTemplate();
        case -938107365:  // rateId
          return ((RepoCurveNode) bean).getRateId();
        case 291232890:  // additionalSpread
          return ((RepoCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((RepoCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((RepoCurveNode) bean).getDate();
        case -263699392:  // dateOrder
          return ((RepoCurveNode) bean).getDateOrder();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code RepoCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<RepoCurveNode> {

    private RepoTemplate template;
    private QuoteId rateId;
    private double additionalSpread;
    private String label;
    private CurveNodeDate date;
    private CurveNodeDateOrder dateOrder;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(RepoCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.rateId = beanToCopy.getRateId();
      this.additionalSpread = beanToCopy.getAdditionalSpread();
      this.label = beanToCopy.getLabel();
      this.date = beanToCopy.getDate();
      this.dateOrder = beanToCopy.getDateOrder();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -938107365:  // rateId
          return rateId;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        case -263699392:  // dateOrder
          return dateOrder;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (RepoTemplate) newValue;
          break;
        case -938107365:  // rateId
          this.rateId = (QuoteId) newValue;
          break;
        case 291232890:  // additionalSpread
          this.additionalSpread = (Double) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 3076014:  // date
          this.date = (CurveNodeDate) newValue;
          break;
        case -263699392:  // dateOrder
          this.dateOrder = (CurveNodeDateOrder) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public RepoCurveNode build() {
      preBuild(this);
      return new RepoCurveNode(
          template,
          rateId,
          additionalSpread,
          label,
          date,
          dateOrder);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the repo associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(RepoTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the identifier of the market data value that provides the rate.
     * @param rateId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateId(QuoteId rateId) {
      JodaBeanUtils.notNull(rateId, "rateId");
      this.rateId = rateId;
      return this;
    }

    /**
     * Sets the additional spread added to the rate.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the repo period if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the method by which the date of the node is calculated, defaulted to 'End'.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    public Builder date(CurveNodeDate date) {
      this.date = date;
      return this;
    }

    /**
     * Sets the date order rules, used to ensure that the dates in the curve are in order.
     * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
     * @param dateOrder  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dateOrder(CurveNodeDateOrder dateOrder) {
      JodaBeanUtils.notNull(dateOrder, "dateOrder");
      this.dateOrder = dateOrder;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("RepoCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("rateId").append('=').append(JodaBeanUtils.toString(rateId)).append(',').append(' ');
      buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
