/**
 *
 */
package de.metas.document.sequence;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import org.adempiere.util.ISingletonService;

import de.metas.document.sequence.impl.IPreliminaryDocumentNoBuilder;

public interface IDocumentNoBuilderFactory extends ISingletonService
{
	IPreliminaryDocumentNoBuilder createPreliminaryDocumentNoBuilder();

	/**
	 * Convenience method to create and prepare the builder for a given tableName.
	 *
	 * @param tableName the method will look for a sequence whose name is this table name, prepended with {@link IDocumentNoBuilder#PREFIX_DOCSEQ}.
	 */
	IDocumentNoBuilder forTableName(String tableName, int AD_Client_ID, int AD_Org_ID);

	/**
	 * Convenient method to create and prepare the builder for a given DocType.
	 *
	 * @param useDefiniteSequence if {@code true}, then the doc type's {@code DefiniteSequence_ID} is used.
	 */
	IDocumentNoBuilder forDocType(int C_DocType_ID, boolean useDefiniteSequence);

	/**
	 * Create a builder that shall set the given modelRecord's {@code Value} column.
	 */
	IDocumentNoBuilder createValueBuilderFor(Object modelRecord);

	/**
	 * Creates a plain builder that does not contain any info yet.
	 * The user needs to invoke {@link IDocumentNoBuilder#setDocumentSequenceInfo(de.metas.document.DocumentSequenceInfo)}.
	 */
	IDocumentNoBuilder createDocumentNoBuilder();
}
