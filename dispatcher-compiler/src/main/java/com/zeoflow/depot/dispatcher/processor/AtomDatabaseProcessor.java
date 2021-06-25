/*
 * Copyright (C) 2017 depot
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

package com.zeoflow.depot.dispatcher.processor;

import com.google.auto.service.AutoService;
import com.google.common.base.VerifyException;
import com.zeoflow.depot.dispatcher.processor.repository.RepositoryAnnotatedClass;
import com.zeoflow.jx.file.ClassName;
import com.zeoflow.jx.file.JavaFile;
import com.zeoflow.jx.file.TypeSpec;
import com.zeoflow.depot.dispatcher.DepotDispatcher;
import com.zeoflow.depot.dispatcher.PreferenceApp;
import com.zeoflow.depot.dispatcher.processor.atom.AtomAnnotatedClass;
import com.zeoflow.depot.dispatcher.processor.atom.AtomGenerator;
import com.zeoflow.depot.dispatcher.processor.model.ModelAnnotatedClass;
import com.zeoflow.depot.dispatcher.processor.model.ModelGenerator;
import com.zeoflow.depot.dispatcher.processor.repository.RepositoryAnnotatedClass;
import com.zeoflow.depot.dispatcher.processor.repository.RepositoryGenerator;
import com.zeoflow.depot.Dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

import static javax.tools.Diagnostic.Kind.ERROR;

@SuppressWarnings({"unused", "RedundantSuppression"})
@AutoService(Processor.class)
public class AtomDatabaseProcessor extends AbstractProcessor
{

    private Map<String, AtomAnnotatedClass> annotatedEntityMap;
    private Map<String, RepositoryAnnotatedClass> annotatedRepositoriesMap;
    private Map<String, ModelAnnotatedClass> annotatedModelsMap;
    private Messager messager;
    private List<AtomAnnotatedClass> entities;
    private Types mTypeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        mTypeUtils = processingEnv.getTypeUtils();
        annotatedEntityMap = new HashMap<>();
        annotatedRepositoriesMap = new HashMap<>();
        annotatedModelsMap = new HashMap<>();
        messager = processingEnv.getMessager();
        entities = new ArrayList<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> supportedTypes = new HashSet<>();
        supportedTypes.add(DepotDispatcher.class.getCanonicalName());
        supportedTypes.add(Dao.class.getCanonicalName());
        return supportedTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env)
    {
//        Set<? extends Element> elements = env.getElementsAnnotatedWith(Observable.class);
//        Set<ExecutableElement> fields = ElementFilter.methodsIn(elements);

        if (annotations.isEmpty())
        {
            return true;
        }

        env.getElementsAnnotatedWith(DepotDispatcher.class).stream()
                .map(annotatedType -> (TypeElement) annotatedType)
                .forEach(
                        annotatedType ->
                        {
                            try
                            {
                                checkValidEntityType(annotatedType);
                                processEntity(annotatedType);
                            } catch (IllegalAccessException e)
                            {
                                showErrorLog(e.getMessage(), annotatedType);
                            }
                        });

        for (Element annotatedType : env.getElementsAnnotatedWith(Dao.class))
        {
            if (!(annotatedType instanceof TypeElement))
                continue;
            try
            {
                checkValidInterfaceType((TypeElement) annotatedType);
                processRepositories((TypeElement) annotatedType);
                processModel((TypeElement) annotatedType);
            } catch (IllegalAccessException e)
            {
                showErrorLog(e.getMessage(), annotatedType);
            }
        }
        return true;
    }
    private void processEntity(TypeElement annotatedType) throws VerifyException
    {
        try
        {
            AtomAnnotatedClass annotatedClazz = new AtomAnnotatedClass(
                    annotatedType,
                    processingEnv.getElementUtils()
            );
            checkDuplicatedPreferenceEntity(annotatedClazz);
            generateProcessEntity(annotatedClazz);
        } catch (VerifyException e)
        {
            showErrorLog(e.getMessage(), annotatedType);
            e.printStackTrace();
        }
    }
    private void generateProcessEntity(AtomAnnotatedClass annotatedClass)
    {
        try
        {
            TypeSpec annotatedClazz = new AtomGenerator(
                    annotatedClass,
                    processingEnv.getElementUtils()
            ).generate();
            JavaFile.builder(annotatedClass.packageName, annotatedClazz)
                    .addStaticImport(ClassName.get(PreferenceApp.class), "getContext")
                    .build()
                    .writeTo(processingEnv.getFiler());
            this.entities.add(annotatedClass);
        } catch (IOException e)
        {
            // ignore ;)
        }
    }
    private void processRepositories(TypeElement annotatedType) throws VerifyException
    {
        try
        {
            RepositoryAnnotatedClass annotatedClazz = new RepositoryAnnotatedClass(
                    annotatedType,
                    processingEnv.getElementUtils(),
                    entities
            );
            checkDuplicatedPreferenceEntity(annotatedClazz);
            generateRepositories(annotatedClazz);
        } catch (VerifyException e)
        {
            showErrorLog(e.getMessage(), annotatedType);
            e.printStackTrace();
        }
    }
    private void generateRepositories(RepositoryAnnotatedClass annotatedClass)
    {
        try
        {
            TypeSpec annotatedClazz = new RepositoryGenerator(
                    annotatedClass,
                    processingEnv.getElementUtils()
            ).generate();
            JavaFile.builder(annotatedClass.atomAnnotatedClass.packageName, annotatedClazz)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e)
        {
//             ignore ;)
        }
    }
    private void processModel(TypeElement annotatedType) throws VerifyException
    {
//        for(AtomAnnotatedClass packages: annotatedEntityMap.values())
//        {
//            for (ClassName className: packages.dbBeans)
//            {
//                messager.printMessage(
//                        WARNING,
//                        className.getPackage()
//                );
//            }
//        }
        try
        {
            ModelAnnotatedClass annotatedClazz = new ModelAnnotatedClass(
                    annotatedType,
                    processingEnv.getElementUtils(),
                    entities,
                    annotatedEntityMap
            );
            checkDuplicatedPreferenceEntity(annotatedClazz);
            generateModel(annotatedClazz);
        } catch (VerifyException e)
        {
            showErrorLog(e.getMessage(), annotatedType);
            e.printStackTrace();
        }
    }
    private void generateModel(ModelAnnotatedClass annotatedClass)
    {
        try
        {
            TypeSpec annotatedClazz = new ModelGenerator(
                    annotatedClass,
                    processingEnv.getElementUtils()
            ).generate();
            JavaFile.builder(annotatedClass.atomAnnotatedClass.packageName, annotatedClazz)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e)
        {
//             ignore ;)
        }
    }
    private void checkValidEntityType(TypeElement annotatedType) throws IllegalAccessException
    {
        if (!annotatedType.getKind().isClass())
        {
            throw new IllegalAccessException("Only classes can be annotated with @PreferenceDepot");
        } else if (annotatedType.getModifiers().contains(Modifier.FINAL))
        {
            showErrorLog("class modifier should not be final", annotatedType);
        } else if (annotatedType.getModifiers().contains(Modifier.PRIVATE))
        {
            showErrorLog("class modifier should not be final", annotatedType);
        }
    }
    private void checkValidInterfaceType(TypeElement annotatedType) throws IllegalAccessException
    {
        if (!annotatedType.getKind().isInterface())
        {
            throw new IllegalAccessException("Only interfaces can be annotated with @PreferenceDepot");
        } else if (annotatedType.getModifiers().contains(Modifier.FINAL))
        {
            showErrorLog("class modifier should not be final", annotatedType);
        } else if (annotatedType.getModifiers().contains(Modifier.PRIVATE))
        {
            showErrorLog("class modifier should not be final", annotatedType);
        }
    }
    private void checkValidComponentType(TypeElement annotatedType) throws IllegalAccessException
    {
        if (!annotatedType.getKind().isInterface())
        {
            throw new IllegalAccessException(
                    "Only interfaces can be annotated with @PreferenceComponent");
        }
    }
    private void checkDuplicatedPreferenceEntity(AtomAnnotatedClass annotatedClazz)
            throws VerifyException
    {
        if (annotatedEntityMap.containsKey(annotatedClazz.entityName))
        {
            throw new VerifyException("@PreferenceDepot key value is duplicated.");
        } else
        {
            annotatedEntityMap.put(annotatedClazz.entityName, annotatedClazz);
        }
    }
    private void checkDuplicatedPreferenceEntity(RepositoryAnnotatedClass annotatedClazz)
            throws VerifyException
    {
        if (annotatedRepositoriesMap.containsKey(annotatedClazz.entityName))
        {
            throw new VerifyException("@PreferenceDepot key value is duplicated.");
        } else
        {
            annotatedRepositoriesMap.put(annotatedClazz.entityName, annotatedClazz);
        }
    }
    private void checkDuplicatedPreferenceEntity(ModelAnnotatedClass annotatedClazz)
            throws VerifyException
    {
        if (annotatedModelsMap.containsKey(annotatedClazz.entityName))
        {
            throw new VerifyException("@PreferenceDepot key value is duplicated.");
        } else
        {
            annotatedModelsMap.put(annotatedClazz.entityName, annotatedClazz);
        }
    }
    private void showErrorLog(String message, Element element)
    {
        messager.printMessage(ERROR, "error: " + message, element);
    }

}
