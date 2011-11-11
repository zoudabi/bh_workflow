
package com.binhunt.workflow.impl;

import com.binhunt.workflow.itf.ErrorHandler;
import com.binhunt.workflow.itf.ProcessContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Class:SimpleErrorHandler
 * Creation Date: Mar 8, 2005
 * CVS ID $Id:$
 * 
 * Class Descriptoin goes here
 * 
 *  @author sdodge
 *  @since $Date:$
 */
public class BHErrorHandler implements ErrorHandler
{
	private Log log = LogFactory.getLog(BHErrorHandler.class);
    private String beanName;
    
    /* (non-Javadoc)
     * @see org.iocworkflow.ErrorHandler#handleError(org.iocworkflow.ProcessContext, java.lang.Throwable)
     */
    public void handleError(ProcessContext context, Throwable th)
    {
    	log.error("Un-handled error during processing of BinHunt Workflow: ", th);
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

}
