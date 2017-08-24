package org.wikipedia.dataclient.restbase;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.model.BaseModel;

/**
 * Gson POJO for a RESTBase API error.
 */
public class RbServiceError extends BaseModel implements ServiceError {
    @SuppressWarnings("unused") private String type;
    @SuppressWarnings("unused") private String title;
    @SuppressWarnings("unused") private String detail;
    @SuppressWarnings("unused") private String method;
    @SuppressWarnings("unused") private String uri;

    public static RbServiceError create(@NonNull String rspBody) {
        return GsonUnmarshaller.unmarshal(RbServiceError.class, rspBody);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDetails() {
        return detail;
    }
}
