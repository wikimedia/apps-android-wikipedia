package org.wikipedia.page.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public abstract class ImagePipelineBitmapGetter {
    private Context context;
    private String imageUrl;

    public ImagePipelineBitmapGetter(Context context, String imageUrl) {
        this.context = context;
        this.imageUrl = imageUrl;
    }

    public abstract void onSuccess(@Nullable Bitmap bitmap);

    public void onError(Throwable t) {
        Toast.makeText(context, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    public void get() {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                .setProgressiveRenderingEnabled(true)
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(request, context);
        dataSource.subscribe(new BitmapDataSubscriber(), UiThreadImmediateExecutorService.getInstance());
    }

    private class BitmapDataSubscriber extends BaseBitmapDataSubscriber {
        @Override
        protected void onNewResultImpl(@Nullable Bitmap tempBitmap) {
            Bitmap bitmap = null;
            if (tempBitmap != null) {
                bitmap = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(tempBitmap, 0f, 0f, new Paint());
            }
            onSuccess(bitmap);
        }

        @Override
        protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
            onError(dataSource.getFailureCause());
        }
    }
}
