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
package org.jboss.arquillian.framework.byteman.impl.container;

import java.io.InputStream;
import java.util.Arrays;

import org.jboss.arquillian.framework.byteman.impl.ScriptUtil;
import org.jboss.arquillian.framework.byteman.impl.SubmitException;
import org.jboss.arquillian.spi.core.annotation.Observes;
import org.jboss.arquillian.spi.event.suite.AfterSuite;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;

/**
 * ScriptUnInstaller
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ScriptUnInstaller
{
   public void uninstall(@Observes AfterSuite event)
   {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      InputStream scriptStream = cl.getResourceAsStream("byteman.script");
      
      if(scriptStream != null)
      {
         String ruleKey = Thread.currentThread().getName();
         String ruleScript = ScriptUtil.toString(scriptStream);
         try
         {
            Submit submit = new Submit();
            submit.deleteScripts(Arrays.asList(new ScriptText(ruleKey, ruleScript)));
         }
         catch (Exception e) 
         {
            throw new SubmitException("Could not uninstall script from file", e);
         }
      }
   }
}
