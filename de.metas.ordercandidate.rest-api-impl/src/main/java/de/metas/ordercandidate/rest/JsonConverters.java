package de.metas.ordercandidate.rest;

import java.util.List;

import org.adempiere.uom.UomId;
import org.compiere.util.TimeUtil;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import de.metas.bpartner.BPartnerContactId;
import de.metas.bpartner.BPartnerId;
import de.metas.bpartner.BPartnerLocationId;
import de.metas.lang.Percent;
import de.metas.ordercandidate.api.OLCand;
import de.metas.ordercandidate.api.OLCandBPartnerInfo;
import de.metas.ordercandidate.api.OLCandCreateRequest;
import de.metas.ordercandidate.api.OLCandCreateRequest.OLCandCreateRequestBuilder;
import de.metas.pricing.PricingSystemId;
import de.metas.product.ProductId;
import lombok.NonNull;

/*
 * #%L
 * de.metas.ordercandidate.rest-api
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

@Service
public class JsonConverters
{
	public final OLCandCreateRequestBuilder fromJson(
			@NonNull final JsonOLCandCreateRequest request,
			@NonNull final MasterdataProvider masterdataProvider)
	{
		final ProductId productId = masterdataProvider.getProductIdByValue(request.getProductCode());
		final UomId uomId = masterdataProvider.getProductUOMId(productId, request.getUomCode());
		final PricingSystemId pricingSystemId = masterdataProvider.getPricingSystemIdByValue(request.getPricingSystemCode());

		return OLCandCreateRequest.builder()
				.externalId(request.getExternalId())
				//
				.orgId(masterdataProvider.getCreateOrgId(request.getOrg()))
				//
				.bpartner(toOLCandBPartnerInfo(request.getBpartner(), masterdataProvider))
				.billBPartner(toOLCandBPartnerInfo(request.getBillBPartner(), masterdataProvider))
				.dropShipBPartner(toOLCandBPartnerInfo(request.getDropShipBPartner(), masterdataProvider))
				.handOverBPartner(toOLCandBPartnerInfo(request.getHandOverBPartner(), masterdataProvider))
				.poReference(request.getPoReference())
				//
				.dateRequired(request.getDateRequired())
				.flatrateConditionsId(request.getFlatrateConditionsId())
				//
				.productId(productId)
				.productDescription(request.getProductDescription())
				.qty(request.getQty())
				.uomId(uomId)
				.huPIItemProductId(request.getPackingMaterialId())
				//
				.pricingSystemId(pricingSystemId)
				.price(request.getPrice())
				.discount(Percent.ofNullable(request.getDiscount()));
	}

	private final OLCandBPartnerInfo toOLCandBPartnerInfo(
			final JsonBPartnerInfo json,
			@NonNull final MasterdataProvider masterdataProvider)
	{
		if (json == null)
		{
			return null;
		}

		final BPartnerId bpartnerId = masterdataProvider.getCreateBPartnerId(json.getBpartner());
		final BPartnerLocationId bpartnerLocationId = masterdataProvider.getCreateBPartnerLocationId(bpartnerId, json.getLocation());
		final BPartnerContactId bpartnerContactId = masterdataProvider.getCreateBPartnerContactId(bpartnerId, json.getContact());

		return OLCandBPartnerInfo.builder()
				.bpartnerId(bpartnerId.getRepoId())
				.bpartnerLocationId(bpartnerLocationId.getRepoId())
				.contactId(BPartnerContactId.toRepoId(bpartnerContactId))
				.build();
	}

	private final JsonBPartnerInfo toJson(
			final OLCandBPartnerInfo bpartnerInfo,
			final MasterdataProvider masterdataProvider)
	{
		if (bpartnerInfo == null)
		{
			return null;
		}

		final BPartnerId bpartnerId = BPartnerId.ofRepoId(bpartnerInfo.getBpartnerId());
		final BPartnerLocationId bpartnerLocationId = BPartnerLocationId.ofRepoIdOrNull(bpartnerId, bpartnerInfo.getBpartnerLocationId());
		final BPartnerContactId contactId = BPartnerContactId.ofRepoIdOrNull(bpartnerId, bpartnerInfo.getContactId());

		return JsonBPartnerInfo.builder()
				.bpartner(masterdataProvider.getJsonBPartnerById(bpartnerId))
				.location(masterdataProvider.getJsonBPartnerLocationById(bpartnerLocationId))
				.contact(masterdataProvider.getJsonBPartnerContactById(contactId))
				.build();
	}

	private JsonOLCand toJson(final OLCand olCand, final MasterdataProvider masterdataProvider)
	{
		return JsonOLCand.builder()
				.id(olCand.getId())
				.externalId(olCand.getExternalId())
				//
				.bpartner(toJson(olCand.getBPartnerInfo(), masterdataProvider))
				.billBPartner(toJson(olCand.getBillBPartnerInfo(), masterdataProvider))
				.dropShipBPartner(toJson(olCand.getDropShipBPartnerInfo(), masterdataProvider))
				.handOverBPartner(toJson(olCand.getHandOverBPartnerInfo(), masterdataProvider))
				//
				.datePromised(TimeUtil.asLocalDate(olCand.getDatePromised()))
				.flatrateConditionsId(olCand.getFlatrateConditionsId())
				//
				.productId(olCand.getM_Product_ID())
				.productDescription(olCand.getProductDescription())
				.qty(olCand.getQty())
				.uomId(olCand.getC_UOM_ID())
				.huPIItemProductId(olCand.getHUPIProductItemId())
				//
				.pricingSystemId(PricingSystemId.getRepoId(olCand.getPricingSystemId()))
				.price(olCand.getPriceActual())
				.discount(olCand.getDiscount())
				//
				.build();
	}

	public JsonOLCandCreateBulkResponse toJson(
			@NonNull final List<OLCand> olCands,
			@NonNull final MasterdataProvider masterdataProvider)
	{
		return JsonOLCandCreateBulkResponse.of(olCands.stream()
				.map(olCand -> toJson(olCand, masterdataProvider))
				.collect(ImmutableList.toImmutableList()));
	}
}
