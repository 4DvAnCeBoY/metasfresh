package de.metas.workflow.api.impl;

/*
 * #%L
 * de.metas.swat.base
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


import java.util.Properties;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.IOrgDAO;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_AD_WF_Responsible;
import org.compiere.model.MTable;
import org.compiere.model.X_AD_WF_Responsible;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import de.metas.workflow.api.IWorkflowBL;
import de.metas.workflow.api.IWorkflowDAO;
import de.metas.workflow.model.I_AD_OrgInfo;
import de.metas.workflow.model.I_C_Doc_Responsible;

public class WorkflowBL implements IWorkflowBL
{
	private static final int AD_WF_RESPONSIBLE_ID_Invoker = 101;

	@Override
	public I_AD_WF_Responsible getOrgWFResponsible(Properties ctx, int adOrgId)
	{
		final I_AD_OrgInfo orgInfo = InterfaceWrapperHelper.create(
				Services.get(IOrgDAO.class).retrieveOrgInfo(ctx, adOrgId, Trx.TRXNAME_None),
				I_AD_OrgInfo.class);
		I_AD_WF_Responsible wfResponsible = orgInfo.getAD_WF_Responsible();
		if (wfResponsible != null)
		{
			return wfResponsible;
		}

		//
		// Default: Invoker
		wfResponsible = InterfaceWrapperHelper.create(ctx, AD_WF_RESPONSIBLE_ID_Invoker, I_AD_WF_Responsible.class, Trx.TRXNAME_None);
		return wfResponsible;
	}

	@Override
	public I_C_Doc_Responsible createDocResponsible(final Object doc, int adOrgId)
	{
		Util.assumeNotNull(doc, "doc is not null");

		final Properties ctx = InterfaceWrapperHelper.getCtx(doc);
		final String trxName = InterfaceWrapperHelper.getTrxName(doc);

		final String tableName = InterfaceWrapperHelper.getModelTableName(doc);
		final int tableId = MTable.getTable_ID(tableName);
		final int recordId = InterfaceWrapperHelper.getId(doc);

		final I_AD_WF_Responsible wfResponsible = getOrgWFResponsible(ctx, adOrgId);

		I_C_Doc_Responsible docResponsible = Services.get(IWorkflowDAO.class).retrieveDocResponsible(doc);
		if (docResponsible == null)
		{
			docResponsible = InterfaceWrapperHelper.create(ctx, I_C_Doc_Responsible.class, trxName);
		}
		docResponsible.setAD_Org_ID(adOrgId);
		docResponsible.setAD_Table_ID(tableId);
		docResponsible.setRecord_ID(recordId);
		docResponsible.setAD_WF_Responsible(wfResponsible);

		if (isInvoker(wfResponsible))
		{
			final int userResponsibleId = Env.getAD_User_ID(ctx); // logged user
			docResponsible.setAD_User_Responsible_ID(userResponsibleId);
		}
		else if (isHuman(wfResponsible))
		{
			final int userResponsibleId = wfResponsible.getAD_User_ID();
			docResponsible.setAD_User_Responsible_ID(userResponsibleId);
		}
		else
		{
			docResponsible.setAD_User_Responsible_ID(-1); // no user responsible
		}

		setAD_WF_Responsible_Name(docResponsible);

		InterfaceWrapperHelper.save(docResponsible);

		return docResponsible;
	}

	private void setAD_WF_Responsible_Name(I_C_Doc_Responsible docResponsible)
	{
		final I_AD_WF_Responsible wfResponsible = docResponsible.getAD_WF_Responsible();
		if (isInvoker(wfResponsible) || isHuman(wfResponsible))
		{
			final I_AD_User userResponsible = docResponsible.getAD_User_Responsible();
			Util.assumeNotNull(userResponsible, "No AD_User_Responsible found for {0}", docResponsible);

			docResponsible.setAD_WF_Responsible_Name(userResponsible.getName());
		}
		else
		{
			docResponsible.setAD_WF_Responsible_Name(wfResponsible.getName());
		}
	}

	private boolean isInvoker(final I_AD_WF_Responsible wfResponsible)
	{
		return X_AD_WF_Responsible.RESPONSIBLETYPE_Human.equals(wfResponsible.getResponsibleType())
				&& wfResponsible.getAD_User_ID() <= 0
				&& wfResponsible.getAD_Role_ID() <= 0;
	}

	private boolean isHuman(final I_AD_WF_Responsible wfResponsible)
	{
		return X_AD_WF_Responsible.RESPONSIBLETYPE_Human.equals(wfResponsible.getResponsibleType())
				&& wfResponsible.getAD_User_ID() > 0;
	}
}
