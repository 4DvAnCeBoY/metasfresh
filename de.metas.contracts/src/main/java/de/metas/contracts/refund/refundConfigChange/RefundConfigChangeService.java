package de.metas.contracts.refund.refundConfigChange;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.IQuery;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import de.metas.contracts.model.I_C_Invoice_Candidate_Assignment;
import de.metas.contracts.refund.AssignmentToRefundCandidate;
import de.metas.contracts.refund.AssignmentToRefundCandidateRepository;
import de.metas.contracts.refund.RefundConfig;
import de.metas.contracts.refund.RefundConfig.RefundBase;
import de.metas.contracts.refund.RefundConfig.RefundMode;
import de.metas.contracts.refund.RefundContract;
import de.metas.contracts.refund.RefundInvoiceCandidate;
import de.metas.invoicecandidate.model.I_C_Invoice_Candidate;
import de.metas.money.MoneyService;
import lombok.NonNull;

/*
 * #%L
 * de.metas.contracts
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

public class RefundConfigChangeService
{
	private final AssignmentToRefundCandidateRepository assignmentToRefundCandidateRepository;
	private final MoneyService moneyService;

	public RefundConfigChangeService(
			@NonNull final AssignmentToRefundCandidateRepository assignmentToRefundCandidateRepository,
			@NonNull final MoneyService moneyService)
	{
		this.assignmentToRefundCandidateRepository = assignmentToRefundCandidateRepository;
		this.moneyService = moneyService;
	}

	/**
	 * Resets/fixes the allocated amount by iterating all candidates that are associated to the given candidate.
	 * Should be used only if e.g. something was manually changed in the DB.
	 *
	 * @param refundConfigAfterAssignment
	 */
	public RefundInvoiceCandidate resetMoneyAmount(
			@NonNull final RefundInvoiceCandidate refundInvoiceCandidate,
			@NonNull final RefundConfig newRefundConfig)
	{
		final RefundConfig oldRefundConfig = refundInvoiceCandidate.getRefundConfig();
		if (oldRefundConfig.equals(newRefundConfig))
		{
			return refundInvoiceCandidate;

		}
		final RefundContract refundContract = refundInvoiceCandidate.getRefundContract();
		final List<RefundConfig> refundConfigs = refundContract.getRefundConfigs();

		Check.errorUnless(RefundMode.ALL_MAX_SCALE.equals(newRefundConfig.getRefundMode()),
				"Parameter 'newRefundConfig' needs to have refundMode={}", RefundMode.ALL_MAX_SCALE);

		Check.errorUnless(refundConfigs.contains(newRefundConfig),
				"Parameter 'newRefundConfig' needs to be one of the given refundInvoiceCandidate's contract's configs; newRefundConfig={}; refundInvoiceCandidate={}",
				newRefundConfig, refundInvoiceCandidate);

		// TODO: resetting the refundCcaandidate to zero and iterating allAssignedCandidates is crap;
		// instead, add new AssignmentToRefundCandidates
		// which reference newRefundConfig and track the additional money that comes with the new refund config.

		final boolean isHigherRefund = newRefundConfig.getMinQty().compareTo(oldRefundConfig.getMinQty()) > 0;

		final List<RefundConfig> refundConfigRange = getRefundConfigRange(refundContract, oldRefundConfig, newRefundConfig);
		Check.assumeNotEmpty(refundConfigRange,
				"There needs to be at least one refundConfig in the range defined by oldRefundConfig={} and newRefundConfig={}; refundInvoiceCandidate={}",
				oldRefundConfig, newRefundConfig, refundInvoiceCandidate);

		final RefundConfigChangeHandler handler = createForConfig(oldRefundConfig);
		if (isHigherRefund)
		{

			for (final RefundConfig currentRangeConfig : refundConfigRange)
			{
				handler.currentRefundConfig(currentRangeConfig);

				final RefundConfig formerRefundConfig = handler.getFormerRefundConfig();
				final Stream<AssignmentToRefundCandidate> assignmentsToExtend = streamAssignmentsToExtend(
						refundInvoiceCandidate,
						formerRefundConfig,
						currentRangeConfig // onlyIfNotExistsConfig
				);

				assignmentsToExtend
						.map(handler::createNewAssignment)
						.forEach(assignmentToRefundCandidateRepository::save);
			}
		}

		return null;

	}

	@VisibleForTesting
	List<RefundConfig> getRefundConfigRange(
			@NonNull final RefundContract contract,
			@NonNull final RefundConfig currentConfig,
			@NonNull final RefundConfig targetConfig)
	{
		Check.errorIf(currentConfig.getMinQty().equals(targetConfig.getMinQty()),
				"Params currentConfig and currentConfig={}; targetConfig={}",
				currentConfig, targetConfig);

		final boolean forward = currentConfig.getMinQty().compareTo(targetConfig.getMinQty()) < 0;

		// make sure we know which order of refund configs we operate on
		final ImmutableList<RefundConfig> configsByMinQty = contract
				.getRefundConfigs()
				.stream()
				.sorted(Comparator.comparing(RefundConfig::getMinQty))
				.collect(ImmutableList.toImmutableList());

		final ImmutableList.Builder<RefundConfig> result = ImmutableList.builder();
		if (forward)
		{
			boolean collectItem = false;
			for (int i = 0; i < configsByMinQty.size(); i++)
			{
				final RefundConfig item = configsByMinQty.get(i);
				if (collectItem)
				{
					result.add(item);
				}
				if (item.getMinQty().equals(targetConfig.getMinQty()))
				{
					return result.build(); // we collected the last item (targetConfig) and are done
				}
				if (item.getMinQty().equals(currentConfig.getMinQty()))
				{
					collectItem = true; // don't collect currentConfig itself, but the next item(s)
				}
			}
		}

		// backward
		boolean collectItem = false;
		for (int i = configsByMinQty.size() - 1; i >= 0; i--)
		{
			final RefundConfig item = configsByMinQty.get(i);
			if (item.getMinQty().equals(currentConfig.getMinQty()))
			{
				collectItem = true; // do collect currentConfig
			}
			if (item.getMinQty().equals(targetConfig.getMinQty()))
			{
				return result.build(); // we collected the last item (one "above" targetConfig) and are done
			}
			if (collectItem)
			{
				result.add(item);
			}
		}
		return result.build();
	}

	private RefundConfigChangeHandler createForConfig(@NonNull final RefundConfig refundConfig)
	{
		final boolean isPercent = RefundBase.PERCENTAGE.equals(refundConfig.getRefundBase());
		if(isPercent)
		{
			return PercentRefundConfigChangeHandler.newInstance(moneyService, refundConfig);
		}

	}

	private Stream<AssignmentToRefundCandidate> streamAssignmentsToExtend(
			@NonNull final RefundInvoiceCandidate refundInvoiceCandidate,
			@NonNull final RefundConfig refundConfig,
			@NonNull final RefundConfig onlyIfNotExistsConfig)
	{
		Check.assumeNotNull(refundInvoiceCandidate.getId(),
				"The given refundInvoiceCandidate needs to have a not-null Id; assignableInvoiceCandidate={}",
				refundInvoiceCandidate);

		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final IQuery<I_C_Invoice_Candidate> assignableCandidatesWithOnlyIfNotExistsConfig = queryBL
				.createQueryBuilder(I_C_Invoice_Candidate_Assignment.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(
						I_C_Invoice_Candidate_Assignment.COLUMN_C_Invoice_Candidate_Term_ID,
						refundInvoiceCandidate.getId().getRepoId())
				.addEqualsFilter(
						I_C_Invoice_Candidate_Assignment.COLUMN_C_Flatrate_RefundConfig_ID,
						onlyIfNotExistsConfig.getId().getRepoId())
				.andCollect(I_C_Invoice_Candidate_Assignment.COLUMN_C_Invoice_Candidate_Assigned_ID,
						I_C_Invoice_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.create();

		return queryBL
				.createQueryBuilder(I_C_Invoice_Candidate_Assignment.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(
						I_C_Invoice_Candidate_Assignment.COLUMN_C_Invoice_Candidate_Term_ID,
						refundInvoiceCandidate.getId().getRepoId())
				.addEqualsFilter(
						I_C_Invoice_Candidate_Assignment.COLUMN_C_Flatrate_RefundConfig_ID,
						refundConfig.getId().getRepoId())
				.addNotInSubQueryFilter(
						I_C_Invoice_Candidate_Assignment.COLUMNNAME_C_Invoice_Candidate_Assigned_ID,
						I_C_Invoice_Candidate.COLUMNNAME_C_Invoice_Candidate_ID,
						assignableCandidatesWithOnlyIfNotExistsConfig)
				.create()
				.iterateAndStream()
				.map(assignmentToRefundCandidateRepository::ofRecordOrNull);

	}
}
