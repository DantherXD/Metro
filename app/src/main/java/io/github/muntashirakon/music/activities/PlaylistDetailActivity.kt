package io.github.muntashirakon.music.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.util.ATHUtil
import io.github.muntashirakon.music.R
import io.github.muntashirakon.music.activities.base.AbsSlidingMusicPanelActivity
import io.github.muntashirakon.music.adapter.song.OrderablePlaylistSongAdapter
import io.github.muntashirakon.music.adapter.song.PlaylistSongAdapter
import io.github.muntashirakon.music.adapter.song.SongAdapter
import io.github.muntashirakon.music.extensions.applyToolbar
import io.github.muntashirakon.music.extensions.extraNotNull
import io.github.muntashirakon.music.helper.menu.PlaylistMenuHelper
import io.github.muntashirakon.music.interfaces.CabHolder
import io.github.muntashirakon.music.loaders.PlaylistLoader
import io.github.muntashirakon.music.model.AbsCustomPlaylist
import io.github.muntashirakon.music.model.Playlist
import io.github.muntashirakon.music.model.Song
import io.github.muntashirakon.music.mvp.presenter.PlaylistSongsPresenter
import io.github.muntashirakon.music.mvp.presenter.PlaylistSongsPresenter.PlaylistSongsPresenterImpl
import io.github.muntashirakon.music.mvp.presenter.PlaylistSongsView
import io.github.muntashirakon.music.providers.RepositoryImpl
import io.github.muntashirakon.music.util.DensityUtil
import io.github.muntashirakon.music.util.PlaylistsUtil
import io.github.muntashirakon.music.util.RetroColorUtil
import com.afollestad.materialcab.MaterialCab
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.android.synthetic.main.activity_playlist_detail.*

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity(), CabHolder, PlaylistSongsView {


    private lateinit var presenter: PlaylistSongsPresenter
    private lateinit var playlist: Playlist
    private var cab: MaterialCab? = null
    private lateinit var adapter: SongAdapter
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setDrawUnderStatusBar()
        super.onCreate(savedInstanceState)
        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()
        setLightNavigationBar(true)
        setBottomBarVisibility(View.GONE)

        presenter = PlaylistSongsPresenterImpl(RepositoryImpl(this))
        presenter.attachView(this)

        playlist = extraNotNull<Playlist>(EXTRA_PLAYLIST).value

        setUpToolBar()
        setUpRecyclerView()
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(R.layout.activity_playlist_detail)
    }

    private fun setUpRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(this)
        if (playlist is AbsCustomPlaylist) {
            adapter = PlaylistSongAdapter(this, ArrayList(), R.layout.item_list, this)
            recyclerView.adapter = adapter
        } else {
            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            val animator = RefactoredDefaultItemAnimator()
            adapter = OrderablePlaylistSongAdapter(this,
                ArrayList(),
                R.layout.item_list,
                this,
                object : OrderablePlaylistSongAdapter.OnMoveItemListener {
                    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
                        if (PlaylistsUtil.moveItem(
                                this@PlaylistDetailActivity,
                                playlist.id,
                                fromPosition,
                                toPosition
                            )
                        ) {
                            val song = adapter.dataSet.removeAt(fromPosition)
                            adapter.dataSet.add(toPosition, song)
                            adapter.notifyItemMoved(fromPosition, toPosition)
                        }
                    }
                })
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)

            recyclerView.adapter = wrappedAdapter
            recyclerView.itemAnimator = animator

            recyclerViewDragDropManager?.attachRecyclerView(recyclerView)
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        presenter.loadPlaylistSongs(playlist)
    }

    private fun setUpToolBar() {
        applyToolbar(toolbar)
        title = playlist.name
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(
            if (playlist is AbsCustomPlaylist) R.menu.menu_smart_playlist_detail
            else R.menu.menu_playlist_detail, menu
        )
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return PlaylistMenuHelper.handleMenuClick(this, playlist, item)
    }

    override fun openCab(menuRes: Int, callback: MaterialCab.Callback): MaterialCab {
        if (cab != null && cab!!.isActive) {
            cab!!.finish()
        }
        cab = MaterialCab(this, R.id.cab_stub).setMenu(menuRes)
            .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
            .setBackgroundColor(
                RetroColorUtil.shiftBackgroundColorForLightText(
                    ATHUtil.resolveColor(
                        this,
                        R.attr.colorSurface
                    )
                )
            ).start(callback)
        return cab!!
    }

    override fun onBackPressed() {
        if (cab != null && cab!!.isActive) {
            cab!!.finish()
        } else {
            recyclerView!!.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        if (playlist !is AbsCustomPlaylist) {
            // Playlist deleted
            if (!PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                finish()
                return
            }
            // Playlist renamed
            val playlistName = PlaylistsUtil.getNameForPlaylist(this, playlist.id.toLong())
            if (playlistName != playlist.name) {
                playlist = PlaylistLoader.getPlaylist(this, playlist.id)
                setToolbarTitle(playlist.name)
            }
        }
        presenter.loadPlaylistSongs(playlist)
    }

    private fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    private fun checkForPadding() {
        val height = DensityUtil.dip2px(this, 52f)
        recyclerView.setPadding(0, 0, 0, (height))
    }

    private fun checkIsEmpty() {
        checkForPadding()
        emptyEmoji.text = getEmojiByUnicode(0x1F631)
        empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        emptyText.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    public override fun onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.cancelDrag()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.release()
            recyclerViewDragDropManager = null
        }

        if (recyclerView != null) {
            recyclerView!!.itemAnimator = null
            recyclerView!!.adapter = null
        }

        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
            wrappedAdapter = null
        }
        super.onDestroy()
        presenter.detachView()
    }

    override fun showEmptyView() {
        empty.visibility = View.VISIBLE
        emptyText.visibility = View.VISIBLE
    }

    override fun songs(songs: List<Song>) {
        adapter.swapDataSet(songs)
    }

    companion object {
        var EXTRA_PLAYLIST = "extra_playlist"
    }
}
