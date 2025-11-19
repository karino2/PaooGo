package org.ligi.gobandroid_hd

import io.github.karino2.paoogo.goengine.fuseki.FusekiBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.gobandroid_hd.CellAssert.Companion.assertThatCell
import org.ligi.gobandroid_hd.CellListAssert.Companion.assertThatCL
import org.ligi.gobandroid_hd.logic.CellImpl
import org.ligi.gobandroid_hd.logic.sgf.SGFReader

class TestFusekiBuilder {
    companion object {
        const val FURIKO = FusekiBuilder.FURIKO
    }
    val target = FusekiBuilder()

    @Test
    fun testHello() {
        val game = SGFReader.sgf2game(FURIKO, null)!!
        assertThat(game.findLastMove().movePos).isEqualTo(5)
        val moveZero = game.actMove
        val nextMoveCell = moveZero.getNextMove(0)!!.cell!!
        assertThatCell(nextMoveCell).posEqualTo(4, 4)
    }

    @Test
    fun testToCellList() {
        val actual = target.toCellList(FURIKO)
        assertThatCL(actual)
            .sizeEqualTo(5)
            .atEqualTo(0, 4, 4)
            .atEqualTo(1, 3, 6)
            .atEqualTo(2, 2, 5)
            .atEqualTo(3, 5, 6)
            .atEqualTo(4, 5, 2)
    }
    @Test
    fun testSymmetryTengen() {
        for (i in 0..<8) {
            assertThatCell(target.symmetry(i, CellImpl(4, 4))).posEqualTo(4, 4)
        }
    }

    @Test
    fun testSymmetryDegree() {
        assertThatCell(target.symmetry(0, CellImpl(3, 6))).posEqualTo(3, 6)
        assertThatCell(target.symmetry(1, CellImpl(3, 6))).posEqualTo(6, 5)
        assertThatCell(target.symmetry(2, CellImpl(3, 6))).posEqualTo(5, 2)
        assertThatCell(target.symmetry(3, CellImpl(3, 6))).posEqualTo(2, 3)

        assertThatCell(target.symmetry(1, CellImpl(5, 2))).posEqualTo(2, 3)
        assertThatCell(target.symmetry(2, CellImpl(5, 2))).posEqualTo(3, 6)
        assertThatCell(target.symmetry(3, CellImpl(5, 2))).posEqualTo(6, 5)

    }

    @Test
    fun testSymmetryMirror() {
        assertThatCell(target.symmetry(4, CellImpl(3, 6))).posEqualTo(5, 6)
        assertThatCell(target.symmetry(5, CellImpl(3, 6))).posEqualTo(6, 3)
        assertThatCell(target.symmetry(6, CellImpl(3, 6))).posEqualTo(3, 2)
        assertThatCell(target.symmetry(7, CellImpl(3, 6))).posEqualTo(2, 5)
    }

    @Test
    fun testBuildFusekiSet() {
        val actual = target.buildFusekiSet(listOf(FURIKO))
        assertThat(actual.fusekiList.size).isEqualTo(8)
    }
}