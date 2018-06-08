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

package com.ivianuu.epoxyextensions.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.ivianuu.epoxyextensions.processor.builder.EpoxyBuilderProcessingStep
import com.ivianuu.epoxyextensions.processor.ext.EpoxyExtProcessingStep
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment

/**
 * Processor
 */
@AutoService(Processor::class)
class EpoxyPrefsProcessor : BasicAnnotationProcessor() {

    private val builderProcessingStep by lazy {
        EpoxyBuilderProcessingStep(processingEnv)
    }

    private val extProcessingStep by lazy {
        EpoxyExtProcessingStep(processingEnv)
    }

    override fun initSteps() =
        mutableSetOf(
            builderProcessingStep,
            extProcessingStep
        )

    override fun postRound(roundEnv: RoundEnvironment) {
        super.postRound(roundEnv)

        // for some reason the epoxy processor don't likes additional files
        // so we write our extensions when the processing is over
        if (roundEnv.processingOver()) {
            builderProcessingStep.writeFiles()
            extProcessingStep.writeFiles()
        }
    }
}