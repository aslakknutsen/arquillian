/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.testng;

import java.lang.reflect.Method;

import org.jboss.arquillian.impl.DeployableTestBuilder;
import org.jboss.arquillian.impl.XmlConfigurationBuilder;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.TestMethodExecutor;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestRunnerAdaptor;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Arquillian
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public abstract class Arquillian implements IHookable
{
   private static ThreadLocal<TestRunnerAdaptor> deployableTest = new ThreadLocal<TestRunnerAdaptor>();

   @BeforeSuite(alwaysRun = true)
   public void arquillianBeforeSuite() throws Exception
   {
      if(deployableTest.get() == null)
      {
         Configuration configuration = new XmlConfigurationBuilder().build();
         deployableTest.set(DeployableTestBuilder.build(configuration));
      }
      deployableTest.get().beforeSuite();
   }

   @AfterSuite(alwaysRun = true)
   public void arquillianAfterSuite() throws Exception
   {
      if (deployableTest.get() == null)
      {
         return;
      }
      deployableTest.get().afterSuite();
   }

   @BeforeClass(alwaysRun = true)
   public void arquillianBeforeClass() throws Exception
   {
      deployableTest.get().beforeClass(getClass());
   }

   @AfterClass(alwaysRun = true)
   public void arquillianAfterClass() throws Exception
   {
      deployableTest.get().afterClass(getClass());
   }
   
   @BeforeMethod(alwaysRun = true)
   public void arquillianBeforeTest(Method testMethod) throws Exception 
   {
      deployableTest.get().before(this, testMethod);
   }

   @AfterMethod(alwaysRun = true)
   public void arquillianAfterTest(Method testMethod) throws Exception 
   {
      deployableTest.get().after(this, testMethod);
   }

   public void run(final IHookCallBack callback, final ITestResult testResult)
   {
      TestResult result;
      try
      {
         result = deployableTest.get().test(new TestMethodExecutor()
         {
            @Override
            public void invoke() throws Throwable
            {
               callback.runTestMethod(testResult);
            }
            
            @Override
            public Method getMethod()
            {
               return testResult.getMethod().getMethod();
            }
            
            @Override
            public Object getInstance()
            {
               return Arquillian.this;
            }
         });
         if(result.getThrowable() != null)
         {
            testResult.setThrowable(result.getThrowable());
         }
      } 
      catch (Exception e)
      {
         testResult.setThrowable(e);
      }
   }
}
