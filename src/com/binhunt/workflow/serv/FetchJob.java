package com.binhunt.workflow.serv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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
import com.binhunt.workflow.itf.ErrorHandler;
import com.binhunt.workflow.itf.ProcessContext;


public class FetchJob extends BaseActivity
{
	private String tempdir;
	
	private Log log = LogFactory.getLog(FetchJob.class);
	
	public void setTempdir(String tempdir)
	{
		this.tempdir = tempdir;
	}
	
	private boolean getOneJob(BHProcessContext context)
	{
		//retrieve the jobid from seeddata
		int jobid = (Integer)context.getSeedData();
		if(jobid < 0)
		{
			log.debug("---The seedObject is not correct! ---");
			return false;
		}
		
		Connection conn = null;
		PreparedStatement ps = null;
		try
		{
			int id = 0;
			String useremail = "";
			String fname1 = "";
			String fname2 = "";
			Blob blob1 = null;
			Blob blob2 = null;
			String userorg = "";
			
			String sql = "select id, useremail, fname1, fname2, file1, file2, userorg from tbl_job where id=?";//order by...
			conn = GetConnection.getConn();
			if (conn == null)
			{
				System.out.println("connection is null");
				return false;
			}
			ps = conn.prepareStatement(sql);
			ps.setInt(1, jobid);
			ResultSet rs = ps.executeQuery();
			log.debug("fetching a job using: " + sql);
			
			if(rs.next())
			{
				id = rs.getInt(1);
				useremail = rs.getString(2);
				fname1 = rs.getString(3);
				fname2 = rs.getString(4);
				blob1 = rs.getBlob(5);
				blob2 = rs.getBlob(6);
				userorg = rs.getString(7);
				
				//
				if(fname1.indexOf(".")<0)
				{
					fname1 = fname1 + ".1";
				}
				if(fname2.indexOf(".")<0)
				{
					fname2 = fname2 + ".2";
				}
				
				
				String tdir = tempdir + "/" + id + "/sample/";
				String f1 = tdir + fname1;
				String f2 = tdir + fname2;
				File dirTmp = new File(tdir);
				if(!dirTmp.exists())
				{
					dirTmp.mkdirs();
				}
				File smpl1 = new File(f1);
				{
					FileOutputStream out = new FileOutputStream(smpl1);//new File(sigfile));
					InputStream in = blob1.getBinaryStream();
					int i=0;
					while( (i=in.read())!=-1)out.write(i);
					in.close();
					out.close();
				}
				File smpl2 = new File(f2);
				{
					FileOutputStream out = new FileOutputStream(smpl2);//new File(sigfile));
					InputStream in = blob2.getBinaryStream();
					int i=0;
					while( (i=in.read())!=-1)out.write(i);
					in.close();
					out.close();
				}
				
//				String sqlup = "update tbl_job set status=? where id=?";
//				PreparedStatement psUp = conn.prepareStatement(sqlup);
//				psUp.setInt(1, JOB_STATUS_DOING);
//				psUp.setInt(2, id);
//				log.debug("updating the status to doing: " + sqlup);
//				if(psUp.executeUpdate()<=0)//no record is updated
//				{
//					GetConnection.close(conn, ps, null);
//					return false;
//				}
					
				GetConnection.close(conn, ps, null);
				
				//set properties
				context.setJobid("" + id);
				context.setTempdir(tempdir);
				context.setUseremail(useremail);
				context.setUserorg(userorg);
				return true;
			}
			else
			{
				//no job to handle
				GetConnection.close(conn, ps, null);
				return false;
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			GetConnection.close(conn, ps, null);
			return false;
		}
	}

	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		log.debug("--- Fetching job for handling ---");
		boolean bok = getOneJob(((BHProcessContext) context));
//		boolean bok = true;
		
		if(bok)
		{
			//setting context
		}
		else
		{
			log.debug("--- No job is fetched! ---");
			((BHProcessContext) context).setStopEntireProcess(true);
		}
		return context;
	}
}

class GetConnection
{
	/**
	 */
	public static Connection getConn()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://10.4.8.24:3306/binhunt?user=user&password=pass";
			Connection connection = DriverManager.getConnection(url);
			return connection;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	  * @param conn
	  * @param rs
	  * @param st
	  */
	public static void close(Connection conn,PreparedStatement ps,ResultSet rs)
	{
		if(rs!=null)
		{
			try
			{
				rs.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		if(ps!=null)
		{
			try
			{
				ps.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		if(conn!=null)
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
}