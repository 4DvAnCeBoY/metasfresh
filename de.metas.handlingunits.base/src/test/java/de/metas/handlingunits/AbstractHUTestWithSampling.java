package de.metas.handlingunits;

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


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.junit.Assert;

import de.metas.handlingunits.allocation.ILUTUConfigurationFactory;
import de.metas.handlingunits.allocation.ILUTUProducerAllocationDestination;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_LUTU_Configuration;
import de.metas.handlingunits.model.I_M_HU_PI;
import de.metas.handlingunits.model.I_M_HU_PI_Item;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.handlingunits.model.X_M_HU_PI_Item;
import de.metas.handlingunits.model.X_M_HU_PI_Version;
import de.metas.handlingunits.storage.IHUStorage;
import de.metas.handlingunits.storage.IHUStorageFactory;

/**
 * Contains BL for easily sampling / creating or splitting Handling Units
 *
 * @author al
 */
public class AbstractHUTestWithSampling extends AbstractHUTest
{
	protected static final BigDecimal CU_QTY_85 = BigDecimal.valueOf(85);
	protected static final BigDecimal CU_QTY_46 = BigDecimal.valueOf(46);

	protected I_M_HU_PI huDefPalet;
	protected I_M_HU_PI_Item huItemIFCO_10;
	protected I_M_HU_PI_Item huItemIFCO_5;
	protected I_M_HU_PI_Item huItemIFCO_2;

	protected I_M_HU_PI huDefIFCO_10;
	protected I_M_HU_PI_Item materialItemTomato_10;
	protected I_M_HU_PI_Item_Product materialItemProductTomato_10;

	protected I_M_HU_PI huDefIFCO_5;
	protected I_M_HU_PI_Item materialItemTomato_5;
	protected I_M_HU_PI_Item_Product materialItemProductTomato_5;

	protected I_M_HU_PI huDefIFCO_2;
	protected I_M_HU_PI_Item materialItemTomato_2;
	protected I_M_HU_PI_Item_Product materialItemProductTomato_2;

	protected I_M_HU_PI huDefPaloxe_430;
	protected I_M_HU_PI_Item materialItemTomato_430;
	protected I_M_HU_PI_Item_Product materialItemProductTomato_430;

	protected I_M_HU_PI_Item packingMaterialItemPalet;
	protected I_M_HU_PI_Item packingMaterialItemIFCO_10;
	protected I_M_HU_PI_Item packingMaterialItemIFCO_5;
	protected I_M_HU_PI_Item packingMaterialItemIFCO_2;
	protected I_M_HU_PI_Item packingMaterialItemPaloxe_430;

	protected IHUContext huContext;
	protected IHUStorageFactory huStorageFactory;
	protected IAttributeStorageFactory attributeStorageFactory;

	@Override
	protected final void initialize()
	{
		//
		// Prepare context
		final String trxName = helper.trxName; // use the helper's thread-inherited trxName

		huContext = helper.createMutableHUContextForProcessing(trxName);
		huStorageFactory = huContext.getHUStorageFactory();
		attributeStorageFactory = huContext.getHUAttributeStorageFactory();

		//
		// Handling Units Definition
		huDefIFCO_10 = helper.createHUDefinition(HUTestHelper.NAME_IFCO_Product, X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit);
		{
			materialItemTomato_10 = helper.createHU_PI_Item_Material(huDefIFCO_10);
			materialItemProductTomato_10 = helper.assignProduct(materialItemTomato_10, getCUProduct(), BigDecimal.TEN, uomKg); // 10 x Tomato Per IFCO

			packingMaterialItemIFCO_10 = helper.createHU_PI_Item_PackingMaterial(huDefIFCO_10, pmIFCO);
		}

		huDefIFCO_5 = helper.createHUDefinition(HUTestHelper.NAME_IFCO_Product, X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit);
		{
			materialItemTomato_5 = helper.createHU_PI_Item_Material(huDefIFCO_5);
			materialItemProductTomato_5 = helper.assignProduct(materialItemTomato_5, getCUProduct(), BigDecimal.valueOf(5), uomKg); // 5 x Tomato Per IFCO

			packingMaterialItemIFCO_5 = helper.createHU_PI_Item_PackingMaterial(huDefIFCO_5, pmIFCO);
		}

		huDefIFCO_2 = helper.createHUDefinition(HUTestHelper.NAME_IFCO_Product, X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit);
		{
			materialItemTomato_2 = helper.createHU_PI_Item_Material(huDefIFCO_2);
			materialItemProductTomato_2 = helper.assignProduct(materialItemTomato_2, getCUProduct(), BigDecimal.valueOf(2), uomKg); // 2 x Tomato Per IFCO

			packingMaterialItemIFCO_2 = helper.createHU_PI_Item_PackingMaterial(huDefIFCO_2, pmIFCO);
		}

		huDefPalet = helper.createHUDefinition(HUTestHelper.NAME_Palet_Product, X_M_HU_PI_Version.HU_UNITTYPE_LoadLogistiqueUnit);
		{
			huItemIFCO_10 = helper.createHU_PI_Item_IncludedHU(huDefPalet, huDefIFCO_10, BigDecimal.valueOf(88)); // 88 x IFCO Per Palette
			huItemIFCO_5 = helper.createHU_PI_Item_IncludedHU(huDefPalet, huDefIFCO_5, BigDecimal.valueOf(88)); // 88 x IFCO Per Palette
			huItemIFCO_2 = helper.createHU_PI_Item_IncludedHU(huDefPalet, huDefIFCO_2, BigDecimal.valueOf(88)); // 88 x IFCO Per Palette
			packingMaterialItemPalet = helper.createHU_PI_Item_PackingMaterial(huDefPalet, pmPallets);
		}

		huDefPaloxe_430 = helper.createHUDefinition(HUTestHelper.NAME_Paloxe_Product, X_M_HU_PI_Version.HU_UNITTYPE_TransportUnit);
		{
			materialItemTomato_430 = helper.createHU_PI_Item_Material(huDefPaloxe_430);
			materialItemProductTomato_430 = helper.assignProduct(materialItemTomato_430, getCUProduct(), BigDecimal.valueOf(430), uomKg); // 430 x Tomato Per Paloxe

			packingMaterialItemPaloxe_430 = helper.createHU_PI_Item_PackingMaterial(huDefPaloxe_430, pmPaloxe);
		}

		afterInitialize();
	}

