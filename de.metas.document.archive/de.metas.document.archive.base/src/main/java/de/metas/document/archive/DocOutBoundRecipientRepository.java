package de.metas.document.archive;

import static org.adempiere.model.InterfaceWrapperHelper.create;
import static org.adempiere.model.InterfaceWrapperHelper.loadOutOfTrx;

import org.adempiere.util.Check;
import org.adempiere.util.StringUtils;
import org.springframework.stereotype.Repository;

import de.metas.document.archive.model.I_AD_User;
import de.metas.document.archive.model.I_C_BPartner;
import lombok.NonNull;

/*
 * #%L
 * de.metas.document.archive.base
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

@Repository
public class DocOutBoundRecipientRepository
{
	public DocOutBoundRecipient getById(@NonNull final DocOutBoundRecipientId id)
	{
		final I_AD_User userRecord = loadOutOfTrx(id.getRepoId(), I_AD_User.class);
		return ofRecord(userRecord);
	}

	private DocOutBoundRecipient ofRecord(@NonNull final I_AD_User userRecord)
	{
		return DocOutBoundRecipient.builder()
				.id(DocOutBoundRecipientId.ofRepoId(userRecord.getAD_User_ID()))
				.emailAddress(userRecord.getEMail())
				.invoiceAsEmail(computeInvoiceAsEmail(userRecord))
				.build();
	}

	private boolean computeInvoiceAsEmail(@NonNull final I_AD_User userRecord)
	{
		if(userRecord.getC_BPartner_ID() > 0)
		{
			final I_C_BPartner bPartnerRecord = create(userRecord.getC_BPartner(), I_C_BPartner.class);
			final String isInvoiceEmailEnabled = bPartnerRecord.getIsInvoiceEmailEnabled();
			if(!Check.isEmpty(isInvoiceEmailEnabled, true))
			{
				return StringUtils.toBoolean(isInvoiceEmailEnabled); // we have our result
			}
		}
		return StringUtils.toBoolean(userRecord.getIsInvoiceEmailEnabled());
	}
}
