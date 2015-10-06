package de.metas.handlingunits.allocation.impl;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.List;

import org.adempiere.util.Check;

import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationResult;
import de.metas.handlingunits.allocation.IAllocationStrategy;
import de.metas.handlingunits.allocation.IAllocationStrategyFactory;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.X_M_HU_PI_Item;

/**
 * Abstract FIFO Allocation/Deallocation strategy
 *
 * @author tsa
 *
 */
public abstract class AbstractFIFOStrategy extends AbstractAllocationStrategy
{
	public AbstractFIFOStrategy(final boolean outTrx)
	{
		super(outTrx);
	}

	@Override
	public final IAllocationResult execute(final I_M_HU hu, final IAllocationRequest request)
	{
		Check.assumeNotNull(hu, "'hu' is not null");

		//
		// Create Initial Result
		final IMutableAllocationResult status = AllocationUtils.createMutableAllocationResult(request);
		if (status.isCompleted())
		{
			return status;
		}

		// TODO: Check if request is about to deallocate the whole HU. If that's the case, don't create allocation requests for storages but consider moving the whole HU

		//
		// Iterate HU Items and try to allocate on them
		for (final I_M_HU_Item item : getHandlingUnitsDAO().retrieveItems(hu))
		{
			//
			// Check: is allocation is completed, stop it here.
			if (status.isCompleted())
			{
				return status;
			}

			//
			// Gets ItemType
			final String itemType = handlingUnitsBL.getItemType(item);

			//
			// Allocate to material item
			if (X_M_HU_PI_Item.ITEMTYPE_Material.equals(itemType))
			{
				final IAllocationRequest itemRequest = AllocationUtils.createQtyRequestForRemaining(request, status);
				final IAllocationResult itemResult = allocateOnMaterialItem(item, itemRequest);
				AllocationUtils.mergeAllocationResult(status, itemResult);
			}
			//
			// Allocate to included handling units
			else if (X_M_HU_PI_Item.ITEMTYPE_HandlingUnit.equals(itemType))
			{
				final IAllocationRequest itemRequest = AllocationUtils.createQtyRequestForRemaining(request, status);
				final IAllocationResult itemResult = allocateOnIncludedHUItem(item, itemRequest);
				AllocationUtils.mergeAllocationResult(status, itemResult);

				//
				// Try to allocate in newly created included HUs
				if (!status.isCompleted())
				{
					final IAllocationRequest requestRemaining = AllocationUtils.createQtyRequestForRemaining(request, status);
					final IAllocationResult resultRemaining = allocateRemainingOnIncludedHUItem(item, requestRemaining);
					AllocationUtils.mergeAllocationResult(status, resultRemaining);
				}
			}
			else
			{
				// other item types are skipped because we cannot allocate on them
			}
		}

		return status;
	}

	/**
	 * Allocate/Deallocate on HUs which are linked to given HU Item.
	 *
	 * The ItemType of given <code>item</code> can be anything (and it's not checked). So, this method can be used to allocate/deallocate on real included HUs or to allocate/deallocate to VHUs linked
	 * to this material HU item.
	 *
	 * @param item
	 * @param request
	 * @return allocation result
	 */
	protected IAllocationResult allocateOnIncludedHUItem(final I_M_HU_Item item, final IAllocationRequest request)
	{
		//
		// Create initial result
		final IMutableAllocationResult result = AllocationUtils.createMutableAllocationResult(request);

		//
		// Try to allocate on existing included HUs
		// TODO: consider retrieving an iterator of included HUs, load only what you need and after each included HU allocation check if you need to go forward
		final List<I_M_HU> includedHUs = getHandlingUnitsDAO().retrieveIncludedHUs(item);
		for (final I_M_HU includedHU : includedHUs)
		{
			final IAllocationRequest includedRequest = AllocationUtils.createQtyRequestForRemaining(request, result);
			final IAllocationResult includedResult = allocateOnIncludedHU(includedHU, includedRequest);
			AllocationUtils.mergeAllocationResult(result, includedResult);
		}

		return result;
	}

	/**
	 * If after {@link #allocateOnIncludedHUItem(I_M_HU_Item, IAllocationRequest)} there is more to allocate then this method will be called to allocate remaining qty.
	 *
	 * @param item
	 * @param request
	 * @return allocation result
	 */
	protected abstract IAllocationResult allocateRemainingOnIncludedHUItem(final I_M_HU_Item item, final IAllocationRequest request);

	/**
	 * Allocate/Deallocate given request to given item of ItemType=Material.
	 *
	 * @param item
	 * @param request
	 * @return allocation result
	 */
	private IAllocationResult allocateOnMaterialItem(final I_M_HU_Item item, final IAllocationRequest request)
	{
		// If there nothing requested to allocate, it's pointless to do something
		if (request.isZeroQty())
		{
			return AllocationUtils.nullResult();
		}

		//
		// If our item is from a Virtual HU, we shall allocate on it directly
		if (handlingUnitsBL.isVirtual(item))
		{
			final I_M_HU_Item vhuItem = item;
			return allocateOnVirtualMaterialItem(vhuItem, request);
		}

		//
		// Deallocation
		if (isOutTrx())
		{
			// iterate all VHU linked to this item and try to deallocate from them
			return allocateOnIncludedHUItem(item, request);
		}
		//
		// Allocation
		else
		{
			return allocateOnNewVHU(request, item);
		}
	}

	/**
	 * Creates an new Virtual HU (linked to given parent item) and allocate the request on it.
	 *
	 * @param request
	 * @param parentItem
	 * @return allocation result
	 */
	private IAllocationResult allocateOnNewVHU(final IAllocationRequest request, final I_M_HU_Item parentItem)
	{
		final IAllocationRequest requestActual = createActualRequest(parentItem, request);
		if (requestActual.isZeroQty())
		{
			return AllocationUtils.nullResult();
		}

		final IHUContext huContext = request.getHUContext();
		final I_M_HU vhu = createVHU(huContext, requestActual, parentItem);
		return allocateOnIncludedHU(vhu, requestActual);
	}

	/**
	 * Allocate/Deallocate <code>request</code> on included <code>hu</code>.
	 *
	 * The {@link IAllocationStrategy} that will be used is provided by {@link #getIncludedHUAllocationStrategy(I_M_HU, IAllocationRequest)}.
	 *
	 * @param includedHU handling unit to allocate on; this handling unit is included in HU on which we were allocating until this call
	 * @param request
	 * @return allocation result
	 */
	protected final IAllocationResult allocateOnIncludedHU(final I_M_HU includedHU, final IAllocationRequest request)
	{
		final IAllocationStrategy allocationStrategy = getIncludedHUAllocationStrategy(includedHU, request);
		final IAllocationResult allocationResult = allocationStrategy.execute(includedHU, request);
		return allocationResult;
	}

	protected final IAllocationStrategy getIncludedHUAllocationStrategy(final I_M_HU hu, final IAllocationRequest request)
	{
		final IAllocationStrategyFactory factory = getAllocationStrategyFactory();

		// If there is no factory, reuse the same strategy
		if (factory == null)
		{
			return this;
		}

		if (isOutTrx())
		{
			final IAllocationStrategy allocationStrategy = factory.getDeallocationStrategy(hu);
			return allocationStrategy;
		}
		else
		{
			final IAllocationStrategy allocationStrategy = factory.getAllocationStrategy(hu);
			return allocationStrategy;

		}
	}
}
