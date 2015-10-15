package org.adempiere.ad.dao.impl;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.List;
import java.util.Properties;

import org.adempiere.ad.dao.ICompositeQueryFilter;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryBuilderDAO;
import org.adempiere.ad.dao.IQueryFilter;
import org.compiere.model.IQuery;

public class PlainQueryBuilderDAO implements IQueryBuilderDAO
{

	@Override
	public <T> IQuery<T> create(final IQueryBuilder<T> builder)
	{
		final Class<T> modelClass = builder.getModelClass();

		final QueryBuilder<T> builderImpl = (QueryBuilder<T>)builder;

		final Properties ctx = builderImpl.getCtx();
		final String trxName = builderImpl.getTrxName();

		final IQueryFilter<T> filter = builderImpl.createFilter();

		final POJOQuery<T> query = new POJOQuery<T>(ctx, modelClass, trxName)
				.addFilter(filter)
				.setOrderBy(builderImpl.createQueryOrderBy())
				.setOnlySelection(builderImpl.getSelectionId())
				//
				;
		
		return query;
	}

	@Override
	public <T> String getSql(Properties ctx, ICompositeQueryFilter<T> filter, List<Object> sqlParamsOut)
	{
		throw new UnsupportedOperationException();
	}

}
