
package com.binhunt.workflow.itf;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.utils.SMTPServerException;


/**
 * Class:SenderDelegate
 * Creation Date: Mar 13, 2005
 * CVS ID $Id:$
 * 
 * Delegate the sending here for easy re-use.
 * 
 *  @author sdodge
 *  @since $Date:$
 */
public interface SenderDelegate
{
	public void send(BHProcessContext context)throws SMTPServerException;
}
