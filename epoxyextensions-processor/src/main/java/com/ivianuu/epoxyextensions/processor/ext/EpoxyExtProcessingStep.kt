/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.epoxyextensions.processor.ext

import com.airbnb.epoxy.EpoxyModelClass
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.base.CaseFormat
import com.google.common.collect.SetMultimap
import com.ivianuu.epoxyextensions.processor.javaToKotlinType
import com.squareup.kotlinpoet.ClassName
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

/**
 * @author Manuel Wrage (IVIanuu)
 */
class EpoxyExtProcessingStep(private val processingEnv: ProcessingEnvironment) : BasicAnnotationProcessor.ProcessingStep {

    private val generators = mutableSetOf<EpoxyExtGenerator>()

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<Element> {
        elementsByAnnotation[EpoxyModelClass::class.java]
            .filterIsInstance<TypeElement>()
            .map(this::createDescriptor)
            .map(::EpoxyExtGenerator)
            .forEach { generators.add(it) }

        return mutableSetOf()
    }

    override fun annotations() =
        mutableSetOf(EpoxyModelClass::class.java)

    fun writeFiles() {
        generators.forEach(this::generateAndWriteFile)
        generators.clear()
    }

    private fun createDescriptor(element: TypeElement): EpoxyExtDescriptor {
        val fileName = ClassName.bestGuess(
            element.asType().toString() + "Ext"
        )

        val builderName =
            ClassName.bestGuess(element.asType().toString() + "Builder_")

        val modelName =
            ClassName.bestGuess(element.asType().toString() + "_")

        // todo handle multiple constructors?
        val constructorParams =
            element.enclosedElements
                .filterIsInstance<ExecutableElement>()
                .first { it.kind == ElementKind.CONSTRUCTOR }
                .parameters
                .map { it.simpleName.toString() to it.javaToKotlinType() }

        var initName= CaseFormat.UPPER_CAMEL.converterTo(
            CaseFormat.LOWER_CAMEL)
            .convert(element.simpleName.toString())!!

        if (initName.endsWith("Model")) {
            initName = initName.replace("Model", "")
        }

        return EpoxyExtDescriptor(
            element,
            initName,
            fileName,
            builderName,
            modelName,
            constructorParams
        )
    }

    private fun generateAndWriteFile(generator: EpoxyExtGenerator) {
        val fileUri = processingEnv.filer
            .createSourceFile(generator.descriptor.fileName.packageName()
                    + "." + generator.descriptor.fileName.simpleName()).toUri()

        generator.generate().writeTo(File(fileUri))
    }
}