package io.github.karino2.paoogo.goengine.fuseki

import org.ligi.gobandroid_hd.logic.Cell

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
        return fusekiList.random().currentCell
    }

    fun clone() = FusekiSet(fusekiList)
}