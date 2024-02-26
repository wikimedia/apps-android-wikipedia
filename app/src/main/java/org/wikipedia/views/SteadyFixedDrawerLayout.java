package org.wikipedia.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.FixedDrawerLayout;
import lib.sublimis.steadyview.ISteadyView;

public class SteadyFixedDrawerLayout extends FixedDrawerLayout implements ISteadyView
{
   public SteadyFixedDrawerLayout(final Context context)
   {
      super(context);

      ISteadyView.super.initSteadyView();
   }

   public SteadyFixedDrawerLayout(final Context context, final AttributeSet attrs)
   {
      super(context, attrs);

      ISteadyView.super.initSteadyView();
   }

   public SteadyFixedDrawerLayout(final Context context, final AttributeSet attrs, final int defStyle)
   {
      super(context, attrs, defStyle);

      ISteadyView.super.initSteadyView();
   }

   @Override
   public boolean performAccessibilityAction(final int action, @Nullable final Bundle arguments)
   {
      final boolean status = ISteadyView.super.performSteadyViewAction(action, arguments);

      return super.performAccessibilityAction(action, arguments) || status;
   }
}
