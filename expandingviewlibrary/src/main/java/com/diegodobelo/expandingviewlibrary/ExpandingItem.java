package com.diegodobelo.expandingviewlibrary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by diego on 5/5/16.
 */
public class ExpandingItem extends RelativeLayout {
    public static final int DEFAULT_ANIM_DURATION = 600;

    private ViewGroup mItemLayout;
    private LayoutInflater mInflater;
    private RelativeLayout mBaseLayout;
    private LinearLayout mBaseListLayout;
    private LinearLayout mBaseSubListLayout;
    private ImageView mIndicatorImage;
    private View mIndicatorBackground;
    private ViewStub mSeparatorStub;
    private ViewGroup mIndicatorContainer;

    private int mSubItemRes;

    private int mItemHeight;
    private int mSubItemHeight;
    private int mSubItemWidth;
    private int mSubItemCount;
    private int mIndicatorSize;
    private int mAnimationDuration;
    private int mIndicatorMarginLeft;
    private int mIndicatorMarginRight;

    private boolean mShowIndicator;
    private boolean mShowAnimation;
    private boolean mSubItemsShown;

    //TODO: make it a list
    private OnItemStateChanged mListener;

    public interface OnItemStateChanged {
        void itemCollapseStateChanged(boolean expanded);
    }

