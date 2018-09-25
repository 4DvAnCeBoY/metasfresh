package de.metas.picking.service.impl;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.PlainContextAware;
import org.adempiere.uom.api.IUOMConversionBL;
import org.adempiere.uom.api.UOMConversionContext;
import org.compiere.model.I_C_UOM;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.IHUContextFactory;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.IMutableHUContext;
import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationResult;
import de.metas.handlingunits.allocation.impl.AllocationUtils;
import de.metas.handlingunits.allocation.impl.GenericAllocationSourceDestination;
import de.metas.handlingunits.allocation.impl.HUListAllocationSourceDestination;
import de.metas.handlingunits.allocation.impl.HULoader;
import de.metas.handlingunits.hutransaction.IHUTrxBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_ShipmentSchedule_QtyPicked;
import de.metas.handlingunits.shipmentschedule.api.IHUShipmentScheduleBL;
import de.metas.handlingunits.shipmentschedule.api.impl.ShipmentScheduleQtyPickedProductStorage;
import de.metas.handlingunits.storage.IHUStorage;
import de.metas.handlingunits.storage.IHUStorageFactory;
import de.metas.handlingunits.storage.IProductStorage;
import de.metas.inoutcandidate.api.IShipmentScheduleEffectiveBL;
import de.metas.inoutcandidate.model.I_M_ShipmentSchedule;
import de.metas.inoutcandidate.model.X_M_ShipmentSchedule;
import de.metas.picking.api.PickingConfigRepository;
import de.metas.picking.service.IFreshPackingItem;
import de.metas.picking.service.PackingItemsMap;
import de.metas.picking.service.PackingSlot;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;
import de.metas.util.Check;
import de.metas.util.Services;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * Class responsible for allocating given HUs to underlying shipment schedules from {@link IFreshPackingItem}.
 *
 * As a result of an allocation, you shall get:
 * <ul>
 * <li>From {@link #getItemToPack()}'s Qty, the HU's Qtys will be subtracted
 * <li>to {@link PackingItemsMap} we will have newly packed items and also current Item to Pack
 * <li>{@link I_M_ShipmentSchedule_QtyPicked} records will be created (shipment schedules are taken from Item to Pack)
 * </ul>
 *
 * @author tsa
 *
 */
public class HU2PackingItemsAllocator
{

	//
	// Services
	private final transient IHUContextFactory huContextFactory = Services.get(IHUContextFactory.class);
	private final transient IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);
	private final transient IShipmentScheduleEffectiveBL shipmentScheduleEffectiveBL = Services.get(IShipmentScheduleEffectiveBL.class);
	private final transient IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
	private final transient IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
	private final transient IHUTrxBL huTrxBL = Services.get(IHUTrxBL.class);
	private final transient IHUShipmentScheduleBL huShipmentScheduleBL = Services.get(IHUShipmentScheduleBL.class);
	private final transient ITrxManager trxManager = Services.get(ITrxManager.class);

	/**
	 * Cannot fully load:
	 */
	private static final String ERR_CANNOT_FULLY_LOAD = "@CannotFullyLoad@ {}";

	//
	// Parameters
	private final IFreshPackingItem itemToPack;
	private final PackingSlot packedItemsSlot;
	private final PackingItemsMap packingItems;
	//
	private final boolean allowOverDelivery;
	private final ImmutableList<I_M_HU> _fromHUs;
	private final I_M_HU _targetHU;
	private final boolean _qtyToPackEnforced;

	//
	// Status
	private IHUContext _huContext = null;
	private Quantity _qtyToPackRemaining = null;

	/**
	 * 
	 * @param packingContext
	 * @param itemToPack
	 * @param allowOverDelivery
	 * @param fromHUs the HUs to assign to the shipment schedule. IMPORTANT: The all need be out of transaction.
	 */
	@Builder
	private HU2PackingItemsAllocator(
			@NonNull final IFreshPackingItem itemToPack,
			@Nullable final PackingItemsMap packingItems,
			@Nullable final PackingSlot packedItemsSlot,
			//
			final boolean allowOverDelivery,
			@NonNull @Singular("fromHU") ImmutableList<I_M_HU> fromHUs,
			@Nullable final I_M_HU targetHU,
			@Nullable final Quantity qtyToPack)
	{
		this.itemToPack = itemToPack;
		this.packingItems = packingItems != null ? packingItems : PackingItemsMap.ofUnpackedItem(itemToPack);
		this.packedItemsSlot = packedItemsSlot != null ? packedItemsSlot : PackingSlot.DEFAULT_PACKED;

		this.allowOverDelivery = allowOverDelivery;
		this._fromHUs = fromHUs;
		this._targetHU = targetHU;

		if (qtyToPack != null)
		{
			Check.assume(qtyToPack.signum() > 0, "qtyToPack > 0 but it was {}", qtyToPack);

			_qtyToPackEnforced = true;
			_qtyToPackRemaining = qtyToPack;
		}
		else
		{
			_qtyToPackEnforced = false;
		}
	}

	/**
	 * Creates initial {@link IHUContext} to be used when performing.
	 *
	 * @return
	 */
	private IHUContext createHUContextInitial()
	{
		final PlainContextAware contextProvider = PlainContextAware.newWithThreadInheritedTrx();
		final IMutableHUContext huContext = huContextFactory.createMutableHUContextForProcessing(contextProvider);
		return huContext;
	}

	private ProductId getProductId()
	{
		return itemToPack.getProductId();
	}

	/**
	 * @return processing {@link IHUContext}; never returns <code>null</code>
	 */
	private final IHUContext getHUContext()
	{
		Check.assumeNotNull(_huContext, "_huContext not null");
		return _huContext;
	}

	private final void setHUContext(final IHUContext huContext)
	{
		Check.assumeNotNull(huContext, "huContext not null");
		Check.assumeNull(_huContext, "huContext not already configured");
		_huContext = huContext;
	}

	private final List<I_M_HU> getFromHUs()
	{
		return _fromHUs;
	}

	private void assertFromHUsOutOfTrx()
	{
		trxManager.assertModelsTrxName(ITrx.TRXNAME_None, getFromHUs());
	}

	private final I_M_HU getTargetHU()
	{
		return _targetHU;
	}

	/**
	 * @return Qty that remains to be package (in itemToPack's UOM)
	 */
	private final Quantity getQtyToPackRemaining()
	{
		Check.assumeNotNull(_qtyToPackRemaining, "_qtyToPackRemaining not null");
		return _qtyToPackRemaining;
	}

	private final void subtractFromQtyToPackRemaining(final Quantity qtyPicked)
	{
		// Do nothing if we are not tracking remaining qty to pack
		if (!isQtyToPackEnforced())
		{
			return;
		}

		_qtyToPackRemaining = _qtyToPackRemaining.subtract(qtyPicked);
	}

	private final boolean isQtyToPackEnforced()
	{
		return _qtyToPackEnforced;
	}

	/**
	 * @return true if we still have enforced quantity to pack; if the quantity to pack is not enforced, this method will always return <code>true</code>.
	 */
	private final boolean hasRemainingQtyToPack()
	{
		// If the qtyToPack is not enforced, then we can allocate as much as we want
		if (!isQtyToPackEnforced())
		{
			return true;
		}

		return _qtyToPackRemaining.signum() > 0;
	}

	/**
	 * If {@link #isQtyToPackEnforced()} then adjusts <code>qtyToPack</code> based on {@link #getQtyToPackRemaining()}.
	 *
	 * @param qtyToPack
	 * @param uom
	 * @return qtyToPack (adjusted)
	 */
	private final Quantity adjustQtyToPackConsideringRemaining(final Quantity qtyToPack)
	{
		// If we are not tracking the remaining qty to pack,
		// then directly accept the given quantity
		if (!isQtyToPackEnforced())
		{
			return qtyToPack;
		}

		// Make sure there is something remaining to be packed
		if (_qtyToPackRemaining.signum() <= 0)
		{
			return qtyToPack.toZero();
		}

		// Make sure QtyToPick is greather then zero
		// shall not happen, but just to be sure
		if (qtyToPack == null || qtyToPack.signum() <= 0)
		{
			return qtyToPack.toZero();
		}

		final Quantity qtyToPackActual = _qtyToPackRemaining.min(qtyToPack);
		return qtyToPackActual;
	}

	/**
	 * Asserts this builder is still in configuration stage
	 */
	private final void assertConfigurable()
	{
		final boolean configurable = _huContext == null; // i.e. processing HUContext was not already set
		Check.assume(configurable, "Builder is in configurable mode: {}", this);
	}

	/**
	 * Allocates the given <code>hus</code> to this instance's current item to pack (see {@link #getItemToPack()}).
	 *
	 * The allocated qty will be the HUs' qty for the product that is currently packed (i.e. the qty will be defined by the handling units, not e.g. by the underlying shipment schedule's
	 * QtyToDeliver).
	 *
	 * The quantity that was allocated on HUs will be subtracted from {@link #getItemToPack()}.
	 *
	 * @param hus
	 */
	public final void allocate()
	{
		// Make sure we did not run "allocate" before
		// i.e. this builder is still configurable
		assertConfigurable();

		//
		// Get the HUs which we need to allocate to shipment schedules
		// and make sure they are ok.
		assertFromHUsOutOfTrx();

		//
		// Make sure we have remaining qty to pack
		if (!hasRemainingQtyToPack())
		{
			return;
		}

		//
		// Allocate
		final IHUContext huContextInitial = createHUContextInitial();
		huTrxBL.createHUContextProcessorExecutor(huContextInitial)
				.run(this::allocate0);
	}

	private final void allocate0(final IHUContext huContext)
	{
		setHUContext(huContext);

		//
		// Iterate the HUs which we need to "back" allocate to our shipment schedules
		// and try allocating them.
		for (final I_M_HU hu : getFromHUs())
		{
			// Stop if we transfered everything
			if (!hasRemainingQtyToPack())
			{
				break;
			}

			allocateHU(hu);
		}

		//
		// If we have an enforced QtyToPack and if we did not allocate and then transfer everything to Target HU
		// then transfer the remaing qty now
		transferRemainingQtyToTargetHU();
	}

	/**
	 * Allocate given LU/TU
	 *
	 * @param hu LU or TU
	 */
	private final void allocateHU(@NonNull final I_M_HU hu)
	{
		//
		// Case: Loading Unit
		if (handlingUnitsBL.isLoadingUnit(hu))
		{
			final List<I_M_HU> tuHUs = handlingUnitsDAO.retrieveIncludedHUs(hu);
			for (final I_M_HU tuHU : tuHUs)
			{
				allocateTU(tuHU);
			}
		}
		//
		// Case: Transport Unit or Virtual HU
		else if (handlingUnitsBL.isTransportUnitOrVirtual(hu))
		{
			allocateTU(hu);
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @HU_UnitType@: " + handlingUnitsBL.getHU_UnitType(hu)
					+ "\n @M_HU_ID@: " + hu);
		}

		//
		// Make sure the HU (or some of its children) gets destroyed if the storage gets empty
		final IHUContext huContext = getHUContext();
		handlingUnitsBL.destroyIfEmptyStorage(huContext, hu);
	}

	private final void allocateTU(final I_M_HU tuHU)
	{
		// Make sure we have remaining qty to pack
		if (!hasRemainingQtyToPack())
		{
			return;
		}

		// Case our TU is a VHU
		if (handlingUnitsBL.isVirtual(tuHU))
		{
			final I_M_HU vhu = tuHU;
			allocateVHU(vhu);
		}
		// Case our TU is really a TU (stricly speaking)
		else if (handlingUnitsBL.isTransportUnit(tuHU))
		{
			final List<I_M_HU> vhus = handlingUnitsDAO.retrieveIncludedHUs(tuHU);
			for (final I_M_HU vhu : vhus)
			{
				allocateVHU(vhu);
			}
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @HU_UnitType@: " + handlingUnitsBL.getHU_UnitType(tuHU)
					+ "\n @M_HU_ID@: " + tuHU);
		}

	}

	private final IProductStorage getProductStorage(final I_M_HU hu, final ProductId productId)
	{
		final IHUContext huContext = getHUContext();
		final IHUStorageFactory huStorageFactory = huContext.getHUStorageFactory();
		final IHUStorage huStorage = huStorageFactory.getStorage(hu);

		final IProductStorage productStorage = huStorage.getProductStorageOrNull(productId);
		if (productStorage == null)
		{
			return null;
		}
		if (productStorage.isEmpty())
		{
			return null;
		}

		return productStorage;
	}

	/**
	 * Creates {@link I_M_ShipmentSchedule_QtyPicked} record to allocate given virtual HU.
	 *
	 * @param sched
	 * @param qtyPicked
	 * @param uom
	 * @param vhu
	 * @return created record
	 */
	// FIXME: rename this method
	private final void onQtyAllocated(
			@NonNull final I_M_ShipmentSchedule sched,
			@NonNull final Quantity qtyPicked,
			@NonNull final I_M_HU vhu)
	{
		// "Back" allocate the qtyPicked from VHU to given shipment schedule
		huShipmentScheduleBL.addQtyPicked(sched, qtyPicked, vhu);

		// Transfer the qtyPicked from vhu to our target HU (if any)
		transferQtyFromVHUToTargetHU(sched, qtyPicked, vhu);

		// Adjust remaining Qty to be packed
		subtractFromQtyToPackRemaining(qtyPicked);
	}

	private void transferQtyFromVHUToTargetHU(final I_M_ShipmentSchedule sched, final Quantity qtyToTransfer, final I_M_HU fromVHU)
	{
		final I_M_HU targetHU = getTargetHU();
		if (targetHU == null)
		{
			return;
		}

		final HUListAllocationSourceDestination source = HUListAllocationSourceDestination.of(fromVHU);
		final HUListAllocationSourceDestination destination = HUListAllocationSourceDestination.of(targetHU);
		final IAllocationRequest request = createShipmentScheduleAllocationRequest(sched, qtyToTransfer);

		HULoader.of(source, destination)
				.setAllowPartialUnloads(false)
				.setAllowPartialLoads(true)
				.load(request);
	}

	private final IAllocationRequest createShipmentScheduleAllocationRequest(
			final I_M_ShipmentSchedule sched,
			final Quantity qty)
	{
		// Force Qty Allocation: we need to allocate even if the HU is full
		// see http://dewiki908/mediawiki/index.php/05706_Meldung_im_Aktuellen_UAT%3F_Sollte_schon_weg_sein%2C_oder%3F_%28105871951705%29
		final boolean forceQtyAllocation = true;

		final IHUContext huContext = getHUContext();
		final ProductId productId = ProductId.ofRepoId(sched.getM_Product_ID());
		final IAllocationRequest request = AllocationUtils.createQtyRequest(huContext,
				productId,
				qty,
				huContext.getDate(), // date
				sched, // referenceModel,
				forceQtyAllocation);

		return request;
	}

	private void allocateVHU(@NonNull final I_M_HU vhu)
	{
		// Make sure we have remaining qty to pack
		if (!hasRemainingQtyToPack())
		{
			return;
		}

		final ProductId productId = getProductId();
		final IProductStorage vhuProductStorage = getProductStorage(vhu, productId);
		if (vhuProductStorage == null)
		{
			return;
		}

		final Quantity qtyToPackSrc = Quantity.of(
				vhuProductStorage.getQty(),
				vhuProductStorage.getC_UOM());

		final I_C_UOM qtyToPackUOM = itemToPack.getC_UOM();
		Quantity qtyToPack = uomConversionBL.convertQuantityTo(qtyToPackSrc, UOMConversionContext.of(productId), qtyToPackUOM);

		//
		qtyToPack = adjustQtyToPackConsideringRemaining(qtyToPack);
		if (qtyToPack.signum() <= 0)
		{
			// nothing to pack here
			return;
		}

		//
		// Pack the qtyToPack from VHU,
		// back allocate it to current shipment schedules,
		// and if configured, transfer those back-allocated quantities to the Target HU.
		packItem(
				qtyToPack,
				Predicates.alwaysTrue(),
				packedItem -> {
					for (final I_M_ShipmentSchedule sched : packedItem.getShipmentSchedules())
					{
						final Quantity schedQty = packedItem.getQtyForSched(sched); // qty to pack, available on current shipment schedule
						if (!allowOverDelivery)
						{
							validateQtyPicked(sched, schedQty);
						}

						if (schedQty.signum() != 0)
						{
							// gh #1712: only create M_ShipmentSchedule_QtyPicked etc etc for 'sched' if there is an actual quantity.
							onQtyAllocated(sched, schedQty, vhu);
						}
					}
				});
	}

	private void validateQtyPicked(final I_M_ShipmentSchedule schedule, final Quantity qtyPickCandidate)
	{
		final BigDecimal currentQtyToDeliver = schedule.getQtyToDeliver();

		if (currentQtyToDeliver.compareTo(qtyPickCandidate.getAsBigDecimal()) < 0)
		{
			throw new AdempiereException("@" + PickingConfigRepository.MSG_WEBUI_Picking_OverdeliveryNotAllowed + "@");
		}
	}

	/**
	 * From <code>itemToPack</code> take out the <code>qtyToPack</code>, create a new packed item for that qty and send it to <code>itemPackedProcessor</code>.
	 */
	private void packItem(
			@NonNull final Quantity qtyToPack,
			@NonNull final Predicate<I_M_ShipmentSchedule> shipmentSchedulesFilter,
			@NonNull final Consumer<IFreshPackingItem> packedItemConsumer)
	{
		//
		// Remove the itemToPack from unpacked items
		// NOTE: If there will be remaining qty, a NEW item with remaining Qty will be added to unpacked items
		packingItems.removeUnpackedItem(itemToPack);

		//
		// Pack our "itemToPack": it will be splitted into 2 items as follows
		// => itemToPackRemaining will be added back to unpacked items
		// => itemPacked will be added to packed items

		final IFreshPackingItem itemToPackRemaining = itemToPack.copy();
		final IFreshPackingItem itemPacked = itemToPackRemaining.subtractToPackingItem(qtyToPack, shipmentSchedulesFilter);

		//
		// Process our packed item
		packedItemConsumer.accept(itemPacked);

		//
		// Add our itemPacked to packed items
		// If an existing matching packed item will be found, our item will be merged there
		// If not, it will be added as a new packed item
		packingItems.appendPackedItem(packedItemsSlot, itemPacked);

		//
		// Update back "itemToPack" to have a up2date version
		// NOTE: so far we worked on a copy (to avoid inconsistencies in case an exception is thrown in the middle)
		itemToPack.setSchedules(itemToPackRemaining);

		//
		// If there was a remaining qty in "itemToPack" then add it back to unpacked items
		// NOTE: we keep the old object instead of adding "itemToPackRemaining" because if we are not doing like this then subsequent calls to this method, using the same itemToPack will fail
		if (itemToPack.getQtySum().signum() != 0)
		{
			packingItems.addUnpackedItem(itemToPack);
		}
	}

	private final void transferRemainingQtyToTargetHU()
	{
		if (!isQtyToPackEnforced())
		{
			return;
		}

		final Quantity qtyToPack = getQtyToPackRemaining();
		if (qtyToPack.signum() <= 0)
		{
			return;
		}

		packItem(
				qtyToPack,
				this::isForceDelivery,
				this::transferQtyToTargetHU);
	}

	private boolean isForceDelivery(final I_M_ShipmentSchedule shipmentSchedule)
	{
		// We are accepting only those shipment schedules which have Force Delivery
		final String deliveryRule = shipmentScheduleEffectiveBL.getDeliveryRule(shipmentSchedule);
		return X_M_ShipmentSchedule.DELIVERYRULE_Force.equals(deliveryRule);
	}

	private void transferQtyToTargetHU(final IFreshPackingItem item)
	{
		item.getQtys().forEach(this::transferQtyToTargetHU);
	}

	private void transferQtyToTargetHU(final I_M_ShipmentSchedule schedule, final Quantity qty)
	{
		//
		// Allocation Request
		final IAllocationRequest request = createShipmentScheduleAllocationRequest(schedule, qty);

		//
		// Allocation Source
		final ShipmentScheduleQtyPickedProductStorage shipmentScheduleQtyPickedStorage = new ShipmentScheduleQtyPickedProductStorage(schedule);
		final GenericAllocationSourceDestination source = new GenericAllocationSourceDestination(shipmentScheduleQtyPickedStorage, schedule);

		//
		// Allocation Destination
		final I_M_HU targetHU = getTargetHU();
		final HUListAllocationSourceDestination destination = HUListAllocationSourceDestination.of(targetHU);

		//
		// Move Qty from shipment schedules to current HU
		final IAllocationResult result = HULoader.of(source, destination)
				.load(request);

		// Make sure result is completed
		// NOTE: this shall not happen because "forceQtyAllocation" is set to true
		if (!result.isCompleted())
		{
			final String errmsg = MessageFormat.format(ERR_CANNOT_FULLY_LOAD, targetHU);
			throw new AdempiereException(errmsg);
		}
	}

	//
	//
	//
	//
	//
	public static class HU2PackingItemsAllocatorBuilder
	{
		public void allocate()
		{
			build().allocate();
		}
	}
}
