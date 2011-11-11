package com.binhunt.workflow.test;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.binhunt.workflow.impl.BaseProcessor;

public class BackendController
{
	protected int JOB_STATUS_FAILED = -1;
	protected int JOB_STATUS_INIT = 0;
	protected int JOB_STATUS_DOING = 1;
	protected int JOB_STATUS_FINISHED = 2;
	
	private ApplicationContext context;
	
	public BackendController()
	{
		init();
	}
	
	private void init()
	{
		String location = "com/binhunt/workflow/conf/bhprocess.xml";
        context = new ClassPathXmlApplicationContext(location);
	}
	
	private void exit()
	{
		((ClassPathXmlApplicationContext)context).close();
	}
	
	private void doTask()
	{
		int jobid = getNotification();
		if(jobid < 0)return;
		
        BaseProcessor processor = (BaseProcessor)context.getBean("bhProcessor");
        if(processor == null)return;
        
        //kick off a single iteration of the processor
        //numProcessor++;
        System.out.println("---Thread_" + Thread.currentThread().getId() + " executing doActivities.---");
        processor.doActivities(jobid);
        //numProcessor--;
	}
	
	class MyThread implements Runnable
	{
		public void run()
		{
			while(true)
			{
				doTask();
				try
				{
					Thread.currentThread().sleep(10000);
				}
				catch(Exception ex)
				{}
			}
		}
	}
	
	private synchronized int getNotification()
	{
		//System.out.println("---Thread_" + Thread.currentThread().getId() + " getting notification.---");
		Connection conn = null;
		PreparedStatement ps = null;
		int jobid = -1;
		try
		{
			String sql = "select id from tbl_job where status=? and userorg<>'Other' order by created";
			conn = GetConnection.getConn();
			if (conn == null)
			{
				System.out.println("connection is null");
				return -1;
			}
			
			ps = conn.prepareStatement(sql);
			ps.setInt(1, JOB_STATUS_INIT);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
			{
				jobid = rs.getInt(1);
				//update status
				String sqlup = "update tbl_job set status=? where id=?";
				PreparedStatement psup = conn.prepareStatement(sqlup);
				psup.setInt(1, JOB_STATUS_DOING);
				psup.setInt(2, jobid);
				psup.executeUpdate();
			}
			else//try to get others
			{
				String sqlOt = "select id from tbl_job where status=? order by created";
				
				PreparedStatement psOt = conn.prepareStatement(sqlOt);
				psOt.setInt(1, JOB_STATUS_INIT);
				ResultSet rsOt = psOt.executeQuery();
				if(rsOt.next())
				{
					jobid = rsOt.getInt(1);
					//update status
					String sqlup = "update tbl_job set status=? where id=?";
					PreparedStatement psup = conn.prepareStatement(sqlup);
					psup.setInt(1, JOB_STATUS_DOING);
					psup.setInt(2, jobid);
					psup.executeUpdate();
				}
			}
			
			GetConnection.close(conn, ps, null);
			if(jobid>=0)System.out.println("---Got one job by thread_" + Thread.currentThread().getId() + "---");
			//else System.out.println("---Got no job by thread_" + Thread.currentThread().getId() + "---");
			return jobid;
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			GetConnection.close(conn, ps, null);
			return -1;
		}
	}
	
	public void exec()
	{
		for(int i=0; i<2; ++i)
		{
			//System.out.println("main: thread_" + i);
			Thread tr = new Thread(new MyThread());
			tr.start();
		}
	}
	
	public static void main(String []args)
	{
		BackendController bc = new BackendController();
		bc.exec();
	}
}

class GetConnection
{
	/**
	 * @return Connection
	 */
	public static Connection getConn()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://10.4.8.24:3306/binhunt?user=username&password=pass";
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
