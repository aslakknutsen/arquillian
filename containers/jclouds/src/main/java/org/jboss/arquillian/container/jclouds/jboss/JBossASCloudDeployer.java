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
package org.jboss.arquillian.container.jclouds.jboss;

import org.jboss.arquillian.container.jclouds.spi.CloudDeployer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;

/**
 * JBossASCloudDeployer
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class JBossASCloudDeployer implements CloudDeployer
{
   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.spi.CloudDeployer#deploy(org.jclouds.ssh.SshClient, org.jboss.shrinkwrap.api.Archive)
    */
   public void deploy(SshClient client, Archive<?> archive)
   {
      Payload toSend = Payloads.newInputStreamPayload(archive.as(ZipExporter.class).exportZip());
      long start = System.currentTimeMillis();
      client.put(archive.getName(), toSend);
      System.out.println("upload: " + (System.currentTimeMillis() - start));
      
      start = System.currentTimeMillis();
      client.exec("/usr/local/jboss/bin/twiddle.sh invoke 'jboss.system:service=MainDeployer' deploy file://`pwd`/" + archive.getName());
      System.out.println("deploy file: " + (System.currentTimeMillis() - start));
   }

   /* (non-Javadoc)
    * @see org.jboss.arquillian.container.jclouds.spi.CloudDeployer#undeploy(org.jclouds.ssh.SshClient, org.jboss.shrinkwrap.api.Archive)
    */
   public void undeploy(SshClient client, Archive<?> archive)
   {
      client.exec(
            "/usr/local/jboss/bin/twiddle.sh invoke 'jboss.system:service=MainDeployer' undeploy file://`pwd`/" + archive.getName() + "\n" + 
            "rm -rf /usr/local/jboss/server/default/deploy/" + archive.getName());
   }

}
