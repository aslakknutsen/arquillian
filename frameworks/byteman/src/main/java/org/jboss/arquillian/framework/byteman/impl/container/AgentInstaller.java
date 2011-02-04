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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

import org.jboss.arquillian.framework.byteman.impl.ScriptUtil;
import org.jboss.arquillian.spi.core.annotation.Observes;
import org.jboss.arquillian.spi.event.suite.BeforeSuite;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.VirtualMachine;

/**
 * AgentInstaller
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class AgentInstaller
{
   public void install(@Observes BeforeSuite event)
   {
      try
      {
         String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
         
         File bytemanHome = File.createTempFile("byteman", "agent");
         bytemanHome.delete();
         bytemanHome.mkdir();
         
         File bytemanLib = new File(bytemanHome, "lib");
         bytemanLib.mkdirs();

         InputStream bytemanInputJar = Thread.currentThread().getContextClassLoader().getResourceAsStream("byteman.jar");
         
         File bytemanJar = new File(bytemanLib, "byteman.jar");
         
         ScriptUtil.copy(bytemanInputJar, new FileOutputStream(bytemanJar));

         VirtualMachine vm = VirtualMachine.attach(pid);
         try
         {
            vm.loadAgent(bytemanJar.getAbsolutePath(), "listener:true,prop:org.jboss.byteman.verbose=true");
         } 
         catch (AgentInitializationException e) 
         {
            // this probably indicates that the agent is already installed
         }
         vm.detach();
      }
      catch (IOException e) 
      {
         throw new RuntimeException("Could not write byteman.jar to disk", e);
      }
      catch (Exception e) 
      {
         throw new RuntimeException("Could not install byteman agent", e);
      }
   }
}
