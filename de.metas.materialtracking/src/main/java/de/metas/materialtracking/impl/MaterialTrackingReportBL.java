package de.metas.materialtracking.impl;

import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.compiere.model.I_M_AttributeSetInstance;

import de.metas.dimension.IDimensionSpecAttributeBL;
import de.metas.dimension.IDimensionspecDAO;
import de.metas.dimension.model.I_DIM_Dimension_Spec;
import de.metas.materialtracking.IMaterialTrackingReportBL;
import de.metas.materialtracking.MaterialTrackingConstants;
import de.metas.materialtracking.ch.lagerkonf.model.I_M_Material_Tracking_Report;
import de.metas.materialtracking.ch.lagerkonf.model.I_M_Material_Tracking_Report_Line;
import de.metas.materialtracking.ch.lagerkonf.model.I_M_Material_Tracking_Report_Line_Alloc;
import de.metas.materialtracking.model.I_M_InOutLine;
import de.metas.materialtracking.process.MaterialTrackingReportAgregationItem;

/*
 * #%L
 * de.metas.materialtracking
 * %%
 * Copyright (C) 2016 metas GmbH
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

public class MaterialTrackingReportBL implements IMaterialTrackingReportBL
{

	@Override
	public I_M_Material_Tracking_Report_Line createMaterialTrackingReportLine(final I_M_Material_Tracking_Report report, final I_M_InOutLine iol, final String lineAggregationKey)
	{

		final I_M_Material_Tracking_Report_Line newLine = InterfaceWrapperHelper.newInstance(I_M_Material_Tracking_Report_Line.class, iol);
		newLine.setM_Material_Tracking_Report(report);

		final org.compiere.model.I_M_Product product = iol.getM_Product();
		newLine.setM_Product(product);

		final I_M_AttributeSetInstance asi = createASIFromRef(iol);
		newLine.setM_AttributeSetInstance(asi);

		newLine.setLineAggregationKey(lineAggregationKey);
		return newLine;

	}

	private I_M_AttributeSetInstance createASIFromRef(final I_M_InOutLine iol)
	{
		final IDimensionspecDAO dimSpecDAO = Services.get(IDimensionspecDAO.class);
		final ISysConfigBL sysConfigBL = Services.get(ISysConfigBL.class);
		final IDimensionSpecAttributeBL dimensionSpecAttributeBL = Services.get(IDimensionSpecAttributeBL.class);

		final IContextAware ctxAware = InterfaceWrapperHelper.getContextAware(iol);

		final String internalName = sysConfigBL.getValue(MaterialTrackingConstants.SYSCONFIG_M_Material_Tracking_Report_Dimension);

		final I_DIM_Dimension_Spec dimensionSpec = dimSpecDAO.retrieveForInternalName(internalName, ctxAware);

		final I_M_AttributeSetInstance resultASI = dimensionSpecAttributeBL.createASIForDimensionSpec(iol.getM_AttributeSetInstance(), dimensionSpec);

		return resultASI;
	}

	@Override
	public void createMaterialTrackingReportLineAllocation(final I_M_Material_Tracking_Report_Line reportLine,
			final MaterialTrackingReportAgregationItem items)
	{
		final I_M_Material_Tracking_Report_Line_Alloc alloc = InterfaceWrapperHelper.newInstance(I_M_Material_Tracking_Report_Line_Alloc.class, reportLine);

		alloc.setM_Material_Tracking_Report_Line(reportLine);
		alloc.setPP_Order(items.getPPOrder());
		alloc.setM_InOutLine(items.getInOutLine());
		alloc.setM_Material_Tracking(items.getMaterialTracking());

		if (items.getPPOrder() != null)
		{
			alloc.setQtyIssued(items.getQty());
		}
		else
		{
			alloc.setQtyReceived(items.getQty());
		}
		InterfaceWrapperHelper.save(alloc);

	}

}
