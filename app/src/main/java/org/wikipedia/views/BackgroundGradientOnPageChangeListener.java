package org.wikipedia.views;


import android.animation.ArgbEvaluator;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.List;

public class BackgroundGradientOnPageChangeListener implements ViewPager.OnPageChangeListener{
    @NonNull private PagerAdapter adapter;
    @NonNull private GradientDrawable background;
    @NonNull private ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    @NonNull private List<Integer> startColors;
    @NonNull private List<Integer> centerColors;
    @NonNull private List<Integer> endColors;

    public BackgroundGradientOnPageChangeListener(@NonNull PagerAdapter adapter,
                                                  @NonNull GradientDrawable background,
                                                  @NonNull MutatableGradientColors colors) {
        this.adapter = adapter;
        this.background = background;
        this.startColors = colors.getStartColors();
        this.centerColors = colors.getCenterColors();
        this.endColors = colors.getEndColors();
    }

    @Override
    public void onPageScrolled(int pos, float posOffset, int posOffsetPx) {
        int last = adapter.getCount() - 1;
        if (pos < last) {
            int start = (Integer) argbEvaluator.evaluate(posOffset, startColors.get(pos), startColors.get(pos + 1));
            int center = (Integer) argbEvaluator.evaluate(posOffset, centerColors.get(pos), centerColors.get(pos + 1));
            int end = (Integer) argbEvaluator.evaluate(posOffset, endColors.get(pos), endColors.get(pos + 1));
            int[] colors = new int[] {start, center, end};
            background.setColors(colors);
        } else {
            int[] colors = new int[] {startColors.get(last), centerColors.get(last), endColors.get(last)};
            background.setColors(colors);
        }
    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    public static class MutatableGradientColors {
        @NonNull private List<Integer> startColors;
        @NonNull private List<Integer> centerColors;
        @NonNull private List<Integer> endColors;

        public MutatableGradientColors(@NonNull List<Integer> startColors,
                                       @NonNull List<Integer> centerColors,
                                       @NonNull List<Integer> endcolors) {
            this.startColors = startColors;
            this.centerColors = centerColors;
            this.endColors = endcolors;
        }

        private List<Integer> getStartColors() {
            return startColors;
        }

        private List<Integer> getCenterColors() {
            return centerColors;
        }

        private List<Integer> getEndColors() {
            return endColors;
        }
    }
}
