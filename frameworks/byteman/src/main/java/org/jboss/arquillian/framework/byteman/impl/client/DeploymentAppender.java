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
package org.jboss.arquillian.framework.byteman.impl.client;

import org.jboss.arquillian.framework.byteman.api.BMRule;
import org.jboss.arquillian.framework.byteman.impl.BytemanProfile;
import org.jboss.arquillian.spi.Profile;
import org.jboss.arquillian.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.asset.ArchiveAsset;

/**
 * BytemanDeploymentAppender
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class DeploymentAppender implements AuxiliaryArchiveAppender
{
   /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.client.deployment.AuxiliaryArchiveAppender#createAuxiliaryArchive()
    */
   public Archive<?> createAuxiliaryArchive()
   {
      return ShrinkWrap.create(JavaArchive.class, "arquillian-byteman.jar")
               .addClasses(Submit.class, ScriptText.class)
               .addPackages(false, 
                     org.jboss.arquillian.framework.byteman.impl.container.ScriptInstaller.class.getPackage(),
                     BytemanProfile.class.getPackage(),
                     BMRule.class.getPackage())
               .addServiceProvider(Profile.class, BytemanProfile.class)
               // add byteman archive as a resource in the jar, needed to install
               .add(
                     new ArchiveAsset(
                           ShrinkWrap.create(JavaArchive.class)
                           .addPackages(true, "org.jboss.byteman", "org.objectweb.asm", "java_cup")
                           .setManifest(
                                 new StringAsset(
                                   "Manifest-Version: 1.0\n" + 
                                   "Ant-Version: Apache Ant 1.8.0\n" + 
                                   "Created-By: 1.6.0_20-b20 (Sun Microsystems Inc.)\n" + 
                                   "Implementation-Version: 1.4.1\n" + 
                                   "Premain-Class: org.jboss.byteman.agent.Main\n" + 
                                   "Agent-Class: org.jboss.byteman.agent.Main\n" + 
                                   "Can-Redefine-Classes: true\n" + 
                                   "Can-Retransform-Classes: true\n")
                           ), ZipExporter.class), 
                    "byteman.jar");

   }
}