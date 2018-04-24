package com.ruin.lsp

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.ruin.lsp.util.differenceFromAction
import com.ruin.lsp.util.getDocument
import org.eclipse.lsp4j.TextEdit
import org.intellivim.FileEditingTestCase

class EditorUtilTest : FileEditingTestCase() {
    override val projectName: String
        get() = JAVA_PROJECT

    override val filePath: String
        get() = DUMMY_FILE_PATH

    fun `test text edits from document differences`() {
        val edits = differenceFromAction(psiFile) { editor, _ ->
            // insert
            editor.document.insertString(0, "Hey, dood!")
            // delete
            editor.document.deleteString(40, 45)
            // replace
            editor.document.deleteString(60, 65)
            editor.document.insertString(60, "blah")
        }
        assertNotNull(edits)
        assertSameElements(edits!!.toList(), listOf(
            TextEdit(range(0, 0, 0, 0), "Hey, dood!"),
            TextEdit(range(2, 0, 2, 5), ""),
            TextEdit(range(2, 25, 4, 1), "blah")
        ))
    }

    fun `test text edits after document change`() {
        val doc = getDocument(psiFile)!!
        runUndoTransparentWriteAction {
            doc.insertString(0, "Cowabunga!")
        }
        val edits = differenceFromAction(psiFile) { editor, _ ->
            editor.document.insertString(0, "Hey, dood!")
        }
        assertNotNull(edits)
        assertSameElements(edits!!.toList(), listOf(
            TextEdit(range(0, 0, 0, 0), "Hey, dood!")
        ))
    }

    fun `test text edits after view provider document change`() {
        val doc = psiFile.viewProvider.document!!
        runUndoTransparentWriteAction {
            doc.insertString(0, "Cowabunga!")
        }
        val edits = differenceFromAction(psiFile) { editor, _ ->
            editor.document.insertString(0, "Hey, dood!")
        }
        assertNotNull(edits)
        assertSameElements(edits!!.toList(), listOf(
            TextEdit(range(0, 0, 0, 0), "Hey, dood!")
        ))
    }
}
