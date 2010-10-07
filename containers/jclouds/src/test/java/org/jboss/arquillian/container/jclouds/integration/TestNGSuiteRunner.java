/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.container.jclouds.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.testng.testspi.MultiThreadedPrClassTestCase.TestCase1;
import org.jboss.arquillian.testng.testspi.MultiThreadedPrClassTestCase.TestCase2;
import org.junit.Test;
import org.testng.TestNG;
import org.testng.internal.AnnotationTypeEnum;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

/**
 * TestNGSuiteRunner
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class TestNGSuiteRunner
{

   @Test
   public void shouldRun() 
   {
      TestNG runner = new TestNG();
      runner.setVerbose(3);
      runner.setXmlSuites(
            Arrays.asList(createSuite(
                  JCloudsIntegrationTestCase1.class, 
                  JCloudsIntegrationTestCase2.class)));
   
      runner.run();
   }
   
   private XmlSuite createSuite(Class<?>... classes)
   {
      XmlSuite suite = new XmlSuite();
      suite.setName("Arquillian");
      suite.setAnnotations(AnnotationTypeEnum.JDK.getName());
      suite.setParallel("classes");
      suite.setThreadCount(2);
      XmlTest test = new XmlTest(suite);
      test.setName("Arquillian - Test");
      List<XmlClass> testClasses = new ArrayList<XmlClass>();
      for(Class<?> clazz :classes)
      {
         testClasses.add(new XmlClass(clazz.getName()));
      }
      test.setXmlClasses(testClasses);
      return suite;
   }

}
