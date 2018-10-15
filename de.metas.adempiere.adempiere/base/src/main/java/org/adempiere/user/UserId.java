package org.adempiere.user;

import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import de.metas.util.Check;
import de.metas.util.lang.RepoIdAware;

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

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@Value
public class UserId implements RepoIdAware
{
	@JsonCreator
	public static UserId ofRepoId(final int repoId)
	{
		return new UserId(repoId);
	}

	public static UserId ofRepoIdOrNull(final int repoId)
	{
		return repoId > 0 ? new UserId(repoId) : null;
	}

	public static int toRepoIdOr(
			@Nullable final UserId userId,
			final int defaultValue)
	{
		return userId != null ? userId.getRepoId() : defaultValue;
	}

	public static boolean equals(final UserId userId1, final UserId userId2)
	{
		return Objects.equals(userId1, userId2);
	}

	int repoId;

	private UserId(final int repoId)
	{
		this.repoId = Check.assumeGreaterOrEqualToZero(repoId, "repoId");
	}

	@Override
	@JsonValue
	public int getRepoId()
	{
		return repoId;
	}
}
