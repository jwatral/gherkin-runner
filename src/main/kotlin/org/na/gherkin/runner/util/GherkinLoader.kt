package org.na.gherkin.runner.util

import gherkin.AstBuilder
import gherkin.Parser
import gherkin.ast.*
import org.na.gherkin.runner.gherkin.GherkinFeature
import org.na.gherkin.runner.gherkin.GherkinScenario
import org.na.gherkin.runner.gherkin.GherkinStep
import org.na.gherkin.runner.gherkin.StepKeyword
import org.na.gherkin.runner.gherkin.StepKeyword.*
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.util.regex.Pattern


object GherkinLoader {
    @JvmStatic
    @JvmOverloads
    fun loadFeatures(packagePrefix: String = ""): List<GherkinFeature> {
        val reflections = Reflections(packagePrefix, ResourcesScanner())
        val fileNames = reflections.getResources(Pattern.compile(".*\\.feature"))
        return fileNames.map({ readFeatureFromFile(it) })
    }

    private fun readFeatureFromFile(path: String): GherkinFeature {
        val parser = Parser<GherkinDocument>(AstBuilder())
        val featureFileContent = this.javaClass.classLoader.getResource(path).readText()
        val gherkinDocument = parser.parse(featureFileContent)
        val feature = gherkinDocument.feature
        return convertFeature(feature)
    }

    private fun convertFeature(feature: Feature): GherkinFeature {
        val gherkinFeature = GherkinFeature(feature.name)
        feature.children
            .map({ convertScenario(gherkinFeature, it) })
            .forEach({ gherkinFeature.addScenario(it) })
        return gherkinFeature

    }

    private fun convertScenario(feature: GherkinFeature, scenario: ScenarioDefinition): GherkinScenario {
        val gherkinScenario = GherkinScenario(scenario.name, scenario.description?.trim(), feature)
        scenario.steps.forEach {
            val stepKeyword = convertStepKeyword(it.keyword)
            val realKeyword =
                    if (stepKeyword == AND)
                        if (scenario.steps.isEmpty()) GIVEN else gherkinScenario.steps.last().realKeyword
                    else stepKeyword
            val gherkinStep = convertStep(gherkinScenario, stepKeyword, realKeyword, it)
            gherkinScenario.addStep(gherkinStep)
        }
        return gherkinScenario
    }

    private fun convertStep(scenario: GherkinScenario, stepKeyword: StepKeyword, realKeyword: StepKeyword,  step: Step): GherkinStep {
        val argument = step.argument
        val data = argument as? DataTable
        return GherkinStep(stepKeyword, realKeyword, step.text, scenario, data?.to2DArray())
    }

    private fun convertStepKeyword(keyword: String): StepKeyword {
        return valueOf(keyword.toUpperCase().trim())
    }

    private fun DataTable.to2DArray()
        = this.rows.map { it.cells.map { it.value }.toTypedArray() }.toTypedArray()

}