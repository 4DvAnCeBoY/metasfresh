package de.metas.inbound.mail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.GuavaCollectors;
import org.compiere.util.Util;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.MultiValueMap;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import de.metas.inbound.mail.config.InboundEMailConfig;
import lombok.Builder;
import lombok.NonNull;

/*
 * #%L
 * de.metas.inbound.mail
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

class InboundEMailMessageHandler implements MessageHandler
{
	private final InboundEMailConfig config;
	private final InboundEMailService emailService;

	@Builder
	public InboundEMailMessageHandler(
			@NonNull final InboundEMailConfig config,
			@NonNull final InboundEMailService emailService)
	{
		this.config = config;
		this.emailService = emailService;
	}

	@Override
	public void handleMessage(final Message<?> message) throws MessagingException
	{
		final InboundEMail email = toInboundEMail(message);
		emailService.onInboundEMailReceived(config, email);
	}

	private static InboundEMail toInboundEMail(final Message<?> message)
	{
		final Object contentObj = message.getPayload();
		final String content = getContent(contentObj);
		final MessageHeaders messageHeaders = message.getHeaders();
		@SuppressWarnings("unchecked")
		final MultiValueMap<String, Object> messageRawHeaders = messageHeaders.get(MailHeaders.RAW_HEADERS, MultiValueMap.class);

		final String messageId = toMessageId(messageRawHeaders.getFirst("Message-ID"));
		final String firstMessageIdReference = toMessageId(messageRawHeaders.getFirst("References"));
		final String initialMessageId = Util.coalesce(firstMessageIdReference, messageId);

		return InboundEMail.builder()
				.from(messageHeaders.get(MailHeaders.FROM, String.class))
				.to(ImmutableList.copyOf(messageHeaders.get(MailHeaders.TO, String[].class)))
				.cc(ImmutableList.copyOf(messageHeaders.get(MailHeaders.CC, String[].class)))
				.bcc(ImmutableList.copyOf(messageHeaders.get(MailHeaders.BCC, String[].class)))
				.subject(messageHeaders.get(MailHeaders.SUBJECT, String.class))
				.content(content)
				.contentType(messageHeaders.get(MailHeaders.CONTENT_TYPE, String.class))
				.receivedDate(toZonedDateTime(messageHeaders.get(MailHeaders.RECEIVED_DATE)))
				.messageId(messageId)
				.initialMessageId(initialMessageId)
				.headers(convertMailHeadersToJson(messageRawHeaders))
				.build();
	}

	private static final String toMessageId(final Object messageIdObj)
	{
		if (messageIdObj == null)
		{
			return null;
		}

		return messageIdObj.toString();
	}

	private static ZonedDateTime toZonedDateTime(final Object dateObj)
	{
		if (dateObj == null)
		{
			return null;
		}
		else if (dateObj instanceof java.util.Date)
		{
			final Instant instant = ((java.util.Date)dateObj).toInstant();
			return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		}
		else
		{
			throw new IllegalArgumentException("Cannot convert " + dateObj + " (" + dateObj.getClass() + ") to " + ZonedDateTime.class);
		}
	}

	private static String getContent(final Object contentObj)
	{
		if (contentObj == null)
		{
			return null;
		}
		else if (contentObj instanceof String)
		{
			final String content = (String)contentObj;
			System.out.println("part=[" + (content.length() > 40 ? content.substring(0, 40) + "..." : content) + "]");
			return content;
		}
		else if (contentObj instanceof Multipart)
		{
			final Multipart content = (Multipart)contentObj;
			return getContentFromMultipart(content);
		}
		else if (contentObj instanceof Part)
		{
			final Part content = (Part)contentObj;
			return getContentFromPart(content);
		}
		else
		{
			return contentObj.toString();
		}
	}

	private static String getContentFromMultipart(Multipart content)
	{
		try
		{
			for (int partIdx = 0, partsCount = content.getCount(); partIdx < partsCount; partIdx++)
			{
				final BodyPart part = content.getBodyPart(partIdx);
				final Object partContentObj = part.getContent();
				getContent(partContentObj);
			}

			return ""; // TODO
		}
		catch (final Exception ex)
		{
			throw AdempiereException.wrapIfNeeded(ex);
		}
	}

	private static String getContentFromPart(final Part part)
	{
		try
		{
			String contentType = part.getContentType();
			System.out.println("part=" + part + ":"
					+ " contentType=" + contentType
					+ ", disposition=" + part.getDisposition()
					+ ", filename=" + part.getFileName());

			return ""; // TOOD
		}
		catch (javax.mail.MessagingException ex)
		{
			throw AdempiereException.wrapIfNeeded(ex);
		}
	}

	private static final ImmutableMap<String, Object> convertMailHeadersToJson(final MultiValueMap<String, Object> mailRawHeaders)
	{
		return mailRawHeaders.entrySet()
				.stream()
				.map(entry -> GuavaCollectors.entry(entry.getKey(), convertListToJson(entry.getValue())))
				.filter(entry -> entry.getValue() != null)
				.collect(GuavaCollectors.toImmutableMap());
	}

	private static final Object convertListToJson(final List<Object> values)
	{
		if (values == null || values.isEmpty())
		{
			return ImmutableList.of();
		}
		else if (values.size() == 1)
		{
			return convertValueToJson(values.get(0));
		}
		else
		{
			return values.stream()
					.map(v -> convertValueToJson(v))
					.filter(Predicates.notNull())
					.collect(ImmutableList.toImmutableList());
		}
	}

	private static final Object convertValueToJson(final Object value)
	{
		if (value == null)
		{
			return null;
		}

		return value.toString();
	}
}
