package de.metas.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.lang.ITableRecordReference;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableList;

import de.metas.event.EventBusConstants;
import de.metas.event.Topic;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation,
 * either version 2 of the
 * License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not,
 * see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
public class UserNotificationRequest
{
	Recipient recipient;
	UserNotificationsConfig notificationsConfig;

	private static final NotificationGroupName DEFAULT_NotificationGroupName = NotificationGroupName.of(EventBusConstants.TOPIC_GeneralUserNotifications.getName());
	NotificationGroupName notificationGroupName;

	boolean important;

	/** Optional; takes precedence over {@link #subjectADMessage}, if set. */
	String subjectPlain;

	/** Optional */
	String subjectADMessage;
	List<Object> subjectADMessageParams;

	/** Optional; takes precedence over {@link #contentADMessage}, if set. */
	String contentPlain;

	/** Optional */
	String contentADMessage;
	List<Object> contentADMessageParams;

	String targetRecordDisplayText;
	ITableRecordReference targetRecord;
	int targetADWindowId;

	List<Resource> attachments;

	// Options
	boolean noEmail;

	@Builder(toBuilder = true)
	private UserNotificationRequest(
			final Recipient recipient,
			final UserNotificationsConfig notificationsConfig,
			//
			final NotificationGroupName notificationGroupName,
			//
			final boolean important,
			//
			@Nullable final String subjectPlain,
			@Nullable final String subjectADMessage,
			@Singular final List<Object> subjectADMessageParams,
			//
			final String contentPlain,
			final String contentADMessage,
			@Singular final List<Object> contentADMessageParams,
			//
			@Nullable String targetRecordDisplayText,
			@Nullable final ITableRecordReference targetRecord,
			final int targetADWindowId,
			//
			@Singular final List<Resource> attachments,
			// Options:
			final boolean noEmail)
	{
		this.notificationsConfig = notificationsConfig;

		if (recipient == null)
		{
			if (notificationsConfig != null)
			{
				this.recipient = Recipient.user(notificationsConfig.getUserId());
			}
			else
			{
				throw new AdempiereException("notificationConfig or recipient shall be specified");
			}
		}
		else if (recipient.isAllUsers())
		{
			this.recipient = recipient;
		}
		else
		{
			this.recipient = recipient;
			if (notificationsConfig != null)
			{
				final Recipient expectedRecipient = Recipient.user(notificationsConfig.getUserId());
				Check.assume(expectedRecipient.equals(recipient), "recipient shall be {} but it was {}", expectedRecipient, recipient);
			}
		}

		this.notificationGroupName = notificationGroupName != null ? notificationGroupName : DEFAULT_NotificationGroupName;

		this.important = important;

		this.subjectPlain = subjectPlain;
		this.subjectADMessage = subjectADMessage;
		this.subjectADMessageParams = copyADMessageParams(subjectADMessageParams);

		this.contentPlain = contentPlain;
		this.contentADMessage = contentADMessage;
		this.contentADMessageParams = copyADMessageParams(contentADMessageParams);

		this.targetRecordDisplayText = targetRecordDisplayText;
		this.targetRecord = targetRecord;
		this.targetADWindowId = targetADWindowId;

		this.attachments = ImmutableList.copyOf(attachments);

		this.noEmail = noEmail;
	}

	private static List<Object> copyADMessageParams(final List<Object> params)
	{
		return params != null && !params.isEmpty() ? Collections.unmodifiableList(new ArrayList<>(params)) : ImmutableList.of();
	}

	public String getADLanguageOrGet(final Supplier<String> defaultLanguageSupplier)
	{
		return getNotificationsConfig().getUserADLanguageOrGet(defaultLanguageSupplier);
	}

	public String getSubjectADMessageOr(final String defaultValue)
	{
		return !Check.isEmpty(subjectADMessage) ? subjectADMessage : defaultValue;
	}

	public UserNotificationRequest deriveByNotificationsConfig(@NonNull final UserNotificationsConfig notificationsConfig)
	{
		if (Objects.equals(this.notificationsConfig, notificationsConfig))
		{
			return this;
		}

		return toBuilder().notificationsConfig(notificationsConfig).build();
	}

	public UserNotificationRequest deriveByRecipient(@NonNull final Recipient recipient)
	{
		if (Objects.equals(this.recipient, recipient))
		{
			return this;
		}

		return toBuilder().recipient(recipient).notificationsConfig(null).build();
	}

	//
	//
	//
	//
	//

	public static class UserNotificationRequestBuilder
	{
		public UserNotificationRequestBuilder recipientUserId(final int userId)
		{
			return recipient(Recipient.user(userId));
		}

		public UserNotificationRequestBuilder topic(final Topic topic)
		{
			return notificationGroupName(NotificationGroupName.of(topic.getName()));
		}
	}
}
