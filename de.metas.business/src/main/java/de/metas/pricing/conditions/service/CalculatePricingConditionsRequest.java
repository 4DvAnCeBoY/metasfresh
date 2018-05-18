/**
 *
 */
package de.metas.pricing.conditions.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_M_AttributeInstance;

import com.google.common.collect.ImmutableList;

import de.metas.pricing.IPricingContext;
import de.metas.pricing.conditions.PricingConditionsBreak;
import de.metas.pricing.conditions.PricingConditionsBreakId;
import de.metas.pricing.conditions.PricingConditionsId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2018 metas GmbH
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
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Value
@Immutable
public class CalculatePricingConditionsRequest
{
	private final PricingConditionsId pricingConditionsId;
	private final PricingConditionsBreak forceSchemaBreak;

	private final BigDecimal qty;
	private final BigDecimal price;
	private final int productId;
	private final BigDecimal bpartnerFlatDiscount;
	private final List<I_M_AttributeInstance> attributeInstances;
	private final IPricingContext pricingCtx;

	@Builder
	private CalculatePricingConditionsRequest(
			@NonNull final PricingConditionsId pricingConditionsId,
			final PricingConditionsBreak forceSchemaBreak,
			final BigDecimal qty,
			final BigDecimal price,
			final int productId,
			final BigDecimal bpartnerFlatDiscount,
			final List<I_M_AttributeInstance> attributeInstances,
			final IPricingContext pricingCtx)
	{
		if (forceSchemaBreak != null && !PricingConditionsBreakId.matching(pricingConditionsId, forceSchemaBreak.getId()))
		{
			throw new AdempiereException("Schema and schema break does not match")
					.setParameter("pricingConditionsId", pricingConditionsId)
					.setParameter("forceSchemaBreak", forceSchemaBreak)
					.appendParametersToMessage();
		}

		this.pricingConditionsId = pricingConditionsId;
		this.forceSchemaBreak = forceSchemaBreak;
		this.qty = qty;
		this.price = price;
		this.productId = productId;
		this.bpartnerFlatDiscount = bpartnerFlatDiscount != null ? bpartnerFlatDiscount : BigDecimal.ZERO;
		this.attributeInstances = attributeInstances != null ? ImmutableList.copyOf(attributeInstances) : ImmutableList.of();
		this.pricingCtx = pricingCtx;
	}
}
