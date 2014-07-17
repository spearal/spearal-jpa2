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
package org.spearal.jpa2;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

/**
 * @author William DRAI
 */
@SuppressWarnings("rawtypes")
public class SpearalPersistenceProvider implements PersistenceProvider {

	private final PersistenceProvider persistenceProvider;
	
	private static final List<PersistenceProvider> persistenceProviders = PersistenceProviderResolverHolder.getPersistenceProviderResolver().getPersistenceProviders();

	
	public SpearalPersistenceProvider() {
		PersistenceProvider persistenceProvider = null;
		for (PersistenceProvider pp : persistenceProviders) {
			if (pp.getClass().equals(getClass()))
				continue;
			persistenceProvider = pp;
			break;
		}
		if (persistenceProvider == null)
			throw new RuntimeException("No persistence provider found");
		
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo puInfo, Map params) {
		EntityManagerFactory entityManagerFactory = persistenceProvider.createContainerEntityManagerFactory(puInfo, params);
		return new EntityManagerFactoryWrapper(entityManagerFactory);
	}
	
	@Override
	public EntityManagerFactory createEntityManagerFactory(String puName, Map params) {
		EntityManagerFactory entityManagerFactory = persistenceProvider.createEntityManagerFactory(puName, params);
		return new EntityManagerFactoryWrapper(entityManagerFactory);
	}

	@Override
	public void generateSchema(PersistenceUnitInfo puInfo, Map params) {
		persistenceProvider.generateSchema(puInfo, params);
	}

	@Override
	public boolean generateSchema(String puName, Map params) {
		return persistenceProvider.generateSchema(puName, params);
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return persistenceProvider.getProviderUtil();
	}

}