    public ExpandingItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExpandingItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAnimationDuration = DEFAULT_ANIM_DURATION;

        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ExpandingItem, 0, 0);
        mInflater = LayoutInflater.from(context);
        mBaseLayout = (RelativeLayout) mInflater.inflate(R.layout.expanding_item_base_layout,
                null, false);
        mBaseListLayout = (LinearLayout) mBaseLayout.findViewById(R.id.base_list_layout);
        mBaseSubListLayout = (LinearLayout) mBaseLayout.findViewById(R.id.base_sub_list_layout);
        mIndicatorImage = (ImageView) mBaseLayout.findViewById(R.id.indicator_image);

        mBaseLayout.findViewById(R.id.icon_container).bringToFront();

        mSeparatorStub = (ViewStub) mBaseLayout.findViewById(R.id.base_separator_stub);

        mIndicatorContainer = (ViewGroup) mBaseLayout.findViewById(R.id.indicator_container);
        mIndicatorContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                expand();
            }
        });

        try {
            int itemLayoutId = array.getResourceId(R.styleable.ExpandingItem_item_layout, 0);
            int separatorLayoutId = array.getResourceId(R.styleable.ExpandingItem_separator_layout, 0);
            mSubItemRes = array.getResourceId(R.styleable.ExpandingItem_sub_item_layout, 0);
            mIndicatorSize = array.getDimensionPixelSize(R.styleable.ExpandingItem_indicator_size, 0);
            mIndicatorMarginLeft = array.getDimensionPixelSize(R.styleable.ExpandingItem_indicator_margin_left, 0);
            mIndicatorMarginRight = array.getDimensionPixelSize(R.styleable.ExpandingItem_indicator_margin_right, 0);
            mShowIndicator = array.getBoolean(R.styleable.ExpandingItem_show_indicator, true);
            mShowAnimation = array.getBoolean(R.styleable.ExpandingItem_show_animation, true);
            if (itemLayoutId != 0) {
                mItemLayout = (ViewGroup) mInflater.inflate(itemLayoutId, null, false);
            }
            if (separatorLayoutId != 0) {
                mSeparatorStub.setLayoutResource(separatorLayoutId);
                mSeparatorStub.inflate();
            }
        } finally {
            array.recycle();
        }

        if (mIndicatorSize != 0) {
            setIndicatorBackgroundSize();
        }

        mIndicatorContainer.setVisibility(mShowIndicator && mIndicatorSize != 0 ? VISIBLE : GONE);

        addItem(mItemLayout);
        addView(mBaseLayout);

        setupIndicatorBackground();
    }

    //TODO: remove if not needed
    public ExpandingItem(Context context) {
        super(context);
    }

    public boolean isExpanded() {
        return mSubItemsShown;
    }

    public void setStateChangedListener(OnItemStateChanged listener) {
        mListener = listener;
    }

    public int getSubItemsCount() {
        return mSubItemCount;
    }

    private void setIndicatorBackgroundSize() {
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_top), mIndicatorSize);
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_bottom), mIndicatorSize);
        setViewHeight(mBaseLayout.findViewById(R.id.icon_indicator_middle), 0);

        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_top), mIndicatorSize);
        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_bottom), mIndicatorSize);
        setViewWidth(mBaseLayout.findViewById(R.id.icon_indicator_middle), mIndicatorSize);

        mItemLayout.post(new Runnable() {
            @Override
            public void run() {
                setViewMargin(mIndicatorContainer,
                        mIndicatorMarginLeft, mItemLayout.getMeasuredHeight()/2 - mIndicatorSize/2, mIndicatorMarginRight, 0);
            }
        });

        setViewMarginTop(mBaseLayout.findViewById(R.id.icon_indicator_middle), (-1 * mIndicatorSize/2));
        setViewMarginTop(mBaseLayout.findViewById(R.id.icon_indicator_bottom), (-1 * mIndicatorSize/2));

    }

    private void setupIndicatorBackground() {
        mIndicatorBackground = mBaseLayout.findViewById(R.id.icon_indicator_middle);
    }


    private void addItem(final ViewGroup item) {
        if (item != null) {
            mBaseListLayout.addView(item);
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    expand();
                }
            });
            setItemHeight(item);
        }
    }

    private void expand() {
        toggleSubItems();
        expandSubItemsWithAnimation();
        expandIconIndicator();
        animateSubItemsIn();
    }

    public void setIndicatorColorRes(int colorRes) {
        setIndicatorColor(ContextCompat.getColor(getContext(), colorRes));
    }

    public void setIndicatorColor(int color) {
        ((GradientDrawable)findViewById(R.id.icon_indicator_top).getBackground().mutate()).setColor(color);
        ((GradientDrawable)findViewById(R.id.icon_indicator_bottom).getBackground().mutate()).setColor(color);
        findViewById(R.id.icon_indicator_middle).setBackgroundColor(color);
    }

    public void setIndicatorIconRes(int res) {
        setIndicatorIcon(ContextCompat.getDrawable(getContext(), res));
    }

    public void setIndicatorIcon(Drawable icon) {
        mIndicatorImage.setImageDrawable(icon);
    }

    @Nullable
    public View createSubItem(int index) {
        if (mSubItemRes == 0) {
            throw new RuntimeException("There is no layout to be inflated. " +
                    "Please set sub_item_layout value");
        }
        if (mBaseSubListLayout.getChildAt(index) != null) {
            return mBaseSubListLayout.getChildAt(index);
        }
        //TODO: verify if not null
        ViewGroup subItemLayout = (ViewGroup) mInflater.inflate(mSubItemRes, null, false);
        mBaseSubListLayout.addView(subItemLayout);
        mSubItemCount++;
        setSubItemDimensions(subItemLayout);
        return subItemLayout;
    }

    public void collapseSubItems() {
        mBaseSubListLayout.post(new Runnable() {
            @Override
            public void run() {
                setViewHeight(mBaseSubListLayout, 0);
            }
        });
    }

    public void expandSubItems() {
        mBaseSubListLayout.post(new Runnable() {
            @Override
            public void run() {
                setViewHeight(mBaseSubListLayout, mSubItemHeight * mSubItemCount);
            }
        });
    }

    private void animateSubItemsIn() {
        for (int i = 0; i < mSubItemCount; i++) {
            animateSubViews((ViewGroup) mBaseSubListLayout.getChildAt(i), i);
            animateViewAlpha((ViewGroup) mBaseSubListLayout.getChildAt(i), i);
        }
    }

    private void setSubItemDimensions(final ViewGroup v) {
        v.post(new Runnable() {
            @Override
            public void run() {
                //TODO: verify if it is set before used
                if (mSubItemHeight <= 0) {
                    mSubItemHeight = v.getMeasuredHeight();
                    mSubItemWidth = v.getMeasuredWidth();

                }
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
        if (mListener != null) {
            mListener.itemCollapseStateChanged(mSubItemsShown);
        }
    }

    private void animateSubViews(final ViewGroup viewGroup, int index) {
        viewGroup.setLayerType(ViewGroup.LAYER_TYPE_HARDWARE, null);
        ValueAnimator animation = mSubItemsShown ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
        animation.setDuration(mAnimationDuration);
        int delay = index * mAnimationDuration / mSubItemCount;
        int invertedDelay = (mSubItemCount - index) * mAnimationDuration / mSubItemCount;
        animation.setStartDelay(mSubItemsShown ? delay/2 : invertedDelay/2);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                viewGroup.setX((mSubItemWidth/2 * val) - mSubItemWidth/2);
            }
        });

        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                viewGroup.setLayerType(ViewGroup.LAYER_TYPE_NONE, null);
            }
        });

        animation.start();
    }

    private void animateViewAlpha(final ViewGroup viewGroup, int index) {
        ValueAnimator animation = mSubItemsShown ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
        animation.setDuration(mSubItemsShown ? mAnimationDuration * 2 : mAnimationDuration);
        int delay = index * mAnimationDuration / mSubItemCount;
        animation.setStartDelay(mSubItemsShown ? delay/2 : 0);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                viewGroup.setAlpha(val);
            }
        });

        animation.start();
    }

    private void expandIconIndicator() {
        if (mSubItemCount == 0) {
            return;
        }
        if (mIndicatorBackground != null) {
            ValueAnimator animation = mSubItemsShown ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
            animation.setDuration(mAnimationDuration);
            final int totalHeight = (mSubItemHeight * mSubItemCount) - mIndicatorSize/2 + mItemHeight/2;
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (float) animation.getAnimatedValue();
                    setViewHeight(mIndicatorBackground, (int) (totalHeight * val));
                }
            });

            animation.start();
        }
    }

    private void expandSubItemsWithAnimation() {
        if (mSubItemCount == 0) {
            return;
        }
        if (mBaseSubListLayout != null) {
            ValueAnimator animation = mSubItemsShown ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
            animation.setDuration(mAnimationDuration);

            final int totalHeight = (mSubItemHeight * mSubItemCount);
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (float) animation.getAnimatedValue();
                    setViewHeight(mBaseSubListLayout, (int) (totalHeight * val));
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
        setViewMargin(v, 0, marginTop, 0, 0);
    }

    private void setViewMargin(View v, int left, int top, int right, int bottom) {
        final MarginLayoutParams params = (MarginLayoutParams) v.getLayoutParams();
        params.setMargins(left, top, right, bottom);
        v.requestLayout();
    }
}