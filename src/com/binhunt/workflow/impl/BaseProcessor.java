
package com.binhunt.workflow.impl;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.binhunt.workflow.itf.Activity;
import com.binhunt.workflow.itf.ErrorHandler;
import com.binhunt.workflow.itf.Processor;

/**
 * Base class for all Workflow Processors.  Responsible of keeping track of an ordered collection
 * of {@link Activity Activities}
 * 
 * @since March 1, 2005
 * @author sdodge
 * @see Activity
 *  
 */
public abstract class BaseProcessor implements InitializingBean, BeanNameAware, BeanFactoryAware, Processor
{

    private BeanFactory beanFactory;
    private String beanName;
    private List activities;
    private ErrorHandler defaultErrorHandler;

    /* Sets name of the spring bean in the application context that this 
     * processor is configured under
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
     */
    public void setBeanName(String beanName)
    {
        this.beanName = beanName;
    }

    /* Sets the spring bean factroy bean that is responsible for this processor. 
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    /*
     * Called after the properties have been set, Ensures the list of activities
     *  is not empty and each activity is supported by this Workflow Processor
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception
    {
    	if(!(beanFactory instanceof ListableBeanFactory))
            throw new BeanInitializationException("The workflow processor ["+beanName+"] " +
            		"is not managed by a ListableBeanFactory, please re-deploy using some dirivative of ListableBeanFactory such as" +
            		"ClassPathXmlApplicationContext ");

        if (activities == null || activities.isEmpty())
            throw new UnsatisfiedDependencyException(getBeanDesc(), beanName, "activities",
                                                     "No activities were wired for this workflow");
        
        for (Iterator iter = activities.iterator(); iter.hasNext();) {
            Activity activitiy = (Activity) iter.next();
            if( !supports(activitiy))
                throw new BeanInitializationException("The workflow processor ["+beanName+"] does " +
                		"not support the activity of type"+activitiy.getClass().getName());
        }

    }

    /**
     * Returns the bean description if the current bean factory allows it.
     * @return spring bean description configure via the spring description tag
     */
    protected String getBeanDesc()
    {
        return (beanFactory instanceof ConfigurableListableBeanFactory) ?
                ((ConfigurableListableBeanFactory) beanFactory).getBeanDefinition(beanName).getResourceDescription()
                : "Workflow Processor: " + beanName;

    }

 
    /**
     * Sets the collection of Activities to be executed by the Workflow Process
     * 
     * @param activities ordered collection (List) of activities to be executed by the processor
     */
    public void setActivities(List activities)
    {
        this.activities = activities;
    }
    
    public void setDefaultErrorHandler(ErrorHandler defaultErrorHandler)
    {
        this.defaultErrorHandler = defaultErrorHandler;
    }
    
    
    public List getActivities()
    {
        return activities;
    }
    public String getBeanName()
    {
        return beanName;
    }
    public ErrorHandler getDefaultErrorHandler()
    {
        return defaultErrorHandler;
    }
    public BeanFactory getBeanFactory()
    {
        return beanFactory;
    }
}