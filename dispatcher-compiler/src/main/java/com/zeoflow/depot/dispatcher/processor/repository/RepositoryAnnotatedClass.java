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

package com.zeoflow.depot.dispatcher.processor.repository;

import androidx.annotation.NonNull;

import com.google.common.base.VerifyException;
import com.zeoflow.jx.file.TypeName;
import com.zeoflow.depot.dispatcher.processor.PreferenceKeyField;
import com.zeoflow.depot.dispatcher.processor.StringUtils;
import com.zeoflow.depot.dispatcher.processor.atom.AtomAnnotatedClass;
import com.zeoflow.depot.Dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

@SuppressWarnings("WeakerAccess")
public class RepositoryAnnotatedClass
{

    public final String packageName;
    public final TypeElement annotatedElement;
    public final TypeName typeName;
    public final String clazzName;
    public final String entityName;
    public final List<PreferenceKeyField> keyFields;
    public final List<String> keyNameFields;
    public final Map<String, PreferenceKeyField> keyFieldMap;
    public final Map<String, Element> setterFunctionsList;
    public final Map<String, Element> getterFunctionsList;
    public final Dao atomDao;
    public final List<ExecutableElement> executableElements;
    public AtomAnnotatedClass atomAnnotatedClass = null;

    public RepositoryAnnotatedClass(
            @NonNull TypeElement annotatedElement, @NonNull Elements elementUtils, List<AtomAnnotatedClass> entities)
            throws VerifyException
    {
        this.atomDao = annotatedElement.getAnnotation(Dao.class);
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
        this.entityName = StringUtils.toUpperCamel(this.clazzName);

        int i = 0;
        while (atomAnnotatedClass == null)
        {
            if (entities.get(i).entities.get(0).getPackage().equals(packageName))
            {
                atomAnnotatedClass = entities.get(i);
            }
            i++;
        }

        executableElements = ElementFilter.methodsIn(annotatedElement.getEnclosedElements());
        Map<String, String> checkMap = new HashMap<>();
        for(ExecutableElement elements: executableElements)
        {
            PreferenceKeyField keyField = new PreferenceKeyField(elements, elementUtils);

            if (checkMap.get(keyField.keyName) != null)
            {
                throw new VerifyException(String.format(
                        "'%s' key is already used in class.",
                        keyField.keyName
                ));
            }

            checkMap.put(keyField.keyName, keyField.clazzName);
            keyFields.add(keyField);
            keyNameFields.add(keyField.keyName);
            keyFieldMap.put(keyField.keyName, keyField);
        }
    }

}
