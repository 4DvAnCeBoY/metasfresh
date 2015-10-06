package de.metas.handlingunits.attribute.storage.impl;

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

import org.adempiere.mm.attributes.spi.IAttributeValueContext;
import org.adempiere.util.Check;
import org.adempiere.util.WeakList;

import de.metas.handlingunits.attribute.IAttributeValue;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageListener;

/**
 *
 * NOTE: listeners will be added weakly
 *
 * @author tsa
 *
 */
public class CompositeAttributeStorageListener implements IAttributeStorageListener
{
	private final List<IAttributeStorageListener> listeners;

	public CompositeAttributeStorageListener()
	{
		super();
		listeners = new WeakList<IAttributeStorageListener>(true); // weakDefault=true
	}

	/**
	 * Registers given listener.
	 *
	 * NOTEs:
	 * <ul>
	 * <li>listeners will be added weakly
	 * <li>a listener won't be added if was already added before
	 * </ul>
	 *
	 * @param listener
	 */
	public void addAttributeStorageListener(final IAttributeStorageListener listener)
	{
		Check.assumeNotNull(listener, "listener not null");
		if (listeners.contains(listener))
		{
			return;
		}
		listeners.add(listener);
	}

	public void removeAttributeStorageListener(final IAttributeStorageListener listener)
	{
		if (listener == null)
		{
			return;
		}
		listeners.remove(listener);
	}

	public void clear()
	{
		listeners.clear();
	}

	@Override
	public void onAttributeValueCreated(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue)
	{
		for (final IAttributeStorageListener listener : listeners)
		{
			listener.onAttributeValueCreated(attributeValueContext, storage, attributeValue);
		}
	}

	@Override
	public void onAttributeValueChanged(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue, final Object valueOld)
	{
		for (final IAttributeStorageListener listener : listeners)
		{
			listener.onAttributeValueChanged(attributeValueContext, storage, attributeValue, valueOld);
		}
	}

	@Override
	public void onAttributeValueDeleted(final IAttributeValueContext attributeValueContext, final IAttributeStorage storage, final IAttributeValue attributeValue)
	{
		for (final IAttributeStorageListener listener : listeners)
		{
			listener.onAttributeValueDeleted(attributeValueContext, storage, attributeValue);
		}
	}

	@Override
	public void onAttributeStorageDisposed(IAttributeStorage storage)
	{
		for (final IAttributeStorageListener listener : listeners)
		{
			listener.onAttributeStorageDisposed(storage);
		}
	}

}
