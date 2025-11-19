package io.github.karino2.paoogo.goengine.fuseki

import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.CellImpl
import org.ligi.gobandroid_hd.logic.sgf.SGFReader

/*
  Board size 9 only fuseki class.
  Fuseki SGF must be board size 9, no variation, short SGF.
 */
class FusekiBuilder {
    companion object {
        const val FURIKO = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-18];B[ee];W[dg];B[cf];W[fg];B[fc])"
        const val MAMEMAKI = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[fd];W[df];B[ef];W[eg];B[fg];W[dg];B[dc])"
        const val BLACK_BOOMERANG = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[fd];W[df];B[ef];W[eg];B[fg];W[ee];B[ff];W[de];B[fe];W[dc])"
        const val KAZAGURUMA = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[ee];W[eg];B[dg];W[df];B[ef];W[cg];B[dh];W[fg];B[gf];W[cf];B[gg])"
        const val LIFTING1 = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[ee];W[ec];B[fg];W[cd];B[dd];W[dc];B[ce];W[bd];B[fd];W[fh])"
        const val LIFTING2 = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[ee];W[ec];B[fg];W[cf];B[cd];W[gc])"
        const val SLIDER = "(;GM[1]FF[4]CA[UTF-8]AP[Sabaki:0.52.2]KM[6.5]SZ[9]DT[2025-11-19];B[ed];W[ef];B[ce];W[cg];B[gg];W[fg];B[fe];W[eb])"

        val fusekiList = listOf(FURIKO, MAMEMAKI, BLACK_BOOMERANG, KAZAGURUMA, LIFTING1, LIFTING2, SLIDER)

        fun buildFusekiSet() = FusekiBuilder().buildFusekiSet(fusekiList)
    }

    fun toCellList(sgf: String) : List<Cell> {
        val game = SGFReader.Companion.sgf2game(sgf, null)!!
        if (game.size != 9) { return emptyList() }

        var move = game.actMove
        val res = mutableListOf<Cell>()
        while(move.hasNextMove())
        {
            move = move.getNextMove(0)!! // .cell!!
            res.add(move.cell!!)
        }
        return res
    }

    /*
    sindex is from 0 to 7.
    0: original
    1: 90 rotate
    2: 180
    3: 270
    4: mirror
    5: mirror and 90 rotate
    6: mirror and 180 rotate
    7: mirror and 270 rotate
     */
    fun symmetry(sindex: Int, cell0: Cell) : Cell {
        // tengen.
        if (cell0.x == 4 && cell0.y == 4)
            return cell0

        val isMirror = sindex >= 4
        //         assertThatCell(target.symmetry(4, CellImpl(3, 6))).posEqualTo(5, 6)
        val cell = if(isMirror) {
            CellImpl(8 - cell0.x, cell0.y)
        } else { cell0 }

        val degreeType = sindex % 4
        return when(degreeType) {
            0 -> cell
            1 -> {
                // 3, 2 -> 2, 5
                // 3, 1 -> 1, 5
                // 1, 3 -> 3, (8-1)
                // 5, 2 -> 2, (8-5)
                CellImpl(cell.y, (8 - cell.x))
            }
            2 -> {
                //         assertThatCell(target.symmetry(2, CellImpl(3, 6))).posEqualTo(5, 2)
                CellImpl(8 - cell.x, 8 - cell.y)
            }
            3 -> {
                //         assertThatCell(target.symmetry(3, CellImpl(3, 6))).posEqualTo(2, 3)
                CellImpl(8 - cell.y, cell.x)
            }
            else -> cell
        }
    }

    // return 8 symmetry list. If all cell is, for example, mirror symmetry, same list exists. But don't care.
    fun symmetrize(cellList: List<Cell>) : List<List<Cell>> {
        val res = mutableListOf<List<Cell>>()
        for(i in 0..<8) {
            val one = cellList.map { symmetry(i, it) }
            res.add(one)
        }
        return res
    }

    fun buildFusekiSet(sgfList: List<String>) = sgfList.map { toCellList(it) }
            .flatMap { symmetrize(it) }
            .let { FusekiSet.fromCellLists(it) }
}