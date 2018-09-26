/**
 *
 */
package de.metas.fresh.picking.form.swing;

/*
 * #%L
 * de.metas.fresh.base
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_UOM;

import de.metas.adempiere.form.terminal.IContainer;
import de.metas.adempiere.form.terminal.ITerminalButton;
import de.metas.adempiere.form.terminal.ITerminalDialog;
import de.metas.adempiere.form.terminal.ITerminalFactory;
import de.metas.adempiere.form.terminal.TerminalException;
import de.metas.adempiere.form.terminal.context.ITerminalContext;
import de.metas.adempiere.form.terminal.context.ITerminalContextReferences;
import de.metas.bpartner.BPartnerId;
import de.metas.bpartner.BPartnerLocationId;
import de.metas.fresh.picking.FreshProductKey;
import de.metas.fresh.picking.FreshProductLayout;
import de.metas.fresh.picking.PickingSlotKey;
import de.metas.fresh.picking.PickingSlotKeyGroup;
import de.metas.fresh.picking.PickingSlotLayout;
import de.metas.fresh.picking.form.FreshSwingPackageTerminalPanel;
import de.metas.handlingunits.IHUAware;
import de.metas.handlingunits.client.terminal.editor.model.IHUKeyFactory;
import de.metas.handlingunits.client.terminal.editor.view.HUEditorPanel;
import de.metas.handlingunits.exceptions.HULoadException;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_PI_Item_Product;
import de.metas.picking.legacy.form.IPackingItem;
import de.metas.picking.legacy.form.ITableRowSearchSelectionMatcher;
import de.metas.picking.legacy.form.PackingItemGroupingKey;
import de.metas.picking.legacy.form.PackingMd;
import de.metas.picking.legacy.form.ShipmentScheduleQtyPickedMap;
import de.metas.picking.service.FreshPackingItemHelper;
import de.metas.picking.service.IFreshPackingItem;
import de.metas.picking.service.IPackingService;
import de.metas.picking.service.PackingItemsMap;
import de.metas.picking.service.PackingSlot;
import de.metas.picking.service.impl.HU2PackingItemsAllocator;
import de.metas.picking.terminal.DefaultPackingStateAggregator;
import de.metas.picking.terminal.IPackingStateAggregator;
import de.metas.picking.terminal.ProductLayout;
import de.metas.picking.terminal.Utils.PackingStates;
import de.metas.picking.terminal.form.swing.SwingPackageBoxesItems;
import de.metas.product.IProductBL;
import de.metas.quantity.Quantity;
import de.metas.util.Check;
import de.metas.util.Services;
import lombok.NonNull;

/**
 * Contains: Picking Slots and products Products are not shown in this panel, but in the main panel
 *
 * @author cg
 *
 */
public class FreshSwingPackageItems extends SwingPackageBoxesItems
{
	// services
	// private final transient IMsgBL msgBL = Services.get(IMsgBL.class);
	private final transient IPackingService packingService = Services.get(IPackingService.class);

	private static final String ERR_Fresh_SWING_PACKAGE_ITEMS_QTY_NULL = "@de.metas.fresh.picking.form.swing.FreshSwingPackageItems.Qty.Null@";
	private static final String ERR_MAX_QTY = "@Max.Qty@";
	private static final String ERR_NO_OPEN_HU_FOUND = "@NoOpenHUFound@";
	/**
	 * Cannot fully load qty to handling unit. Qty Not Loaded:
	 */
	private static final String ERR_CANNOT_FULLY_LOAD_QTY_TO_HANDLING_UNIT = "@CannotFullyLoadQtyToHandlingUnit@ {}";

	public static final String ACTION_CloseCurrentHU = "Close_HU";
	private ITerminalButton bCloseCurrentHU;

	public static final String ACTION_DistributeQtyToNewHUs = "DistributeQtyToNewHUs";
	private ITerminalButton bDistributeQtyToNewHUs;

	public static final String ACTION_HUEditor = "HUEditor";
	private ITerminalButton bHUEditor;

