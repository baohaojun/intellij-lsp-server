package com.ruin.lsp.commands.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import com.ruin.lsp.commands.Command
import com.ruin.lsp.util.withEditor
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.*

class CompletionCommand(val position: Position,
                        val snippetSupport: Boolean) : Command<Either<MutableList<CompletionItem>, CompletionList>>, Disposable {

    override fun execute(project: Project, file: PsiFile): Either<MutableList<CompletionItem>, CompletionList> {
        val result: MutableList<CompletionItem> = mutableListOf()
        val prefix: String? = null

        withEditor(this, file, position) { editor ->
            val params = makeCompletionParameters(editor, file, position)
            performCompletion(params!!, prefix, Consumer { completionResult ->
                val el = completionResult.lookupElement
                val dec = CompletionDecorator.from(el, snippetSupport)
                if (dec != null) {
                    result.add(dec.completionItem)
                }
            })
        }
        return Either.forRight(CompletionList(false, result))
    }
}

fun performCompletion(parameters: CompletionParameters,
                      prefix: String?,
                      consumer: Consumer<CompletionResult>?): Array<LookupElement> {
    val lookupSet = LinkedHashSet<LookupElement>()

    getVariantsFromContributors(parameters, prefix, null, Consumer { result ->
        if (lookupSet.add(result.lookupElement) && consumer != null) {
            consumer.consume(result)
        }
    })
    return lookupSet.toTypedArray()
}

/**
 * Run all contributors until any of them returns false or the list is exhausted. If from parameter is not null, contributors
 * will be run starting from the next one after that.
 */
fun getVariantsFromContributors(parameters: CompletionParameters,
                                prefix: String?, from: CompletionContributor?,
                                consumer: Consumer<CompletionResult>) {
    val contributors = CompletionContributor.forParameters(parameters)
    for (i in contributors.indexOf(from) + 1 until contributors.size) {
        val contributor = contributors[i]

        val result = createResultSet(parameters, prefix, consumer, contributor)
        contributor.fillCompletionVariants(parameters, result)
        if (result.isStopped) {
            return
        }
    }

}

fun createResultSet(parameters: CompletionParameters, userPrefix: String?,
                    consumer: Consumer<CompletionResult>, contributor: CompletionContributor): CompletionResultSet {
    val position = parameters.position
    val prefix = userPrefix ?: findPrefix(position, parameters.offset)
    val lengthOfTextBeforePosition = parameters.offset
    val matcher = CamelHumpMatcher(prefix, false)
    val sorter = CompletionService.getCompletionService().defaultSorter(parameters, matcher)
    return CompletionResultSetImpl(consumer, lengthOfTextBeforePosition, matcher,
        contributor, parameters, sorter, null)
}

fun findPrefix(position: PsiElement, offset: Int): String {
    // Class is deprecated, but the method seems to be used...
    return CompletionData.findPrefixStatic(position, offset)
}
