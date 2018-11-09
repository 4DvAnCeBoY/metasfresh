package de.metas.material.planning;

import java.sql.Timestamp;

import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_S_ResourceType;

import de.metas.product.ResourceId;
import de.metas.util.ISingletonService;

/*
 * #%L
 * metasfresh-material-planning
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

public interface IResourceProductService extends ISingletonService
{
	I_M_Product getProductByResourceId(ResourceId resourceId);

	/**
	 * Get how many hours/day a is available.
	 * Minutes, secords and millis are discarded.
	 *
	 * @return available hours
	 */
	int getTimeSlotHoursForResourceType(I_S_ResourceType resourceType);

	/**
	 * Get available days / week.
	 *
	 * @return available days / week
	 */
	int getAvailableDaysWeekForResourceType(I_S_ResourceType resourceType);

	Timestamp getDayStartForResourceType(I_S_ResourceType resourceType, Timestamp date);

	Timestamp getDayEndForResourceType(I_S_ResourceType resourceType, Timestamp date);

	/**
	 * @return true if a resource of this type is generally available
	 *         (i.e. active, at least 1 day available, at least 1 hour available)
	 */
	boolean isAvailableForResourceType(I_S_ResourceType resourceType);

	boolean isDayAvailableForResourceType(I_S_ResourceType resourceType, Timestamp dateTime);

	I_C_UOM getResourceUOM(ResourceId resourceId);
}
