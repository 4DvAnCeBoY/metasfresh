package org.adempiere.mm.attributes.api;

import static org.adempiere.model.InterfaceWrapperHelper.load;
import static org.adempiere.model.InterfaceWrapperHelper.loadOutOfTrx;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.AttributeSetInstanceId;
import org.adempiere.mm.attributes.spi.IAttributeValueCallout;
import org.adempiere.mm.attributes.spi.NullAttributeValueCallout;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_AttributeInstance;
import org.compiere.model.X_M_Attribute;
import org.compiere.util.Env;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/**
 * Immutable {@link IAttributeSet} implementation.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public final class ImmutableAttributeSet implements IAttributeSet
{
	public static final Builder builder()
	{
		return new Builder();
	}

	public static final ImmutableAttributeSet ofAttributesetInstanceId(
			@NonNull final AttributeSetInstanceId attributeSetInstanceId)
	{
		final IAttributeDAO attributeDAO = Services.get(IAttributeDAO.class);
		final List<I_M_AttributeInstance> attributeInstances = attributeDAO.retrieveAttributeInstances(attributeSetInstanceId);

		final Map<Object, Object> valuesByAttributeIdObj = new HashMap<>();
		for (final I_M_AttributeInstance instance : attributeInstances)
		{
			final Object value;

			final String attributeValueType = instance.getM_Attribute().getAttributeValueType();
			if (X_M_Attribute.ATTRIBUTEVALUETYPE_Date.equals(attributeValueType))
			{
				value = instance.getValueDate();
			}
			else if (X_M_Attribute.ATTRIBUTEVALUETYPE_List.equals(attributeValueType))
			{
				value = instance.getValue();
			}
			else if (X_M_Attribute.ATTRIBUTEVALUETYPE_Number.equals(attributeValueType))
			{
				value = instance.getValueNumber();
			}
			else if (X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40.equals(attributeValueType))
			{
				value = instance.getValue();
			}
			else
			{
				Check.fail("Unsupported attributeValueType={}; M_AttributeInstance={}, M_Attribute={ ", attributeValueType, instance, instance.getM_Attribute());
				value = null;
			}

			valuesByAttributeIdObj.put(instance.getM_Attribute_ID(), value);
		}
		return ofValuesIndexByAttributeId(valuesByAttributeIdObj);
	}

	public static final ImmutableAttributeSet ofValuesIndexByAttributeId(
			@Nullable final Map<Object, Object> valuesByAttributeIdObj)
	{
		if (valuesByAttributeIdObj == null || valuesByAttributeIdObj.isEmpty())
		{
			return EMPTY;
		}

		final HashMap<Integer, I_M_Attribute> attributes = new HashMap<Integer, I_M_Attribute>();
		final HashMap<String, I_M_Attribute> attributesByKey = new HashMap<String, I_M_Attribute>();
		final HashMap<String, Object> valuesByAttributeKey = new HashMap<String, Object>();

		valuesByAttributeIdObj.forEach((attributeIdObj, value) -> {

			final int attributeId = Integer.parseInt(attributeIdObj.toString());
			final I_M_Attribute attribute = load(attributeId, I_M_Attribute.class);
			final String attributeKey = attribute.getValue();

			attributes.put(attributeId, attribute);
			attributesByKey.put(attributeKey, attribute);
			valuesByAttributeKey.put(attributeKey, value);
		});

		return new ImmutableAttributeSet(attributes, attributesByKey, valuesByAttributeKey);
	}

	public static ImmutableAttributeSet createSubSet(
			@NonNull final IAttributeSet attributeSet,
			@NonNull final Predicate<I_M_Attribute> filter)
	{

		final Builder builder = builder();
		attributeSet.getAttributes()
				.stream()
				.filter(filter)
				.forEach(attribute -> {
					final Object value = attributeSet.getValue(attribute);
					builder.attributeValue(attribute, value);
				});

		return builder.build();
	}

	public static final ImmutableAttributeSet EMPTY = new ImmutableAttributeSet();

	private final Map<Integer, I_M_Attribute> attributes;
	private final Map<String, I_M_Attribute> attributesByKey;
	private final Map<String, Object> valuesByAttributeKey;

	private ImmutableAttributeSet(
			@NonNull final Map<Integer, I_M_Attribute> attributes,
			@NonNull final Map<String, I_M_Attribute> attributesByKey,
			@NonNull final Map<String, Object> valuesByAttributeKey)
	{
		this.attributes = attributes;
		this.attributesByKey = attributesByKey;
		this.valuesByAttributeKey = valuesByAttributeKey;
	}

	private ImmutableAttributeSet()
	{
		attributes = ImmutableMap.of();
		attributesByKey = ImmutableMap.of();
		valuesByAttributeKey = ImmutableMap.of();
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.addValue(valuesByAttributeKey)
				.toString();
	}

	public boolean isEmpty()
	{
		return attributesByKey.isEmpty();
	}

	@Override
	public Collection<I_M_Attribute> getAttributes()
	{
		return attributesByKey.values();
	}

	@Override
	public boolean hasAttribute(final String attributeKey)
	{
		return attributesByKey.containsKey(attributeKey);
	}

	private final void assertAttributeExists(final String attributeKey)
	{
		if (!hasAttribute(attributeKey))
		{
			throw new AdempiereException("Attribute does not exist: " + attributeKey);
		}
	}

	@Override
	public I_M_Attribute getAttributeByIdIfExists(final int attributeId)
	{
		return attributes.get(attributeId);
	}

	@Override
	public String getAttributeValueType(@NonNull final I_M_Attribute attribute)
	{
		assertAttributeExists(attribute.getValue());
		return attribute.getAttributeValueType();
	}

	@Override
	public Object getValue(@NonNull final String attributeKey)
	{
		assertAttributeExists(attributeKey);
		return valuesByAttributeKey.get(attributeKey);
	}

	@Override
	public BigDecimal getValueAsBigDecimal(final String attributeKey)
	{
		final Object valueObj = getValue(attributeKey);
		if (valueObj == null)
		{
			return null;
		}
		else if (valueObj instanceof BigDecimal)
		{
			return (BigDecimal)valueObj;
		}
		else
		{
			final String valueStr = valueObj.toString().trim();
			if (valueStr.isEmpty())
			{
				return null;
			}

			return new BigDecimal(valueStr);
		}
	}

	@Override
	public int getValueAsInt(final String attributeKey)
	{
		final Object valueObj = getValue(attributeKey);
		if (valueObj == null)
		{
			return 0;
		}
		else if (valueObj instanceof Number)
		{
			return ((Number)valueObj).intValue();
		}
		else
		{
			final String valueStr = valueObj.toString().trim();
			if (valueStr.isEmpty())
			{
				return 0;
			}

			return Integer.parseInt(valueStr);
		}
	}

	@Override
	public Date getValueAsDate(final String attributeKey)
	{
		final Object valueObj = getValue(attributeKey);
		if (valueObj == null)
		{
			return null;
		}
		else if (valueObj instanceof Number)
		{
			return new Date(((Number)valueObj).longValue());
		}
		else
		{
			final String valueStr = valueObj.toString().trim();
			if (valueStr.isEmpty())
			{
				return null;
			}

			return Env.parseTimestamp(valueStr);
		}
	}

	@Override
	public String getValueAsString(final String attributeKey)
	{
		final Object valueObj = getValue(attributeKey);
		return valueObj != null ? valueObj.toString() : null;
	}

	@Override
	public void setValue(final String attributeKey, final Object value)
	{
		throw new AdempiereException("Attribute set is immutable: " + this);
	}

	@Override
	public IAttributeValueCallout getAttributeValueCallout(final I_M_Attribute attribute)
	{
		return NullAttributeValueCallout.instance;
	}

	@Override
	public boolean isNew(final I_M_Attribute attribute)
	{
		return false;
	}

	//
	//
	// -------------------------
	//
	//
	public static final class Builder
	{
		private final LinkedHashMap<String, Object> valuesByAttributeKey = new LinkedHashMap<>();
		private final LinkedHashMap<Integer, I_M_Attribute> attributes = new LinkedHashMap<>();
		private final LinkedHashMap<String, I_M_Attribute> attributesByKey = new LinkedHashMap<>();

		private Builder()
		{
		}

		public ImmutableAttributeSet build()
		{
			if (attributes.isEmpty())
			{
				return EMPTY;
			}
			return new ImmutableAttributeSet(
					ImmutableMap.copyOf(attributes),
					ImmutableMap.copyOf(attributesByKey),
					ImmutableMap.copyOf(valuesByAttributeKey));
		}

		public Builder attributeValue(final int attributeId, final Object attributeValue)
		{
			final I_M_Attribute attribute = loadOutOfTrx(attributeId, I_M_Attribute.class);
			attributeValue(attribute, attributeValue);
			return this;
		}

		public Builder attributeValue(@NonNull final I_M_Attribute attribute, final Object attributeValue)
		{
			final int attributeId = attribute.getM_Attribute_ID();
			attributes.put(attributeId, attribute);

			final String attributeKey = attribute.getValue();
			attributesByKey.put(attributeKey, attribute);

			if (attributeValue == null)
			{
				valuesByAttributeKey.remove(attributeKey);
			}
			else
			{
				valuesByAttributeKey.put(attributeKey, attributeValue);
			}

			return this;
		}

	}
}
