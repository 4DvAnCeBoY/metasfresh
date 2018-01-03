package de.metas.material.dispo.service.event.handler.pporder;

import java.util.Collection;

import org.adempiere.util.Check;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import de.metas.Profiles;
import de.metas.event.log.EventLogUserService;
import de.metas.material.dispo.commons.RequestMaterialOrderService;
import de.metas.material.dispo.commons.candidate.CandidateBusinessCase;
import de.metas.material.dispo.commons.candidate.CandidateType;
import de.metas.material.dispo.commons.repository.CandidateRepositoryRetrieval;
import de.metas.material.dispo.commons.repository.CandidatesQuery;
import de.metas.material.dispo.commons.repository.MaterialDescriptorQuery;
import de.metas.material.dispo.service.candidatechange.CandidateChangeService;
import de.metas.material.event.commons.ProductDescriptor;
import de.metas.material.event.pporder.AbstractPPOrderEvent;
import de.metas.material.event.pporder.PPOrder;
import de.metas.material.event.pporder.PPOrderCreatedEvent;
import de.metas.material.event.pporder.PPOrderLine;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-material-dispo
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

@Service
@Profile(Profiles.PROFILE_MaterialDispo)
public final class PPOrderCreatedHandler
		extends PPOrderAdvisedOrCreatedHandler<PPOrderCreatedEvent>
{

	private final EventLogUserService eventLogUserService;

	/**
	 *
	 * @param candidateChangeHandler
	 * @param candidateService needed in case we directly request a {@link PpOrderSuggestedEvent}'s proposed PP_Order to be created.
	 */
	public PPOrderCreatedHandler(
			@NonNull final CandidateChangeService candidateChangeHandler,
			@NonNull final CandidateRepositoryRetrieval candidateRepositoryRetrieval,
			@NonNull final RequestMaterialOrderService candidateService,
			@NonNull final EventLogUserService eventLogUserService)
	{
		super(candidateChangeHandler, candidateRepositoryRetrieval, candidateService);
		this.eventLogUserService = eventLogUserService;
	}

	@Override
	public Collection<Class<? extends PPOrderCreatedEvent>> getHandeledEventType()
	{
		return ImmutableList.of(PPOrderCreatedEvent.class);
	}

	@Override
	public void validateEvent(@NonNull final PPOrderCreatedEvent event)
	{
		final PPOrder ppOrder = event.getPpOrder();

		final int ppOrderId = ppOrder.getPpOrderId();
		Check.errorIf(ppOrderId <= 0, "The given ppOrderCreatedEvent event has a ppOrder with ppOrderId={}", ppOrderId);

		ppOrder.getLines().forEach(ppOrderLine -> {

			final int ppOrderLineId = ppOrderLine.getPpOrderLineId();
			Check.errorIf(ppOrderLineId <= 0,
					"The given ppOrderCreatedEvent event has a ppOrderLine with ppOrderLineId={}; ppOrderLine={}",
					ppOrderLineId, ppOrderLine);
		});
	}

	@Override
	public void handleEvent(@NonNull final PPOrderCreatedEvent event)
	{
		final boolean advised = false;
		handlePPOrderAdvisedOrCreatedEvent(event, advised);
	}

	@Override
	protected CandidatesQuery createQuery(@NonNull final AbstractPPOrderEvent ppOrderEvent)
	{
		final PPOrderCreatedEvent ppOrderCreatedEvent = (PPOrderCreatedEvent)ppOrderEvent;
		if (ppOrderCreatedEvent.getGroupId() <= 0)
		{
			eventLogUserService.newEventLogRequest(this.getClass())
					.message("The given ppOrderCreatedEvent has no groupId, so it was created by a user and not via material-dispo. Going to create new candidate records.")
					.storeEventLog();
			return CandidatesQuery.FALSE;
		}

		final PPOrder ppOrder = ppOrderCreatedEvent.getPpOrder();
		final ProductDescriptor productDescriptor = ppOrder.getProductDescriptor();

		final CandidatesQuery query = CandidatesQuery.builder()
				.type(CandidateType.SUPPLY)
				.businessCase(CandidateBusinessCase.PRODUCTION)
				.groupId(ppOrderCreatedEvent.getGroupId())
				.materialDescriptorQuery(createMaterialDescriptorQuery(productDescriptor))
				.build();

		return query;
	}

	@Override
	protected CandidatesQuery createQuery(
			@NonNull final PPOrderLine ppOrderLine,
			@NonNull final AbstractPPOrderEvent ppOrderEvent)
	{
		final PPOrderCreatedEvent ppOrderCreatedEvent = (PPOrderCreatedEvent)ppOrderEvent;
		if (ppOrderCreatedEvent.getGroupId() <= 0)
		{
			// we already logged in the other createQuery() method; no need to log again.
			return CandidatesQuery.FALSE;
		}

		final CandidatesQuery query = CandidatesQuery.builder()
				.type(extractCandidateType(ppOrderLine))
				.businessCase(CandidateBusinessCase.PRODUCTION)
				.groupId(ppOrderCreatedEvent.getGroupId())
				.materialDescriptorQuery(createMaterialDescriptorQuery(ppOrderLine.getProductDescriptor()))
				.build();

		return query;
	}

	private static MaterialDescriptorQuery createMaterialDescriptorQuery(
			@NonNull final ProductDescriptor productDescriptor)
	{
		return MaterialDescriptorQuery.builder()
				.productId(productDescriptor.getProductId())
				.storageAttributesKey(productDescriptor.getStorageAttributesKey())
				.build();
	}
}
