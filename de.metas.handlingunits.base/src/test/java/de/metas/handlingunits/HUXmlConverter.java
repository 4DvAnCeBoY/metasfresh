package de.metas.handlingunits;

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


import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.adempiere.ad.wrapper.POJOLookupMap;
import org.adempiere.ad.wrapper.POJOWrapper;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.junit.Ignore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.metas.adempiere.model.I_M_Product;
import de.metas.handlingunits.attribute.IHUAttributesDAO;
import de.metas.handlingunits.attribute.impl.HUAttributesDAO;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Attribute;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_Item_Storage;
import de.metas.handlingunits.model.I_M_HU_Storage;
import de.metas.handlingunits.storage.IHUStorageDAO;
import de.metas.handlingunits.storage.IHUStorageFactory;

/**
 * Helper used to convert HUs to XML to ease validation
 *
 * @author tsa
 *
 */
@Ignore
public class HUXmlConverter
{
	private static final Map<String, String> tableName2tagName = new HashMap<String, String>();
	static
	{
		HUXmlConverter.tableName2tagName.put(I_M_HU_Item.Table_Name, "Item");
		HUXmlConverter.tableName2tagName.put(I_M_HU_Storage.Table_Name, "Storage");
		HUXmlConverter.tableName2tagName.put(I_M_HU_Item_Storage.Table_Name, "Storage");
	}

	public static Node toXml(final I_M_HU hu)
	{
		final HUXmlConverter converter = new HUXmlConverter();
		final Document document = converter.document;
		converter.createNode(document, hu);
		return document;
	}

	public static Node toXml(final String nodeName, final List<I_M_HU> hus)
	{
		final HUXmlConverter converter = new HUXmlConverter();
		final Document document = converter.document;

		final Node node = document.createElement(nodeName);
		document.appendChild(node);

		for (final I_M_HU hu : hus)
		{
			converter.createNode(node, hu);
		}
		return document;
	}

	private final Document document;

	public HUXmlConverter()
	{
		document = HUXmlConverter.newDocument();
	}

	private Node createNode(final Node parentNode, final I_M_HU hu)
	{
		final String name = "HU-" + hu.getM_HU_PI_Version().getM_HU_PI().getName();
		final Node node = createNodeFromModel(parentNode, name, hu);

		//
		// HU Attributes
		final IHUAttributesDAO huAttributesDAO = HUAttributesDAO.instance;
		final List<I_M_HU_Attribute> attrs = huAttributesDAO.retrieveAttributesOrdered(hu);
		for (final I_M_HU_Attribute attr : attrs)
		{
			createNodeFromModel(node, attr);
		}

		//
		// HU Items
		final List<I_M_HU_Item> items = Services.get(IHandlingUnitsDAO.class).retrieveItems(hu);
		for (final I_M_HU_Item item : items)
		{
			createNode(node, item);
		}

		//
		// HU Storage
		final IHUStorageFactory storageFactory = Services.get(IHandlingUnitsBL.class).getStorageFactory();
		final IHUStorageDAO storageDAO = storageFactory.getHUStorageDAO();
		final List<I_M_HU_Storage> storages = storageDAO.retrieveStorages(hu);
		for (final I_M_HU_Storage storage : storages)
		{
			createNodeFromModel(node, storage);
		}

		return node;
	}

	private Node createNode(final Node parentNode, final I_M_HU_Item item)
	{
		final Node node = createNodeFromModel(parentNode, item);

		//
		// Item level storage
		final IHUStorageDAO dao = Services.get(IHandlingUnitsBL.class).getStorageFactory().getHUStorageDAO();
		final List<I_M_HU_Item_Storage> itemStorages = dao.retrieveItemStorages(item);
		for (final I_M_HU_Item_Storage itemStorage : itemStorages)
		{
			createNodeFromModel(node, itemStorage);
		}

		//
		// Included HUs
		final List<I_M_HU> includedHUs = Services.get(IHandlingUnitsDAO.class).retrieveIncludedHUs(item);
		for (final I_M_HU includedHU : includedHUs)
		{
			createNode(node, includedHU);
		}

		return node;
	}

	private Node createNodeFromModel(final Node parentNode, final Object model)
	{
		final String tableName = InterfaceWrapperHelper.getModelTableName(model);
		String tagName = HUXmlConverter.tableName2tagName.get(tableName);
		if (tagName == null)
		{
			tagName = tableName;
		}

		return createNodeFromModel(parentNode, tagName, model);
	}

	private Node createNodeFromModel(final Node parentNode, final String tagName, final Object model)
	{
		final POJOWrapper wrapper = POJOWrapper.getWrapper(model);
		final Map<String, Object> map = wrapper.getValuesMap();

		final Element node = document.createElement(tagName);

		for (final Map.Entry<String, Object> e : map.entrySet())
		{
			final String name = e.getKey();
			final Object value = e.getValue();
			final String valueStr;

			if (POJOWrapper.isHandled(value))
			{
				// skip included beans; only IDs are sufficient
				continue;
			}
			else
			{
				valueStr = value == null ? "" : value.toString();
			}

			node.setAttribute(name, valueStr);

			//
			// Product
			if ("M_Product_ID".equals(name) && value != null)
			{
				final int id = (Integer)value;
				final I_M_Product product = POJOLookupMap.get().lookup(I_M_Product.class, id);
				final String productValue = product == null ? "" : product.getValue();
				node.setAttribute("M_Product_Value", productValue);
			}

		}

		if (parentNode != null)
		{
			parentNode.appendChild(node);
		}

		return node;
	}

	private static Document newDocument()
	{
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try
		{
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		}
		catch (final ParserConfigurationException e)
		{
			throw new RuntimeException(e); // NOPMD by al on 7/24/13 11:15 AM
		}
		final Document document = documentBuilder.newDocument();
		return document;

	}

	public static String toString(final Node node)
	{
		final Writer writer = new StringWriter();

		// Construct Transformer Factory and Transformer
		final TransformerFactory tranFactory = TransformerFactory.newInstance();
		// String jVersion = System.getProperty("java.version");
		// if (jVersion.startsWith("1.5.0"))
		tranFactory.setAttribute("indent-number", Integer.valueOf(4));

		Transformer aTransformer;
		try
		{
			aTransformer = tranFactory.newTransformer();
		}
		catch (final TransformerConfigurationException e)
		{
			throw new AdempiereException(e);
		}
		aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
		final Source src = new DOMSource(node);

		// =================================== Write to String
		// Writer writer = new StringWriter();
		final Result dest2 = new StreamResult(writer);
		try
		{
			aTransformer.transform(src, dest2);
		}
		catch (final TransformerException e)
		{
			throw new AdempiereException(e);
		}

		return writer.toString();
	}

}
