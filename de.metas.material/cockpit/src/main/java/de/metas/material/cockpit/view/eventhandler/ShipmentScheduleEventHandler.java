package de.metas.material.cockpit.view.eventhandler;

import java.util.Collection;

import org.adempiere.util.Check;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import de.metas.Profiles;
import de.metas.event.log.EventLogUserService;
import de.metas.material.cockpit.view.AddDetailRequest;
import de.metas.material.cockpit.view.AddDetailRequest.AddDetailRequestBuilder;
import de.metas.material.cockpit.view.DataRecordIdentifier;
import de.metas.material.cockpit.view.UpdateMainDataRequest;
import de.metas.material.cockpit.view.MainDataRequestHandler;
import de.metas.material.cockpit.view.DetailRequestHandler;
import de.metas.material.cockpit.view.RemoveDetailsRequest;
import de.metas.material.cockpit.view.RemoveDetailsRequest.RemoveDetailsRequestBuilder;
import de.metas.material.event.MaterialEventHandler;
import de.metas.material.event.commons.DocumentLineDescriptor;
import de.metas.material.event.commons.MaterialDescriptor;
import de.metas.material.event.commons.OrderLineDescriptor;
import de.metas.material.event.commons.SubscriptionLineDescriptor;
import de.metas.material.event.shipmentschedule.AbstractShipmentScheduleEvent;
import de.metas.material.event.shipmentschedule.ShipmentScheduleCreatedEvent;
import de.metas.material.event.shipmentschedule.ShipmentScheduleDeletedEvent;
import de.metas.material.event.shipmentschedule.ShipmentScheduleUpdatedEvent;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-material-cockpit
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
@Profile(Profiles.PROFILE_App) // it's important to have just *one* instance of this listener, because on each event needs to be handled exactly once.
public class ShipmentScheduleEventHandler
		implements MaterialEventHandler<AbstractShipmentScheduleEvent>
{
	private final MainDataRequestHandler dataUpdateRequestHandler;
	private final DetailRequestHandler detailRequestHandler;
	private EventLogUserService eventLogUserService;

	public ShipmentScheduleEventHandler(
			@NonNull final MainDataRequestHandler dataUpdateRequestHandler,
			@NonNull final DetailRequestHandler detailRequestHandler,
			@NonNull final EventLogUserService eventLogUserService)
	{
		this.detailRequestHandler = detailRequestHandler;
		this.dataUpdateRequestHandler = dataUpdateRequestHandler;
		this.eventLogUserService = eventLogUserService;
	}

	@Override
	public Collection<Class<? extends AbstractShipmentScheduleEvent>> getHandeledEventType()
	{
		return ImmutableList.of(
				ShipmentScheduleCreatedEvent.class,
				ShipmentScheduleUpdatedEvent.class,
				ShipmentScheduleDeletedEvent.class);
	}

	@Override
	public void handleEvent(@NonNull final AbstractShipmentScheduleEvent event)
	{
		final MaterialDescriptor materialDescriptor = event.getMaterialDescriptor();
		final DataRecordIdentifier identifier = DataRecordIdentifier.createForMaterial(materialDescriptor);

		createDataUpdateRequestForEvent(event, identifier);
		createAndHandleDetailRequest(event, identifier);
	}

	private void createDataUpdateRequestForEvent(
			@NonNull final AbstractShipmentScheduleEvent shipmentScheduleEvent,
			@NonNull final DataRecordIdentifier identifier)
	{
		final UpdateMainDataRequest request = UpdateMainDataRequest.builder()
				.identifier(identifier)
				.orderedSalesQty(shipmentScheduleEvent.getOrderedQuantityDelta())
				.reservedSalesQty(shipmentScheduleEvent.getReservedQuantityDelta())
				.build();
		dataUpdateRequestHandler.handleDataUpdateRequest(request);
	}

	private void createAndHandleDetailRequest(
			@NonNull final AbstractShipmentScheduleEvent shipmentScheduleEvent,
			@NonNull final DataRecordIdentifier identifier)
	{
		if (shipmentScheduleEvent instanceof ShipmentScheduleCreatedEvent)
		{
			final ShipmentScheduleCreatedEvent shipmentScheduleCreatedEvent = (ShipmentScheduleCreatedEvent)shipmentScheduleEvent;
			createAndHandleAddDetailRequest(identifier, shipmentScheduleCreatedEvent);
		}
		else if (shipmentScheduleEvent instanceof ShipmentScheduleDeletedEvent)
		{
			final RemoveDetailsRequestBuilder removeDetailsRequest = RemoveDetailsRequest.builder().identifier(identifier);
			final int deletedCount = detailRequestHandler.handleRemoveDetailsRequest(removeDetailsRequest.build());
			eventLogUserService
					.newLogEntry(ShipmentScheduleEventHandler.class)
					.formattedMessage("Deleted {} detail records", deletedCount)
					.storeEntry();
		}
	}

	private void createAndHandleAddDetailRequest(
			@NonNull final DataRecordIdentifier identifier,
			@NonNull final ShipmentScheduleCreatedEvent shipmentScheduleCreatedEvent)
	{
		final DocumentLineDescriptor documentLineDescriptor = //
				shipmentScheduleCreatedEvent
						.getDocumentLineDescriptor();

		final AddDetailRequestBuilder addDetailsRequest = AddDetailRequest.builder()
				.identifier(identifier);

		if (documentLineDescriptor instanceof OrderLineDescriptor)
		{
			final OrderLineDescriptor orderLineDescriptor = //
					(OrderLineDescriptor)documentLineDescriptor;
			addDetailsRequest
					.orderId(orderLineDescriptor.getOrderId())
					.orderLineId(orderLineDescriptor.getOrderLineId());
		}
		else if (documentLineDescriptor instanceof SubscriptionLineDescriptor)
		{
			final SubscriptionLineDescriptor subscriptionLineDescriptor = //
					(SubscriptionLineDescriptor)documentLineDescriptor;
			addDetailsRequest
					.subscriptionId(subscriptionLineDescriptor.getFlatrateTermId())
					.subscriptionLineId(subscriptionLineDescriptor.getSubscriptionProgressId());
		}
		else
		{
			Check.errorIf(true,
					"The DocumentLineDescriptor has an unexpected type; documentLineDescriptor={}", documentLineDescriptor);
		}

		detailRequestHandler.handleAddDetailRequest(addDetailsRequest.build());
	}
}
