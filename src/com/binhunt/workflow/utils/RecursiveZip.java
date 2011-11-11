package com.binhunt.workflow.utils;

import java.io.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//for cn-file names, has to use ant.jar
//import org.apache.tools.zip.ZipEntry;
//import org.apache.tools.zip.ZipOutputStream;

public class RecursiveZip
{
	public static void zip(String path, File basePath,
			ZipOutputStream zo, boolean isRecursive, boolean isOutBlankDir)
		throws IOException
	{
		File inFile = new File(path);
		
		File[] files = new File[0];
		if(inFile.isDirectory())
		{
			files = inFile.listFiles();
		}
		else if(inFile.isFile())
		{
			files = new File[1];
			files[0] = inFile;
		}
		
		byte[] buf = new byte[1024];
		int len = 0;
		for(int i=0; i<files.length; ++i)
		{
			String pathName = "";
			if(basePath != null)
			{
				if(basePath.isDirectory())
				{
					pathName = files[i].getPath().substring(basePath.getPath().length()+1);
				}
				else
				{
					pathName = files[i].getPath().substring(basePath.getParent().length()+1);
				}
			}
			else
			{
				pathName = files[i].getName();
			}
			
			if(files[i].isDirectory())
			{
				if(isOutBlankDir && basePath!=null)
				{
					zo.putNextEntry(new ZipEntry(pathName + "/"));
				}
				if(isRecursive)
				{
					zip(files[i].getPath(), basePath, zo, isRecursive, isOutBlankDir);
				}
			}
			else
			{
				FileInputStream fin = new FileInputStream(files[i]);
				zo.putNextEntry(new ZipEntry(pathName));
				while( (len=fin.read(buf)) > 0)
				{
					zo.write(buf, 0, len);
				}
				fin.close();
			}
		}
	}
	
	public static void zipit(String inpath, String outname) throws Exception
	{
		OutputStream os = new FileOutputStream(outname);
		BufferedOutputStream bs = new BufferedOutputStream(os);
		ZipOutputStream zo = new ZipOutputStream(bs);
		
		zip(inpath, new File(inpath), zo, true, true);
		
		zo.closeEntry();
		zo.close();
	}
	
//	public static void main(String[] args)
//	{
//		try
//		{
//			RecursiveZip.zipit("/home/mengpan/binhunt-website/webwork/bhworkflow/tempdir/44/result", "/home/mengpan/binhunt-website/webwork/bhworkflow/tempdir/44.zip");
//		}
//		catch(Exception e)
//		{
//			
//		}
//	}
}
