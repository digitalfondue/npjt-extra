/**
 * Copyright © 2019 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.npjt;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Map;
import java.util.Set;

@Configuration
public class RepositoriesDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

        Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(EnableNpjt.class.getCanonicalName());
        String[] basePackages = (String[]) annotationAttributes.get("basePackages");
        String activeDb = (String) annotationAttributes.get("activeDB");

        if (basePackages != null) {
            CustomClasspathScanner scanner = new CustomClasspathScanner();
            for (String packageToScan : basePackages) {
                Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageToScan);
                handleCandidates(candidates, beanDefinitionRegistry, activeDb);
            }
        }
    }

    private void handleCandidates(Set<BeanDefinition> candidates, BeanDefinitionRegistry beanDefinitionRegistry, String activeDB) {
        try {
            for (BeanDefinition beanDefinition : candidates) {
                Class<?> c = Class.forName(beanDefinition.getBeanClassName());
                AbstractBeanDefinition abd = BeanDefinitionBuilder.rootBeanDefinition(QueryFactory2.class)
                        .addConstructorArgValue(c)
                        .addConstructorArgValue(activeDB)
                        .getBeanDefinition();
                beanDefinitionRegistry.registerBeanDefinition(beanDefinition.getBeanClassName(), abd);
                //beanFactory.registerSingleton(beanDefinition.getBeanClassName(), abd);
                //System.err.println("bean definition is " + beanDefinition.getBeanClassName());
            }
        } catch (ClassNotFoundException cnf) {
            throw new IllegalStateException("Error while loading class", cnf);
        }
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
}
