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

import java.util.Arrays;

import org.jboss.arquillian.framework.byteman.api.BMRule;
import org.jboss.arquillian.framework.byteman.api.BMRules;
import org.jboss.arquillian.framework.byteman.impl.ScriptUtil;
import org.jboss.arquillian.framework.byteman.impl.SubmitException;
import org.jboss.arquillian.spi.core.annotation.Observes;
import org.jboss.arquillian.spi.event.suite.BeforeClass;
import org.jboss.byteman.agent.submit.ScriptText;
import org.jboss.byteman.agent.submit.Submit;

/**
 * ScriptInstaller
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ScriptInstaller
{
   /*
    * TODO: Handle multiple BMRule / BMRules pr Deployment, different rules for different containers
    */
   public void install(@Observes BeforeClass event)
   {
      BMRule rule = event.getTestClass().getAnnotation(BMRule.class);
      BMRules rules = event.getTestClass().getAnnotation(BMRules.class);
      if(rule != null && rules != null)
      {
         throw new IllegalArgumentException(
               "Both " + BMRule.class + " and " + BMRules.class + " annotations found on class " + event.getTestClass().getName() + 
               ", choose one.");
      }
      if(rule != null || rules != null)
      {
         String ruleKey = event.getTestClass().getName();
         String ruleScript = ScriptUtil.constructScriptText(toRuleArray(rule, rules));
         try
         {
            Submit submit = new Submit(); // new Submit(event.getDeploymentTarget().getAddress())
            submit.addScripts(Arrays.asList(new ScriptText(ruleKey, ruleScript)));
         }
         catch (Exception e) 
         {
            throw new SubmitException(
                  "Could not install script '" + rule.name() + "' from class " + event.getTestClass().getName(),
                  e);
         }
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
