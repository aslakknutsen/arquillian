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
package org.jboss.arquillian.framework.byteman.test;

import java.lang.management.ManagementFactory;
import java.util.List;

import org.junit.Test;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * AttachTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class AttachTestCase
{

   @Test
   public void test() throws Exception
   {
      String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
      System.out.println("pid: " + pid);
      
      VirtualMachine vm = VirtualMachine.attach(pid);
      
      System.out.println("System");
      vm.getSystemProperties().save(System.out, "");
       
      System.out.println("Agent");
      vm.getAgentProperties().save(System.out, "");
      
      //vm.
   }
}
