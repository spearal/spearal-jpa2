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

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;

import org.spearal.SpearalFactory;

/**
 * @author William DRAI
 */
public class SpearalPersistenceProviderResolver implements PersistenceProviderResolver {
	
	private final PersistenceProviderResolver persistenceProviderResolver;
	private final SpearalFactory factory;
	
	public SpearalPersistenceProviderResolver(PersistenceProviderResolver wrappedPersistenceProviderResolver, SpearalFactory factory) {
		this.persistenceProviderResolver = wrappedPersistenceProviderResolver;
		this.factory = factory;
	}
	
	public PersistenceProviderResolver getWrappedPersistenceProviderResolver() {
		return persistenceProviderResolver;
	}
	
	public static void initSpearalJPA(SpearalFactory factory) {
		PersistenceProviderResolver persistenceProviderResolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();
		if (persistenceProviderResolver instanceof SpearalPersistenceProviderResolver) {
			// Rewrap to update the factory if necessary
			persistenceProviderResolver = ((SpearalPersistenceProviderResolver)persistenceProviderResolver).getWrappedPersistenceProviderResolver();
		}
		
		PersistenceProviderResolverHolder.setPersistenceProviderResolver(
				new SpearalPersistenceProviderResolver(persistenceProviderResolver, factory));
	}
	
	public static void resetSpearalJPA(SpearalFactory factory) {
		if (!(PersistenceProviderResolverHolder.getPersistenceProviderResolver() instanceof SpearalPersistenceProviderResolver))
			return;
		PersistenceProviderResolverHolder.setPersistenceProviderResolver(
				((SpearalPersistenceProviderResolver)PersistenceProviderResolverHolder.getPersistenceProviderResolver()).getWrappedPersistenceProviderResolver());
	}
	
	@Override
	public List<PersistenceProvider> getPersistenceProviders() {
		List<PersistenceProvider> persistenceProviders = persistenceProviderResolver.getPersistenceProviders();
		for (int i = 0; i < persistenceProviders.size(); i++)
			persistenceProviders.set(i, new PersistenceProviderWrapper(persistenceProviders.get(i), factory));
		
		return persistenceProviders;
	}
	
	@Override
	public void clearCachedProviders() {
		persistenceProviderResolver.clearCachedProviders();
	}

}
