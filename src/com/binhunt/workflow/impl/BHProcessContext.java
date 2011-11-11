
package com.binhunt.workflow.impl;

import java.util.Set;

import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.binhunt.workflow.itf.ProcessContext;
import  com.binhunt.workflow.itf.XsltAware;

/**
 * Class:RateDropContext
 * Creation Date: Mar 12, 2005
 * CVS ID $Id:$
 * 
 * Process context used for the Airline Rate Drop example, Javaworld Article 2005
 * 
 *  @author sdodge
 *  @since $Date:$
 */
public class BHProcessContext implements ProcessContext, XsltAware
{
	private boolean stopEntireProcess;
	private Set recipients;
	private DOMSource messageDom;
	private String messageContent;
	private Object seedObject;
	private Boolean failed;
	
	private Log log = LogFactory.getLog(BHProcessContext.class);
	
	/* (non-Javadoc)
     * @see org.iocworkflow.ProcessContext#stopProcess()
     */
	public boolean stopProcess()
	{
		return stopEntireProcess;
	}
	
	/* (non-Javadoc)
     * @see org.iocworkflow.ProcessContext#setSeedData(java.lang.Object)
     */
	public void setSeedData(Object seedObject)
	{
		this.seedObject = seedObject;
    }
	
	public void setStopEntireProcess(boolean stopEntireProcess)
	{
		this.stopEntireProcess = stopEntireProcess;
	}
	
	/* (non-Javadoc)
     * @see org.iocworkflow.support.XsltAware#getDomSource()
     */
	public DOMSource getDomSource()
	{
		return messageDom;
	}

    /* (non-Javadoc)
     * @see org.iocworkflow.support.XsltAware#getTransformedContent()
     */
    public String getTransformedContent() {
        
        return messageContent;
    }

    /* (non-Javadoc)
     * @see org.iocworkflow.support.XsltAware#setTransformedContent(java.lang.String)
     */
    public void setTransformedContent(String transformedContent) {
       messageContent = transformedContent;
        
    }
    
    public Set getRecipients() {
        return recipients;
    }
    public void setRecipients(Set recipients) {
        this.recipients = recipients;
    }
    public Object getSeedData()
    {//AirlineRouteSeedData
        return seedObject;
    }
    public void setMessageDom(DOMSource messageDom) {
        this.messageDom = messageDom;
    }
    
    //added by dabi
    private String tempdir;
    private String jobid;
    private String useremail;
    private String instanceIp;
    private String instanceId;
    private String userorg;

	public String getUserorg() {
		return userorg;
	}

	public void setUserorg(String userorg) {
		this.userorg = userorg;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getInstanceIp() {
		return instanceIp;
	}

	public void setInstanceIp(String instanceIp) {
		this.instanceIp = instanceIp;
	}

	public String getTempdir() {
		return tempdir;
	}

	public void setTempdir(String tempdir) {
		this.tempdir = tempdir;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public String getUseremail() {
		return useremail;
	}

	public void setUseremail(String useremail) {
		this.useremail = useremail;
	}

	public Boolean getFailed() {
		return failed;
	}

	public void setFailed(Boolean failed) {
		this.failed = failed;
	}
}
