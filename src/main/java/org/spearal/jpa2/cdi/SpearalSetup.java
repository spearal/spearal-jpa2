/**
 * == @Spearal ==>
 * 
 * Copyright (C) 2014 Franck WOLFF & William DRAI (http://www.spearal.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearal.jpa2.cdi;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.spearal.SpearalFactory;
import org.spearal.jpa2.SpearalConfigurator;

/**
 * @author William DRAI
 */
@ApplicationScoped
public class SpearalSetup {
	
	@Inject
	private Instance<EntityManagerFactory> entityManagerFactories;
	
	@Inject
	private Instance<SpearalFactory> spearalFactory;

	@PostConstruct
	public void init() {
		if (spearalFactory.isUnsatisfied())
			return;
		
		if (spearalFactory.isAmbiguous())
			throw new RuntimeException("Multiple SpearalFactory definitions found");
			
		for (EntityManagerFactory entityManagerFactory : entityManagerFactories) {
			SpearalConfigurator.init(spearalFactory.get(), entityManagerFactory);
		}
	}	
}
