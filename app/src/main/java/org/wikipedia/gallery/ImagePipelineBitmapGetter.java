package org.wikipedia.gallery;

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

import org.wikipedia.WikipediaApp;

public abstract class ImagePipelineBitmapGetter {
    private String imageUrl;

    public ImagePipelineBitmapGetter(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public abstract void onSuccess(@Nullable Bitmap bitmap);

    public void onError(Throwable t) {
        Toast.makeText(WikipediaApp.getInstance(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    public void get() {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource
                = imagePipeline.fetchDecodedImage(request, WikipediaApp.getInstance());
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
