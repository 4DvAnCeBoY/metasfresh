package de.metas.pricing.service.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.util.Services;
import org.compiere.model.I_Test;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.metas.bpartner.BPartnerId;
import de.metas.money.CurrencyId;
import de.metas.pricing.IEditablePricingContext;
import de.metas.pricing.PriceListId;
import de.metas.pricing.PriceListVersionId;
import de.metas.pricing.PricingSystemId;
import de.metas.product.ProductId;

public class PricingContextTest
{
	@Before
	public void init()
	{
		AdempiereTestHelper.get().init();
	}

	/**
	 * Tests if {@link PricingContext#copy()} works correctly.
	 */
	@Test
	public void test_copy()
	{
		final PricingContext pricingCtx = new PricingContext();

		int nextId = 1;
		final Timestamp priceDate = TimeUtil.getDay(2015, 2, 3, 4, 5, 6);

		final String trxName = Services.get(ITrxManager.class).createTrxName("MyTrxName1", true);
		final I_Test referencedObject = InterfaceWrapperHelper.create(Env.getCtx(), I_Test.class, trxName);
		InterfaceWrapperHelper.save(referencedObject);

		pricingCtx.setAD_Table_ID(nextId++);
		pricingCtx.setBPartnerId(BPartnerId.ofRepoId(nextId++));
		pricingCtx.setCurrencyId(CurrencyId.ofRepoId(nextId++));
		pricingCtx.setC_UOM_ID(nextId++);
		pricingCtx.setConvertPriceToContextUOM(true);
		pricingCtx.setDisallowDiscount(true);
		pricingCtx.setPricingSystemId(PricingSystemId.ofRepoId(nextId++));
		pricingCtx.setPriceListId(PriceListId.ofRepoId(nextId++));
		pricingCtx.setPriceListVersionId(PriceListVersionId.ofRepoId(nextId++));
		pricingCtx.setProductId(ProductId.ofRepoId(nextId++));
		pricingCtx.setManualPrice(true);
		pricingCtx.setPP_Product_BOM_ID(nextId++);
		pricingCtx.setPP_Product_BOMLine_ID(nextId++);
		pricingCtx.setPriceDate(priceDate);
		pricingCtx.setProperty("PropertyName1", "Value1");
		pricingCtx.setProperty("PropertyName2", "Value2");
		pricingCtx.setQty(new BigDecimal("123.456"));
		pricingCtx.setRecord_ID(nextId++);
		pricingCtx.setReferencedObject(referencedObject);
		pricingCtx.setSOTrx(true);
		pricingCtx.setTrxName("MyTrxName2"); // overrides the one set from setReferencedObject()

		final IEditablePricingContext pricingCtxCopy = pricingCtx.copy();

		System.out.println("PricingCtx: " + pricingCtx);
		System.out.println("PricingCtxCopy: " + pricingCtxCopy);

		//
		// Assert string representations are equal
		// NOTE: we assume PricingContext.toString() is using our generic to string method which is introspecting and printing all fields.
		Assert.assertEquals(pricingCtx.toString(), pricingCtxCopy.toString());
	}
}
