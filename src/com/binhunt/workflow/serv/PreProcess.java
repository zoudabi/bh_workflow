package com.binhunt.workflow.serv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.impl.BaseActivity;
import com.binhunt.workflow.itf.ProcessContext;
import com.binhunt.workflow.serv.BinHuntProcess.StreamGobbler;

public class PreProcess extends BaseActivity
{
	private Log log = LogFactory.getLog(PreProcess.class);

	/**
	 * 1.     Running IDA in Windows guest from server 
	 * #  copy malware sample from server to windows machine
	 * #  user name:qemu  password: xxx  using RSA authentication
	 * $ scp -r sample/ qemu@192.168.10.2:/c/preprocess/ida-auto/
	 * 
	 * # automatically execute IDA to generate db files
	 * $ ssh qemu@192.168.10.2 "preprocess.bat"
	 * # preprocess.bat:
	 *     cd ..\..\preprocess
	 *     java BHPreprocess ida-auto/sample ida-auto/dgf-db
	 *        
	 * # copy db files from windows machine to the server
	 *  $ scp -r qemu@192.168.10.2:/c/preprocess/ida-auto/dgf-db ./
	 */
	private boolean runbash(BHProcessContext context)
	{
		try
		{
			boolean bok = false;
			String tempdir = context.getTempdir();
			String jobid = context.getJobid();
			
			String srcDir = tempdir + "/" + jobid + "/";//get id
			String cmd1 = "scp -r " + srcDir + " qemu@192.168.10.2:/c/preprocess/ida-auto/";
			log.debug("Copying data to win server for preprocessing: " + cmd1);
			bok = myExec(cmd1);
			if(!bok)return bok;
			
			String cmd2 = "ssh qemu@192.168.10.2 \"preprocess.bat " + jobid + "\"";
			log.debug("Starting preprocessing: " + cmd2);
			bok = myExec(cmd2);
			if(!bok)return bok;
			
			String cmd3 = "scp -r qemu@192.168.10.2:/c/preprocess/ida-auto/" + jobid + "/dgf-db " + srcDir;
			log.debug("Copying results back: " + cmd3);
			bok = myExec(cmd3);
			if(!bok)return bok;
			
			//remove mid files
			String cmd4 = "ssh qemu@192.168.10.2 \"rmmidfiles.bat " + jobid + "\"";
			log.debug("Removing the intermediate files on the win server: " + cmd4);
			bok = myExec(cmd4);
			if(!bok)return bok;
		}
		catch(Exception e)
		{
			log.debug(e.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		log.debug("--- Entering execution of Pre-Process ---");
		
		//
		boolean bok = runbash((BHProcessContext)context);
//		boolean bok = true;
		if(bok)
		{
			
		}
		else
		{
			log.debug("--- PreProcess failed! ---");
			((BHProcessContext) context).setStopEntireProcess(true);
		}
		
		return context;
	}

	boolean myExec(String cmd)
	{
		try
		{
			Process proc = Runtime.getRuntime().exec(cmd);
			StreamGobbler errSG = new StreamGobbler(proc.getErrorStream(), "Error");
			StreamGobbler outSG = new StreamGobbler(proc.getInputStream(), "Output");
			errSG.start();
			outSG.start();
			if(proc.waitFor() != 0)
			{
				log.debug("Cmd execution failed, error code: " + proc.exitValue());
				return false;
			}
			return true;
		}
		catch(Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
	}
	
	class StreamGobbler extends Thread
	{
		InputStream is;
		String type;
		
		StreamGobbler(InputStream is, String type)
		{
			this.is = is;
			this.type = type;
		}
		
		public void run()
		{
			try
			{
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while( (line=br.readLine()) != null )
				{
					log.debug(type + ">" + line);
				}
			}
			catch(Exception ex)
			{
				log.debug(ex.getMessage());
			}
		}
	}
}
