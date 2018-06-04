package de.metas.shipper.gateway.derkurier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import de.metas.shipper.gateway.derkurier.misc.Converters;
import de.metas.shipper.gateway.derkurier.misc.DerKurierDeliveryOrderEmailer;
import de.metas.shipper.gateway.derkurier.misc.DerKurierShipperConfig;
import de.metas.shipper.gateway.derkurier.misc.DerKurierShipperConfigRepository;
import de.metas.shipper.gateway.derkurier.restapi.models.Routing;
import de.metas.shipper.gateway.derkurier.restapi.models.RoutingRequest;

/*
 * #%L
 * de.metas.shipper.gateway.derkurier
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
 * This test makes a real invocation on the "Der Kurier" web service. It's usually {@link Ignore}d.
 */
public class DerKurierClientFactoryManualTest
{
	@Test
	@Ignore // remove the ignore to run this test manually
	public void manualTest()
	{
		final Converters converters = new Converters();
		final DerKurierShipperConfigRepository derKurierShipperConfigRepository = new DerKurierShipperConfigRepository();

		final DerKurierClientFactory derKurierClientFactory = new DerKurierClientFactory(
				derKurierShipperConfigRepository,
				new DerKurierDeliveryOrderRepository(converters),
				new DerKurierDeliveryOrderEmailer(derKurierShipperConfigRepository),
				converters);

		final DerKurierShipperConfig shipperConfig = DerKurierShipperConfig.builder()
				.restApiBaseUrl("https://leoz.derkurier.de:13000/rs/api/v1")
				.customerNumber("12345")
				.parcelNumberAdSequenceId(23)
				.build();
		final DerKurierClient client = derKurierClientFactory.createClient(shipperConfig);

		final RoutingRequest routingRequest = DerKurierTestTools.createRoutingRequest_times_with_seconds();
		final Routing routing = client.postRoutingRequest(routingRequest);
		assertThat(routing).isNotNull();
	}

}
