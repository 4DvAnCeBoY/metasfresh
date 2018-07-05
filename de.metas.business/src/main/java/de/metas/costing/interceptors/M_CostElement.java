package de.metas.costing.interceptors;

import java.util.stream.Collectors;

import org.adempiere.acct.api.IAcctSchemaDAO;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_AcctSchema;
import org.compiere.model.I_M_CostElement;
import org.compiere.model.I_M_Product_Category;
import org.compiere.model.I_M_Product_Category_Acct;
import org.compiere.model.ModelValidator;
import org.compiere.util.Env;
import org.springframework.stereotype.Component;

import de.metas.costing.CostElementType;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2018 metas GmbH
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

@Component
@Interceptor(I_M_CostElement.class)
public class M_CostElement
{
	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW, ModelValidator.TYPE_BEFORE_CHANGE }, ifColumnsChanged = I_M_CostElement.COLUMNNAME_CostingMethod)
	public void assertUniqueCostingMethod(final I_M_CostElement costElement)
	{
		final CostElementType costElementType = CostElementType.ofCode(costElement.getCostElementType());
		if (CostElementType.Material.equals(costElementType)
				// || COSTELEMENTTYPE_Resource.equals(costElementType)
				// || COSTELEMENTTYPE_BurdenMOverhead.equals(costElementType)
				// || COSTELEMENTTYPE_Overhead.equals(costElementType)
				|| CostElementType.OutsideProcessing.equals(costElementType))
		{
			final boolean costingMethodAlreadyExists = Services.get(IQueryBL.class)
					.createQueryBuilder(I_M_CostElement.class)
					.addNotEqualsFilter(I_M_CostElement.COLUMN_M_CostElement_ID, costElement.getM_CostElement_ID())
					.addEqualsFilter(I_M_CostElement.COLUMN_AD_Client_ID, costElement.getAD_Client_ID())
					.addEqualsFilter(I_M_CostElement.COLUMN_CostingMethod, costElement.getCostingMethod())
					.addEqualsFilter(I_M_CostElement.COLUMN_CostElementType, costElementType.getCode())
					.create()
					.match();
			if (costingMethodAlreadyExists)
			{
				throw new AdempiereException("@AlreadyExists@ @CostingMethod@");
			}
		}
	}

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW, ModelValidator.TYPE_BEFORE_CHANGE })
	public void beforeSave(final I_M_CostElement costElement)
	{
		// Maintain Calculated
		/*
		 * if (COSTELEMENTTYPE_Material.equals(getCostElementType()))
		 * {
		 * String cm = getCostingMethod();
		 * if (cm == null || cm.length() == 0
		 * || COSTINGMETHOD_StandardCosting.equals(cm))
		 * setIsCalculated(false);
		 * else
		 * setIsCalculated(true);
		 * }
		 * else
		 * {
		 * if (isCalculated())
		 * setIsCalculated(false);
		 * if (getCostingMethod() != null)
		 * setCostingMethod(null);
		 * }
		 */

		if (costElement.getAD_Org_ID() != 0)
		{
			costElement.setAD_Org_ID(0);
		}
	}

	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_DELETE })
	public void beforeDelete(final I_M_CostElement costElement)
	{
		final CostElementType costElementType = CostElementType.ofCode(costElement.getCostElementType());
		final boolean isCostingMethod = CostElementType.Material.equals(costElementType)
				&& costElement.getCostingMethod() != null;
		if (!isCostingMethod)
		{
			return;
		}

		// Costing Methods on AS level
		for (final I_C_AcctSchema as : Services.get(IAcctSchemaDAO.class).retrieveClientAcctSchemas(Env.getCtx(), costElement.getAD_Client_ID()))
		{
			if (as.getCostingMethod().equals(costElement.getCostingMethod()))
			{
				throw new AdempiereException("@CannotDeleteUsed@ @C_AcctSchema_ID@");
			}
		}

		// Costing Methods on PC level
		final String productCategoriesUsingCostingMethod = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Product_Category_Acct.class)
				.addEqualsFilter(I_M_Product_Category_Acct.COLUMN_AD_Client_ID, costElement.getAD_Client_ID())
				.addEqualsFilter(I_M_Product_Category_Acct.COLUMN_CostingMethod, costElement.getCostingMethod())
				.andCollect(I_M_Product_Category_Acct.COLUMN_M_Product_Category_ID)
				.orderBy(I_M_Product_Category.COLUMN_Name)
				.create()
				.setLimit(50)
				.listDistinct(I_M_Product_Category.COLUMNNAME_Name, String.class)
				.stream()
				.collect(Collectors.joining(", "));
		if (!Check.isEmpty(productCategoriesUsingCostingMethod, true))
		{
			throw new AdempiereException("@CannotDeleteUsed@ @M_Product_Category_ID@ (" + productCategoriesUsingCostingMethod + ")");
		}
	}	// beforeDelete

}
