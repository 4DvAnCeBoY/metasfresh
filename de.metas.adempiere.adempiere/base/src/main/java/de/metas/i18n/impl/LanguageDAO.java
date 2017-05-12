package de.metas.i18n.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.proxy.Cached;
import org.compiere.model.I_AD_Language;
import org.compiere.model.I_AD_Table;
import org.compiere.model.POInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.slf4j.Logger;

import de.metas.i18n.ADLanguageList;
import de.metas.i18n.ILanguageDAO;
import de.metas.logging.LogManager;
import lombok.NonNull;

public class LanguageDAO implements ILanguageDAO
{
	private static final transient Logger logger = LogManager.getLogger(LanguageDAO.class);

	private static final String SQL_retriveBaseLanguage_1P = "SELECT " + I_AD_Language.COLUMNNAME_AD_Language
			+ " FROM " + I_AD_Language.Table_Name
			+ " WHERE " + I_AD_Language.COLUMNNAME_IsBaseLanguage + "=?";

	@Override
	@Cached(cacheName = I_AD_Language.Table_Name)
	public ADLanguageList retrieveAvailableLanguages()
	{
		final IQueryBuilder<I_AD_Language> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilderOutOfTrx(I_AD_Language.class)
				.addEqualsFilter(I_AD_Language.COLUMNNAME_AD_Client_ID, Env.CTXVALUE_AD_Client_ID_System) // just to make sure, even though AD_Language's AccessLevel=4-System
				.addOnlyActiveRecordsFilter();

		// Only Base or System languages
		queryBuilder.addCompositeQueryFilter()
				.setJoinOr()
				.addEqualsFilter(I_AD_Language.COLUMNNAME_IsBaseLanguage, true)
				.addEqualsFilter(I_AD_Language.COLUMNNAME_IsSystemLanguage, true);

		// Ordered by Name
		queryBuilder.orderBy()
				.addColumn(I_AD_Language.COLUMNNAME_Name)
				.addColumn(I_AD_Language.COLUMNNAME_AD_Language);

		final ADLanguageList.Builder languages = ADLanguageList.builder();
		queryBuilder
				.create()
				.stream(I_AD_Language.class)
				.forEach(adLanguage -> languages.addLanguage(adLanguage.getAD_Language(), adLanguage.getName(), adLanguage.isBaseLanguage()));

		return languages.build();
	}

	@Override
	public String retrieveBaseLanguage()
	{
		// IMPORTANT: because this method is called right after database connection is established
		// we cannot use the Query API which is requiring MLanguage.getBaseLanguage() to be set

		final String baseADLanguage = DB.getSQLValueStringEx(ITrx.TRXNAME_None, SQL_retriveBaseLanguage_1P, true);
		Check.assumeNotEmpty(baseADLanguage, "Base AD_Language shall be defined in database");

		return baseADLanguage;
	}

	@Override
	public I_AD_Language retrieveByAD_Language(final Properties ctx, final String adLanguage)
	{
		return Services.get(IQueryBL.class)
				.createQueryBuilder(I_AD_Language.class, ctx, ITrx.TRXNAME_None)
				.addEqualsFilter(I_AD_Language.COLUMNNAME_AD_Language, adLanguage)
				.create()
				.firstOnly(I_AD_Language.class);
	}

	private List<String> retrieveTrlTableNames()
	{
		return Services.get(IQueryBL.class)
				.createQueryBuilderOutOfTrx(I_AD_Table.class)
				.addEndsWithQueryFilter(I_AD_Table.COLUMNNAME_TableName, "_Trl")
				.orderBy()
				.addColumn(I_AD_Table.COLUMNNAME_TableName)
				.endOrderBy()
				.create()
				.listDistinct(I_AD_Table.COLUMNNAME_TableName, String.class);
	}

	@Override
	public void addAllMissingTranslations()
	{
		final ADLanguageList languages = retrieveAvailableLanguages();

		final List<String> errorLanguages = new ArrayList<>();
		final List<Throwable> errorCauses = new ArrayList<>();
		for (final String adLanguage : languages.getAD_Languages())
		{
			// Skip base language
			if(languages.isBaseLanguage(adLanguage))
			{
				continue;
			}

			try
			{
				addRemoveLanguageTranslations(adLanguage, true);
			}
			catch (Exception ex)
			{
				// collect error
				errorLanguages.add(adLanguage);
				errorCauses.add(ex);
			}
		}

		// If we got some errors, build and throw an exception
		if (!errorLanguages.isEmpty())
		{
			final AdempiereException ex = new AdempiereException("Adding missing translations failed for following languages: " + errorLanguages);
			for (final Throwable cause : errorCauses)
			{
				ex.addSuppressed(cause);
			}
			throw ex;
		}
	}

	@Override
	public int addMissingTranslations(final I_AD_Language language)
	{
		final boolean add = true;
		return addRemoveLanguageTranslations(language.getAD_Language(), add);
	}

	@Override
	public int removeTranslations(final I_AD_Language language)
	{
		final boolean add = false;
		return addRemoveLanguageTranslations(language.getAD_Language(), add);
	}

