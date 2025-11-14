package io.github.karino2.paoogo.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.ui.links.LinkWithDescription
import org.ligi.gobandroid_hd.ui.links.TwoLineRecyclerAdapter


class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }

        findViewById<RecyclerView>(R.id.content_recycler).apply {
            adapter = TwoLineRecyclerAdapter(getData())
            layoutManager = LinearLayoutManager(this@AboutActivity)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun getData() = arrayOf(
        LinkWithDescription("https://github.com/karino2/PaooGo", "PaooGo source", "karino2"),
        LinkWithDescription("https://amigogtp.sourceforge.net/", "AmiGoGtp"),
        LinkWithDescription("https://lists.gnu.org/archive/html/gnugo-devel/2005-08/msg00036.html", "Liberty", "GnuGo fork"),
        LinkWithDescription("https://github.com/lightvector/KataGo", "KataGo"),
        LinkWithDescription("https://www.gnu.org/software/gnugo/devel.html", "GnuGo"),

        LinkWithDescription("http://ligi.de", "gobandroid, PaooGo forked from", "Ligi"),
        LinkWithDescription("http://jakewharton.github.io/butterknife/", "Jake Wharton", "ButterKnife"),
        LinkWithDescription("https://developers.google.com/", "Google", "Android, Dagger, .."),
        LinkWithDescription("https://square.github.io/", "Square", "okhttp, assertj-android"),
        LinkWithDescription("https://kotlinlang.org/", "JetBrains Kotlin", "one awesome langueage used"),
        LinkWithDescription("https://github.com/greenrobot", "GreenRobot", "eventbus"),
        LinkWithDescription("https://github.com/N3TWORK/alphanum", "N3TWORK", "Alphanum comparator"),
        LinkWithDescription("https://jchardet.sourceforge.net/index.html", "jchardet", "jchardet is a java port of the source from mozilla's automatic charset detection algorithm"),
        LinkWithDescription("https://github.com/Zenigata", "French Translation #2", "Zenigata"),
        LinkWithDescription("https://github.com/p3l", "Swedish Translation", "Peter Lundqvist"),
        LinkWithDescription("https://github.com/PhilJay/MPAndroidChart/", "MPAndroidChart", "PhilJay"),
        LinkWithDescription("https://github.com/gthazmatt", "Code contributions", "gthazmatt")
    )
}