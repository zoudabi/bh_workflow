
package com.binhunt.workflow.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.binhunt.workflow.itf.ErrorHandler;
import com.binhunt.workflow.itf.ProcessContext;
import com.binhunt.workflow.itf.SenderDelegate;
import com.binhunt.workflow.utils.SMTPServerException;

/**
 * Class:MailErrorHandler Creation Date: Mar 13, 2005 CVS ID $Id:$
 * 
 * Class Descriptoin goes here
 * 
 * @author sdodge
 * @since $Date:$
 */
public class MailErrorHandler implements ErrorHandler
{
	private String beanName;
    private SenderDelegate delegate;

    private Log log = LogFactory.getLog(MailErrorHandler.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.iocworkflow.ErrorHandler#handleError(org.iocworkflow.ProcessContext, java.lang.Throwable)
     */
    public void handleError(ProcessContext context, Throwable th)
    {
    	if (th instanceof SMTPServerException)
    	{
    		log.error("Found an error while trying to send for the first time", th);
            try
            {
                delegate.send((BHProcessContext) context);
            }
            catch (SMTPServerException e)
            {
                log.error("Failed durinn second try at sending, let's stop now");
                ((BHProcessContext) context).setStopEntireProcess(true);
            }
        }
        else
        {
            log.error("Unknown error occured, forcing a stop", th);
            ((BHProcessContext) context).setStopEntireProcess(true);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    public void setDelegate(SenderDelegate delegate)
    {
        this.delegate = delegate;
    }
}