	/**
	 * Executed after {@link #initialize()}. To be overridden by implementing classes
	 */
	@OverridingMethodsMustInvokeSuper
	protected void afterInitialize()
	{
		// nothing at this level
	}

	protected final I_M_Product getCUProduct()
	{
		return pTomato;
	}

	protected final I_C_UOM getCUUOM()
	{
		return uomEach;
	}

	/**
	 * Note: Sets GROSS weight on the LoadingUnit to given <code>grossWeight</code>
	 *
	 * @param loadingUnitPIItem the PI item of the LU to allocate on
	 * @param tuPIItemProduct
	 * @param cuQty the full amount of customer units (i.e Tomatoes) which you'll have on your LU-TU structure
	 * @param grossWeight weight which was supposedly input by the user
	 *
	 * @return LU which was created
	 */
	protected final I_M_HU createIncomingLoadingUnit(
			final I_M_HU_PI_Item loadingUnitPIItem,
			final I_M_HU_PI_Item_Product tuPIItemProduct,
			final BigDecimal cuQty,
			final BigDecimal grossWeight)
	{
		final I_C_BPartner bpartner = null;
		final int bpartnerLocationId = -1;

		final ILUTUConfigurationFactory lutuConfigurationFactory = Services.get(ILUTUConfigurationFactory.class);
		final I_M_HU_LUTU_Configuration lutuConfiguration = lutuConfigurationFactory.createLUTUConfiguration(tuPIItemProduct, getCUProduct(), getCUUOM(), bpartner);
		lutuConfiguration.setC_BPartner(bpartner);
		lutuConfiguration.setC_BPartner_Location_ID(bpartnerLocationId);
		lutuConfigurationFactory.save(lutuConfiguration);

		final ILUTUProducerAllocationDestination luProducerDestination = lutuConfigurationFactory.createLUTUProducerAllocationDestination(lutuConfiguration);
		luProducerDestination.setLUPI(loadingUnitPIItem.getM_HU_PI_Version().getM_HU_PI());
		luProducerDestination.setLUItemPI(loadingUnitPIItem);
		luProducerDestination.setMaxLUsInfinite(); // allow creating as much LUs as needed

		//
		// Create and destroy instances only with an I_M_Transaction
		final List<I_M_HU> loadingUnits = helper.createHUs(huContext, luProducerDestination, cuQty);

		Assert.assertEquals("Invalid amount of initial LoadingUnits", 1, loadingUnits.size());
		final I_M_HU loadingUnit = loadingUnits.get(0);

		//
		// Propagate WeightGross (this will also calculate Net); Net = Gross - Tare - TareAdjust
		final IAttributeStorage attributeStorageLoadingUnit = attributeStorageFactory.getAttributeStorage(loadingUnit);
		attributeStorageLoadingUnit.setValue(attr_WeightGross, grossWeight);

		return loadingUnit;
	}

