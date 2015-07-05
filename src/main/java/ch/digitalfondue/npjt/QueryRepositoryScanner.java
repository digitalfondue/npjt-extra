/**
 * Copyright (C) 2015 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.npjt;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class QueryRepositoryScanner implements BeanFactoryPostProcessor {

	private final QueryFactory queryFactory;
	private final String[] packagesToScan;

	public QueryRepositoryScanner(QueryFactory queryFactory, String... packagesToScan) {
		this.queryFactory = queryFactory;
		this.packagesToScan = packagesToScan;
	}
	

	private static class CustomClasspathScanner extends ClassPathScanningCandidateComponentProvider {

		public CustomClasspathScanner() {
			super(false);
			addIncludeFilter(new AnnotationTypeFilter(QueryRepository.class, false));
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return beanDefinition.getMetadata().isInterface();
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		
		if (packagesToScan != null) {
			CustomClasspathScanner scanner = new CustomClasspathScanner();
			for (String packageToScan : packagesToScan) {
				Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageToScan);
				handleCandidates(candidates, beanFactory);
			}
		}
	}

	private void handleCandidates(Set<BeanDefinition> candidates, final ConfigurableListableBeanFactory beanFactory) {
		try {
			for (BeanDefinition beanDefinition : candidates) {
				final Class<?> c = Class.forName(beanDefinition.getBeanClassName());
				beanFactory.registerSingleton(beanDefinition.getBeanClassName(), queryFactory.from(c));
			}
		} catch (ClassNotFoundException cnf) {
			throw new IllegalStateException("Error while loading class", cnf);
		}
	}
}
