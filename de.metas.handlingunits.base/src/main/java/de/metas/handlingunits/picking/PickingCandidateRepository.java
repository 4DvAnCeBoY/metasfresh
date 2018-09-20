package de.metas.handlingunits.picking;

import static org.adempiere.model.InterfaceWrapperHelper.load;
import static org.adempiere.model.InterfaceWrapperHelper.loadByRepoIdAwares;
import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryUpdater;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.uom.api.IUOMDAO;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.IQuery;
import org.compiere.model.I_C_UOM;
import org.springframework.stereotype.Service;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.handlingunits.HuId;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_Picking_Candidate;
import de.metas.handlingunits.model.X_M_HU;
import de.metas.handlingunits.model.X_M_Picking_Candidate;
import de.metas.inoutcandidate.api.ShipmentScheduleId;
import de.metas.picking.api.IPickingSlotDAO;
import de.metas.picking.api.PickingSlotId;
import de.metas.picking.api.PickingSlotQuery;
import de.metas.quantity.Quantity;
import lombok.NonNull;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * Dedicated DAO'ish class centered around {@link I_M_Picking_Candidate}s
 * 
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@Service
public class PickingCandidateRepository
{
	// private static final Logger logger = LogManager.getLogger(PickingCandidateRepository.class);
	private final IUOMDAO uomsRepo = Services.get(IUOMDAO.class);

	public PickingCandidate getById(@NonNull final PickingCandidateId id)
	{
		return toPickingCandidate(getRecordById(id));
	}

	private I_M_Picking_Candidate getRecordById(@NonNull final PickingCandidateId id)
	{
		final I_M_Picking_Candidate record = load(id, I_M_Picking_Candidate.class);
		if (record == null)
		{
			throw new AdempiereException("@NotFound@ @M_Picking_Candidate_ID@: " + id);
		}
		return record;
	}

	public void saveAll(@NonNull final Collection<PickingCandidate> candidates)
	{
		candidates.forEach(this::save);
	}

	public void save(@NonNull final PickingCandidate candidate)
	{
		final I_M_Picking_Candidate record;
		if (candidate.getId() == null)
		{
			record = newInstance(I_M_Picking_Candidate.class);
		}
		else
		{
			record = getRecordById(candidate.getId());
		}

		updateRecord(record, candidate);
		saveRecord(record);

		candidate.setId(PickingCandidateId.ofRepoId(record.getM_Picking_Candidate_ID()));
	}

	private PickingCandidate toPickingCandidate(final I_M_Picking_Candidate record)
	{
		final I_C_UOM uom = uomsRepo.getById(record.getC_UOM_ID());
		final Quantity qtyPicked = Quantity.of(record.getQtyPicked(), uom);

		return PickingCandidate.builder()
				.id(PickingCandidateId.ofRepoId(record.getM_Picking_Candidate_ID()))
				.status(PickingCandidateStatus.ofCode(record.getStatus()))
				.qtyPicked(qtyPicked)
				.huId(HuId.ofRepoId(record.getM_HU_ID()))
				.shipmentScheduleId(ShipmentScheduleId.ofRepoId(record.getM_ShipmentSchedule_ID()))
				.pickingSlotId(PickingSlotId.ofRepoIdOrNull(record.getM_PickingSlot_ID()))
				.build();
	}

	private static void updateRecord(final I_M_Picking_Candidate record, final PickingCandidate from)
	{
		record.setStatus(from.getStatus().getCode());
		record.setQtyPicked(from.getQtyPicked().getAsBigDecimal());
		record.setC_UOM_ID(from.getQtyPicked().getUOMId());
		record.setM_HU_ID(from.getHuId().getRepoId());
		record.setM_ShipmentSchedule_ID(from.getShipmentScheduleId().getRepoId());
		record.setM_PickingSlot_ID(PickingSlotId.toRepoId(from.getPickingSlotId()));
	}

