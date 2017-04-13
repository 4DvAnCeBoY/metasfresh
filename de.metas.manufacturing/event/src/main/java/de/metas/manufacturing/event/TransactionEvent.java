package de.metas.manufacturing.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import org.adempiere.util.lang.impl.TableRecordReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-manufacturing-event-api
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
@Data
@AllArgsConstructor // used by jackson when it deserializes a string
@Builder // used by devs to make sure they know with parameter-value goes into which property
public class TransactionEvent implements ManufacturingEvent
{
	public static final String TYPE = "TransactionEvent";

	@NonNull
	private Instant when;

	@NonNull
	private final TableRecordReference reference;

	@NonNull
	private final Date movementDate;

	@NonNull
	private final Integer warehouseId;

	@NonNull
	private final Integer locatorId;

	@NonNull
	private final BigDecimal qty;

	@NonNull
	private final Integer productId;

	private final boolean transactionDeleted;

}
