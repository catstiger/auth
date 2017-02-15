package com.github.catstiger.core.db;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Entity;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class ModelClassLoader {
  private static final String RESOURCE_PATTERN = "/**/*.class";
  private static Set<Class<?>> entityClasses = new HashSet<>();
  
  public Iterator<Class<?>> getEntityClasses() {
    return entityClasses.iterator();
  }
  
  public void scanPackages(String... packagesToScan) {
    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    Set<String> entityClassNames = new TreeSet<String>();
    
    try {
      for (String pkg : packagesToScan) {
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
            ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        for (Resource resource : resources) {
          if (resource.isReadable()) {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            if (matchesEntityTypeFilter(reader, readerFactory)) {
              entityClassNames.add(className);
            }
          }
        }
      }
    }
    catch (IOException ex) {
      throw new MappingException("Failed to scan classpath for unlisted classes", ex);
    }
    try {
      ClassLoader cl = resourcePatternResolver.getClassLoader();
      for (String className : entityClassNames) {
        addAnnotatedClass(cl.loadClass(className));
      }
    }
    catch (ClassNotFoundException ex) {
      throw new MappingException("Failed to load annotated classes from classpath", ex);
    }
  }
  
  private void addAnnotatedClass(Class<?> loadClass) {
    if(!entityClasses.contains(loadClass)) {
      entityClasses.add(loadClass);
    }
  }

  private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
    TypeFilter[] entityTypeFilters = new TypeFilter[] {new AnnotationTypeFilter(Entity.class, false)};
    if (entityTypeFilters != null) {
      for (TypeFilter filter : entityTypeFilters) {
        if (filter.match(reader, readerFactory)) {
          return true;
        }
      }
    }
    return false;
  }
}
