package de.metas.handlingunits.process.api;

/*
 * #%L
 * de.metas.handlingunits.base
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

import org.adempiere.util.ISingletonService;

import de.metas.handlingunits.model.I_M_HU_Process;

public interface IMHUProcessDAO extends ISingletonService
{
	/**
	 * Retrieve the M_HU_Process entry that contains the AD_Process.
	 * 
	 * NOTE: there is only one active M_HU_Process entry for one AD_Process_ID
	 *
	 * @param adProcessId
	 * @return {@link I_M_HU_Process} or <code>null</code>
	 */
	I_M_HU_Process retrieveHUProcess(int adProcessId);

}
