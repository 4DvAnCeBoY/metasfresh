package de.metas.ordercandidate.api;

import java.util.stream.Stream;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.model.RelationTypeZoomProvidersFactory;
import org.adempiere.util.Services;
import org.compiere.util.Env;

import com.google.common.base.MoreObjects;

import de.metas.ordercandidate.model.I_C_OLCand;
import de.metas.ordercandidate.model.I_C_OLCandProcessor;
import lombok.Builder;
import lombok.NonNull;

/*
 * #%L
 * de.metas.swat.base
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

public class RelationTypeOLCandSource implements OLCandSource
{
	private final IOLCandBL olCandBL = Services.get(IOLCandBL.class);
	private final IOLCandEffectiveValuesBL olCandEffectiveValuesBL = Services.get(IOLCandEffectiveValuesBL.class);

	private final OLCandOrderDefaults orderDefaults;
	private final I_C_OLCandProcessor processor;
	private final String relationTypeInternalName;

	@Builder
	private RelationTypeOLCandSource(
			@NonNull final OLCandOrderDefaults orderDefaults,
			@NonNull final I_C_OLCandProcessor processor)
	{
		this.processor = processor;
		this.orderDefaults = orderDefaults;

		relationTypeInternalName = olCandBL.mkRelationTypeInternalName(processor);
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("relationTypeInternalName", relationTypeInternalName)
				.add("processor", processor)
				.toString();
	}

	@Override
	public Stream<OLCand> streamOLCands()
	{
		return RelationTypeZoomProvidersFactory.instance
				.getZoomProviderBySourceTableNameAndInternalName(I_C_OLCand.Table_Name, relationTypeInternalName)
				.retrieveDestinations(Env.getCtx(), InterfaceWrapperHelper.getPO(processor), I_C_OLCand.class, ITrx.TRXNAME_ThreadInherited)
				.stream()
				.map(this::toOLCand);
	}

	private OLCand toOLCand(final I_C_OLCand candidatePO)
	{
		final int pricingSystemId = olCandBL.getPricingSystemId(candidatePO, orderDefaults);
		return OLCand.builder()
				.candidate(candidatePO)
				.pricingSystemId(pricingSystemId)
				.olCandEffectiveValuesBL(olCandEffectiveValuesBL)
				.build();
	}
}