	private PickingSlotKey selectedPickingSlotKey;
	private final PropertyChangeListener selectedPickingSlotKeyListener = evt -> onSelectedPickingSlotKeyChanged();

	public FreshSwingPackageItems(final FreshSwingPackageTerminalPanel basePanel)
	{
		super(basePanel);
	}

	@Override
	public FreshSwingPackageTerminalPanel getPackageTerminalPanel()
	{
		return (FreshSwingPackageTerminalPanel)super.getPackageTerminalPanel();
	}

	@Override
	protected final void initComponents()
	{
		super.initComponents();

		//
		// Button: Distribute Qty to new TUs (fresh_08754)
		{
			bDistributeQtyToNewHUs = createButtonAction(FreshSwingPackageItems.ACTION_DistributeQtyToNewHUs, null, 17f);
			bDistributeQtyToNewHUs.setEnabled(false);
			bDistributeQtyToNewHUs.addListener(this);

			final IContainer buttonsPanel = getButtonsPanel();
			buttonsPanel.addAfter(bDistributeQtyToNewHUs, getButtonAdd(), getButtonSize());
		}

		//
		// Button: HU Editor
		{
			bHUEditor = createButtonAction(FreshSwingPackageItems.ACTION_HUEditor, null, 17f);
			bHUEditor.setEnabled(false);
			bHUEditor.addListener(this);

			final IContainer buttonsPanel = getButtonsPanel();
			buttonsPanel.addAfter(bHUEditor, getButtonAdd(), getButtonSize());
		}

		//
		// Button: Close Current HU
		{
			bCloseCurrentHU = createButtonAction(FreshSwingPackageItems.ACTION_CloseCurrentHU, null, 17f);
			bCloseCurrentHU.setEnabled(false);
			bCloseCurrentHU.addListener(evt -> onCloseCurrentHU());
		}
	}

	@Override
	protected void initLayout()
	{
		add(getPickingSlotsKeyLayoutPanel(), "dock center, hmin 45%, hmax 100%");

		// NOTE: don't add "buttonsPanel" here

		// NOTE: don't add "productsKeyLayoutPanel" here

		// panelButtons.add(bCloseCurrentHU, getButtonSize()); // fresh_05833: moved to de.metas.customer.picking.form.FreshSwingPackageDataPanel
	}

	@Override
	public ProductLayout createProductsKeyLayout()
	{
		return new FreshProductLayout(getTerminalContext());
	}

	/**
	 * Create Picking Slots buttons
	 */
	@Override
	protected PickingSlotLayout createPickingSlotsKeyLayout()
	{
		final PickingSlotLayout pickingSlotLayout = new PickingSlotLayout(getTerminalContext());
		pickingSlotLayout.setRows(2);
		pickingSlotLayout.setBasePanel(getTerminalBasePanel());
		return pickingSlotLayout;
	}

	/**
	 * Gets {@link PickingSlotLayout}.
	 */
	@Override
	public final PickingSlotLayout getPickingSlotsKeyLayout()
	{
		return (PickingSlotLayout)super.getPickingSlotsKeyLayout();
	}

	@Override
	protected FreshQtyListener createQtyListener()
	{
		return new FreshQtyListener(this);
	}

	@Override
	public final void createAddRemoveAll()
	{
		// nothing to do
		// we do not want the ALL buttons
	}

	@Override
	public final void propertyChange(final PropertyChangeEvent evt)
	{
		//
		// NOTE: never call super.propertyChange because we want to always overwrite the functionality
		//

		try
		{
			propertyChange0(evt);
		}
		catch (final Exception e)
		{
			showWarning(e);
		}
	}

	private final void showWarning(final Exception e)
	{
		final ITerminalFactory terminalFactory = getTerminalFactory();

		if (e instanceof HULoadException)
		{
			final HULoadException loadEx = (HULoadException)e;
			final BigDecimal qtyToLoad = loadEx.getAllocationResult().getQtyToAllocate();
			final String errmsg = MessageFormat.format(FreshSwingPackageItems.ERR_CANNOT_FULLY_LOAD_QTY_TO_HANDLING_UNIT, qtyToLoad);
			terminalFactory.showWarning(this, ITerminalFactory.TITLE_ERROR, new TerminalException(errmsg, loadEx));
		}
		else
		{
			terminalFactory.showWarning(this, ITerminalFactory.TITLE_ERROR, e);
		}

	}

