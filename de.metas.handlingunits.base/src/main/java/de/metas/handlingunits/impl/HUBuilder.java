package de.metas.handlingunits.impl;

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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_M_Attribute;
import org.compiere.model.I_M_Locator;

import de.metas.handlingunits.IHUBuilder;
import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.IHUIterator;
import de.metas.handlingunits.IHUTrxBL;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.attribute.Constants;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.exceptions.HUException;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_LUTU_Configuration;
import de.metas.handlingunits.model.I_M_HU_PI;
import de.metas.handlingunits.model.I_M_HU_PI_Item;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.handlingunits.model.I_M_HU_PI_Version;
import de.metas.handlingunits.model.X_M_HU;
import de.metas.handlingunits.storage.IHUStorageDAO;

/* package */final class HUBuilder extends AbstractHUIterator implements IHUBuilder
{
	// Services
	private final IHUTrxBL huTrxBL = Services.get(IHUTrxBL.class);

	private I_C_BPartner _bpartner = null;
	private int _bpartnerLocationId = -1;
	private I_M_HU_Item _parentItem = null;
	private I_M_HU_PI_Item_Product _piip = null;
	private I_M_Locator _locator = null;

	private boolean _huPlanningReceiptOwnerPM = false; // DB default false

	/**
	 * HU Status to be used when creating a new HU.
	 *
	 * Default: {@link X_M_HU#HUSTATUS_Planning}.
	 */
	private String _huStatus = X_M_HU.HUSTATUS_Planning;

	private I_M_HU_LUTU_Configuration _lutuConfiguration = null;

	public HUBuilder(final IHUContext huContext)
	{
		super();

		Check.assumeNotNull(huContext, "huContext not null");
		setHUContext(huContext);

		registerNodeIterator(I_M_HU.class, new HUNodeBuilder());
		registerNodeIterator(I_M_HU_Item.class, new HUItemNodeBuilder());
	}

	@Override
	public final void setC_BPartner(final I_C_BPartner bpartner)
	{
		_bpartner = bpartner;
	}

	@Override
	public final I_C_BPartner getC_BPartner()
	{
		return _bpartner;
	}

	@Override
	public void setC_BPartner_Location_ID(final int bpartnerLocationId)
	{
		_bpartnerLocationId = bpartnerLocationId;
	}

	public int getC_BPartner_Location_ID()
	{
		return _bpartnerLocationId;
	}

	@Override
	public final void setM_HU_Item_Parent(final I_M_HU_Item parentItem)
	{
		_parentItem = parentItem;
	}

	@Override
	public final I_M_HU_Item getM_HU_Item_Parent()
	{
		return _parentItem;
	}

	@Override
	public I_M_HU_PI_Item_Product getM_HU_PI_Item_Product()
	{
		return _piip;
	}

	@Override
	public void setM_HU_PI_Item_Product(final I_M_HU_PI_Item_Product piip)
	{
		_piip = piip;
	}

	@Override
	public final void setM_Locator(final I_M_Locator locator)
	{
		_locator = locator;
	}

	@Override
	public final I_M_Locator getM_Locator()
	{
		return _locator;
	}

	@Override
	public final void setHUStatus(final String huStatus)
	{
		Check.assumeNotEmpty(huStatus, "huStatus not empty");
		_huStatus = huStatus;
	}

	@Override
	public final String getHUStatus()
	{
		return _huStatus;
	}

	@Override
	public final void setM_HU_LUTU_Configuration(final I_M_HU_LUTU_Configuration lutuConfiguration)
	{
		if (lutuConfiguration == null)
		{
			_lutuConfiguration = null;
			return;
		}

		if (lutuConfiguration.getM_HU_LUTU_Configuration_ID() <= 0)
		{
			throw new HUException("lutuConfiguration shall be saved: " + lutuConfiguration);
		}

		if (InterfaceWrapperHelper.hasChanges(lutuConfiguration))
		{
			throw new HUException("lutuConfiguration shall have no changes: " + lutuConfiguration);
		}

		_lutuConfiguration = lutuConfiguration;
	}

	@Override
	public final I_M_HU_LUTU_Configuration getM_HU_LUTU_Configuration()
	{
		return _lutuConfiguration;
	}

	@Override
	public void setHUPlanningReceiptOwnerPM(final boolean huPlanningReceiptOwnerPM)
	{
		_huPlanningReceiptOwnerPM = huPlanningReceiptOwnerPM;
	}

	@Override
	public boolean isHUPlanningReceiptOwnerPM()
	{
		return _huPlanningReceiptOwnerPM;
	}

	private final Map<I_M_Attribute, Object> getInitialAttributeValueDefaults()
	{
		return getHUContext().getProperty(Constants.CTXATTR_DefaultAttributesValue);
	}

	@Override
	public IHUIterator iterate(final Collection<I_M_HU> hus)
	{
		// this method is not supported and will never be supported because it's not the scope of this class
		throw new UnsupportedOperationException();
	}

	@Override
	public IHUIterator iterate(final I_M_HU hu)
	{
		// this method is not supported and will never be supported because it's not the scope of this class
		throw new UnsupportedOperationException();
	}

	@Override
	public I_M_HU create(final I_M_HU_PI pi)
	{
		final I_M_HU_PI_Version piVersion = dao.retrievePICurrentVersion(pi);
		return create(piVersion);
	}

	@Override
	public I_M_HU create(final I_M_HU_PI_Version piVersion)
	{
		return createInstanceRecursivelly(piVersion, getM_HU_Item_Parent());
	}

	private I_M_HU createInstanceRecursivelly(final I_M_HU_PI_Version huPIVersion, final I_M_HU_Item parentItem)
	{
		//
		// Check if parent item was specified, make sure it was saved
		if (parentItem != null && parentItem.getM_HU_Item_ID() <= 0)
		{
			throw new AdempiereException(parentItem + " not saved");
		}

		//
		// Create instance and save it
		final I_M_HU hu = createHUInstance(huPIVersion);

		final IHUContext huContext = getHUContext();
		//
		// Assign HU to Parent
		huTrxBL.setParentHU(huContext, parentItem, hu);

		//
		// Generate HU Attributes
		final IAttributeStorageFactory attributesStorageFactory = huContext.getHUAttributeStorageFactory();
		final IAttributeStorage attributeStorage = attributesStorageFactory.getAttributeStorage(hu);
		attributeStorage.generateInitialAttributes(getInitialAttributeValueDefaults());

		setStatus(HUIteratorStatus.Running);

		//
		// Call HU Builder to create items and other included things (if any)
		final AbstractNodeIterator<I_M_HU> huBuilder = getNodeIterator(I_M_HU.class);
		huBuilder.iterate(hu);

		// Collect the HU (only if physical) in order to be taken from the gebindelager into the current lager
		if (Services.get(IHandlingUnitsBL.class).isPhysicalHU(hu.getHUStatus()))
		{
			huContext.getDestroyedHUPackingMaterialsCollector().removeHURecursively(hu);
		}
		//
		// If after running everything the status is still running, switch it to finished
		if (getStatus() == HUIteratorStatus.Running)
		{
			setStatus(HUIteratorStatus.Finished);
		}

		return hu;
	}

	/**
	 * Creates new {@link I_M_HU} and saves it.
	 *
	 * This method is creating ONLY the {@link I_M_HU} object and not it's children.
	 *
	 * It will use {@link #getM_HU_Item_Parent()} as it's parent.
	 *
	 * @param huPIVersion
	 * @return
	 * @see #createHUInstance(I_M_HU_PI_Version, I_M_HU_Item)
	 */
	private final I_M_HU createHUInstance(final I_M_HU_PI_Version huPIVersion)
	{
		final I_M_HU_Item parentItem = getM_HU_Item_Parent();
		return createHUInstance(huPIVersion, parentItem);
	}

	/**
	 * Creates new {@link I_M_HU} and saves it.
	 *
	 * This method is creating ONLY the {@link I_M_HU} object and not it's children.
	 *
	 * @param huPIVersion
	 * @param parentItem parent HU Item to link on
	 * @return created {@link I_M_HU}.
	 */
	private final I_M_HU createHUInstance(final I_M_HU_PI_Version huPIVersion, final I_M_HU_Item parentItem)
	{
		final IHUContext huContext = getHUContext();

		final I_M_HU hu = InterfaceWrapperHelper.newInstance(I_M_HU.class, huContext);
		hu.setM_HU_PI_Version(huPIVersion);

		//
		// Get Parent HU (if any)
		final I_M_HU parentHU;
		if (parentItem != null)
		{
			parentHU = parentItem.getM_HU();
		}
		else
		{
			parentHU = null;
		}

		//
		// Copy HUStatus from parent
		final String huStatus;
		if (parentHU != null)
		{
			huStatus = parentHU.getHUStatus();

		}
		else
		{
			// Set configured HUStatus if no parent item/parent item HU found.
			huStatus = getHUStatus();
		}

		Services.get(IHandlingUnitsBL.class).setHUStatus(huContext, hu, huStatus);

		//
		// Copy C_BPartner_ID from parent
		final int parentBPartnerId = parentHU == null ? -1 : parentHU.getC_BPartner_ID();
		if (parentBPartnerId > 0)
		{
			hu.setC_BPartner_ID(parentBPartnerId);
			hu.setC_BPartner_Location_ID(parentHU.getC_BPartner_Location_ID());
		}
		else
		{
			final I_C_BPartner bpartner = getC_BPartner();
			hu.setC_BPartner(bpartner);

			final int bpartnerLocationId = getC_BPartner_Location_ID();
			if (bpartner != null && bpartnerLocationId > 0)
			{
				hu.setC_BPartner_Location_ID(bpartnerLocationId);
			}
		}

		//
		// Copy M_Locator from parent or get it from HUBuilder configuration
		if (parentHU != null)
		{
			hu.setM_Locator_ID(parentHU.getM_Locator_ID());
		}
		else
		{
			final I_M_Locator locator = getM_Locator();
			hu.setM_Locator(locator);
		}

		//
		// Set LU/TU configuration reference
		final I_M_HU_LUTU_Configuration lutuConfig = getM_HU_LUTU_Configuration();
		hu.setM_HU_LUTU_Configuration(lutuConfig);

		//
		// fresh 07970: Set M_HU.M_HU_PI_Item_Product_ID
		final I_M_HU_PI_Item_Product piip = getM_HU_PI_Item_ProductOrNull(lutuConfig, parentHU, hu);
		hu.setM_HU_PI_Item_Product(piip);

		//
		// fresh 08162: Set M_HU.HUPlanningReceiptOwnerPM
		final boolean huPlanningReceiptOwnerPM = isHUPlanningReceiptOwnerPM();
		hu.setHUPlanningReceiptOwnerPM(huPlanningReceiptOwnerPM);

		//
		// Notify Storage and Attributes DAO that a new HU was created
		// NOTE: depends on their implementation, but they have a chance to do some optimizations
		huContext.getHUStorageFactory().getHUStorageDAO().initHUStorages(hu);
		huContext.getHUAttributeStorageFactory().getHUAttributesDAO().initHUAttributes(hu);

		//
		// Save HU
		dao.saveHU(hu);

		//
		// Return
		return hu;
	}

	/**
	 * @param parentHU
	 * @param hu
	 * @return Handling Unit PI Item Product or null, depending on the HU type and what's configured
	 */
	private I_M_HU_PI_Item_Product getM_HU_PI_Item_ProductOrNull(final I_M_HU_LUTU_Configuration lutuConfig, final I_M_HU parentHU, final I_M_HU hu)
	{
		//
		// Top level HUs shall not have a PIIP (can have a complete mix inside)
		// if (handlingUnitsBL.isTopLevel(hu))
		// {
		// return null;
		// }

		I_M_HU_PI_Item_Product piip = getM_HU_PI_Item_Product();
		if (piip != null)
		{
			return piip;
		}

		if (lutuConfig != null)
		{
			piip = lutuConfig.getM_HU_PI_Item_Product();
		}

		if (piip != null)
		{
			return piip;
		}

		if (parentHU != null)
		{
			piip = parentHU.getM_HU_PI_Item_Product();
		}
		return piip;
	}

	/**
	 * Builder used to create {@link I_M_HU_Item}s for given {@link I_M_HU}
	 *
	 * @author tsa
	 *
	 */
	protected class HUNodeBuilder extends HUNodeIterator
	{
		@Override
		public List<I_M_HU_Item> retrieveDownstreamNodes(final I_M_HU hu)
		{
			final I_M_HU_PI_Version piVersion = hu.getM_HU_PI_Version();

			final List<I_M_HU_PI_Item> piItems = dao.retrievePIItems(piVersion, getC_BPartner());
			final List<I_M_HU_Item> result = new ArrayList<I_M_HU_Item>(piItems.size());

			//
			// Create HU Items
			final IHUStorageDAO huStorageDAO = getHUContext().getHUStorageFactory().getHUStorageDAO();
			for (final I_M_HU_PI_Item piItem : piItems)
			{
				final I_M_HU_Item item = dao.createHUItem(hu, piItem);
				result.add(item);

				// Notify Storage DAO that a new item was just created
				huStorageDAO.initHUItemStorages(item);
			}

			return result;
		}
	}

	/**
	 * Builder used to create included {@link I_M_HU}s for given {@link I_M_HU_Item}.
	 *
	 *
	 * Actually is doing nothing because we are not creating included HUs recursivelly.
	 */
	protected class HUItemNodeBuilder extends HUItemNodeIterator
	{
		@Override
		public AbstractNodeIterator<?> getDownstreamNodeIterator(final I_M_HU_Item node)
		{
			return null;
		}

		@Override
		public List<Object> retrieveDownstreamNodes(final I_M_HU_Item huItem)
		{
			throw new IllegalStateException("Shall not be called because we don't have a downstream node iterator");
		}
	}
}
