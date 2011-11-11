package com.binhunt.workflow.serv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.impl.BaseActivity;
import com.binhunt.workflow.itf.ProcessContext;
import com.binhunt.workflow.utils.RecursiveZip;

public class BinHuntProcess extends BaseActivity
{
	private Log log = LogFactory.getLog(BinHuntProcess.class);
	

	private String tempdir;
	private String clouddir;
	private String clouduser;
	private String rmtempdir;
	private String level;
	
	private boolean bFailed = false;
	
	public void setTempdir(String tempdir)
	{
		this.tempdir = tempdir;
	}
	public void setClouddir(String clouddir)
	{
		this.clouddir = clouddir;
	}
	public void setClouduser(String clouduser)
	{
		this.clouduser = clouduser;
	}
	public void setRmtempdir(String rmtempdir)
	{
		this.rmtempdir = rmtempdir;
	}
	public void setLevel(String level)
	{
		this.level = level;
	}
	
	private int runlevel = 0;
	private void calcLevel(BHProcessContext context)
	{
		String userorg = context.getUserorg();
		
		if(userorg.equalsIgnoreCase("Other"))
		{
			runlevel = 0;//unchanged
		}
		else
		{
			if(level.equals("2"))
			{
				runlevel = 1;
			}
			else if(level.equals("3"))
			{
				runlevel = 2;
			}
			else
			{
				runlevel = 0;
			}
		}
	}

	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		calcLevel((BHProcessContext)context);
		
		log.debug("--- Entering execution of BinHunt Processing ---");
		boolean bok = runBinhunt((BHProcessContext)context);
		if(!bok)
		{
			log.debug("--- BinHunt process failed! ---");
			bFailed = true;
		}
		//else
		{
			bok = writeResult((BHProcessContext)context);
			if(!bok)
			{
				log.debug("--- Write back results failed! ---");
			}
			if(rmtempdir.equals("1"))
			{
				File dir = new File(tempdir + "/" + ((BHProcessContext)context).getJobid());
				deleteFolder(dir);
			}
		}
		if(!bok)((BHProcessContext)context).setFailed(true);
		return context;
	}
	
	private void deleteFolder(File dir)
	{
		File []files = dir.listFiles();
		for(int i=0; i<files.length; ++i)
		{
			if(files[i].isFile())
			{
				files[i].delete();
			}
			else
			{
				deleteFolder(files[i]);
			}
		}
		dir.delete();
	}
	
	/**
	 * 1.copy preprocessed files from server to instance
	 * scp -r dgf-db/ user@10.4.7.202:/home/user/bitblaze/vine/bindiff/malware
	 * 
	 * 2.running binhunt
	 * ssh user@10.4.7.202 "./binhunt.sh"
	 * 
	 * 3.copy result from instance to server
	 * scp -r user@10.4.7.202:/home/user/bitblaze/vine/bindiff/malware/result ./
	 * scp -r user@10.4.7.202:/home/user/bitblaze/vine/bindiff/malware/result_score ./
	 */
	private boolean runBinhunt(BHProcessContext context)
	{
		try
		{
			boolean bOk = false;
			
			String jobid = context.getJobid();
			String insip = context.getInstanceIp();
			String srcdir = tempdir + "/" + jobid + "/dgf-db/";
			String line = "";
			String cmdTo = "scp -r " + srcdir + " " + clouduser + "@" + insip + ":" + clouddir;
			log.debug("Copying files to the instance: " + cmdTo);
			Thread.sleep(20000);
			bOk = myExec(cmdTo);
			if(!bOk)return bOk;
			
			//String cmdRun = "ssh " + clouduser + "@" + insip + " \"./binhunt.sh " + runlevel + "\"";
			String cmdRun = "ssh " + clouduser + "@" + insip;
			if(runlevel == 0)
			{
				cmdRun += " \"/home/user/binhunt-0.sh\"";
			}
			else if(runlevel == 1)
			{
				cmdRun += " \"/home/user/binhunt-1.sh\"";
			}
			else
			{
				cmdRun += " \"/home/user/binhunt-2.sh\"";
			}
			//String cmdRun = "ssh " + clouduser + "@" + insip + " \"/home/user/hello.sh\"";
			log.debug("Starting binhunt: " + cmdRun);
			Thread.sleep(20000);
			bOk = myExec(cmdRun);
			if(!bOk)return bOk;
			
			String dstdir = tempdir + "/" + jobid + "/";
			String cmdFrom1 = "scp -r " + clouduser + "@" + insip + ":" + clouddir + "/result " + dstdir;
			String cmdFrom2 = "scp -r " + clouduser + "@" + insip + ":" + clouddir + "/result_score " + dstdir;
			
			log.debug("Copying result back: " + cmdFrom1);
			bOk = myExec(cmdFrom1);
			if(!bOk)return bOk;
			log.debug("Copying result_score back: " + cmdFrom2);
			bOk = myExec(cmdFrom2);
			if(!bOk)return bOk;
		}
		catch(Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * write result_score back to database
	 * @return
	 */
	private boolean writeResult(BHProcessContext context)
	{
		Connection conn = null;
		PreparedStatement ps = null;
		int status = JOB_STATUS_FINISHED;
		try
		{
			String jobid = context.getJobid();
			int id = Integer.parseInt(jobid);
			String resdir = tempdir + "/" + jobid + "/";
			
			//create zip file
			String zipdir = tempdir + "/" + jobid + "/result";
			String zipfile = tempdir + "/" + jobid + ".zip";
			
			try
			{
				RecursiveZip.zipit(zipdir, zipfile);
			}
			catch(Exception ex)
			{
				log.debug("Calling RecursiveZip.zipit failed.");
				bFailed = true;
			}
			
			//insert result
			String sql = "insert into tbl_job_result(id, resultscore, resultzip) value(?,?,?)";
			conn = GetConnection.getConn();
			if (conn == null)
			{
				System.out.println("connection is null");
				return false;
			}
			
			ps = conn.prepareStatement(sql);
			ps.setInt(1, id);
			
			File scoreFile = new File(resdir + "result_score");
			if(!scoreFile.exists() || bFailed)
			{
				log.debug("scoreFile not existed: " + scoreFile.getName());
				status = JOB_STATUS_FAILED;
				ps.setString(2, "Some internal errors occurred. Please contact us by sending email to binhunt@smu.edu.sg");
			}
			else
			{
				FileInputStream fis = new FileInputStream(scoreFile);
				ps.setBinaryStream(2, fis, fis.available());
			}	
			File zipFile = new File(zipfile);
			if(!zipFile.exists())
			{
				log.debug("zipFile not existed: " + zipFile.getName());
				ps.setString(3, "Some internal errors occurred. Please contact us by sending email to binhunt@smu.edu.sg");
			}
			else
			{
				FileInputStream fis = new FileInputStream(zipFile);
				ps.setBinaryStream(3, fis, fis.available());
			}
			
			ps.executeUpdate();
			
			//update status
			String sqlup = "update tbl_job set status=? where id=?";
			PreparedStatement psUp = conn.prepareStatement(sqlup);
			psUp.setInt(1, status);
			psUp.setInt(2, id);
			if(psUp.executeUpdate()<=0)//no record is updated
			{
				GetConnection.close(conn, ps, null);
				return false;
			}
					
			GetConnection.close(conn, ps, null);

			return true;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			GetConnection.close(conn, ps, null);
			return false;
		}
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












