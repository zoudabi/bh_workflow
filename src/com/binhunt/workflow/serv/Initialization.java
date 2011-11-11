package com.binhunt.workflow.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.impl.BaseActivity;
import com.binhunt.workflow.itf.ProcessContext;

public class Initialization extends BaseActivity
{
	private Log log = LogFactory.getLog(Initialization.class);
	
	//available types
	private int m1Small;
	private int c1Medium;
	private int m1Large;
	private int m1Xlarge;
	private int c1Xlarge;
	
	private String strtype = "c1.medium";
	private String timeoutcnt;
	
	public void setTimeoutcnt(String timeoutcnt)
	{
		this.timeoutcnt = timeoutcnt;
	}
	
	//generate different instance type according to the userorg
	private String getType(BHProcessContext context)
	{
		String strtmp = "m1.small"; 
		String userorg = context.getUserorg();
		//String useremail = context.getUseremail();
		
		if(userorg.equalsIgnoreCase("SMU"))
		{
			if(m1Large>0)strtmp = "m1.large";
			else if(c1Medium>0)strtmp = "c1.medium";
		}
		else if(userorg.equalsIgnoreCase("DSO"))
		{
			if(m1Large>0)strtmp = "m1.large";
			else if(c1Medium>0)strtmp = "c1.medium";
		}
		else if(userorg.equalsIgnoreCase("Partner"))
		{
			if(c1Medium>0)strtmp = "c1.medium";
		}
		else//other
		{
		}
		return strtmp;
	}

	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		return initInst(context);
	}
	
	private synchronized ProcessContext initInst(ProcessContext context) throws Exception
	{
		log.debug("--- Entering execution of Cloud Platform Initialization ---");
		boolean bavl = checkAvail();
		if(!bavl)
		{
			log.debug("--- Instances checkAvail failed! ---");
			((BHProcessContext) context).setStopEntireProcess(true);
			return context;
		}
		
		//wait until timeout or available
		int tmc = Integer.parseInt(timeoutcnt);
		int idx = 0;
		while(m1Small <= 0)//not available
		{
			Thread.sleep(60000);
			
			bavl = checkAvail();
			if(!bavl)
			{
				log.debug("--- Instances checkAvail failed! ---");
				((BHProcessContext) context).setStopEntireProcess(true);
				return context;
			}
			idx++;
			if(idx>=tmc)break;
		}
		
		if(m1Small <= 0)
		{
			log.debug("--- No available instances! ---");
			((BHProcessContext) context).setStopEntireProcess(true);
			return context;
		}
		else//at least, m1Small
		{
			strtype = getType((BHProcessContext)context);
		}
		
		//
		boolean bstart = initInstance((BHProcessContext) context);
		if(!bstart)
		{
			log.debug("--- initInstance failed! ---");
			((BHProcessContext) context).setStopEntireProcess(true);
			return context;
		}
		return context;
	}
	
	private boolean testSh()
	{
		try
		{
			String cmdSrc[] = {"/bin/bash","-c","source ~/.euca/eucarc ; ~/binhunt-website/webwork/bhworkflow/hello.sh"};
			Process p1 = Runtime.getRuntime().exec(cmdSrc);
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			String line1 = "";
			while( (line1=br1.readLine())!=null)
			{
				log.debug(line1);
			}
			int retcode1 = p1.waitFor();
		}
		catch(Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
		return true;
	}

	//string format:AVAILABILITYZONE |-c1.xlarge	0000/0001	6	4096	20
	private int getFree(String line, int begidx)
	{
		String tmp = line.substring(begidx+1).trim();
		int sepidx = tmp.indexOf('/');
		String freestr = tmp.substring(0, sepidx).trim();
		int free = Integer.parseInt(freestr);
		return free;
	}

	/**
	 * euca-describe-availability-zones verbose
	 * AVAILABILITYZONE	flyer-1	10.4.7.200
	 * AVAILABILITYZONE |-vm types	free/max	cpu	ram	disk
	 * AVAILABILITYZONE	|-m1.small	0002/0010	1	512	5
	 * AVAILABILITYZONE |-c1.medium	0001/0005	2	1024	10
	 * AVAILABILITYZONE |-m1.large	0000/0003	3	2048	20
	 * AVAILABILITYZONE |-m1.xlarge	0000/0002	4	3072	20
	 * AVAILABILITYZONE |-c1.xlarge	0000/0001	6	4096	20
	 */	
	private boolean checkAvail()
	{
		try
		{
			String cmdAvl[] = {"/bin/bash","-c","source ~/.euca/eucarc ; euca-describe-availability-zones verbose"};
			log.debug("Checking available: " + cmdAvl);
			Process p = Runtime.getRuntime().exec(cmdAvl);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while( (line=br.readLine())!=null )
			{
				int idx = -1;
				if( (idx=line.indexOf("m1.small")) != -1)
				{
					m1Small = getFree(line, idx + "ml.small".length());
				}
				else if( (idx=line.indexOf("c1.medium")) != -1)
				{
					c1Medium = getFree(line, idx + "c1.medium".length());
				}
				else if( (idx=line.indexOf("m1.large")) != -1)
				{
					m1Large = getFree(line, idx + "m1.large".length());
				}
				else if( (idx=line.indexOf("m1.xlarge")) != -1)
				{
					m1Xlarge = getFree(line, idx + "m1.xlarge".length());
				}
				else if( (idx=line.indexOf("c1.xlarge")) != -1)
				{
					c1Xlarge = getFree(line, idx + "c1.xlarge".length());
				}
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
	
	/**
	 * euca-describe-instances
	 * RESERVATION	r-xxxx	admin	default
	 * INSTANCE	i-xxxx	emi-xxxx	10.4.7.201	172.19.1.2	running	0	m1.xlarge	2011xxx	flyer-1	eki-xxx	eri-xxx
	 * RESERVATION	r-xxxx	mingjiang	default
	 * INSTANCE	i-xxxx	emi-xxxx	10.4.7.202	172.19.1.34	running	0	m1.xlarge	2011xxx	flyer-1	eki-xxx	eri-xxx
	 */
	private boolean descInstance()
	{
		return false;
	}
	
	/**
	 * running "euca-describe-images | grep binhunt" to get emi-id
	 * euca-run-instances -n 1 <emi-id> -t type
	 * IMAGE	emi-E1F918D4	ubuntu-image-bucket/ubuntu-8.04-i386-binhunt.img.manifest.xml \
	 * 	admin	available	public		x86_64	machine	 	 	instance-store
	 * euca-describe-instances //to watch whether your instance's state become "running"
	 * @return
	 */
	private boolean initInstance(BHProcessContext context)
	{
		try
		{
			String stremi = "";
			String strId = "";
			String strIp = "";
			String cmdEmi[] = {"/bin/bash","-c","source ~/.euca/eucarc ; euca-describe-images | grep binhunt"};
			log.debug("Finding the related image: " + cmdEmi);
			Process p = Runtime.getRuntime().exec(cmdEmi);
			String line = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while( (line=br.readLine()) != null)
			{
				int idx = 0;
				if( (idx=line.indexOf("emi-")) != -1)
				{
					stremi = line.substring(idx);
					int endidx = 0;
					for(endidx=0; endidx<stremi.length(); ++endidx)
					{
						if(stremi.charAt(endidx)==' ' || stremi.charAt(endidx)=='\t')break;
					}
					stremi = stremi.substring(0, endidx).trim();
					break;
				}
			}
			p.waitFor();
			
			String strRun = "source ~/.euca/eucarc ;euca-run-instances -n 1 " + stremi + " -t " + strtype;
			log.debug("Starting an instance: " + strRun);
			String cmdRun[] = {"/bin/bash", "-c", strRun};
			Process pRun = Runtime.getRuntime().exec(cmdRun);
			BufferedReader brRun = new BufferedReader(new InputStreamReader(pRun.getInputStream()));
			while( (line=brRun.readLine()) != null)//log.debug(line);
			{
				//INSTANCE	i-42D10845	emi-E1F918D4	0.0.0.0	0.0.0.0	pending ...
				if(line.indexOf(stremi)!=-1)
				{
					int bidx = "INSTANCE".length();
					int eidx = line.indexOf("emi-");
					strId = line.substring(bidx, eidx).trim();
				}
			}
			pRun.waitFor();
			
			
			boolean bRunning = false;
			String strChk[] = {"/bin/bash","-c","source ~/.euca/eucarc ; euca-describe-instances"};
			log.debug("--- Waiting for the instance to be ready. ---");
			while(!bRunning)
			{
				Process pChk = Runtime.getRuntime().exec(strChk);
				BufferedReader brChk = new BufferedReader(new InputStreamReader(pChk.getInputStream()));
				while( (line=brChk.readLine()) != null)
				{
					if(line.indexOf(strId) != -1)
					{
						if(line.indexOf("running") != -1)//status becomes running
						{
							bRunning = true;
							
							//get ip address
							int bidx = line.indexOf(stremi) + stremi.length();
							String strtemp = line.substring(bidx).trim();
							int endidx = 0;
							for(endidx=0; endidx<strtemp.length(); ++endidx)
							{
								if(strtemp.charAt(endidx)==' ' || strtemp.charAt(endidx)=='\t')break;
							}
							strIp = strtemp.substring(0, endidx).trim();
							
							context.setInstanceIp(strIp);
							context.setInstanceId(strId);
						}
					}
				}
				pChk.waitFor();
				if(!bRunning)Thread.currentThread().sleep(1000);
			}
		}
		catch(Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
		return true;
	}
}




























