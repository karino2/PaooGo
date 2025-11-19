package org.ligi.gobandroid_hd

import io.github.karino2.paoogo.goengine.fuseki.FusekiBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.gobandroid_hd.CellAssert.Companion.assertThatCell
import org.ligi.gobandroid_hd.logic.CellImpl

class TestFusekiSet {
    val target = FusekiBuilder().buildFusekiSet(listOf(TestFusekiBuilder.FURIKO))

    @Test
    fun testFirstTengen() {
        target.moveTo(CellImpl(4, 4))
        assertThat(target.fusekiList.size).isEqualTo(8)
        assertThat(target.stillFuseki).isTrue()
    }

    @Test
    fun testFirstNonMatch() {
        target.moveTo(CellImpl(3, 3))
        assertThat(target.fusekiList.size).isEqualTo(0)
        assertThat(target.stillFuseki).isFalse()
    }

    @Test
    fun testFirstPeekMove() {
        val first = target.peekMove()
        assertThatCell(first).posEqualTo(4, 4)
        target.moveTo(first)

        target.moveTo(CellImpl(4, 4))
        assertThat(target.fusekiList.size).isEqualTo(8)
    }

    @Test
    fun testTwoMove() {
        target.moveTo(CellImpl(4, 4))
        val second = target.peekMove()
        // second is random, but anyway only one fuseki remains.
        target.moveTo(second)
        assertThat(target.fusekiList.size).isEqualTo(1)
        assertThat(target.stillFuseki).isTrue()
        /*
        val third = target.peekMove()!!
        // println(third)
        target.moveTo(third)
         */
    }

    @Test
    fun testFurikoFiveMove() {
        target.moveTo(CellImpl(4, 4))
        val second = target.peekMove()
        target.moveTo(second)
        assertThat(target.stillFuseki).isTrue()
         target.moveTo(second)
        assertThat(target.stillFuseki).isTrue()
        val third = target.peekMove()
        target.moveTo(third)
        assertThat(target.stillFuseki).isTrue()
        val fourth = target.peekMove()
        target.moveTo(fourth)
        assertThat(target.stillFuseki).isTrue()
        val fifth = target.peekMove()
        target.moveTo(fifth)
        // Furiko fuseki end.
        assertThat(target.stillFuseki).isFalse()
    }
}