	private void propertyChange0(final PropertyChangeEvent evt) throws HULoadException
	{
		if (selectedPickingSlotKey == null)
		{
			warn(SwingPackageBoxesItems.ERR_NO_BOX_SELECTED);
			return;
		}

		final FreshProductKey selectedProduct = getSelectedProduct();
		final Object action = evt.getNewValue();
		if (SwingPackageBoxesItems.ACTION_Add.equals(action))
		{
			if (selectedProduct == null)
			{
				warn(SwingPackageBoxesItems.ERR_NO_PRODUCT_SELECTED);
				return;
			}

			final BigDecimal newQty = getQty();
			if (newQty == null || newQty.signum() == 0)
			{
				warn(FreshSwingPackageItems.ERR_Fresh_SWING_PACKAGE_ITEMS_QTY_NULL);
				return;
			}

			final BigDecimal unallocQty = selectedProduct.getQtyUnallocated();

			if (newQty.compareTo(unallocQty) > 0)
			{
				warn(FreshSwingPackageItems.ERR_MAX_QTY);
				setQty(BigDecimal.ZERO);
				return;
			}

			//
			// Add Qty to picking slot's current HU
			final I_M_HU hu = selectedPickingSlotKey.getM_HU(true);
			if (hu == null)
			{
				// CASE: an external process cleared the current HU
				// e.g. de.metas.shipping.process.M_ShippingPackage_CreateFromPickingSlots
				throw new AdempiereException(FreshSwingPackageItems.ERR_NO_OPEN_HU_FOUND);
			}
			final IFreshPackingItem unallocPackingItem = selectedProduct.getUnAllocatedPackingItem();
			packItemToHU(unallocPackingItem, Quantity.of(newQty, unallocPackingItem.getC_UOM()), hu);

			//
			// reset the qty field
			setQty(BigDecimal.ZERO);
		}
		else if (FreshSwingPackageItems.ACTION_DistributeQtyToNewHUs.equals(action))
		{
			try
			{
				onDistributeQtyToNewHUs();
			}
			catch (final Exception e)
			{
				// NOTE: we are catching, showing to user and consume the exception here,
				// because at the end we want to refresh keys, status etc because it might be that we created some HUs but not all.
				showWarning(e);
			}
		}
		else if (FreshSwingPackageItems.ACTION_HUEditor.equals(action))
		{
			onHUEditor();
		}
		else if (SwingPackageBoxesItems.ACTION_Remove.equals(action))
		{
			if (selectedProduct == null)
			{
				warn(SwingPackageBoxesItems.ERR_NO_PRODUCT_SELECTED);
				return;
			}

			final BigDecimal allocQty = selectedProduct.getQty();
			final BigDecimal qtyToRemove = getQty();

			if (qtyToRemove.signum() == 0)
			{
				warn(FreshSwingPackageItems.ERR_Fresh_SWING_PACKAGE_ITEMS_QTY_NULL);
				return;
			}

			if (qtyToRemove.compareTo(allocQty) > 0)
			{
				warn(FreshSwingPackageItems.ERR_MAX_QTY);
				setQty(BigDecimal.ZERO);
				return;
			}

			//
			// Remote Qty from HU
			// there was no explicit assumption about the UOM of qtyToRemove. I now assume stockingUOM.
			final I_C_UOM stockingUOM = Services.get(IProductBL.class).getStockingUOM(selectedProduct.getProductId());

			final IPackingItem pckItem = selectedProduct.getPackingItem();
			removeProductQty(pckItem, selectedPickingSlotKey.getM_HU(), Quantity.of(qtyToRemove, stockingUOM));

			// FIXME: reset the qty field, remove button etc
		}

		refreshProducts();

		// update slot status
		updatePackingState(selectedPickingSlotKey);

		// Forward the event
		getPackageTerminalPanel().keyPressed(selectedPickingSlotKey);
	}

