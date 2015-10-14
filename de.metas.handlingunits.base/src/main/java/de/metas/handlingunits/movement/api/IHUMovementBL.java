package de.metas.handlingunits.movement.api;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.adempiere.model.IContextAware;
import org.adempiere.util.ISingletonService;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Warehouse;

import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_MovementLine;
import de.metas.inoutcandidate.spi.impl.HUPackingMaterialDocumentLineCandidate;
import de.metas.interfaces.I_M_Movement;

public interface IHUMovementBL extends ISingletonService
{

	String SYSCONFIG_DirectMove_Warehouse_ID = "de.metas.handlingunits.client.terminal.inventory.model.InventoryHUEditorModel#DirectMove_Warehouse_ID";

	/**
	 * After the movement lines for product were created, we are creating packing material lines for them the packing material lines contain the qtys aggregated for all the product lines of the same
	 * locatorFrom, locatorTo
	 *
	 * @param movement
	 */
	void createPackingMaterialMovementLines(I_M_Movement movement);

	/**
	 * NOTE: Only use for packing material Movement Lines!!!! Note 2: the movement line is saved
	 *
	 * Set the correct activity in the movement line In the case of packing material movement lines, this is always the activity of the prosuct ( Usually Gebinde)
	 *
	 * @param movementLine
	 * @task 07689, 07690
	 */
	void setPackingMaterialCActivity(I_M_MovementLine movementLine);

	/**
	 * Generate empties movements from HUPackingMaterialDocumentLineCandidate entries
	 *
	 * @param warehouse
	 * @param direction
	 * @param lines
	 * @return
	 */
	I_M_Movement generateMovementFromPackingMaterialCandidates(I_M_Warehouse warehouse, boolean direction, List<HUPackingMaterialDocumentLineCandidate> lines);

	/**
	 * Generate movements for the empties (Leergut) inOut. If the given <code>inout</code> is a receipt, the movement will be from inOut's warehouse to the empties-warehouse (Gebindelager). If the
	 * inOut is a shipment, the movement will be in the opposite direction.
	 *
	 * @param inout
	 * @return
	 * @task 08070
	 */
	I_M_Movement generateMovementFromEmptiesInout(I_M_InOut inout);

	/**
	 * Move the given <code>hus</code>HUs to the given <code>destinationWarehouse</code>
	 *
	 * @param destinationWarehouse
	 * @param hu
	 * @return
	 */
	List<I_M_Movement> generateMovementsToWarehouse(I_M_Warehouse destinationWarehouse, Collection<I_M_HU> hus, IContextAware ctxAware);

	/**
	 * Method uses <code>AD_SysConfig</code> {@value #SYSCONFIG_DirectMove_Warehouse_ID} to get the {@link I_M_Warehouse} for direct movements.
	 *
	 * @param ctx the context we use to get the <code>AD_Client_ID</code> and <code>AD_Org_ID</code> used to retrieve the AD_SysConfig value.
	 * @param throwEx if <code>true</code> then the method throws a exception rather than of returning <code>null</code>.
	 *
	 * @task http://dewiki908/mediawiki/index.php/08205_HU_Pos_Inventory_move_Button_%28105838505937%29
	 * @return {@link I_M_Warehouse} for direct movements or <code>null</code>.
	 */
	I_M_Warehouse getDirectMove_Warehouse(Properties ctx, boolean throwEx);
}
