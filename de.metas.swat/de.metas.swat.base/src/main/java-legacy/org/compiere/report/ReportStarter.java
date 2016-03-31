/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *                      *
 *****************************************************************************/
package org.compiere.report;

import java.io.File;
import java.util.Properties;

import org.adempiere.ad.service.ITaskExecutorService;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_PInstance;
import org.compiere.process.ClientProcess;
import org.compiere.process.ProcessCall;
import org.compiere.process.ProcessInfo;
import org.compiere.report.IJasperServiceRegistry.ServiceType;
import org.compiere.util.DB;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;

import de.metas.adempiere.form.IClientUI;
import de.metas.adempiere.model.I_AD_Process;
import de.metas.adempiere.report.jasper.OutputType;
import de.metas.adempiere.report.jasper.client.JRClient;
import de.metas.logging.LogManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * Jasper Report Process: this process is used on all AD_Processes which are about creating jasper reports.
 * 
 * @author rlemeill originaly coming from an application note from compiere.co.uk --- Modifications: Allow Jasper
 *         Reports to be able to be run on VPN profile (i.e: no direct connection to DB). Implemented ClientProcess for
 *         it to run on client.
 * @author Ashley Ramdass
 * @author tsa
 */
public class ReportStarter implements ProcessCall, ClientProcess
{
	// services
	private static final Logger log = LogManager.getLogger(ReportStarter.class);
	private static JRViewerProvider viewerProvider = new SwingJRViewerProvider();

