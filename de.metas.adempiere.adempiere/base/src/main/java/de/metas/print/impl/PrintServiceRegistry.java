package de.metas.print.impl;

import org.adempiere.util.Check;

import de.metas.print.IPrintService;
import de.metas.print.IPrintServiceRegistry;

public class PrintServiceRegistry implements IPrintServiceRegistry
{
	private IPrintService service;

	@Override
	public IPrintService getJasperService()
	{
		Check.errorIf(service == null, "Missing IJasperService implementation");
		return service;
	}

	@Override
	public IPrintService registerJasperService(final IPrintService jasperService)
	{
		final IPrintService oldService = this.service;
		this.service = jasperService;
		return oldService;
	}

	@Override
	public boolean isServiceRegistered()
	{
		return service != null;
	}
}
