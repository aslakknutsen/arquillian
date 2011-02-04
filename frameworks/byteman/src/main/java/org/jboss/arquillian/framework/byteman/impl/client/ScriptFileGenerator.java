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
import org.jboss.arquillian.framework.byteman.api.BMRules;
import org.jboss.arquillian.framework.byteman.impl.ScriptUtil;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.arquillian.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * ScriptFileGenerator
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ScriptFileGenerator implements ApplicationArchiveProcessor
{
   public void process(Archive<?> applicationArchive, TestClass testClass)
   {
      BMRule rule = testClass.getAnnotation(BMRule.class);
      BMRules rules = testClass.getAnnotation(BMRules.class);
      if(rule != null && rules != null)
      {
         throw new IllegalArgumentException(
               "Both " + BMRule.class + " and " + BMRules.class + " annotations found on class " + testClass.getName() + 
               ", choose one.");
      }
      if(rule != null || rules != null)
      {
         String ruleScript = ScriptUtil.constructScriptText(toRuleArray(rule, rules));
         applicationArchive.add(new StringAsset(ruleScript), "byteman.script");
      }
   }


   private BMRule[] toRuleArray(BMRule rule, BMRules rules)
   {
      if(rule != null)
      {
         return new BMRule[] {rule};
      }
      return rules.value();
   }
}
