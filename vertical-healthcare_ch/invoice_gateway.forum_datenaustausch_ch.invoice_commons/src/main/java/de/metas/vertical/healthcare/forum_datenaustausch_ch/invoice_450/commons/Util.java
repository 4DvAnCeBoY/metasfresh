package de.metas.vertical.healthcare.forum_datenaustausch_ch.invoice_450.commons;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.NonNull;

/*
 * #%L
 * metasfresh-healthcare.invoice_gateway.forum_datenaustausch_ch.invoice_commons
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

public class Util
{
	public static double toDouble(@NonNull final BigDecimal amount)
	{
		return amount
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
