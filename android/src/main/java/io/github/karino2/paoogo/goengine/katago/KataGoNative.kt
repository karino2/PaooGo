package io.github.karino2.paoogo.goengine.katago

import io.github.karino2.paoogo.goengine.AnalyzeInfo
import io.github.karino2.paoogo.goengine.GoAnalyzer
import io.github.karino2.paoogo.goengine.GoEngine
import io.github.karino2.paoogo.goengine.gnugo2.MovePos
import org.ligi.gobandroid_hd.logic.GTPHelper
import org.ligi.gobandroid_hd.logic.GoGame

class KataGoNative : GoEngine, GoAnalyzer {
    companion object {
        init{
            System.loadLibrary("katago")
        }

    }
    external fun initNativeHum(threadNum: Int, cfgPath: String, modelPath: String, humanModelPath: String)
    external fun initNative(threadNum: Int, cfgPath: String, modelPath: String)
    override external fun setKomi(komi: Float)
    override external fun clearBoard()
    override external fun setBoardSize(size: Int)
    external fun setGenmoveProfile(profile: String)
    override external fun doMove(x: Int, y: Int, isBlack: Boolean) : Boolean
    override external fun genMoveInternal(isBlack: Boolean) : Int
    override fun debugInfo(): String? {
        return null
    }

    override external fun doPass(isBlack: Boolean)

    // based on lz-analyze.
    // [KataGo/docs/GTP_Extensions.md at master · lightvector/KataGo](https://github.com/lightvector/KataGo/blob/master/docs/GTP_Extensions.md)
    // winrate: [0, 10000]
    // F4 5506 is the candidate and winrate. After it, pv is followed.
    // each info is separate by ",".
    // example is following.
    // F4 5506 F4 E2 G4 E7 F7 H3,D6 5546 D6 D7 C7 E6 D5 E7 E5 C8 B7 B8 B6,D7 5341 D7 D6 E6 C7 D8 B4,G5 5099 G5 D2 D7 C7 C3,C5 5092 C5 D7 F4 H5,E3 4829 E3 H5 H6 G5 G6 D7,E7 4955 E7 D7 F4 E2,E5 4706 E5 B4 H4,B5 4847 B5 E5,G4 4320 G4,F3 4450 F3,E2 4238 E2,C7 4212 C7,H5 4164 H5
    external fun analyze(msec: Int, isBlack: Boolean) : String

    override external fun score(msec: Int, isBlack: Boolean) : Double

    override fun analyzeSituation(isBlack: Boolean, game: GoGame): List<AnalyzeInfo> {
        val res = analyze(2000, isBlack)
        println(res)
        if (res.isEmpty())
            return emptyList()
        return res.split("\n")
            .filterNot { it.isEmpty() }
            .last()
            .split(",")
            .mapNotNull {
                val arr = it.split(" ")
                if (arr.size <= 2)
                    return@mapNotNull null
                if (arr[0].uppercase() == "PASS")
                    return@mapNotNull null
                val pos = MovePos.fromString(arr[0], game)
                val rate = arr[1].toDouble()/(10000.0)
                val pv = arr.subList(2, arr.size).mapNotNull {
                    if (it.uppercase() == "PASS") null else GTPHelper.strToCell(it, game)
                }
                AnalyzeInfo(game.visualBoard.getCell(pos.x, pos.y), rate, pv)
            }
    }

    override fun hint(isBlack: Boolean, game: GoGame): MovePos {
        val res = analyze(2000, isBlack)
        println(res)
        if (res.isEmpty())
            return MovePos.PASS
        val arr = res.split("\n")
            .filterNot { it.isEmpty() }
            .last()
            .split(",")
            .first()
            .split(" ")
        if (arr.size <= 2)
            return MovePos.PASS
        if (arr[0].uppercase() == "PASS")
            return MovePos.PASS

        return MovePos.fromString(arr[0], game)
    }

}
