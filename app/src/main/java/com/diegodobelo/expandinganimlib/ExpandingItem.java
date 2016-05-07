package com.diegodobelo.expandinganimlib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by diego on 5/5/16.
 */
public class ExpandingItem extends LinearLayout {
    private ViewGroup mItemLayout;
    private int mSubItemRes;
    private LayoutInflater mInflater;
    private boolean mSubItemsShown;
    private LinearLayout mBaseLayout;
    private LinearLayout mBaseListLayout;
    private ImageView mIndicatorImage;
    private View mIndicatorBackground;
    private int mItemHeight;
    private int mSubItemHeight;
    private int mSubItemCount;
    private int mIndicatorSize;

    public static final String SUB_ITEM_TAG = "SUB_ITEM_TAG";

    public ExpandingItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExpandingItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ExpandingItem, 0, 0);
        mInflater = LayoutInflater.from(context);
        mBaseLayout = (LinearLayout) mInflater.inflate(R.layout.expanding_item_base_layout,
                null, false);
        mBaseListLayout = (LinearLayout) mBaseLayout.findViewById(R.id.base_list_layout);
        mIndicatorImage = (ImageView) mBaseLayout.findViewById(R.id.indicator_image);

        mBaseLayout.findViewById(R.id.icon_container).bringToFront();

        try {
            int itemLayoutId = array.getResourceId(R.styleable.ExpandingItem_item_layout, 0);
            mSubItemRes = array.getResourceId(R.styleable.ExpandingItem_sub_item_layout, 0);
            mIndicatorSize = array.getDimensionPixelSize(R.styleable.ExpandingItem_indicator_size, 0);
            if (itemLayoutId != 0) {
                mItemLayout = (ViewGroup) mInflater.inflate(itemLayoutId, null, false);
            }
        } finally {
            array.recycle();
        }

        if (mIndicatorSize != 0) {
            setIndicatorBackgroundSize();
        }

        addItem(mItemLayout);
        addView(mBaseLayout);

        setupIndicatorBackground();
    }

    private void setIndicatorBackgroundSize() {
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_top), mIndicatorSize);
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_bottom), mIndicatorSize);
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_middle), 0);

        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_top), mIndicatorSize);
        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_bottom), mIndicatorSize);
        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_middle), mIndicatorSize);

        setViewMarginTop(mBaseLayout.findViewById(R.id.icon_indicator_middle), (-1 * mIndicatorSize/2));
        setViewMarginTop(mBaseLayout.findViewById(R.id.icon_indicator_bottom), (-1 * mIndicatorSize/2));
    }

    private void setupIndicatorBackground() {
        mIndicatorBackground = mBaseLayout.findViewById(R.id.icon_indicator_middle);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mIndicatorBackground.getLayoutParams();
        mIconIndicatorHeight = params.height;
        mIconIndicatorWidth = params.width;
    }

    public ExpandingItem(Context context) {
        super(context);
    }

    private void addItem(ViewGroup item) {
        if (item != null) {
            mBaseListLayout.addView(item);
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSubItems();
                    expandIconIndicator();
                }
            });
            setItemHeight(item);
        }
    }

    public void setIndicatorColorRes(int colorRes) {
        setIndicatorColor(ContextCompat.getColor(getContext(), colorRes));
    }

    public void setIndicatorColor(int color) {
        findViewById(R.id.icon_indicator_top).getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
        findViewById(R.id.icon_indicator_bottom).getBackground().setColorFilter(color,
                PorterDuff.Mode.SRC_ATOP);
        findViewById(R.id.icon_indicator_middle).setBackgroundColor(color);
    }

    public void setIndicatorIcon(Drawable icon) {
        mIndicatorImage.setImageDrawable(icon);
    }

    public View createSubItem() {
        //TODO: verify if not null
        ViewGroup subItemLayout = (ViewGroup) mInflater.inflate(mSubItemRes, null, false);
        subItemLayout.setTag(SUB_ITEM_TAG);
        mBaseListLayout.addView(subItemLayout);
        mSubItemCount++;
        subItemLayout.setVisibility(GONE);
        return subItemLayout;
    }

    private void setSubItemHeight(final ViewGroup v) {
        v.post(new Runnable() {
            @Override
            public void run() {
                //TODO: verify if it is set before used
                mSubItemHeight = v.getMeasuredHeight();
            }
        });

    }

    private void setItemHeight(final ViewGroup v) {
        v.post(new Runnable() {
            @Override
            public void run() {
                //TODO: verify if it is set before used
                mItemHeight = v.getMeasuredHeight();
            }
        });

    }

    private void toggleSubItems() {
        mSubItemsShown = !mSubItemsShown;
        for (int i = 0; i < mBaseListLayout.getChildCount(); i++) {
            ViewGroup subItem = (ViewGroup) mBaseListLayout.getChildAt(i);
            if (subItem.getTag() != null && subItem.getTag().equals(SUB_ITEM_TAG)) {
                subItem.setVisibility(mSubItemsShown ? VISIBLE : GONE);
                if (subItem.getVisibility() == VISIBLE && mSubItemHeight == 0) {
                    setSubItemHeight(subItem); //need to be calculated when visible
                }
            }
        }
    }

    private void expandIconIndicator() {
        if (mIndicatorBackground != null) {
            ValueAnimator animation = mSubItemsShown ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
            animation.setDuration(300);

            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (float) animation.getAnimatedValue();
                    final int totalHeight = (mSubItemHeight * mSubItemCount) - mIndicatorSize + mItemHeight;
                    setViewHeight(mIndicatorBackground, (int) (totalHeight * val));
                }
            });

            animation.start();
        }
    }

    private void setViewHeight(View v, int height) {
        final ViewGroup.LayoutParams params = v.getLayoutParams();
        params.height = height;
        v.requestLayout();
    }

    private void setViewWidth(View v, int width) {
        final ViewGroup.LayoutParams params = v.getLayoutParams();
        params.width = width;
        v.requestLayout();
    }

    private void setViewMarginTop(View v, int marginTop) {
        final ViewGroup.MarginLayoutParams params = (MarginLayoutParams) v.getLayoutParams();
        params.setMargins(0, marginTop, 0, 0);
        v.requestLayout();
    }
}