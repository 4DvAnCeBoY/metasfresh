package org.adempiere.service;

import org.adempiere.util.Check;
import org.compiere.util.Env;

import lombok.Value;

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

@Value
public class OrgId
{
	public static OrgId ofRepoId(final int repoId)
	{
		if (repoId == ANY.repoId)
		{
			return ANY;
		}
		return new OrgId(repoId);
	}

	public static OrgId ofRepoIdOrNull(final int repoId)
	{
		if (repoId == ANY.repoId)
		{
			return ANY;
		}
		else if (repoId < 0)
		{
			return null;
		}
		else
		{
			return ofRepoId(repoId);
		}
	}

	public static int toRepoId(final OrgId orgId)
	{
		return orgId != null ? orgId.getRepoId() : -1;
	}

	public static final OrgId ANY = new OrgId();

	int repoId;

	private OrgId(final int repoId)
	{
		this.repoId = Check.assumeGreaterThanZero(repoId, "repoId");
	}

	private OrgId()
	{
		this.repoId = Env.CTXVALUE_AD_Org_ID_Any;
	}

	public boolean isAny()
	{
		return repoId == Env.CTXVALUE_AD_Org_ID_Any;
	}
}
