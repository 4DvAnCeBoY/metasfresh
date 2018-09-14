package de.metas.print;

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


import org.adempiere.util.ISingletonService;

/**
 * This service allows it modules to register jasper services for different use cases
 *
 *
 */
public interface IPrintServiceRegistry extends ISingletonService
{
	/**
	 * Throws an exception is no service is registered.
	 */
	IPrintService getJasperService();

	/**
	 * @return the jasper service that was previously installed, or <code>null</code>.
	 */
	IPrintService registerJasperService(IPrintService jasperService);

	boolean isServiceRegistered();
}
