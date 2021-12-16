package com.hiroshi.cimoc.ui.activity;

import android.graphics.Point;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.hiroshi.cimoc.R;
import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.ui.adapter.ReaderAdapter;
import com.hiroshi.cimoc.ui.widget.ZoomableRecyclerView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.List;

/**
 * Created by Hiroshi on 2016/8/5.
 */
public class StreamReaderActivity extends ReaderActivity {

    private int mLastPosition = 0;
    private RecyclerView.SmoothScroller mSmoothScroller;

    @Override
    protected void initView() {
        super.initView();
        mLoadPrev = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_PREV, false);
        mLoadNext = mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_LOAD_NEXT, true);
        mReaderAdapter.setReaderMode(ReaderAdapter.READER_STREAM);
        if (mPreference.getBoolean(PreferenceManager.PREF_READER_STREAM_INTERVAL, false)) {
            mRecyclerView.addItemDecoration(mReaderAdapter.getItemDecoration());
        }
        ((ZoomableRecyclerView) mRecyclerView).setScaleFactor(
                mPreference.getInt(PreferenceManager.PREF_READER_SCALE_FACTOR, 200) * 0.01f);
        ((ZoomableRecyclerView) mRecyclerView).setVertical(turn == PreferenceManager.READER_TURN_ATB);
        ((ZoomableRecyclerView) mRecyclerView).setDoubleTap(
                !mPreference.getBoolean(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false));
        ((ZoomableRecyclerView) mRecyclerView).setTapListenerListener(this);

        // To smooth scroll position at item top, rather than bottom,
        // https://stackoverflow.com/a/43505830
        mSmoothScroller = new LinearSmoothScroller(mRecyclerView.getContext()) {
            @Override
            protected int getHorizontalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        hideControl();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (mLoadPrev) {
                            int item = mLayoutManager.findFirstVisibleItemPosition();
                            if (item == 0) {
                                mPresenter.loadPrev();
                            }
                        }
                        if (mLoadNext) {
                            int item = mLayoutManager.findLastVisibleItemPosition();
                            if (item == mReaderAdapter.getItemCount() - 1) {
                                mPresenter.loadNext();
                            }
                        }
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int target = mLayoutManager.findFirstVisibleItemPosition();
                if (target != mLastPosition) {
                    ImageUrl newImage = mReaderAdapter.getItem(target);
                    ImageUrl oldImage = mReaderAdapter.getItem(mLastPosition);

                    if (!oldImage.getChapter().equals(newImage.getChapter())) {
                        switch (turn) {
                            case PreferenceManager.READER_TURN_ATB:
                                if (dy > 0) {
                                    mPresenter.toNextChapter();
                                } else if (dy < 0) {
                                    mPresenter.toPrevChapter();
                                }
                                break;
                            case PreferenceManager.READER_TURN_LTR:
                                if (dx > 0) {
                                    mPresenter.toNextChapter();
                                } else if (dx < 0) {
                                    mPresenter.toPrevChapter();
                                }
                                break;
                            case PreferenceManager.READER_TURN_RTL:
                                if (dx > 0) {
                                    mPresenter.toPrevChapter();
                                } else if (dx < 0) {
                                    mPresenter.toNextChapter();
                                }
                                break;
                        }
                    }
                    progress = mReaderAdapter.getItem(target).getNum();
                    mLastPosition = target;
                    updateProgress();
                }
            }
        });
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) {
            int current = mLastPosition + value - progress;
            int pos = mReaderAdapter.getPositionByNum(current, value, value < progress);
            mLayoutManager.scrollToPositionWithOffset(pos, 0);
        }
    }

    @Override
    protected void prevPage() {
        boolean isScrollByWindow = false;
        if (isScrollByWindow) {
            scrollPrevByViewPortSize();
        } else {
            int position = getCurPosition();
            int firstVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findFirstVisibleItemPosition();
            int firstCompletelyVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findFirstCompletelyVisibleItemPosition();
            // TODO get the screen can view page count at same time, and use this to scroll prev
            int delta = 1;
            int targetPos = firstVisibleItemPosition < firstCompletelyVisibleItemPosition ?
                    firstVisibleItemPosition : firstVisibleItemPosition - delta;
            if (targetPos == position) {
                scrollPrevByViewPortSize();
            } else {
                targetPos = Math.max(targetPos, 0);
                mSmoothScroller.setTargetPosition(targetPos);
                mLayoutManager.startSmoothScroll(mSmoothScroller);

            }
        }

        if (mLayoutManager.findFirstVisibleItemPosition() == 0) {
            loadPrev();
        }
    }

    private void scrollPrevByViewPortSize() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, -point.y);
        } else {
            mRecyclerView.smoothScrollBy(-point.x, 0);
        }
    }

    @Override
    protected void nextPage() {
        boolean isScrollByWindow = false;
        if (isScrollByWindow) {
            scrollNextByViewPortSize();
        } else {
            int position = getCurPosition();
            int lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
            int lastCompletelyVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastCompletelyVisibleItemPosition();
            int targetPos = lastCompletelyVisibleItemPosition < lastVisibleItemPosition ?
                    lastVisibleItemPosition : lastVisibleItemPosition + 1;
            if (targetPos == position) {
                scrollNextByViewPortSize();
            } else {
                targetPos = Math.min(mReaderAdapter.getItemCount() - 1, targetPos);
                mSmoothScroller.setTargetPosition(targetPos);
                mLayoutManager.startSmoothScroll(mSmoothScroller);
            }
        }
        if (mLayoutManager.findLastVisibleItemPosition() == mReaderAdapter.getItemCount() - 1){
            loadNext();
        }
    }

    private void scrollNextByViewPortSize() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        if (turn == PreferenceManager.READER_TURN_ATB) {
            mRecyclerView.smoothScrollBy(0, point.y);
        } else {
            mRecyclerView.smoothScrollBy(point.x, 0);
        }
    }

    @Override
    public void onPrevLoadSuccess(List<ImageUrl> list) {
        super.onPrevLoadSuccess(list);
        if (mLastPosition == 0) {
            mLastPosition = list.size();
        }
    }

    @Override
    protected int getCurPosition() {
        return mLastPosition;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_stream_reader;
    }

}
