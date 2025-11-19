package org.ligi.gobandroid_hd

import org.assertj.core.api.AbstractAssert
import org.ligi.gobandroid_hd.CellAssert.Companion.assertThatCell
import org.ligi.gobandroid_hd.logic.Cell

class CellAssert(actual: Cell) :
    AbstractAssert<CellAssert, Cell>(actual, CellAssert::class.java) {

    fun posEqualTo(x: Int, y: Int): CellAssert {
        if (x != actual.x) {
            failWithMessage("Expected x to be <%s> but was <%s>", x, actual.x)
        }
        if (y != actual.y) {
            failWithMessage("Expected y to be <%s> but was <%s>", y, actual.y)
        }
        return this
    }

    companion object {
        fun assertThatCell(actual: Cell): CellAssert {
            return CellAssert(actual)
        }
    }
}

class CellListAssert(actual: List<Cell>) :
    AbstractAssert<CellListAssert, List<Cell>>(actual, CellListAssert::class.java) {

    fun sizeEqualTo(sz: Int) : CellListAssert {
        if(sz != actual.size) {
            failWithMessage("Expected size to be <%s> but was <%s>", sz, actual.size)
        }
        return this
    }

    fun atEqualTo(index: Int, x: Int, y: Int): CellListAssert {
        assertThatCell(actual[index]).posEqualTo(x, y)
        return this
    }

    companion object {
        fun assertThatCL(actual: List<Cell>): CellListAssert {
            return CellListAssert(actual)
        }
    }
}