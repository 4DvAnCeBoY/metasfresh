package de.metas.handlingunits.impl;

import java.util.Collection;

import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.util.Services;

import com.google.common.base.Preconditions;

import de.metas.handlingunits.IHULockBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.lock.api.ILockCommand.AllowAdditionalLocks;
import de.metas.lock.api.ILockManager;
import de.metas.lock.api.LockOwner;

/*
 * #%L
 * de.metas.handlingunits.base
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

public class HULockBL implements IHULockBL
{
	@Override
	public boolean isLocked(final I_M_HU hu)
	{
		return Services.get(ILockManager.class).isLocked(hu);
	}

	@Override
	public boolean isLockedBy(final I_M_HU hu, final LockOwner lockOwner)
	{
		return Services.get(ILockManager.class).isLocked(I_M_HU.class, hu.getM_HU_ID(), lockOwner);
	}

	@Override
	public void lock(final I_M_HU hu, final LockOwner lockOwner)
	{
		Preconditions.checkNotNull(hu, "hu is null");
		Preconditions.checkNotNull(lockOwner, "lockOwner is null");
		Preconditions.checkArgument(!lockOwner.isAnyOwner(), "{} not allowed", lockOwner);
		
		Services.get(ILockManager.class)
				.lock()
				.setOwner(lockOwner)
				.setFailIfAlreadyLocked(false)
				.setAllowAdditionalLocks(AllowAdditionalLocks.FOR_DIFFERENT_OWNERS)
				.setAutoCleanup(false)
				.setRecordByModel(hu)
				.acquire();
	}
	
	@Override
	public void lockAll(final Collection<I_M_HU> hus, final LockOwner lockOwner)
	{
		if(hus.isEmpty())
		{
			return;
		}
		
		Preconditions.checkNotNull(lockOwner, "lockOwner is null");
		Preconditions.checkArgument(!lockOwner.isAnyOwner(), "{} not allowed", lockOwner);
		
		Services.get(ILockManager.class)
				.lock()
				.setOwner(lockOwner)
				.setFailIfAlreadyLocked(false)
				.setAllowAdditionalLocks(AllowAdditionalLocks.FOR_DIFFERENT_OWNERS)
				.setAutoCleanup(false)
				.addRecordsByModel(hus)
				.acquire();
	}


	@Override
	public void unlock(final I_M_HU hu, final LockOwner lockOwner)
	{
		Preconditions.checkNotNull(hu, "hu is null");
		Preconditions.checkNotNull(lockOwner, "lockOwner is null");
		Preconditions.checkArgument(!lockOwner.isAnyOwner(), "{} not allowed", lockOwner);
		
		Services.get(ILockManager.class)
				.unlock()
				.setOwner(lockOwner)
				.setRecordByModel(hu)
				.release();
	}
	
	@Override
	public void unlockAll(final Collection<I_M_HU> hus, final LockOwner lockOwner)
	{
		if(hus.isEmpty())
		{
			return;
		}
		
		Preconditions.checkNotNull(lockOwner, "lockOwner is null");
		Preconditions.checkArgument(!lockOwner.isAnyOwner(), "{} not allowed", lockOwner);
		
		Services.get(ILockManager.class)
				.unlock()
				.setOwner(lockOwner)
				.setRecordsByModels(hus)
				.release();
	}


	@Override
	public IQueryFilter<I_M_HU> isLockedFilter()
	{
		return Services.get(ILockManager.class).getLockedByFilter(I_M_HU.class, LockOwner.ANY);
	}
	
	@Override
	public IQueryFilter<I_M_HU> isNotLockedFilter()
	{
		return Services.get(ILockManager.class).getNotLockedFilter(I_M_HU.class);
	}

}
