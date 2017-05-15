package de.metas.handlingunits.inout.impl;

import java.util.List;
import java.util.Properties;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_DocType;
import org.compiere.util.Env;

import de.metas.document.IDocTypeDAO;
import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.IHUTrxBL;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.empties.EmptiesInOutLinesProducer;
import de.metas.handlingunits.inout.IHUPackingMaterialDAO;
import de.metas.handlingunits.inout.IReturnsInOutProducer;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Assignment;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_PI;
import de.metas.handlingunits.model.I_M_HU_PI_Item;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.handlingunits.model.I_M_HU_PackingMaterial;
import de.metas.handlingunits.model.I_M_InOutLine;
import de.metas.handlingunits.storage.IHUProductStorage;
import de.metas.handlingunits.storage.IHUStorage;
import de.metas.handlingunits.storage.IHUStorageFactory;
import de.metas.inoutcandidate.spi.impl.HUPackingMaterialDocumentLineCandidate;
import de.metas.inoutcandidate.spi.impl.HUPackingMaterialsCollector;

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

/**
 * Producer for vendor returns.
 * Used when some products were received from a vendor and it was decided they are not respecting the quality standards they can be sent back to the vendor.
 *
 * Introduced in #1062
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class QualityReturnsInOutProducer extends AbstractReturnsInOutProducer
{
	private HUPackingMaterialsCollector collector = null;

	/**
	 * Builder for lines with products that are not packing materials
	 */
	private final QualityReturnsInOutLinesBuilder inoutLinesBuilder = QualityReturnsInOutLinesBuilder.newBuilder(inoutRef);

	/**
	 * Builder for packing material lines
	 */
	private final EmptiesInOutLinesProducer packingMaterialInoutLinesBuilder = EmptiesInOutLinesProducer.newInstance(inoutRef);

	// services
	private final transient IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
	private final transient IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
	private final transient IHUPackingMaterialDAO huPackingMaterialDAO = Services.get(IHUPackingMaterialDAO.class);

	/**
	 * List of handling units that have to be returned to vendor
	 */
	// private List<I_M_HU> husToReturn = new ArrayList<>();

	private final List<I_M_HU_Assignment> huAssignments;

	public QualityReturnsInOutProducer(final Properties ctx, final List<I_M_HU_Assignment> huAssignments)
	{
		super();

		Check.assumeNotNull(ctx, "ctx not null");
		setCtx(ctx);

		this.huAssignments = huAssignments;

	}

	@Override
	protected void createLines()
	{

		if (collector == null)
		{
			final IHUContext huContext = handlingUnitsBL.createMutableHUContext(getCtx());
			collector = new HUPackingMaterialsCollector(huContext);
		}

		for (final I_M_HU_Assignment huAssignment : huAssignments)
		{

			final I_M_HU hu = huAssignment.getM_HU();

			// we know for sure the huAssignments are for inoutlines
			final I_M_InOutLine inOutLine = InterfaceWrapperHelper.create(getCtx(), huAssignment.getRecord_ID(), I_M_InOutLine.class, ITrx.TRXNAME_None);

			collector.addHURecursively(hu, inOutLine);

			// Create product (non-packing material) lines
			{
				// Iterate the product storages of this HU and create/update the inout lines
				final IHUStorageFactory huStorageFactory = handlingUnitsBL.getStorageFactory();
				final IHUStorage huStorage = huStorageFactory.getStorage(hu);
				final List<IHUProductStorage> productStorages = huStorage.getProductStorages();
				for (final IHUProductStorage productStorage : productStorages)
				{
					inoutLinesBuilder.addHUProductStorage(productStorage, inOutLine);
				}
			}
		}

		final List<HUPackingMaterialDocumentLineCandidate> pmCandidates = collector.getAndClearCandidates();

		for (HUPackingMaterialDocumentLineCandidate pmCandidate : pmCandidates)
		{
			final I_M_HU_PackingMaterial packingMaterial = huPackingMaterialDAO.retrivePackingMaterialOfProduct(pmCandidate.getM_Product());

			addPackingMaterial(packingMaterial, pmCandidate.getQty().intValueExact());
		}
		// Step 3: Create the packing material lines that were prepared
		packingMaterialInoutLinesBuilder.create();

	}

	@Override
	public boolean isEmpty()
	{
		// only empty if there are no product lines.
		return inoutLinesBuilder.isEmpty();
	}

	@Override
	protected int getReturnsDocTypeId(final String docBaseType, final boolean isSOTrx, final int adClientId, final int adOrgId)
	{
		// in the case of returns the docSubType is null
		final String docSubType = IDocTypeDAO.DOCSUBTYPE_NONE;

		final I_C_DocType returnsDocType = Services.get(IDocTypeDAO.class)
				.getDocTypeOrNullForSOTrx(
						Env.getCtx() // ctx
						, docBaseType // doc basetype
						, docSubType // doc subtype
						, isSOTrx // isSOTrx
						, adClientId // client
						, adOrgId // org
						, ITrx.TRXNAME_None // trx
		);

		return returnsDocType == null ? -1 : returnsDocType.getC_DocType_ID();
	}

	@Override
	public IReturnsInOutProducer addPackingMaterial(final I_M_HU_PackingMaterial packingMaterial, final int qty)
	{
		packingMaterialInoutLinesBuilder.addSource(packingMaterial, qty);
		return this;
	}



}
