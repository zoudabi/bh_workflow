package com.binhunt.workflow.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.impl.BaseActivity;
import com.binhunt.workflow.itf.ProcessContext;

public class Terminate extends BaseActivity
{
	private Log log = LogFactory.getLog(Terminate.class);
	
	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		log.debug("--- Terminating the instance. ---");
		boolean bok = term((BHProcessContext)context);
		if(!bok)
		{
			log.debug("=== terminate failed! ===");
			((BHProcessContext)context).setStopEntireProcess(true);
			return context;
		}
		return context;
	}
	
	private boolean term(BHProcessContext context)
	{
		try
		{
			String insid = context.getInstanceId();
			String cmdTerm[] = {
					"/bin/bash",
					"-c",
					"source ~/.euca/eucarc ; euca-terminate-instances " + insid
			};
			log.debug("Terminating the instance: " + cmdTerm);
			Process p = Runtime.getRuntime().exec(cmdTerm);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while( (line=br.readLine())!=null )
			{
				log.debug(line);
				Thread.currentThread().sleep(100);
			}
			
			int retcode = p.waitFor();
		}
		catch(Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
		return true;
	}
}
