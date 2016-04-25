package de.metas.flatrate.api.impl;

/*
 * #%L
 * de.metas.contracts
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


import java.math.BigDecimal;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.adempiere.ad.wrapper.POJOWrapper;
import org.compiere.util.Env;
import org.junit.Test;

import de.metas.adempiere.model.I_C_Currency;
import de.metas.adempiere.model.I_M_Product;
import de.metas.flatrate.model.I_C_Flatrate_Conditions;
import de.metas.flatrate.model.I_C_Flatrate_DataEntry;
import de.metas.flatrate.model.I_C_Flatrate_Term;
import de.metas.flatrate.model.X_C_Flatrate_Conditions;
import de.metas.flatrate.model.X_C_Flatrate_DataEntry;

public class MockedFlatrateBLTest_Old
{
	private static final int C_UOM_TERM_ID = 10;

	@Mocked
	I_C_Flatrate_DataEntry dataEntry;

	@Mocked
	I_C_Flatrate_Term term;

	@Mocked
	I_C_Currency currency;

	@Mocked
	I_C_Flatrate_Conditions conditions;


	I_M_Product flatrateProduct = POJOWrapper.create(Env.getCtx(), I_M_Product.class);

	/**
	 * Class under test, partially mocked.
	 */
	final FlatrateBL flatrateBL = new FlatrateBL();

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has the same UOM as its term</li>
	 * <li>dataEntry has type 'IP'</li>
	 * <li>dataEntry has QtyReported and ActualQty = 0</li>
	 * </ul>
	 */
	@Test
	public void updateIncompleteMainInvoiceEntry()
	{
		setupCommon();
		setupEntryWithIncompleteValues();

		entryHasUOM(C_UOM_TERM_ID);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Invoicing_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		new Verifications()
		{{
				dataEntry.setActualQtyPerUnit(BigDecimal.ZERO);

				dataEntry.setActualQtyDiffAbs(BigDecimal.ZERO);

				dataEntry.setActualQtyDiffPerUOM(new BigDecimal("-5.00"));
				dataEntry.setActualQtyDiffPercent(BigDecimal.ZERO);

				dataEntry.setFlatrateAmt(BigDecimal.ZERO);

				dataEntry.setFlatrateAmtCorr(BigDecimal.ZERO);
		}};
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has a different UOM than its term</li>
	 * <li>dataEntry has type 'IP'</li>
	 * <li>dataEntry has QtyReported and ActualQty = 0</li>
	 * </ul>
	 */
	@Test
	public void updateIncompleteAuxInvoiceEntry()
	{
		setupCommon();
		setupEntryWithIncompleteValues();

		entryHasUOM(C_UOM_TERM_ID + 1);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Invoicing_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		new Verifications()
		{
			{
				dataEntry.setActualQtyPerUnit(BigDecimal.ZERO);

				dataEntry.setActualQtyDiffAbs(null);

				dataEntry.setActualQtyDiffPerUOM(null);
				dataEntry.setActualQtyDiffPercent(null);

				dataEntry.setFlatrateAmt(null);

				dataEntry.setFlatrateAmtCorr(null);
			}
		};
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has the a the same UOM as its term</li>
	 * <li>dataEntry has type 'IP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * </ul>
	 */
	@Test
	public void updateMainInvoiceEntry()
	{
		setupCommon();
		setupEntryWithValues();

		entryHasUOM(C_UOM_TERM_ID);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Invoicing_PeriodBased);
		entryHasQty_Reported(new BigDecimal("23"));

		flatrateBL.updateEntry(dataEntry);

		verifyMainEntryWithValues();
	}

	/**
	 * Similar to {@link #updateMainInvoiceEntry()}, but  QtyActual is zero.
	 * <ul>
	 * <li>dataEntry has the a the same UOM as its term</li>
	 * <li>dataEntry has type 'IP'</li>
	 * <li>dataEntry has QtyReported and ActualQty = 0</li>
	 * </ul>
	 */
	@Test
	public void updateMainInvoiceEntryWithActualQtyZero()
	{
		setupCommon();
		setupEntryWithValuesAndQtyActualZero();

		entryHasUOM(C_UOM_TERM_ID);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Invoicing_PeriodBased);
		entryHasQty_Reported(new BigDecimal("23"));

		flatrateBL.updateEntry(dataEntry);

		new Verifications()
		{{
				// Istmenge: 0 Stück pro Pflegetag
				dataEntry.setActualQtyPerUnit(BigDecimal.ZERO);

				dataEntry.setFlatrateAmt(new BigDecimal("46.00"));
				dataEntry.setFlatrateAmtPerUOM(new BigDecimal("2"));

				// Unterschreitung: 115 (23*5)
				dataEntry.setActualQtyDiffAbs(new BigDecimal("-115"));

				// Überschreitung pro Pflegetag: 5 Stück oder 5/5=100%
				dataEntry.setActualQtyDiffPerUOM(new BigDecimal("-5.00"));
				dataEntry.setActualQtyDiffPercent(new BigDecimal("-100.00"));

				// Nachzahlung: 2€ * 23 * 0,95 = 43,70€
				dataEntry.setFlatrateAmtCorr(new BigDecimal("-43.70"));
		}};
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has a different UOM than its term</li>
	 * <li>dataEntry has type 'IP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * </ul>
	 */
	@Test
	public void updateAuxInvoiceEntry()
	{
		setupCommon();
		setupEntryWithValues();

		entryHasUOM(C_UOM_TERM_ID + 1);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Invoicing_PeriodBased);
		entryHasQty_Reported(new BigDecimal("23"));

		flatrateBL.updateEntry(dataEntry);

		verifyAuxEntryWithValues();
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has the same UOM as its term</li>
	 * <li>dataEntry has type 'CP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * </ul>
	 */
	@Test
	public void updateMainCorrectionEntry()
	{
		setupCommon();
		setupEntryWithValues();
		conditionsHaveCorrectionAmtAtClosing(true);

		// the method under test should work with the different of these two values (i.e. 23)
		// therefore, we can use the same verifications that we already made for other tests.
		entryHasQty_Planned(new BigDecimal("20"));
		entryHasQty_Reported(new BigDecimal("43"));

		entryHasUOM(C_UOM_TERM_ID);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Correction_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		verifyMainEntryWithValues();
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has a different UOM than its term</li>
	 * <li>dataEntry has type 'CP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * <li>the flatrate conditions have IsCorrectionAmtAtClosing='Y'</li>
	 * </ul>
	 */
	@Test
	public void updateAuxCorrectionEntry()
	{
		setupCommon();
		setupEntryWithValues();
		conditionsHaveCorrectionAmtAtClosing(true);

		// the method under test should work with the different of these two values (i.e. 23)
		// therefore, we can use the same verifications that we already made for other tests.
		entryHasQty_Planned(new BigDecimal("20"));
		entryHasQty_Reported(new BigDecimal("43"));

		entryHasUOM(C_UOM_TERM_ID + 1);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Correction_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		verifyAuxEntryWithValues();
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has the same UOM as its term</li>
	 * <li>dataEntry has type 'CP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * <li>the flatrate conditions have IsCorrectionAmtAtClosing='N'</li>
	 * </ul>
	 */
	@Test
	public void updateMainCorrectionEntryWithoutActAmt()
	{
		setupCommon();
		setupEntryWithValues();
		conditionsHaveCorrectionAmtAtClosing(false);

		// the method under test should work with the different of these two values (i.e. 23)
		// therefore, we can use the same verifications that we already made for other tests.
		entryHasQty_Planned(new BigDecimal("20"));
		entryHasQty_Reported(new BigDecimal("43"));

		entryHasUOM(C_UOM_TERM_ID);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Correction_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		new Verifications()
		{
			{
				// Istmenge: 6 Stück pro Pflegetag
				dataEntry.setActualQtyPerUnit(new BigDecimal("6.00"));

				dataEntry.setFlatrateAmt(new BigDecimal("46.00"));
				dataEntry.setFlatrateAmtPerUOM(new BigDecimal("2"));

				// Überschreitung: 23
				dataEntry.setActualQtyDiffAbs(null);

				// Überschreitung pro Pflegetag: 1 Stück oder 1/5=20%
				dataEntry.setActualQtyDiffPerUOM(null);
				dataEntry.setActualQtyDiffPercent(null);

				// Nachzahlung: 2€ * 23 * 0,15 = 6,9€
				dataEntry.setFlatrateAmtCorr(null);
			}
		};
	}

	/**
	 * Calls {@link FlatrateBL#updateEntry(I_C_Flatrate_DataEntry)} with these conditions
	 * <ul>
	 * <li>dataEntry has a different UOM than its term</li>
	 * <li>dataEntry has type 'CP'</li>
	 * <li>dataEntry has QtyReported and ActualQty > 0</li>
	 * <li>the flatrate conditions have IsCorrectionAmtAtClosing='N'</li>
	 * </ul>
	 */
	@Test
	public void updateAuxCorrectionEntryWithoutActAmt()
	{
		setupCommon();
		setupEntryWithValues();
		conditionsHaveCorrectionAmtAtClosing(false);

		// the method under test should work with the different of these two values (i.e. 23)
		// therefore, we can use the same verifications that we already made for other tests.
		entryHasQty_Planned(new BigDecimal("20"));
		entryHasQty_Reported(new BigDecimal("43"));

		entryHasUOM(C_UOM_TERM_ID + 1);
		entryHasType(X_C_Flatrate_DataEntry.TYPE_Correction_PeriodBased);

		flatrateBL.updateEntry(dataEntry);

		new Verifications()
		{
			{
				// Istmenge: 6 Stück pro Pflegetag
				dataEntry.setActualQtyPerUnit(new BigDecimal("6.00"));

				dataEntry.setFlatrateAmt(null);
				dataEntry.setFlatrateAmtPerUOM(null);

				// Überschreitung: 23
				dataEntry.setActualQtyDiffAbs(null);

				// Überschreitung pro Pflegetag: 1 Stück oder 1/5=20%
				dataEntry.setActualQtyDiffPerUOM(null);
				dataEntry.setActualQtyDiffPercent(null);

				// Nachzahlung: 2€ * 23 * 0,15 = 6,9€
				dataEntry.setFlatrateAmtCorr(null);
			}
		};
	}

	private void verifyMainEntryWithValues()
	{
		new Verifications()
		{{
				// Istmenge: 6 Stück pro Pflegetag
				dataEntry.setActualQtyPerUnit(new BigDecimal("6.00"));

				dataEntry.setFlatrateAmt(new BigDecimal("46.00"));
				dataEntry.setFlatrateAmtPerUOM(new BigDecimal("2"));

				// Überschreitung: 23
				dataEntry.setActualQtyDiffAbs(new BigDecimal("23"));

				// Überschreitung pro Pflegetag: 1 Stück oder 1/5=20%
				dataEntry.setActualQtyDiffPerUOM(new BigDecimal("1.00"));
				dataEntry.setActualQtyDiffPercent(new BigDecimal("20.00"));

				// Nachzahlung: 2€ * 23 * 0,15 = 6,9€
				dataEntry.setFlatrateAmtCorr(new BigDecimal("6.90"));
		}};
	}

	private void verifyAuxEntryWithValues()
	{
		new Verifications()
		{
			{
				// Istmenge: 6 Stück pro Pflegetag
				dataEntry.setActualQtyPerUnit(new BigDecimal("6.00"));

				dataEntry.setFlatrateAmtPerUOM(null);
				dataEntry.setFlatrateAmt(null);

				// Überschreitung: 23
				dataEntry.setActualQtyDiffAbs(null);

				// Überschreitung pro Pflegetag: 1 Stück oder 1/5=20%
				dataEntry.setActualQtyDiffPerUOM(null);
				dataEntry.setActualQtyDiffPercent(null);

				// Nachzahlung: 2€ * 23 * 0,15 = 6,9€
				dataEntry.setFlatrateAmtCorr(null);
			}
		};
	}

	private void entryHasUOM(final int C_UOM_ID)
	{
		new NonStrictExpectations()
		{
			{
				dataEntry.getC_UOM_ID();
				result = C_UOM_ID;
			}
		};
	}

	private void entryHasQty_Reported(final BigDecimal qtyReported)
	{
		new NonStrictExpectations()
		{
			{
				dataEntry.getQty_Reported();
				result = qtyReported;
			}
		};
	}

	private void entryHasQty_Planned(final BigDecimal qtyReported)
	{
		new NonStrictExpectations()
		{
			{
				dataEntry.getQty_Planned();
				result = qtyReported;
			}
		};
	}

	private void entryHasType(final String type)
	{
		new NonStrictExpectations()
		{
			{
				dataEntry.getType();
				result = type;
			}
		};
	}

	private void conditionsHaveCorrectionAmtAtClosing(final boolean value)
	{
		new NonStrictExpectations()
		{{
				conditions.isCorrectionAmtAtClosing();
				result = value;
		}};
	}

	private void setupCommon()
	{
		new NonStrictExpectations()
		{{
				dataEntry.getC_Flatrate_Term();
				result = term;

				term.getC_UOM_ID();
				result = C_UOM_TERM_ID;

				term.getC_Flatrate_Conditions();
				result = conditions;

				conditions.getType_Flatrate();
				result = X_C_Flatrate_Conditions.TYPE_FLATRATE_Korridor;

				conditions.getM_Product_Flatrate();
				result = flatrateProduct;

				term.getC_Currency();
				result = currency;

				currency.getC_Currency_ID();
				result = 5;
		}};
	}

	private void setupEntryWithValuesAndQtyActualZero()
	{
		new NonStrictExpectations()
		{{
				dataEntry.getActualQty();
				result = BigDecimal.ZERO;
		}};
		setupEntryWithValuesCommon();
	}

	private void setupEntryWithValues()
	{
		new NonStrictExpectations()
		{{
				dataEntry.getActualQty();
				result = new BigDecimal("138");
		}};
		setupEntryWithValuesCommon();
	}

	private void setupEntryWithValuesCommon()
	{
		new NonStrictExpectations(flatrateBL)
		{{
				term.getPlannedQtyPerUnit();	result = new BigDecimal("5");

				conditions.getMargin_Max(); 	result = new BigDecimal("5");

				conditions.getMargin_Min();	    result = new BigDecimal("5");

				conditions.getType_Clearing();
				result = X_C_Flatrate_Conditions.TYPE_CLEARING_Ueber_Unterschreitung;

				conditions.getClearingAmtBaseOn();
				result = X_C_Flatrate_Conditions.CLEARINGAMTBASEON_Pauschalenpreis;

				flatrateBL.getFlatFeePricePerUnit(term, new BigDecimal("23"), dataEntry);
				result = new BigDecimal("2");

				flatrateBL.getFlatFeePricePerUnit(term, new BigDecimal("23"), dataEntry);
				result = new BigDecimal("2");
		}};
	}

	private void setupEntryWithIncompleteValues()
	{
		new NonStrictExpectations(flatrateBL)
		{{
				term.getPlannedQtyPerUnit();	result = new BigDecimal("5");

				conditions.getMargin_Max();	    result = new BigDecimal("5");

				conditions.getMargin_Min();	    result = new BigDecimal("5");

				conditions.getType_Clearing();
				result = X_C_Flatrate_Conditions.TYPE_CLEARING_Ueber_Unterschreitung;

				conditions.getClearingAmtBaseOn();
				result = X_C_Flatrate_Conditions.CLEARINGAMTBASEON_Pauschalenpreis;

				dataEntry.getQty_Reported();
				result = BigDecimal.ZERO;

				dataEntry.getActualQty();
				result = BigDecimal.ZERO;

				flatrateBL.getFlatFeePricePerUnit(term, BigDecimal.ONE, dataEntry);
				result = new BigDecimal("2");
//
//				flatrateBL.getFlatFeePricePerUnit(term, BigDecimal.ONE, dataEntry);
//				result = new BigDecimal("2");
		}};
	}
}