	@Override
	public final FreshProductKey getSelectedProduct()
	{
		return (FreshProductKey)super.getSelectedProduct();
	}

	/**
	 * @return all product keys which are currently available
	 */
	public List<FreshProductKey> getAllProductKeys()
	{
		return getProductsKeyLayout().getKeys(FreshProductKey.class);
	}

	public List<IPackingItem> createUnpackedForBpAndBPLoc(final List<IPackingItem> packingItems, final PickingSlotKey pickingSlotKey)
	{
		if (packingItems == null || packingItems.isEmpty())
		{
			return Collections.emptyList();
		}

		final List<IPackingItem> result = new ArrayList<>();
		for (final IPackingItem apck : packingItems)
		{
			final IFreshPackingItem packingItem = FreshPackingItemHelper.cast(apck);

			// Skip those which are not compatible with our picking slot
			if (!pickingSlotKey.isCompatible(packingItem))
			{
				continue;
			}

			result.add(packingItem);
		}

		return result;
	}

	/**
	 * get the packing state by items form the slot
	 *
	 * @param pickingSlotKey
	 * @return
	 */
	private PackingStates getPackingState(final PickingSlotKey pickingSlotKey)
	{
		final PackingItemsMap packingItems = getPackingItems();
		final PackingSlot packedItemsSlot = PackingSlot.ofPickingSlotId(pickingSlotKey.getPickingSlotId());
		final List<IPackingItem> packedItems = packingItems.getBySlot(packedItemsSlot);
		final List<IPackingItem> unpackedItems = createUnpackedForBpAndBPLoc(packingItems.getUnpackedItems(), pickingSlotKey);

		if (unpackedItems == null || unpackedItems.isEmpty())
		{
			return PackingStates.packed;
		}

		if (packedItems == null || packedItems.isEmpty())
		{
			return PackingStates.unpacked;
		}

		final List<IPackingItem> allQtyremainingItems = new ArrayList<>();

		final List<IPackingItem> partialQtyRemainingItems = new ArrayList<>();

		for (final IPackingItem unpackedItem : unpackedItems)
		{
			for (final IPackingItem packedItem : packedItems)
			{
				if (PackingItemGroupingKey.equals(packedItem.getGroupingKey(), unpackedItem.getGroupingKey()))
				{
					partialQtyRemainingItems.add(unpackedItem);
				}
				else
				{
					allQtyremainingItems.add(unpackedItem);
				}
			}
		}

		if (allQtyremainingItems.isEmpty() && partialQtyRemainingItems.isEmpty())
		{
			return PackingStates.packed;
		}
		else
		{
			return PackingStates.partiallypacked;
		}
	}

	private void updatePackingState(final PickingSlotKey pickingSlotKey)
	{
		Check.assumeNotNull(pickingSlotKey, "pickingSlotKey not null");

		final PickingSlotKeyGroup group = pickingSlotKey.getPickingSlotKeyGroup();
		final PickingSlotLayout pickingSlotLayout = getPickingSlotsKeyLayout();

		final List<PickingSlotKey> pickingSlotKeysInGroup = pickingSlotLayout.getPickingSlotKeys(group);
		if (pickingSlotKeysInGroup.isEmpty())
		{
			// shall not happen: at least we need to have our pickingSlotKey in this list
			// but as a fallback we are adding our current pickingSlotKey
			pickingSlotKeysInGroup.add(pickingSlotKey);
		}

		//
		// Calculate state for entire group
		final IPackingStateAggregator stateAggregator = new DefaultPackingStateAggregator();
		stateAggregator.setDefaultPackingState(PackingStates.unpacked);
		for (final PickingSlotKey key : pickingSlotKeysInGroup)
		{
			final PackingStates state = getPackingState(key);
			stateAggregator.addPackingState(state);

			//
			// fresh_06178: Releases the currently selected picking slot from it's current BPartner if the PickingSlot is unpacked
			if (PackingStates.unpacked.equals(state))
			{
				key.releasePickingSlot();
			}
		}

		//
		// Update state for all picking slots from group
		final PackingStates state = stateAggregator.getPackingState();
		for (final PickingSlotKey key : pickingSlotKeysInGroup)
		{
			key.updateStatus(state);
		}
	}