	/**
	 * Start jasper reporting process. Based on {@link ProcessInfo#isPrintPreview()}, it will:
	 * <ul>
	 * <li>directly print the report
	 * <li>will open the report viewer and it will display the report
	 * </ul>
	 */
	@Override
	public final boolean startProcess(final Properties ctx, final ProcessInfo pi, final ITrx trx)
	{
		final ReportPrintingInfo reportPrintingInfo = extractReportPrintingInfo(ctx, pi);
		
		String errorMsg = null;
		try
		{
			//
			// Create report and print it directly
			if (!reportPrintingInfo.isPrintPreview())
			{
				// task 08283: direct print can be done in background; no need to let the user wait for this
				Services.get(ITaskExecutorService.class).submit(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							log.info("Doing direct print without preview: {}", reportPrintingInfo);
							startProcessDirectPrint(reportPrintingInfo);
						}
						catch (final Exception e)
						{
							reportResult(pi.getAD_PInstance_ID(), e.getLocalizedMessage(), ITrx.TRXNAME_None);
							Services.get(IClientUI.class).warn(pi.getWindowNo(), e);
						}
					}
				},
				ReportStarter.class.getSimpleName());
			}
			//
			// Create report and preview 
			else
			{
				startProcessPrintPreview(reportPrintingInfo);
			}
		}
		catch (final Exception e)
		{
			errorMsg = e.getLocalizedMessage();
			if(errorMsg == null || errorMsg.length() < 4)
			{
				errorMsg = e.toString();
			}
				
			log.error("Error while running the report: {}", errorMsg);
			throw AdempiereException.wrapIfNeeded(e);
		}
		finally
		{
			final String trxName = trx == null ? ITrx.TRXNAME_None : trx.getTrxName();
			reportResult(pi.getAD_PInstance_ID(), errorMsg, trxName);
		}
		return true;
	}

	private void startProcessDirectPrint(final ReportPrintingInfo reportPrintingInfo)
	{
		final Properties ctx = reportPrintingInfo.getCtx();
		final ProcessInfo pi = reportPrintingInfo.getProcessInfo();
		final JRClient jrClient = JRClient.get();
		final JasperPrint jasperPrint = jrClient.createJasperPrint(ctx, pi);
		log.info("ReportStarter.startProcess print report: {}", jasperPrint.getName());

		//
		// 08284: if we work without preview, use the mass printing framework
		final IJasperServiceRegistry jasperServiceFactory = Services.get(IJasperServiceRegistry.class);
		final IJasperService jasperService = jasperServiceFactory.getJasperService(ServiceType.MASS_PRINT_FRAMEWORK);
		final boolean displayPrintDialog = false;
		jasperService.print(jasperPrint, pi, displayPrintDialog);
	}
	
	private void startProcessPrintPreview(final ReportPrintingInfo reportPrintingInfo) throws Exception
	{
		final Properties ctx = reportPrintingInfo.getCtx();
		final ProcessInfo pi = reportPrintingInfo.getProcessInfo();
		
		//
		// Jasper reporting
		if (ReportingSystemType.Jasper == reportPrintingInfo.getReportingSystemType())
		{
			final String title = pi.getTitle();
			final JRViewerProvider viewerLauncher = getReportViewerProvider();
			final OutputType outputType = viewerLauncher.getDesiredOutputType();
			log.info("ReportStarter.startProcess run report - {}, outputType={}", new Object[] { title, outputType });

			final JRClient jrClient = JRClient.get();
			final byte[] data = jrClient.report(ctx, pi, outputType);
			viewerLauncher.openViewer(data, outputType, title, pi);
		}
		//
		// Excel reporting
		else if (ReportingSystemType.Excel == reportPrintingInfo.getReportingSystemType())
		{
			final OutputType outputType = OutputType.XLS;
			final JRClient jrClient = JRClient.get();
			final byte[] data = jrClient.report(ctx, pi, outputType);

			final File file = File.createTempFile("report_", "."+outputType.getFileExtension());
			Files.write(data, file);
			Services.get(IClientUI.class).showURL(file.toURI().toString());
		}
		else
		{
			throw new AdempiereException("Unknown " + ReportingSystemType.class + ": " + reportPrintingInfo.getReportingSystemType());
		}
	}

	/**
	 * Update {@link I_AD_PInstance} result
	 */
	private void reportResult(final int AD_PInstance_ID, final String errMsg, final String trxName)
	{
		final int result = errMsg == null ? 1 : 0;
		final String sql = "UPDATE AD_PInstance SET result=?, errormsg=? WHERE AD_PInstance_ID=?";
		final Object[] sqlParams = new Object[] { result, errMsg, AD_PInstance_ID };
		try
		{
			DB.executeUpdateEx(sql, sqlParams, trxName);
		}
		catch (final Exception e)
		{
			log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Set jasper report viewer provider.
	 *
	 * @param provider
	 */
	public static void setReportViewerProvider(final JRViewerProvider provider)
	{
		Check.assumeNotNull(provider, "provider not null");
		viewerProvider = provider;
	}

	/**
	 * Get the current jasper report viewer provider
	 *
	 * @return {@link JRViewerProvider}; never returns null
	 */
	public static JRViewerProvider getReportViewerProvider()
	{
		return viewerProvider;
	}
	
	private ReportPrintingInfo extractReportPrintingInfo(Properties ctx, ProcessInfo pi)
	{
		final ReportPrintingInfo info = new ReportPrintingInfo();
		info.setCtx(ctx);
		info.setProcessInfo(pi);
		info.setPrintPreview(pi.isPrintPreview());

		//
		// Determine the ReportingSystem type based on report template file extension
		// TODO: make it more general and centralized with the other reporting code
		final I_AD_Process process = InterfaceWrapperHelper.create(ctx, pi.getAD_Process_ID(), I_AD_Process.class, ITrx.TRXNAME_None);
		final String reportTemplate = process.getJasperReport();
		final String reportFileExtension = Files.getFileExtension(reportTemplate);
		if ("jasper".equalsIgnoreCase(reportFileExtension)
				|| "jrxml".equalsIgnoreCase(reportFileExtension))
		{
			info.setReportingSystemType(ReportingSystemType.Jasper);
		}
		else if ("xls".equalsIgnoreCase(reportFileExtension))
		{
			info.setReportingSystemType(ReportingSystemType.Excel);
			info.setPrintPreview(true); // TODO: atm only print preview is supported
		}

		return info;
	}
	
	private static enum ReportingSystemType
	{
		Jasper,
		Excel,
	};
	
	private static class ReportPrintingInfo
	{
		private Properties ctx;
		private ProcessInfo processInfo;
		private ReportingSystemType reportingSystemType;
		private boolean printPreview;
		
		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this)
					.add("reportingSystemType", reportingSystemType)
					.add("printPreview", printPreview)
					.add("processInfo", processInfo)
					.toString();
		}
		
		public void setCtx(Properties ctx)
		{
			this.ctx = ctx;
		}
		
		public Properties getCtx()
		{
			return ctx;
		}
		
		public void setProcessInfo(ProcessInfo processInfo)
		{
			this.processInfo = processInfo;
		}
		
		public ProcessInfo getProcessInfo()
		{
			return processInfo;
		}
		
		public void setReportingSystemType(ReportingSystemType reportingSystemType)
		{
			this.reportingSystemType = reportingSystemType;
		}
		
		public ReportingSystemType getReportingSystemType()
		{
			return reportingSystemType;
		}
		
		public void setPrintPreview(boolean printPreview)
		{
			this.printPreview = printPreview;
		}
		
		public boolean isPrintPreview()
		{
			return printPreview;
		}
	}
}
