package de.metas.request.model;

/*
 * #%L
 * de.metas.swat.base
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public interface I_R_Request extends org.compiere.model.I_R_Request
{
	// @formatter:off
	public static String COLUMNNAME_IsMaterialReturned = "IsMaterialReturned";
	
	public String getIsMaterialReturned();
	public void setIsMaterialReturned(String IsMaterialReturned);

	public static final String IsMaterialReturned_NotReturned = "N";
	public static final String IsMaterialReturned_Returned = "Y";
	public static final String IsMaterialReturned_Partially = "P";
	// @formatter:on
	
	// @formatter:off
	public static final String COLUMNNAME_DateDelivered = "DateDelivered";

	public void setDateDelivered(java.sql.Timestamp DateDelivered);
	public java.sql.Timestamp getDateDelivered();
	// @formatter:on
	
	// @formatter:off
	public static final String COLUMNNAME_QualityNote = "QualityNote";
		
	public void setQualityNote(String QualityNote);
	public String getQualityNote();
	// @formatter:on

}
