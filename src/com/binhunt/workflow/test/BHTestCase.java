package com.binhunt.workflow.test;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.After;
import org.junit.Before;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.binhunt.workflow.impl.BaseProcessor;

public class BHTestCase extends TestCase {

	private ApplicationContext context;
	
	@Before
	public void setUp() throws Exception {
		String location = "com/binhunt/workflow/conf/bhprocess.xml";
        context = new ClassPathXmlApplicationContext(location);
	}

	@After
	public void tearDown() throws Exception {
		((ClassPathXmlApplicationContext)context).close();
	}

    public void testWorkflow() throws Exception
    {
        BaseProcessor processor = (BaseProcessor)context.getBean("bhProcessor");
        assertNotNull(processor);
        
        assertTrue("No activities have been wired up to the processor "+processor.getBeanName(), !processor.getActivities().isEmpty());
        
        //kick off a single iteration of the processor
        processor.doActivities();
    }
    
	/**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BHTestCase.class );
    }
}