	private void packItemToHU(
			final IFreshPackingItem itemToPack,
			final Quantity qtyToPack,
			@NonNull final I_M_HU targetHU)
	{
		//
		// Find out which are the available HUs from which we can take
		final FreshProductKey productKey = getSelectedProduct();
		final List<I_M_HU> availableHUsToPickFrom = productKey.findAvailableHUs(true); // considerAttributes = true
		availableHUsToPickFrom.remove(targetHU); // remove target from source, just to be sure

		// the allocation shall be done even if there are no source HUs
		// Check.assumeNotEmpty(availableSourceHUs, "availableHUsSource not empty"); // shall not happen

		// NOTE: we are doing a copy and work on it, in case something fails. At the end we will set it back
		final PackingItemsMap packingItems = getPackingItems().copy();

		// Allocate given HUs to "itemToPack"
		HU2PackingItemsAllocator.builder()
				.itemToPack(itemToPack)
				.packingItems(packingItems)
				.packedItemsSlot(getSelectedPackingSlot())
				.fromHUs(availableHUsToPickFrom)
				.targetHU(targetHU)
				.qtyToPack(qtyToPack)
				.allocate();

		//
		// Copy back the results
		setPackingItems(packingItems);

		//
		// fresh_06178: Allocate picking slot on the newly packed item
		getSelectedPickingSlotKey().allocateDynamicPickingSlotIfPossible(itemToPack.getBPartnerId(), itemToPack.getBPartnerLocationId());
	}

	private void removeProductQty(
			final IPackingItem pckItem,
			final I_M_HU hu,
			final Quantity qtyToRemove)
	{
		// Packing items
		// NOTE: we are doing a copy and work on it, in case something fails. At the end we will set it back
		final PackingItemsMap packItems = getPackingItems().copy();

		IPackingItem itemUnpacked = null;
		for (final IPackingItem item : packItems.getUnpackedItems())
		{
			if (PackingItemGroupingKey.equals(item.getGroupingKey(), pckItem.getGroupingKey()))
			{
				Check.assumeNull(itemUnpacked, "Item with grouping key {} shall exist only once in the list", item.getGroupingKey());
				itemUnpacked = item;
			}
		}

		final PackingSlot slot = PackingSlot.ofPickingSlotId(selectedPickingSlotKey.getPickingSlotId());

		// we need to remove the recently unpacked item from packed
		final List<IPackingItem> itemsPackedRemaining = new ArrayList<>();
		for (final IPackingItem itemPacked : packItems.removeBySlot(slot))
		{
			if (!pckItem.isSameAs(itemPacked))
			{
				itemsPackedRemaining.add(itemPacked);
			}
			else
			{
				final IFreshPackingItem itemPackedNew = FreshPackingItemHelper.copy(itemPacked);

				//
				// take out qtyToRemove from our packing item
				final Properties ctx = getTerminalContext().getCtx();
				final ShipmentScheduleQtyPickedMap qtyToRemoveAlloc = itemPackedNew.subtract(qtyToRemove);
				packingService.removeProductQtyFromHU(ctx, hu, qtyToRemoveAlloc);

				// if all qty is packed, no need to store the current item
				if (pckItem.getQtySum().compareTo(qtyToRemove) != 0)
				{
					itemsPackedRemaining.add(itemPackedNew);
				}

				// if we already have an item, add the extracted scheds to the existent one
				if (itemUnpacked != null)
				{
					itemUnpacked.addSchedules(qtyToRemoveAlloc);
				}
				else
				{
					final IFreshPackingItem newPi = FreshPackingItemHelper.create(qtyToRemoveAlloc);
					packItems.addUnpackedItem(newPi);
				}
			}
		}

		packItems.addItems(slot, itemsPackedRemaining);

		setPackingItems(packItems);
	}

	/**
	 * @return currently selected {@link PickingSlotKey} or null
	 */
	public PickingSlotKey getSelectedPickingSlotKey()
	{
		return selectedPickingSlotKey;
	}

