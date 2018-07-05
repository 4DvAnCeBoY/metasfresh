/** Generated Model - DO NOT CHANGE */
package de.metas.vertical.pharma.msv3.server.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for MSV3_Server
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_MSV3_Server extends org.compiere.model.PO implements I_MSV3_Server, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = -871631164L;

    /** Standard Constructor */
    public X_MSV3_Server (Properties ctx, int MSV3_Server_ID, String trxName)
    {
      super (ctx, MSV3_Server_ID, trxName);
      /** if (MSV3_Server_ID == 0)
        {
			setFixedQtyAvailableToPromise (BigDecimal.ZERO);
			setM_Warehouse_PickingGroup_ID (0);
			setMSV3_Server_ID (0);
        } */
    }

    /** Load Constructor */
    public X_MSV3_Server (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }


    /** Load Meta Data */
    @Override
    protected org.compiere.model.POInfo initPO (Properties ctx)
    {
      org.compiere.model.POInfo poi = org.compiere.model.POInfo.getPOInfo (ctx, Table_Name, get_TrxName());
      return poi;
    }

	/** Set Konst. Zusagbar (ATP) Wert.
		@param FixedQtyAvailableToPromise Konst. Zusagbar (ATP) Wert	  */
	@Override
	public void setFixedQtyAvailableToPromise (java.math.BigDecimal FixedQtyAvailableToPromise)
	{
		set_Value (COLUMNNAME_FixedQtyAvailableToPromise, FixedQtyAvailableToPromise);
	}

	/** Get Konst. Zusagbar (ATP) Wert.
		@return Konst. Zusagbar (ATP) Wert	  */
	@Override
	public java.math.BigDecimal getFixedQtyAvailableToPromise () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_FixedQtyAvailableToPromise);
		if (bd == null)
			 return BigDecimal.ZERO;
		return bd;
	}

	@Override
	public org.compiere.model.I_M_Warehouse_PickingGroup getM_Warehouse_PickingGroup() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_M_Warehouse_PickingGroup_ID, org.compiere.model.I_M_Warehouse_PickingGroup.class);
	}

	@Override
	public void setM_Warehouse_PickingGroup(org.compiere.model.I_M_Warehouse_PickingGroup M_Warehouse_PickingGroup)
	{
		set_ValueFromPO(COLUMNNAME_M_Warehouse_PickingGroup_ID, org.compiere.model.I_M_Warehouse_PickingGroup.class, M_Warehouse_PickingGroup);
	}

	/** Set Warehouse Picking Group.
		@param M_Warehouse_PickingGroup_ID Warehouse Picking Group	  */
	@Override
	public void setM_Warehouse_PickingGroup_ID (int M_Warehouse_PickingGroup_ID)
	{
		if (M_Warehouse_PickingGroup_ID < 1) 
			set_Value (COLUMNNAME_M_Warehouse_PickingGroup_ID, null);
		else 
			set_Value (COLUMNNAME_M_Warehouse_PickingGroup_ID, Integer.valueOf(M_Warehouse_PickingGroup_ID));
	}

	/** Get Warehouse Picking Group.
		@return Warehouse Picking Group	  */
	@Override
	public int getM_Warehouse_PickingGroup_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_PickingGroup_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set MSV3 Server.
		@param MSV3_Server_ID MSV3 Server	  */
	@Override
	public void setMSV3_Server_ID (int MSV3_Server_ID)
	{
		if (MSV3_Server_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_MSV3_Server_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_MSV3_Server_ID, Integer.valueOf(MSV3_Server_ID));
	}

	/** Get MSV3 Server.
		@return MSV3 Server	  */
	@Override
	public int getMSV3_Server_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_MSV3_Server_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}