	public Set<ShipmentScheduleId> retrieveShipmentScheduleIdsForPickingCandidateIds(final Collection<PickingCandidateId> pickingCandidateIds)
	{
		if (pickingCandidateIds.isEmpty())
		{
			return ImmutableSet.of();
		}

		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addInArrayFilter(I_M_Picking_Candidate.COLUMN_M_Picking_Candidate_ID, pickingCandidateIds)
				.addNotNull(I_M_Picking_Candidate.COLUMNNAME_M_ShipmentSchedule_ID)
				.create()
				.listDistinct(I_M_Picking_Candidate.COLUMNNAME_M_ShipmentSchedule_ID, Integer.class)
				.stream()
				.map(ShipmentScheduleId::ofRepoId)
				.collect(ImmutableSet.toImmutableSet());
	}

	public List<PickingCandidate> retrievePickingCandidatesByHUIds(@NonNull final Collection<HuId> huIds)
	{
		// tolerate empty
		if (huIds.isEmpty())
		{
			return ImmutableList.of();
		}

		return retrievePickingCandidatesByHUIdsQuery(huIds)
				.create()
				.stream()
				.map(this::toPickingCandidate)
				.collect(ImmutableList.toImmutableList());
	}

	private IQueryBuilder<I_M_Picking_Candidate> retrievePickingCandidatesByHUIdsQuery(@NonNull final Collection<HuId> huIds)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addInArrayFilter(I_M_Picking_Candidate.COLUMN_M_HU_ID, huIds);
	}

	public Optional<PickingCandidate> getByShipmentScheduleIdAndHuIdAndPickingSlotId(
			@NonNull final HuId huId,
			@NonNull final ShipmentScheduleId shipmentScheduleId,
			@NonNull final PickingSlotId pickingSlotId)
	{
		final I_M_Picking_Candidate existingRecord = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, pickingSlotId)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_HU_ID, huId)
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_ShipmentSchedule_ID, shipmentScheduleId)
				.create()
				.firstOnly(I_M_Picking_Candidate.class);

		if (existingRecord == null)
		{
			return Optional.empty();
		}
		else
		{
			return Optional.of(toPickingCandidate(existingRecord));
		}
	}

	public void deletePickingCandidates(@NonNull final Collection<PickingCandidate> candidates)
	{
		final Set<PickingCandidateId> ids = candidates.stream()
				.map(PickingCandidate::getId)
				.filter(Predicates.notNull())
				.collect(ImmutableSet.toImmutableSet());
		if (ids.isEmpty())
		{
			return;
		}

		final List<I_M_Picking_Candidate> records = loadByRepoIdAwares(ids, I_M_Picking_Candidate.class);
		InterfaceWrapperHelper.deleteAll(records);
	}

	public List<PickingCandidate> retrievePickingCandidatesByShipmentScheduleIdsAndStatus(
			@NonNull final Set<ShipmentScheduleId> shipmentScheduleIds,
			@NonNull final PickingCandidateStatus status)
	{
		if (shipmentScheduleIds.isEmpty())
		{
			return ImmutableList.of();
		}

		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_Status, status.getCode())
				.addInArrayFilter(I_M_Picking_Candidate.COLUMN_M_ShipmentSchedule_ID, shipmentScheduleIds)
				.create()
				.stream(I_M_Picking_Candidate.class)
				.map(this::toPickingCandidate)
				.collect(ImmutableList.toImmutableList());

	}

	public boolean hasNotClosedCandidatesForPickingSlot(final PickingSlotId pickingSlotId)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, pickingSlotId)
				.addNotEqualsFilter(I_M_Picking_Candidate.COLUMN_Status, X_M_Picking_Candidate.STATUS_CL)
				.create()
				.match();
	}

	public void inactivateForHUIds(@NonNull final Collection<HuId> huIds)
	{
		Check.assumeNotEmpty(huIds, "huIds is not empty");

		retrievePickingCandidatesByHUIdsQuery(huIds)
				.create()
				.update(record -> {
					markAsInactiveNoSave(record);
					return IQueryUpdater.MODEL_UPDATED;

				});
	}

	private void markAsInactiveNoSave(I_M_Picking_Candidate record)
	{
		record.setIsActive(false);
		record.setStatus(PickingCandidateStatus.Closed.getCode());
	}

	public TableRecordReference toTableRecordReference(@NonNull final PickingCandidate candidate)
	{
		final PickingCandidateId id = candidate.getId();
		if (id == null)
		{
			throw new AdempiereException("Candidate is not saved: " + candidate);
		}
		return toTableRecordReference(id);
	}

	public TableRecordReference toTableRecordReference(@NonNull final PickingCandidateId id)
	{
		return TableRecordReference.of(I_M_Picking_Candidate.Table_Name, id.getRepoId());
	}

	public List<PickingCandidate> query(@NonNull final PickingCandidatesQuery pickingCandidatesQuery)
	{
		// configure the query builder
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final IQueryBuilder<I_M_Picking_Candidate> queryBuilder = queryBL
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addInArrayFilter(I_M_Picking_Candidate.COLUMN_M_ShipmentSchedule_ID, pickingCandidatesQuery.getShipmentScheduleIds());

		if (pickingCandidatesQuery.isOnlyNotClosedOrNotRackSystem())
		{
			final IHUPickingSlotDAO huPickingSlotsRepo = Services.get(IHUPickingSlotDAO.class);
			final Set<PickingSlotId> rackSystemPickingSlotIds = huPickingSlotsRepo.retrieveAllPickingSlotIdsWhichAreRackSystems();
			queryBuilder.addCompositeQueryFilter()
					.setJoinOr()
					.addNotEqualsFilter(I_M_Picking_Candidate.COLUMN_Status, X_M_Picking_Candidate.STATUS_CL)
					.addNotInArrayFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, rackSystemPickingSlotIds);
		}

		//
		// Picking slot Barcode filter
		final String pickingSlotBarcode = pickingCandidatesQuery.getPickingSlotBarcode();
		if (!Check.isEmpty(pickingSlotBarcode, true))
		{
			final IPickingSlotDAO pickingSlotDAO = Services.get(IPickingSlotDAO.class);
			final Set<PickingSlotId> pickingSlotIds = pickingSlotDAO.retrievePickingSlotIds(PickingSlotQuery.builder()
					.barcode(pickingSlotBarcode)
					.build());
			if (pickingSlotIds.isEmpty())
			{
				return ImmutableList.of();
			}

			queryBuilder.addInArrayFilter(I_M_Picking_Candidate.COLUMN_M_PickingSlot_ID, pickingSlotIds);
		}

		//
		// HU filter
		final IQuery<I_M_HU> husQuery = queryBL.createQueryBuilder(I_M_HU.class)
				.addNotEqualsFilter(I_M_HU.COLUMNNAME_HUStatus, X_M_HU.HUSTATUS_Shipped) // not already shipped (https://github.com/metasfresh/metasfresh-webui-api/issues/647)
				.create();
		queryBuilder.addInSubQueryFilter(I_M_Picking_Candidate.COLUMN_M_HU_ID, I_M_HU.COLUMN_M_HU_ID, husQuery);

		return queryBuilder
				.orderBy(I_M_Picking_Candidate.COLUMNNAME_M_Picking_Candidate_ID)
				.create()
				.stream()
				.map(this::toPickingCandidate)
				.collect(ImmutableList.toImmutableList());
	}
	
	/**
	 * @return Return {@code true} if the given HU is referenced by an active picking candidate.<br>
	 *         Note that we use the ID for performance reasons.
	 */
	//@Cached(cacheName = I_M_Picking_Candidate.Table_Name + "#by#" + I_M_HU.COLUMNNAME_M_HU_ID)
	public boolean isHuIdPicked(@NonNull final HuId huId)
	{
		final boolean isAlreadyPicked = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_Picking_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_M_Picking_Candidate.COLUMNNAME_M_HU_ID, huId)
				.create()
				.match();
		return isAlreadyPicked;
	}


}
