
DROP FUNCTION X_MRP_ProductInfo_AttributeVal_V(IN DateAt date) ;
CREATE OR REPLACE FUNCTION X_MRP_ProductInfo_AttributeVal_V(IN DateAt date) 
RETURNS TABLE(
	ad_client_id numeric, ad_org_id numeric, m_product_id numeric, name text, 
	value text, ispurchased character(1), issold character(1), m_product_category_id numeric, isactive character(1),
	DateGeneral date,
	GroupName text,
	qtyreserved_ondate numeric, 
	qtyordered_ondate numeric, 
	qtymaterialentnahme numeric, 
	fresh_qtyonhand_ondate numeric, 
	"fresh_qtyonhand_ondate_stö2" numeric, 
	fresh_qtyonhand_ondate_ind9 numeric, 
	fresh_qtypromised numeric, 
	fresh_qtymrp numeric
) AS
$BODY$
SELECT
	ad_client_id, ad_org_id, m_product_id, name, 
	value, ispurchased, issold, m_product_category_id, isactive,
	DateGeneral,
	GroupName,
	SUM(qtyreserved_ondate) AS qtyreserved_ondate, 
	SUM(qtyordered_ondate) AS qtyordered_ondate, 
	SUM(qtymaterialentnahme) AS qtymaterialentnahme, 
	SUM(fresh_qtyonhand_ondate) AS fresh_qtyonhand_ondate, 
	SUM("fresh_qtyonhand_ondate_stö2") AS "fresh_qtyonhand_ondate_stö2", 
	SUM(fresh_qtyonhand_ondate_ind9) AS fresh_qtyonhand_ondate_ind9, 
	SUM(fresh_qtypromised) AS fresh_qtypromised, 
	SUM(fresh_qtymrp) AS fresh_qtymrp
FROM (
	SELECT *
	FROM "de.metas.fresh".X_MRP_ProductInfo_AttributeVal_Raw_V
	WHERE DateGeneral=$1
	UNION ALL
	SELECT DISTINCT
		p.ad_client_id, p.ad_org_id, p.m_product_id, p.name, 
		p.value, p.ispurchased, p.issold, p.m_product_category_id, p.isactive,
		$1::date,
		dim.GroupName,
		0::numeric,
		0::numeric,
		0::numeric,
		0::numeric,
		0::numeric,
		0::numeric,
		0::numeric,
		0::numeric		
	FROM "de.metas.dimension".DIM_Dimension_Spec_Attribute_AllValues dim
		JOIN M_Product p ON true
	WHERE true
		AND dim.InternalName='MRP_Product_Info_ASI_Values'
		/*
		AND NOT EXISTS (
			SELECT 1 
			FROM X_MRP_ProductInfo_Detail_MV mv
			WHERE true
				AND mv.M_Product_ID=p.M_Product_ID
				AND mv.DateGeneral=$1
				AND (
						-- match on ASIKey containing ValueName as substring
						(mv.ASIKey ILIKE '%'||dim.ValueName||'%')
						-- or match on 'DIM_EMPTY' and the absence of any other match 
						OR (dim.ValueName='DIM_EMPTY' AND NOT EXISTS (select 1 from "de.metas.dimension".DIM_Dimension_Spec_Attribute_AllValues dim2 where dim2.InternalName=dim.InternalName and mv.ASIKey ILIKE '%'||dim2.ValueName||'%'))
					)
		)
		*/
) data
GROUP BY
	ad_client_id, ad_org_id, m_product_id, name, 
	value, ispurchased, issold, m_product_category_id, isactive,
	DateGeneral,
	GroupName
;
$BODY$
LANGUAGE sql STABLE;
COMMENT ON FUNCTION X_MRP_ProductInfo_AttributeVal_V(date) IS 'This function is a union of X_MRP_ProductInfo_AttributeVal_V_Raw and the dimension spec''s attribute values that do *not* have matching X_MRP_ProductInfo_Detail_MV records for the given date';
