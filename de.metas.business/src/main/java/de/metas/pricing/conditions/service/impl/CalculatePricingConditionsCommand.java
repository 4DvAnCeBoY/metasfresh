/**
 *
 */
package de.metas.pricing.conditions.service.impl;

import java.math.BigDecimal;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.Services;

import de.metas.pricing.IEditablePricingContext;
import de.metas.pricing.IPricingContext;
import de.metas.pricing.IPricingResult;
import de.metas.pricing.conditions.PricingConditions;
import de.metas.pricing.conditions.PricingConditionsBreak;
import de.metas.pricing.conditions.PricingConditionsBreak.PriceOverrideType;
import de.metas.pricing.conditions.PricingConditionsBreakQuery;
import de.metas.pricing.conditions.PricingConditionsDiscountType;
import de.metas.pricing.conditions.service.CalculatePricingConditionsRequest;
import de.metas.pricing.conditions.service.CalculatePricingConditionsResult;
import de.metas.pricing.conditions.service.CalculatePricingConditionsResult.CalculatePricingConditionsResultBuilder;
import de.metas.pricing.conditions.service.IPricingConditionsRepository;
import de.metas.pricing.service.IPricingBL;
import de.metas.product.IProductDAO;
import de.metas.product.ProductAndCategoryId;
import lombok.NonNull;

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
/* package */ class CalculatePricingConditionsCommand
{
	private final IPricingBL pricingBL = Services.get(IPricingBL.class);
	private final IPricingConditionsRepository pricingConditionsRepo = Services.get(IPricingConditionsRepository.class);
	private final IProductDAO productsRepo = Services.get(IProductDAO.class);

	final CalculatePricingConditionsRequest request;

	public CalculatePricingConditionsCommand(@NonNull final CalculatePricingConditionsRequest request)
	{
		this.request = request;
	}

	public CalculatePricingConditionsResult calculate()
	{
		final PricingConditions pricingConditions = pricingConditionsRepo.getPricingConditionsById(request.getDiscountSchemaId());

		final PricingConditionsDiscountType discountType = pricingConditions.getDiscountType();
		if (discountType == PricingConditionsDiscountType.FLAT_PERCENT)
		{
			return computeFlatDiscount(pricingConditions);
		}
		else if (discountType == PricingConditionsDiscountType.FORMULA
				|| discountType == PricingConditionsDiscountType.PRICE_LIST)
		{
			return CalculatePricingConditionsResult.ZERO;
		}
		else if (discountType == PricingConditionsDiscountType.BREAKS)
		{
			return computeBreaksDiscount(pricingConditions);
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @DiscountType@: " + discountType);
		}
	}

	private CalculatePricingConditionsResult computeFlatDiscount(final PricingConditions pricingConditions)
	{
		if (pricingConditions.isBpartnerFlatDiscount())
		{
			return CalculatePricingConditionsResult.discount(request.getBpartnerFlatDiscount());
		}
		else
		{
			return CalculatePricingConditionsResult.discount(pricingConditions.getFlatDiscount());
		}
	}

	private CalculatePricingConditionsResult computeBreaksDiscount(final PricingConditions pricingConditions)
	{
		final PricingConditionsBreak breakApplied = fetchDiscountSchemaBreak(pricingConditions);
		if (breakApplied == null)
		{
			return CalculatePricingConditionsResult.ZERO;
		}

		final CalculatePricingConditionsResultBuilder result = CalculatePricingConditionsResult.builder()
				.discountSchemaBreakId(breakApplied.getDiscountSchemaBreakId())
				.C_PaymentTerm_ID(breakApplied.getPaymentTermId());

		computePriceForDiscountSchemaBreak(result, breakApplied);
		computeDiscountForDiscountSchemaBreak(result, breakApplied);

		return result.build();
	}

	private void computePriceForDiscountSchemaBreak(final CalculatePricingConditionsResultBuilder result, final PricingConditionsBreak pricingConditionsBreak)
	{
		final PriceOverrideType priceOverride = pricingConditionsBreak.getPriceOverride();
		if (priceOverride == PriceOverrideType.NONE)
		{
			// nothing
		}
		else if (priceOverride == PriceOverrideType.BASE_PRICING_SYSTEM)
		{
			final int basePricingSystemId = pricingConditionsBreak.getBasePricingSystemId();

			final IPricingResult productPrices = computePricesForPricingSystem(basePricingSystemId);
			final BigDecimal priceStd = productPrices.getPriceStd();
			final BigDecimal priceList = productPrices.getPriceList();
			final BigDecimal priceLimit = productPrices.getPriceLimit();

			final BigDecimal priceStdAddAmt = pricingConditionsBreak.getBasePriceAddAmt();

			result.discountSchemaBreak_BasePricingSystem_Id(basePricingSystemId);
			result.priceListOverride(priceList);
			result.priceLimitOverride(priceLimit);
			result.priceStdOverride(priceStd.add(priceStdAddAmt));
		}
		else if (priceOverride == PriceOverrideType.FIXED_PRICE)
		{
			result.priceStdOverride(pricingConditionsBreak.getFixedPrice());
		}
		else
		{
			throw new AdempiereException("Unknow price override type: " + priceOverride)
					.setParameter("break", pricingConditionsBreak);
		}
	}

	private IPricingResult computePricesForPricingSystem(final int basePricingSystemId)
	{
		Check.assumeGreaterThanZero(basePricingSystemId, "basePricingSystemId");

		final IPricingContext pricingCtx = request.getPricingCtx();
		Check.assumeNotNull(pricingCtx, "pricingCtx shall not be null for {}", request);

		final IPricingContext basePricingSystemPricingCtx = createBasePricingSystemPricingCtx(pricingCtx, basePricingSystemId);
		final IPricingResult pricingResult = pricingBL.calculatePrice(basePricingSystemPricingCtx);

		return pricingResult;
	}

	private IPricingContext createBasePricingSystemPricingCtx(final IPricingContext pricingCtx, final int basePricingSystemId)
	{
		Check.assumeGreaterThanZero(basePricingSystemId, "basePricingSystemId");

		final IEditablePricingContext newPricingCtx = pricingCtx.copy();
		newPricingCtx.setM_PricingSystem_ID(basePricingSystemId);
		newPricingCtx.setM_PriceList_ID(-1); // will be recomputed
		newPricingCtx.setM_PriceList_Version_ID(-1); // will be recomputed
		newPricingCtx.setSkipCheckingPriceListSOTrxFlag(true);
		newPricingCtx.setDisallowDiscount(true);
		newPricingCtx.setFailIfNotCalculated(true);

		return newPricingCtx;
	}

	private void computeDiscountForDiscountSchemaBreak(final CalculatePricingConditionsResultBuilder result, final PricingConditionsBreak pricingConditionsBreak)
	{
		final BigDecimal discount;
		if (pricingConditionsBreak.isBpartnerFlatDiscount())
		{
			discount = request.getBpartnerFlatDiscount();
		}
		else
		{
			discount = pricingConditionsBreak.getDiscount();
		}

		result.discount(discount);
	}

	private PricingConditionsBreak fetchDiscountSchemaBreak(final PricingConditions pricingConditions)
	{
		if (request.getForceSchemaBreak() != null)
		{
			return request.getForceSchemaBreak();
		}

		final int productId = request.getProductId();
		final int productCategoryId = productsRepo.retrieveProductCategoryByProductId(productId);

		return pricingConditions.pickApplyingBreak(PricingConditionsBreakQuery.builder()
				.attributeInstances(request.getAttributeInstances())
				.productAndCategoryId(ProductAndCategoryId.of(productId, productCategoryId))
				.qty(request.getQty())
				.amt(request.getPrice().multiply(request.getQty()))
				.build());
	}
}
