package io.github.karino2.paoogo.goengine

import android.content.Context
import android.content.res.AssetManager
import io.github.karino2.paoogo.goengine.amigo.AmiGoNative
import io.github.karino2.paoogo.goengine.fuseki.FusekiBuilder
import io.github.karino2.paoogo.goengine.gnugo2.GnuGo2Native
import io.github.karino2.paoogo.goengine.gnugo3.GnuGo3Native
import io.github.karino2.paoogo.goengine.katago.KataGoNative
import io.github.karino2.paoogo.goengine.katago.KataGoSetup
import io.github.karino2.paoogo.goengine.liberty.LibertyNative
import io.github.karino2.paoogo.goengine.ray.RayNative
import org.ligi.gobandroid_hd.R


class EngineRepository(val context: Context, val assetManager: AssetManager) {
    val fusekiSet by lazy { FusekiBuilder.buildFusekiSet() }

    val gnugo2Engine by lazy {
        GnuGo2Native().apply {
            initNative()
            setDepth(4)
        }
    }

    val libertyEngine by lazy {
        LibertyNative().apply {
            initNative()
        }
    }

    val gnugo3Engine by lazy {
        GnuGo3Native().apply {
            initNative()
        }
    }

    val rayEngine by lazy {
        RayNative().apply {
            initNative(Runtime.getRuntime().availableProcessors(), 1.0)
            setupAssetParams(assetManager)
            initGame()
        }
    }

    val katagoEngine by lazy {
        val setup = KataGoSetup(context, assetManager)
        setup.extractFiles()
        KataGoNative().apply {
            initNativeHum(
                Runtime.getRuntime().availableProcessors(),
                setup.configFile.absolutePath,
                setup.modelFile.absolutePath,
                setup.humanModelFile?.absolutePath ?: "",
            )
        }
    }

    val amigoEngine by lazy {
        AmiGoNative().apply {
            initNative()
        }
    }

    fun getAnalyzer() : GoAnalyzer { return katagoEngine }


    fun getEngine(level: Int) : Pair<GoEngine, Int> {
        return when(level) {
            1-> Pair(amigoEngine.apply { setLevel(0) }, R.string.paomigojr)
            2-> Pair(amigoEngine.apply { setLevel(7)}, R.string.paomigo)
            3-> Pair(libertyEngine, R.string.paolibe)
            4-> {
                hybridEngine(1, R.string.paognulijr)
            }
            5-> {
                hybridEngine(2, R.string.paognuli)
            }
            6-> Pair(FusekiEngine(fusekiSet, FusekiLevel.SELDOM, gnugo2Engine), R.string.paognujr)
            else-> Pair(FusekiEngine(fusekiSet, FusekiLevel.ALMOST, gnugo3Engine), R.string.paognu)
        }
    }

    private fun hybridEngine(
        level: Int,
        label: Int
    ): Pair<HybridEngine, Int> {
        val hybrid = HybridEngine(gnugo2Engine, libertyEngine, Policy(level))
        return Pair(hybrid, label)
    }
}