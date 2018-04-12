# --- Prevent crashes when getting SearchView from ActionMode --
# TODO: remove this file if no longer be able to reproduce this issue.
-keep class android.support.v7.widget.SearchView { *; }
# --- /Prevent crashes when getting SearchView from ActionMode  --