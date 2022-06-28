/*
 * Copyright (C) 2022 ZeoFlow SRL
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

import androidx.annotation.NonNull;

import com.zeoflow.jx.file.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import com.zeoflow.depot.dispatcher.Observable;
import com.zeoflow.depot.dispatcher.TypeConverter;

@SuppressWarnings({"WeakerAccess", "StringConcatenationInsideStringBufferAppend"})
public class PreferenceKeyField
{

    public VariableElement variableElement;
    public ExecutableElement executableElement;
    public String packageName;
    public TypeName typeName;
    public String clazzName;
    public String typeStringName;
    public String keyName;
    public String variableName;
    public Object value;

    public String converter;
    public String converterPackage;
    public boolean isObjectField = false;
    public Observable observable;

    public PreferenceKeyField(
            @NonNull VariableElement variableElement,
            @NonNull Elements elementUtils
    ) {
        this.variableElement = variableElement;
        PackageElement packageElement = elementUtils.getPackageOf(variableElement);
        this.packageName =
                packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        this.typeName = TypeName.get(variableElement.asType());
        this.clazzName = variableElement.getSimpleName().toString();
        this.value = variableElement.getConstantValue();
        setTypeStringName();

        this.keyName = StringUtils.toLowerCamel(this.clazzName);
        this.variableName = keyName;
        if (variableElement.getAnnotation(Observable.class) != null)
        {
            observable = executableElement.getAnnotation(Observable.class);
//            if (!observable.value().equals(""))
//            {
//                variableName = observable.value();
//            }
        }

        if (this.isObjectField)
        {
            variableElement.getAnnotationMirrors().stream()
                    .filter(
                            annotationMirror ->
                                    TypeName.get(annotationMirror.getAnnotationType())
                                            .equals(TypeName.get(TypeConverter.class)))
                    .forEach(
                            annotationMirror ->
                                    annotationMirror
                                            .getElementValues()
                                            .forEach(
                                                    (type, value) ->
                                                    {
                                                        String[] split = value.getValue().toString().split("\\.");
                                                        StringBuilder builder = new StringBuilder();
                                                        for (int i = 0; i < split.length - 1; i++)
                                                            builder.append(split[i] + ".");
                                                        this.converterPackage =
                                                                builder.toString().substring(0, builder.toString().length() - 1);
                                                        this.converter = split[split.length - 1];
                                                    }));
        }
    }
    public PreferenceKeyField(
            ExecutableElement element,
            Elements elementUtils
    ) {
        this.executableElement = element;
        PackageElement packageElement = elementUtils.getPackageOf(executableElement);
        this.packageName =
                packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        this.typeName = TypeName.get(executableElement.getReturnType());
        this.clazzName = executableElement.getSimpleName().toString();
        this.value = executableElement.getDefaultValue();
        setTypeStringName();

        this.keyName = StringUtils.toLowerCamel(this.clazzName);

        this.variableName = keyName;
        if (executableElement.getAnnotation(Observable.class) != null)
        {
            observable = executableElement.getAnnotation(Observable.class);
//            if (!observable.value().equals(""))
//            {
//                variableName = observable.value();
//            }
        }
    }
    private void setTypeStringName()
    {
        if (this.typeName.equals(TypeName.BOOLEAN))
        {
            this.typeStringName = "Boolean";
        } else if (this.typeName.equals(TypeName.INT))
        {
            this.typeStringName = "Int";
        } else if (this.typeName.equals(TypeName.FLOAT))
        {
            this.typeStringName = "Float";
        } else if (this.typeName.equals(TypeName.LONG))
        {
            this.typeStringName = "Long";
        } else if (this.typeName.equals(TypeName.get(String.class)))
        {
            this.typeStringName = "String";
        } else
        {
            this.typeStringName = this.typeName.toString();
            this.isObjectField = true;
        }
    }

}