	public void setSelectedPickingSlotKey(final PickingSlotKey selectedPickingSlotKey)
	{
		// NOTE: we are not checking if selectedPickingSlotKey was actually changed because we want to refresh all time

		if (this.selectedPickingSlotKey != null)
		{
			this.selectedPickingSlotKey.removeListener(selectedPickingSlotKeyListener);
		}

		this.selectedPickingSlotKey = selectedPickingSlotKey;
		if (this.selectedPickingSlotKey != null)
		{
			this.selectedPickingSlotKey.addListener(selectedPickingSlotKeyListener);
			onSelectedPickingSlotKeyChanged();
		}
	}

	/**
	 * Refresh Product Keys: i.e. resets current product keys and create them again.
	 */
	public void refreshProducts()
	{
		final ProductLayout productlayout = getProductsKeyLayout();

		// Clear product keys.
		// They will be automatically recreated by the products key layout.
		productlayout.resetKeys();
	}

	private void onCloseCurrentHU()
	{
		final PickingSlotKey selectedPickingSlotKey = getSelectedPickingSlotKey();
		if (selectedPickingSlotKey == null)
		{
			// No picking slot selected => nothing to do
			return;
		}

		selectedPickingSlotKey.closeCurrentHU();

		// update slot status
		updatePackingState(selectedPickingSlotKey);

		// Forward the event
		getPackageTerminalPanel().keyPressed(selectedPickingSlotKey);

		refreshProducts();
	}

	private void onSelectedPickingSlotKeyChanged()
	{
		final PickingSlotKey pickingSlotKey = getSelectedPickingSlotKey();

		if (pickingSlotKey == null || !pickingSlotKey.hasOpenHU())
		{
			bCloseCurrentHU.setEnabled(false);
		}
		else
		{
			bCloseCurrentHU.setEnabled(true);
		}

		setEnableDistributeQtyToNewHUsButton();
	}

	public ITerminalButton getbCloseCurrentHU()
	{
		return bCloseCurrentHU;
	}

	public final void setEnableHUEditorButton(final boolean enabled)
	{
		if (bHUEditor == null)
		{
			return;
		}

		bHUEditor.setEnabled(enabled);
	}

	/**
	 * Updates the enabled/disabled status of {@link #ACTION_DistributeQtyToNewHUs} button.
	 */
	public final void setEnableDistributeQtyToNewHUsButton()
	{
		bDistributeQtyToNewHUs.setEnabled(isDistributeQtyToNewHUsAvailable());
	}

	/**
	 * Checks is the {@value #ACTION_DistributeQtyToNewHUs} shall be available.
	 *
	 * @return true if it shall be available
	 */
	private boolean isDistributeQtyToNewHUsAvailable()
	{
		final PickingSlotKey pickingSlotKey = getSelectedPickingSlotKey();
		if (pickingSlotKey == null)
		{
			return false;
		}

		final I_M_HU_PI_Item_Product piItemProduct = pickingSlotKey.getM_HU_PI_Item_Product();
		if (piItemProduct == null)
		{
			return false;
		}

		// Allow distributing a quantity over new HUs even if this packing material is infinite capacity (fresh_08844).
		// if (piItemProduct.isInfiniteCapacity())
		// {
		// return false;
		// }

		final FreshProductKey productKey = getSelectedProduct();
		if (productKey == null)
		{
			return false;
		}

		final BigDecimal qtyToAllocate = productKey.getQtyUnallocated();
		if (qtyToAllocate.signum() <= 0)
		{
			return false;
		}

		return true;
	}

