package de.metas.bpartner.config;

import org.adempiere.ad.modelvalidator.annotations.Init;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BPartner;
import org.springframework.stereotype.Component;

import de.metas.adempiere.model.I_AD_User;
import de.metas.elasticsearch.IESSystem;
import de.metas.elasticsearch.config.ESIncludedModelsConfig;
import de.metas.elasticsearch.config.ESModelIndexerProfile;

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

@Interceptor(I_C_BPartner.class)
@Component
public class C_BPartner_FullTextSearch_Config
{
	@Init
	public void enableFullTextSearch()
	{
		final IESSystem esSystem = Services.get(IESSystem.class);
		if (esSystem.isEnabled())
		{
			esSystem.newModelIndexerConfig(ESModelIndexerProfile.FULL_TEXT_SEARCH, "fts_bpartners", I_C_BPartner.class)
					.includeModel(ESIncludedModelsConfig.builder()
							.attributeName("contacts")
							.childLinkColumnName(I_AD_User.COLUMNNAME_C_BPartner_ID)
							.childTableName(I_AD_User.Table_Name)
							.build())
					.triggerOnChangeOrDelete()
					.buildAndInstall();
		}
	}
}
