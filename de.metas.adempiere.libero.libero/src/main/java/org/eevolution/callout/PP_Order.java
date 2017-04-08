package org.eevolution.callout;

/*
 * #%L
 * de.metas.adempiere.libero.libero
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
import java.util.Properties;

import org.adempiere.ad.callout.annotations.Callout;
import org.adempiere.ad.callout.annotations.CalloutMethod;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.adempiere.warehouse.api.IWarehouseBL;
import org.compiere.model.CalloutEngine;
import org.compiere.model.I_AD_Workflow;
import org.compiere.model.I_M_Locator;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.MUOMConversion;
import org.compiere.util.Env;
import org.eevolution.api.IPPOrderBL;
import org.eevolution.api.IPPWorkflowDAO;
import org.eevolution.api.IProductBOMDAO;
import org.eevolution.api.IProductPlanningDAO;
import org.eevolution.model.I_PP_Order;
import org.eevolution.model.I_PP_Product_BOM;
import org.eevolution.model.I_PP_Product_Planning;

/**
 * Manufacturing order callout
 *
 * @author metas-dev <dev@metasfresh.com>
 * @author based on initial version developed by Victor Perez, Teo Sarca under ADempiere project
 */
@Callout(I_PP_Order.class)
public class PP_Order extends CalloutEngine
{
	@CalloutMethod(columnNames = I_PP_Order.COLUMNNAME_M_Product_ID)
	public void onProductChanged(final I_PP_Order ppOrder)
	{
		final I_M_Product product = ppOrder.getM_Product();
		if (product == null)
		{
			return;
		}

		ppOrder.setC_UOM_ID(product.getC_UOM_ID());

		final I_PP_Product_Planning pp = findPP_Product_Planning(ppOrder);
		ppOrder.setAD_Workflow_ID(pp.getAD_Workflow_ID());
		ppOrder.setPP_Product_BOM_ID(pp.getPP_Product_BOM_ID());

		if (pp.getPP_Product_BOM_ID() > 0)
		{
			final I_PP_Product_BOM bom = pp.getPP_Product_BOM();
			ppOrder.setC_UOM_ID(bom.getC_UOM_ID());
		}

		Services.get(IPPOrderBL.class).updateQtyBatchs(ppOrder, true); // override
	}

	@CalloutMethod(columnNames = I_PP_Order.COLUMNNAME_QtyEntered)
	public void onQtyEnteredChanged(final I_PP_Order ppOrder)
	{
		updateQtyOrdered(ppOrder);

		Services.get(IPPOrderBL.class).updateQtyBatchs(ppOrder, true); // override
	}

	@CalloutMethod(columnNames = I_PP_Order.COLUMNNAME_C_UOM_ID)
	public void onC_UOM_ID(final I_PP_Order ppOrder)
	{
		updateQtyOrdered(ppOrder);
	}

	@CalloutMethod(columnNames = I_PP_Order.COLUMNNAME_M_Warehouse_ID)
	public void onM_Warehouse_ID(final I_PP_Order ppOrder)
	{
		final I_M_Warehouse warehouse = ppOrder.getM_Warehouse();
		if (warehouse == null)
		{
			return;
		}

		I_M_Locator locator = Services.get(IWarehouseBL.class).getDefaultLocator(warehouse);
		ppOrder.setM_Locator(locator);
	}

	/** Calculates and sets QtyOrdered from QtyEntered and UOM */
	private final void updateQtyOrdered(final I_PP_Order ppOrder)
	{
		final int productId = ppOrder.getM_Product_ID();
		final int uomToId = ppOrder.getC_UOM_ID();
		final BigDecimal qtyEntered = ppOrder.getQtyEntered();

		BigDecimal qtyOrdered;
		if (productId <= 0 || uomToId <= 0)
		{
			qtyOrdered = qtyEntered;
		}
		else
		{
			qtyOrdered = MUOMConversion.convertToProductUOM(Env.getCtx(), productId, uomToId, qtyEntered);
			if (qtyOrdered == null)
			{
				qtyOrdered = qtyEntered;
			}
		}

		ppOrder.setQtyOrdered(qtyOrdered);
	}

	/**
	 * Find Product Planning Data for given manufacturing order. If not planning found, a new one is created and filled with default values.
	 * <p>
	 * TODO: refactor with org.eevolution.process.MRP.getProductPlanning method
	 *
	 * @param ctx context
	 * @param order manufacturing order
	 * @return product planning data (never return null)
	 */
	protected static I_PP_Product_Planning findPP_Product_Planning(final I_PP_Order order)
	{
		final Properties ctx = Env.getCtx();
		I_PP_Product_Planning pp = Services.get(IProductPlanningDAO.class).find(ctx,
				order.getAD_Org_ID(), order.getM_Warehouse_ID(),
				order.getS_Resource_ID(), order.getM_Product_ID(),
				null);
		if (pp == null)
		{
			pp = InterfaceWrapperHelper.create(ctx, I_PP_Product_Planning.class, ITrx.TRXNAME_None);
			pp.setAD_Org_ID(order.getAD_Org_ID());
			pp.setM_Warehouse_ID(order.getM_Warehouse_ID());
			pp.setS_Resource_ID(order.getS_Resource_ID());
			pp.setM_Product(order.getM_Product());
		}
		InterfaceWrapperHelper.setSaveDeleteDisabled(pp, true);

		final I_M_Product product = pp.getM_Product();
		//
		if (pp.getAD_Workflow_ID() <= 0)
		{
			final I_AD_Workflow workflow = Services.get(IPPWorkflowDAO.class).retrieveWorkflowForProduct(product);
			pp.setAD_Workflow(workflow);
		}
		if (pp.getPP_Product_BOM_ID() <= 0)
		{
			final I_PP_Product_BOM bom = Services.get(IProductBOMDAO.class).retrieveDefaultBOM(product);
			pp.setPP_Product_BOM(bom);
		}
		//
		return pp;
	}
}