	/**
	 * Distribute the user entered Qty to a specified number of TUs.
	 *
	 * @task http://dewiki908/mediawiki/index.php/08754_Kommissionierung_Erweiterung_Verteilung_%28103380135151%29
	 */
	private void onDistributeQtyToNewHUs()
	{
		final PickingSlotKey pickingSlotKey = getSelectedPickingSlotKey();
		Check.assumeNotNull(pickingSlotKey, "pickingSlotKey not null");

		final FreshProductKey productKey = getSelectedProduct();
		Check.assumeNotNull(productKey, "productKey not null");

		//
		// Create the distributor command
		final DistributeQtyToNewHUsResultExecutorTemplate distributor = new DistributeQtyToNewHUsResultExecutorTemplate()
		{
			/** Create distribution request based on current ProductKey, PickingSlot Key and the Qty field */
			@Override
			protected DistributeQtyToNewHUsRequest getInitialRequest()
			{
				return DistributeQtyToNewHUsRequest.builder()
						.setM_HU_PI_Item_Product(pickingSlotKey.getM_HU_PI_Item_Product())
						.setQtyToDistribute(productKey.getQtyUnallocated())
						.setQtyToDistributeUOM(productKey.getQtyUnallocatedUOM())
						.setQtyToDistributePerHU(getQty())
						.build();
			}

			/** Ask user about how to distribute and get the result */
			@Override
			protected DistributeQtyToNewHUsResult calculateResult(final DistributeQtyToNewHUsRequest request)
			{
				return DistributeQtyToNewHUsReadPanel.builder()
						.setTerminalContext(getTerminalContext())
						.setParentComponent(FreshSwingPackageItems.this)
						.setTitle(bDistributeQtyToNewHUs.getText())
						.setRequestCloseCurrentHUConfirmation(pickingSlotKey.hasOpenNotEmptyHU())
						.setRequest(request)
						.getResultOrNull();
			}

			/** Create a new HU on current picking slot */
			@Override
			protected I_M_HU createNewHU(final I_M_HU_PI_Item_Product piItemProduct)
			{
				pickingSlotKey.createHU(piItemProduct);
				final I_M_HU hu = pickingSlotKey.getM_HU();
				return hu;
			}

			/** Pick given quantity from current product key and load it to given HU */
			@Override
			protected void loadQtyToHU(final I_M_HU hu, final BigDecimal qty)
			{
				final IFreshPackingItem itemToPack = productKey.getUnAllocatedPackingItem();
				packItemToHU(itemToPack, Quantity.of(qty, itemToPack.getC_UOM()), hu);
			}

			/** Close picking slot current HU */
			@Override
			protected void closeCurrentHU()
			{
				pickingSlotKey.closeCurrentHU();
			}
		};

		//
		// Execute the distribution
		distributor.execute();
	}

