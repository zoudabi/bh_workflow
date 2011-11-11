
package com.binhunt.workflow.impl;

import com.binhunt.workflow.itf.Activity;
import com.binhunt.workflow.itf.ErrorHandler;


/**
 * Abstract implemention of activity designed for
 * re-use by business purpose specific Activities
 * 
 * Class:BaseActivity
 * Creation Date: Mar 4, 2005
 * CVS ID $Id:$
 * 
 *  @author sdodge
 *  @since $Date:$
 */
public abstract class BaseActivity implements Activity
{
	protected int JOB_STATUS_FAILED = -1;
	protected int JOB_STATUS_INIT = 0;
	protected int JOB_STATUS_DOING = 1;
	protected int JOB_STATUS_FINISHED = 2;
	
	private ErrorHandler errorHandler;
    private String beanName;

    public ErrorHandler getErrorHandler()
    {
    	return errorHandler;
    }

    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    /**
     * Set the fine grained error handler
     * @param errorHandler
     */
    public void setErrorHandler(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
    }
    
    public String getBeanName()
    {
        return beanName;
    }
}
