package org.wikipedia.edit

import android.text.Layout

class LineNumberHelper {
    lateinit var goodLines: BooleanArray
    lateinit var realLines: IntArray

    fun computeLines(startLine: Int, lineCount: Int, layout: Layout, text: String?) {
        val lineContainsNewlineChar = BooleanArray(lineCount)

        goodLines = BooleanArray(lineCount)
        realLines = IntArray(lineCount)

        if (text.isNullOrEmpty()) {
            goodLines[0] = false
            realLines[0] = 1
            return
        }

        var i = 0

        while (i < lineCount) {
            // check if this line contains "\n"
            //hasNewLineArray[i] = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).endsWith("\n");
            lineContainsNewlineChar[i] = text[layout.getLineEnd(i) - 1] == '\n'
            // if true
            if (lineContainsNewlineChar[i]) {
                var j = i - 1
                while (j > -1 && !lineContainsNewlineChar[j]) {
                    j--
                }
                goodLines[j + 1] = true

            }
            i++
        }

        goodLines[lineCount - 1] = true

        var realLine = startLine // the first line is not 0, is 1. We start counting from 1

        i = 0
        while (i < goodLines.size) {
            if (goodLines[i]) {
                realLine++
            }
            realLines[i] = realLine
            i++
        }
    }
}
