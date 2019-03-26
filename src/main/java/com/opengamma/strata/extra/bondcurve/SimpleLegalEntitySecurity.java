/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.LegalEntitySecurity;

/**
 * A simple legal entity security.
 * <p>
 * The legal entity identifier is the only information stored in this class. 
 * Thus this is not able to create {@code SecuritizedProduct} or {@code Trade}.
 */
@BeanDefinition(builderScope = "private")
public final class SimpleLegalEntitySecurity
    implements LegalEntitySecurity, ImmutableBean, Serializable {

  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the security.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LegalEntityId legalEntityId;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param legalEntityId  the legal entity identifier
   * @return the instance
   */
  public static SimpleLegalEntitySecurity of(LegalEntityId legalEntityId) {
    return new SimpleLegalEntitySecurity(legalEntityId);
  }

  @Override
  public LegalEntitySecurity withInfo(SecurityInfo info) {
    throw new IllegalArgumentException("SecurityInfo is not stored in SimpleLegalEntitySecurity.");
  }

  //-------------------------------------------------------------------------
  @Override
  public Currency getCurrency() {
    throw new IllegalArgumentException("Currency is not stored in SimpleLegalEntitySecurity.");
  }

  @Override
  public SecurityInfo getInfo() {
    throw new IllegalArgumentException("SecurityInfo is not stored in SimpleLegalEntitySecurity.");
  }

  @Override
  public ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.of();
  }

  @Override
  public SecuritizedProduct createProduct(ReferenceData refData) {
    throw new IllegalArgumentException("SecuritizedProduct can not be created form SimpleLegalEntitySecurity.");
  }

  @Override
  public SecurityQuantityTrade createTrade(TradeInfo tradeInfo, double quantity, double tradePrice, ReferenceData refData) {
    throw new IllegalArgumentException("Trade can not be created form SimpleLegalEntitySecurity.");
  }

  @Override
  public Position createPosition(PositionInfo positionInfo, double quantity, ReferenceData refData) {
    throw new IllegalArgumentException("Position can not be created form SimpleLegalEntitySecurity.");
  }

  @Override
  public Position createPosition(PositionInfo positionInfo, double longQuantity, double shortQuantity, ReferenceData refData) {
    throw new IllegalArgumentException("Position can not be created form SimpleLegalEntitySecurity.");
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimpleLegalEntitySecurity}.
   * @return the meta-bean, not null
   */
  public static SimpleLegalEntitySecurity.Meta meta() {
    return SimpleLegalEntitySecurity.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SimpleLegalEntitySecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SimpleLegalEntitySecurity(
      LegalEntityId legalEntityId) {
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    this.legalEntityId = legalEntityId;
  }

  @Override
  public SimpleLegalEntitySecurity.Meta metaBean() {
    return SimpleLegalEntitySecurity.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the security.
   * @return the value of the property, not null
   */
  @Override
  public LegalEntityId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleLegalEntitySecurity other = (SimpleLegalEntitySecurity) obj;
      return JodaBeanUtils.equal(legalEntityId, other.legalEntityId);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SimpleLegalEntitySecurity{");
    buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimpleLegalEntitySecurity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<LegalEntityId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", SimpleLegalEntitySecurity.class, LegalEntityId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legalEntityId");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return legalEntityId;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimpleLegalEntitySecurity> builder() {
      return new SimpleLegalEntitySecurity.Builder();
    }

    @Override
    public Class<? extends SimpleLegalEntitySecurity> beanType() {
      return SimpleLegalEntitySecurity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LegalEntityId> legalEntityId() {
      return legalEntityId;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return ((SimpleLegalEntitySecurity) bean).getLegalEntityId();
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
   * The bean-builder for {@code SimpleLegalEntitySecurity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SimpleLegalEntitySecurity> {

    private LegalEntityId legalEntityId;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return legalEntityId;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          this.legalEntityId = (LegalEntityId) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SimpleLegalEntitySecurity build() {
      return new SimpleLegalEntitySecurity(
          legalEntityId);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SimpleLegalEntitySecurity.Builder{");
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}