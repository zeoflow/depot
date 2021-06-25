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

package com.zeoflow.depot.dispatcher.processor.atom;

import androidx.annotation.NonNull;

import com.google.common.base.VerifyException;
import com.zeoflow.depot.dispatcher.processor.PreferenceKeyField;
import com.zeoflow.depot.dispatcher.processor.StringUtils;
import com.zeoflow.jx.file.ClassName;
import com.zeoflow.jx.file.TypeName;
import com.zeoflow.depot.dispatcher.DepotDispatcher;
import com.zeoflow.depot.dispatcher.DispatcherName;
import com.zeoflow.depot.dispatcher.processor.PreferenceKeyField;
import com.zeoflow.depot.dispatcher.processor.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

@SuppressWarnings("WeakerAccess")
public class AtomAnnotatedClass
{

    public final String packageName;
    public final TypeElement annotatedElement;
    public final TypeName typeName;
    public final String clazzName;
    public final String entityName;
    public final List<PreferenceKeyField> keyFields;
    public final List<String> keyNameFields;
    public final List<ClassName> dbBeans;
    public final List<ClassName> entities;
    public final Map<String, PreferenceKeyField> keyFieldMap;
    public final Map<String, Element> setterFunctionsList;
    public final Map<String, Element> getterFunctionsList;
    public final DepotDispatcher depotDispatcher;

    public AtomAnnotatedClass(
            @NonNull TypeElement annotatedElement, @NonNull Elements elementUtils)
            throws VerifyException
    {
        depotDispatcher = annotatedElement.getAnnotation(DepotDispatcher.class);
        PackageElement packageElement = elementUtils.getPackageOf(annotatedElement);
        this.packageName =
                packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        this.annotatedElement = annotatedElement;
        this.typeName = TypeName.get(annotatedElement.asType());
        this.clazzName = annotatedElement.getSimpleName().toString();
        this.keyFields = new ArrayList<>();
        this.keyNameFields = new ArrayList<>();
        this.keyFieldMap = new HashMap<>();
        this.setterFunctionsList = new HashMap<>();
        this.getterFunctionsList = new HashMap<>();
        this.dbBeans = new ArrayList<>();
        this.entities = new ArrayList<>();

        for(AnnotationMirror annotationMirror : annotatedElement.getAnnotationMirrors())
        {
            if(!TypeName.get(annotationMirror.getAnnotationType()).equals(TypeName.get(DepotDispatcher.class)))
            {
                continue;
            }
            for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> annotationValue: annotationMirror.getElementValues().entrySet())
            {
                if(annotationValue.getKey().getSimpleName().contentEquals("dbBeans"))
                {
                    String[] values = annotationValue.getValue().getValue().toString().split(",");
                    for(String value: values)
                    {
                        try
                        {
                            dbBeans.add(ClassName.get(Class.forName(value)));
                        } catch (ClassNotFoundException e)
                        {
                            List<String> components = Arrays.asList(value.split("\\."));
                            String className = components.get(components.size() - 1);
                            value = value.replaceFirst(".class", "");
                            value = value.replaceFirst(className, "");
                            dbBeans.add(ClassName.get(value, className));
                        }
                    }
                } else if(annotationValue.getKey().getSimpleName().contentEquals("entities"))
                {
                    String[] values = annotationValue.getValue().getValue().toString().split(",");
                    for(String value: values)
                    {
                        try
                        {
                            entities.add(ClassName.get(Class.forName(value)));
                        } catch (ClassNotFoundException e)
                        {
                            List<String> components = Arrays.asList(value.split("\\."));
                            String className = components.get(components.size() - 1);
                            value = value.replaceFirst(".class", "");
                            value = value.replaceFirst(className, "");
                            entities.add(ClassName.get(value, className));
                        }
                    }
                }
            }
        }

        // set class name
        DispatcherName dispatcherName = annotatedElement.getAnnotation(DispatcherName.class);
        if (dispatcherName != null && !dispatcherName.value().equals(""))
        {
            this.entityName = dispatcherName.value();
        } else
        {
            this.entityName = StringUtils.toUpperCamel(this.clazzName);
        }

        Map<String, String> checkMap = new HashMap<>();
        for(Element element: annotatedElement.getEnclosedElements())
        {
            if (!(element instanceof VariableElement))
            {
                continue;
            }
            VariableElement variable = (VariableElement) element;
            PreferenceKeyField keyField = new PreferenceKeyField(variable, elementUtils);

            if (checkMap.get(keyField.keyName) != null)
            {
                throw new VerifyException(
                        String.format("'%s' key is already used in class.", keyField.keyName));
            }

            checkMap.put(keyField.keyName, keyField.clazzName);
            keyFields.add(keyField);
            keyNameFields.add(keyField.keyName);
            keyFieldMap.put(keyField.keyName, keyField);
        }
    }

}