	private int addRemoveLanguageTranslations(@NonNull final String adLanguage, final boolean add)
	{
		final List<String> trlTableNames = retrieveTrlTableNames();

		int retNo = 0;
		final List<String> errorTables = new ArrayList<>();
		final List<Throwable> errorCauses = new ArrayList<>();
		for (final String trlTableName : trlTableNames)
		{
			try
			{
				if (add)
				{
					retNo += addTableTranslations(trlTableName, adLanguage);
				}
				else
				{
					retNo += deleteTableTranslations(trlTableName, adLanguage);
				}
			}
			catch (final Exception ex)
			{
				// collect error
				errorTables.add(trlTableName + "(" + adLanguage + ")");
				errorCauses.add(ex);
			}
		}

		// If we got some errors, build and throw an exception
		if (!errorTables.isEmpty())
		{
			final AdempiereException ex = new AdempiereException((add ? "Adding" : "Removing") + " missing translations failed for following tables: " + errorTables);
			for (final Throwable cause : errorCauses)
			{
				ex.addSuppressed(cause);
			}
			throw ex;
		}

		return retNo;
	}

	/**
	 * Delete Translation
	 *
	 * @param trlTableName table name
	 * @param adLanguage
	 * @return number of records deleted
	 */
	private int deleteTableTranslations(final String trlTableName, final String adLanguage)
	{
		final String sql = "DELETE FROM  " + trlTableName + " WHERE AD_Language=?";
		final int no = DB.executeUpdateEx(sql, new Object[] { adLanguage }, ITrx.TRXNAME_ThreadInherited);
		logger.info("Removed {} translations for {} ({})", no, trlTableName, adLanguage);
		return no;
	}

	/**
	 * Add Translation to table
	 *
	 * @param trlTableName table name
	 * @param adLanguage
	 * @return number of records inserted
	 */
	private int addTableTranslations(final String trlTableName, final String adLanguage)
	{
		final String baseTableName = trlTableName.substring(0, trlTableName.length() - 4);
		final POInfo poInfo = POInfo.getPOInfo(baseTableName);
		if (poInfo == null)
		{
			logger.error("No POInfo found for {}", baseTableName);
			return 0;
		}

		final List<String> columns = poInfo.getTranslatedColumnNames();
		if (columns.isEmpty())
		{
			logger.error("No Columns found for {}", baseTableName);
			return 0;
		}

		final String tblAlias = "t";
		final StringBuilder cols = new StringBuilder();
		final StringBuilder colsWithAlias = new StringBuilder();
		for (int i = 0; i < columns.size(); i++)
		{
			cols.append(", ").append(columns.get(i));
			colsWithAlias.append(", ").append(tblAlias + "." + columns.get(i));
		}

		//
		// Insert Statement
		final String trlAlias = "trl";
		final int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		final String keyColumn = poInfo.getKeyColumnName();
		Check.assumeNotEmpty(keyColumn, "keyColumn not empty for {}", baseTableName); // shall not happen
		//
		final StringBuilder insertSql = new StringBuilder();
		// @formatter:off
		insertSql.append(" INSERT INTO " + trlTableName + "(")
			.append("AD_Language")
			.append(", IsTranslated")
			.append(", AD_Client_ID")
			.append(", AD_Org_ID")
			.append(", Created")
			.append(", CreatedBy")
			.append(", Updated")
			.append(", UpdatedBy")
			.append(", IsActive")
			.append(", " + keyColumn)
			.append(cols)
		.append(")")
		.append("\n SELECT ")
			.append(DB.TO_STRING(adLanguage)) // AD_Language
			.append(", ").append(DB.TO_BOOLEAN(false)) // IsTranslated
			.append(", " + tblAlias + ".AD_Client_ID") // AD_Client_ID
			.append(", " + tblAlias + ".AD_Org_ID") // AD_Org_ID
			.append(", now()") // Created
			.append(", " + AD_User_ID) // CreatedBy
			.append(", now()") // Updated
			.append(", " + AD_User_ID) // UpdatedBy
			.append(", ").append(DB.TO_BOOLEAN(true)) // IsActive
			.append(", " + tblAlias + "." + keyColumn) // KeyColumn
			.append(colsWithAlias)
		.append("\n FROM " + baseTableName + " " + tblAlias)
			.append("\n LEFT JOIN " + baseTableName + "_Trl " + trlAlias)
			.append(" ON (")
			.append(trlAlias + "." + keyColumn + " = " + tblAlias + "." + keyColumn)
			.append(" AND " + trlAlias + ".AD_Language=" + DB.TO_STRING(adLanguage))
			.append(")")
		.append("\n WHERE " + trlAlias + "." + keyColumn + " IS NULL");
		// @formatter:on

		final int no = DB.executeUpdateEx(insertSql.toString(), null, ITrx.TRXNAME_ThreadInherited);
		if (no != 0)
		{
			logger.info("Added {} missing translations for {} ({})", no, trlTableName, adLanguage);
		}
		return no;
	}
}