	/**
	 * Called when HUEditor button is pressed.
	 *
	 * It will:
	 * <ul>
	 * <li>open the HU Editor, let user select some HUs
	 * <li>will allocate those HUs to selected product key's shipment schedules
	 * <li>will move those selected HUs directly in picking slot's queue.
	 * </ul>
	 */
	private void onHUEditor()
	{
		final FreshProductKey productKey = getSelectedProduct();
		Check.assumeNotNull(productKey, "productKey not null");

		final PickingSlotKey pickingSlotKey = getSelectedPickingSlotKey();
		Check.assumeNotNull(pickingSlotKey, "pickingSlotKey not null");

		// TODO: this is a workaround just to deliver a working increment.
		// We shall not have such a tight coupling but instead the selectedHU should be provided in some packing model
		final PackingMd pickingOKPanelModel = getPackageTerminalPanel() // Packing window main panel (second window)
				.getParent() // Packing window (second window)
				.getPickingOKPanel() // Picking OK Panel (that one that contains table rows)
				.getModel(); // PackingMd

		//
		// Get selected HU from first panel.
		// (i.e. when user was scanning by SSCC18 or internal HU barcode)
		I_M_HU selectedHU = null;
		final ITableRowSearchSelectionMatcher tableRowsMatcher = pickingOKPanelModel.getTableRowSearchSelectionMatcher();
		if (tableRowsMatcher instanceof IHUAware)
		{
			final IHUAware huAware = (IHUAware)tableRowsMatcher;
			selectedHU = huAware.getM_HU();
		}

		//
		// Get HUKeyFactory
		final ITerminalContext terminalContext = getTerminalContext();

		//
		// Clear (attribute) cache before opening editor
		final IHUKeyFactory huKeyFactory = terminalContext.getService(IHUKeyFactory.class);
		huKeyFactory.clearCache();

		// the selected HUs are set from the HU-Editor within the following try-with-resources block
		final Set<I_M_HU> husSelected;

		try (final ITerminalContextReferences refs = terminalContext.newReferences())
		{
			//
			// Create HU Editor Model
			final PickingHUEditorModel huEditorModel = new PickingHUEditorModel(
					terminalContext // context
					, selectedHU // default HU to select after query
					, productKey::findAvailableHUs // HUs provider
			);
			huEditorModel.setConsiderAttributes(true); // the default

			// Create HU Editor UI Panel
			final HUEditorPanel huEditorPanel = new PickingHUEditorPanel(huEditorModel);

			// Wrap our HU Editor Panel with a model dialog
			final ITerminalDialog editorDialog = getTerminalFactory().createModalDialog(this, "Edit", huEditorPanel);
			editorDialog.setSize(terminalContext.getScreenResolution());

			// Activate editor dialog and wait for user
			editorDialog.activate();

			if (editorDialog.isCanceled())
			{
				return; // nothing to do
			}

			husSelected = huEditorModel.getSelectedHUs();
		}

		if (husSelected.isEmpty())
		{
			// nothing selected, nothing to do
			return;
		}

		//
		// Get the selected shipment schedules' C_BPartner_ID and C_BPartner_Location_ID (fresh_06974)
		final IFreshPackingItem unallocatedPackingItem = productKey.getUnAllocatedPackingItem();
		Check.assumeNotNull(unallocatedPackingItem, "unallocatedPackingItem not null"); // shall not happen if we reached this point
		final BPartnerId bpartnerId = unallocatedPackingItem.getBPartnerId();
		final BPartnerLocationId bpartnerLocationId = unallocatedPackingItem.getBPartnerLocationId();

		//
		// Make sure the picking slot (this is necessary if it's a dynamic one) is allocated to them (fresh_06974)
		pickingSlotKey.allocateDynamicPickingSlotIfPossible(bpartnerId, bpartnerLocationId);

		// 07161: this also associates the HUs to the shipment schedule we are currently picking for,
		// causing the hu's C_BPartner_ID and C_BPartner_Location_ID to be updated from the schedule.
		// TODO it might be required to split the selected HU's VHU if one HU is assigned to two or more shipment scheds
		allocateItemToHUs(productKey.getUnAllocatedPackingItem(), husSelected);

		// Move selected HUs directly to Picking slot queue
		// this also calls IHUPickingSlotBL.addToPickingSlotQueue()
		pickingSlotKey.addHUsToQueue(husSelected);
	}

	/**
	 * Allocates <code>itemToPack</code> to given HUs.
	 *
	 * @param itemToPack
	 * @param hus HUs on which the quantity will be allocated
	 */
	private void allocateItemToHUs(final IFreshPackingItem itemToPack, final Collection<I_M_HU> hus)
	{
		// NOTE: we are doing a copy and work on it, in case something fails. At the end we will set it back
		final PackingItemsMap packingItems = getPackingItems().copy();

		//
		// Allocate given HUs to "itemToPack"
		HU2PackingItemsAllocator.builder()
				.itemToPack(itemToPack)
				.packingItems(packingItems)
				.packedItemsSlot(getSelectedPackingSlot())
				.fromHUs(hus)
				.allocate();

		//
		// Copy back the results
		setPackingItems(packingItems);
	}

	private PackingSlot getSelectedPackingSlot()
	{
		final PickingSlotKey selectedPickingSlotKey = getSelectedPickingSlotKey();
		Check.assumeNotNull(selectedPickingSlotKey, "selectedPickingSlotKey not null");
		final PackingSlot packedItemsSlot = PackingSlot.ofPickingSlotId(selectedPickingSlotKey.getPickingSlotId());
		return packedItemsSlot;
	}

	private PackingItemsMap getPackingItems()
	{
		return getPackageTerminalPanel().getPackItems();
	}

	private void setPackingItems(final PackingItemsMap packingItems)
	{
		getPackageTerminalPanel().setPackItems(packingItems);
	}
}