	/**
	 * Note: Sets GROSS weight on the TradingUnits to given <code>grossWeight</code>
	 *
	 * @param tradingUnitPIItem the PI item of the TU to allocate on
	 * @param tuPIItemProduct
	 * @param cuQty the full amount of customer units (i.e Tomatoes) which you'll have on your LU-TU structure
	 * @param grossWeight weight which was supposedly input by the user
	 *
	 * @return TUs created
	 */
	protected final List<I_M_HU> createIncomingTradingUnits(
			final I_M_HU_PI_Item tradingUnitPIItem,
			final I_M_HU_PI_Item_Product tuPIItemProduct,
			final BigDecimal cuQty,
			final BigDecimal grossWeight)
	{
		final I_M_HU_LUTU_Configuration lutuConfiguration = createM_HU_LUTU_Configuration_ForTU(tuPIItemProduct);

		final BigDecimal tuCapacity = tuPIItemProduct.getQty();

		final ILUTUConfigurationFactory lutuConfigurationFactory = Services.get(ILUTUConfigurationFactory.class);
		final ILUTUProducerAllocationDestination luProducerDestination = lutuConfigurationFactory.createLUTUProducerAllocationDestination(lutuConfiguration);
		luProducerDestination.setNoLU(); // disallow creation of LUs
		luProducerDestination.setTUPI(tradingUnitPIItem.getM_HU_PI_Version().getM_HU_PI());

		//
		// Create and destroy instances only with an I_M_Transaction
		final List<I_M_HU> tradingUnits = helper.createHUs(huContext, luProducerDestination, cuQty);

		Assert.assertEquals("Invalid amount of initial TradingUnits", cuQty.divide(tuCapacity, 2, RoundingMode.CEILING).intValueExact(), tradingUnits.size());

		//
		// Set and propagate WeightGross (this will also calculate Net)
		setWeightGross(tradingUnits, grossWeight);

		return tradingUnits;
	}

	/**
	 * Sets WeightGross to given HUs.
	 *
	 * Please note that WeightGross will be propagated and WeightNet will be also calculated (Net = Gross - Tare - TareAdjust).
	 *
	 * @param hus
	 * @param grossWeight
	 */
	protected void setWeightGross(final List<I_M_HU> hus, final BigDecimal grossWeight)
	{
		//
		for (final I_M_HU hu : hus)
		{
			final IAttributeStorage attributeStorageTradingUnit = attributeStorageFactory.getAttributeStorage(hu);
			attributeStorageTradingUnit.setValue(attr_WeightGross, grossWeight);
		}

	}

	protected I_M_HU_LUTU_Configuration createM_HU_LUTU_Configuration_ForTU(final I_M_HU_PI_Item_Product tuPIItemProduct)
	{
		final I_C_BPartner bpartner = null;
		final int bpartnerLocationId = -1;

		final ILUTUConfigurationFactory lutuConfigurationFactory = Services.get(ILUTUConfigurationFactory.class);
		final I_M_HU_LUTU_Configuration lutuConfiguration = lutuConfigurationFactory.createLUTUConfiguration(tuPIItemProduct, getCUProduct(), getCUUOM(), bpartner);
		lutuConfiguration.setC_BPartner(bpartner);
		lutuConfiguration.setC_BPartner_Location_ID(bpartnerLocationId);
		lutuConfigurationFactory.save(lutuConfiguration);
		return lutuConfiguration;
	}

	/**
	 * @param loadingUnit
	 * @param qty
	 * @return first found tradingUnit inside the loadingUnit with given customerUnit qty
	 */
	protected final I_M_HU findTUInLUWithQty(final I_M_HU loadingUnit, final int customerUnitQty)
	{
		final IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
		final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);

		//
		// Iterate through the loadingUnit's items
		final List<I_M_HU_Item> luItems = handlingUnitsDAO.retrieveItems(loadingUnit);
		for (final I_M_HU_Item luItem : luItems)
		{
			// We only have one item with handling unit Item Type
			final String itemType = handlingUnitsBL.getItemType(luItem);
			if (!X_M_HU_PI_Item.ITEMTYPE_HandlingUnit.equals(itemType))
			{
				continue;
			}

			//
			// Iterate through the tradingUnits
			final List<I_M_HU> tuHUs = handlingUnitsDAO.retrieveIncludedHUs(luItem);
			for (final I_M_HU tuHU : tuHUs)
			{
				final IHUStorage tradingUnitStorage = huStorageFactory.getStorage(tuHU);
				final BigDecimal tradingUnitQty = tradingUnitStorage.getQty(getCUProduct(), getCUUOM());
				if (tradingUnitQty.intValueExact() != customerUnitQty)
				{
					continue;
				}

				return tuHU;
			}
		}

		throw new AdempiereException("Storage not found with desired qty {} in {}", new Object[] { customerUnitQty, loadingUnit });
	}

	protected final List<I_M_HU> splitLU(final I_M_HU luToSplit,
			final I_M_HU_PI_Item luPIItem,
			final I_M_HU_PI_Item tuPIItem,
			final BigDecimal cuQty,
			final BigDecimal cuPerTU,
			final BigDecimal tuPerLU,
			final BigDecimal maxLUToAllocate)
	{
		final I_M_Product cuProduct = getCUProduct();
		final I_C_UOM cuUOM = getCUUOM();

		return helper.splitHUs(huContext,
				luToSplit,
				cuProduct, cuQty, cuUOM,
				cuPerTU, tuPerLU, maxLUToAllocate,
				tuPIItem, luPIItem);
	}
}
