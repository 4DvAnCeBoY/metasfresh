package org.adempiere.warehouse;

import javax.annotation.Nullable;

import org.compiere.model.I_M_Locator;

import de.metas.lang.RepoIdAware;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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

@Value
public class LocatorId implements RepoIdAware
{
	int repoId;

	WarehouseId warehouseId;

	public static LocatorId ofRepoId(@NonNull final WarehouseId warehouseId, final int repoId)
	{
		return new LocatorId(repoId, warehouseId);
	}

	public static LocatorId ofRepoIdOrNull(@Nullable final WarehouseId warehouseId, final int repoId)
	{
		if (repoId <= 0)
		{
			return null;
		}
		return ofRepoId(warehouseId, repoId);
	}

	public static LocatorId ofRecordOrNull(@Nullable final I_M_Locator locatorRecord)
	{
		if (locatorRecord == null)
		{
			return null;
		}
		return ofRecord(locatorRecord);
	}

	public static LocatorId ofRecord(@Nullable final I_M_Locator locatorRecord)
	{
		return ofRepoId(
				WarehouseId.ofRepoId(locatorRecord.getM_Warehouse_ID()),
				locatorRecord.getM_Locator_ID());
	}

}
