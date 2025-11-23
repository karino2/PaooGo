package io.github.karino2.paoogo.goengine.fuseki

import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.CellImpl

class Fuseki(val currentPos : Int, val fuseki : List<Cell>) {
    val currentCell: Cell
        get() = fuseki[currentPos]

    val stillFuseki: Boolean
        get() = currentPos < fuseki.size

    fun gotoNext() = Fuseki(currentPos+1, fuseki)

    fun isCurrentCellAt(cell: Cell) = stillFuseki && currentCell.isEqual(cell)
}

class FusekiSet(var fusekiList: List<Fuseki>) {
    companion object {
        fun fromCellLists(cellLists : List<List<Cell>>) =
            cellLists.map { Fuseki(0, it) }
                .let { FusekiSet(it) }
    }

    val stillFuseki : Boolean
        get() = fusekiList.isNotEmpty()

    fun moveTo(cell: Cell) {
        fusekiList = fusekiList.filter { it.isCurrentCellAt(cell) }
            .map { it.gotoNext() }
            .filter { it.stillFuseki }
    }

    fun peekMove() : Cell {
        assert(stillFuseki)
        // To OudouSinkou and Furiko equally distributed.
        val nextSet = fusekiList.map {
            val cell = it.currentCell
            Pair(cell.x, cell.y)
        }.toSet()
        val nmove = nextSet.random()
        return CellImpl(nmove.first, nmove.second)
    }

    fun clone() = FusekiSet(fusekiList)
}