package de.metas.inoutcandidate.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import de.metas.util.ISingletonService;

/**
 * Packaging related DAO
 *
 * @author ts
 * @author tsa
 *
 * @see <a href="http://dewiki908/mediawiki/index.php/Transportverpackung_%282009_0022_G61%29">(2009_0022_G61)</a>
 */
public interface IPackagingDAO extends ISingletonService
{
	List<Packageable> retrievePackableLines(PackageableQuery query);

	/**
	 * @return The current QtyPickedPlanned (qty that was picked but not yet processed) for the given schedule if found, null otherwise
	 */
	BigDecimal retrieveQtyPickedPlannedOrNull(ShipmentScheduleId shipmentScheduleId);

	Stream<Packageable> streamAll();

	Packageable getByShipmentScheduleId(ShipmentScheduleId shipmentScheduleId);

	List<Packageable> getByShipmentScheduleIds(Collection<ShipmentScheduleId> shipmentScheduleIds);
}
