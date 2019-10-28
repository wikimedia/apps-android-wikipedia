package org.wikipedia.gallery;

import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.annotation.Nullable;

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
        /*
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource
                = imagePipeline.fetchDecodedImage(request, WikipediaApp.getInstance());
        dataSource.subscribe(new BitmapDataSubscriber(), UiThreadImmediateExecutorService.getInstance());
        */
    }

    // TODO
    /*
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
    */
}
