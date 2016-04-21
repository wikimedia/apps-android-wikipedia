package org.wikipedia.database.contract;

import android.content.ContentResolver;
import android.net.Uri;

import org.wikipedia.BuildConfig;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.CodeEnumColumn;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.database.http.HttpColumns;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;

@SuppressWarnings("checkstyle:interfaceistype")
public interface UserOptionContract {
    String AUTHORITY = BuildConfig.USER_OPTION_AUTHORITY;
    Uri AUTHORITY_BASE = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .build();

    String TABLE_OPTION = "useroption";
    String TABLE_HTTP = "useroptionhttp";

    interface OptionCol {
        IdColumn ID = new IdColumn(TABLE_OPTION);
        StrColumn KEY = new StrColumn(TABLE_OPTION, "key", "text not null unique");
        StrColumn VAL = new StrColumn(TABLE_OPTION, "val", "text");

        @Deprecated interface Legacy {
            IntColumn HTTP_STATUS = new IntColumn(TABLE_OPTION, "syncStatus", "integer not null");
            LongColumn HTTP_TIMESTAMP = new LongColumn(TABLE_OPTION, "syncTimestamp", "integer not null");
            LongColumn HTTP_TRANSACTION_ID = new LongColumn(TABLE_OPTION, "syncTransactionId", "integer not null");
        }

        String[] SELECTION = DbUtil.qualifiedNames(KEY);
    }

    HttpColumns<UserOption> HTTP_COLS = new HttpColumns<>(TABLE_HTTP);
    interface HttpCol {
        IdColumn ID = HTTP_COLS.id();
        StrColumn KEY = HTTP_COLS.key();
        CodeEnumColumn<HttpStatus> STATUS = HTTP_COLS.status();
        LongColumn TIMESTAMP = HTTP_COLS.timestamp();
        LongColumn TRANSACTION_ID = HTTP_COLS.transactionId();

        String[] SELECTION = HTTP_COLS.selection();
    }

    interface Option extends OptionCol {
        String TABLES = TABLE_OPTION;
        String PATH = "option";
        Uri URI = Uri.withAppendedPath(AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
    }

    interface Http extends HttpCol {
        String TABLES = TABLE_HTTP;

        // HACK: Http has no real dependency on Option. However, HttpWithOption is a composite of
        //       Option and Http and observers expect to be notified when _either_ change. Making
        //       this path hierarchical allows HttpWithOption to also be hierarchical but needlessly
        //       notifies Http clients when Option changes. More here:
        //       - http://chalup.github.io/blog/2014/09/14/contentprovider-series-uris/
        //       - https://gist.github.com/chalup/4201307da02b9cfe4f40
        String PATH = Option.PATH + "/http";

        Uri URI = Uri.withAppendedPath(AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
    }

    interface HttpWithOption extends Option {
        String TABLES = ":httpTbl left join :tbl on (:tbl.keyCol = :httpTbl.keyCol)"
                .replaceAll(":tbl.keyCol", KEY.qualifiedName())
                .replaceAll(":httpTbl.keyCol", HttpCol.KEY.qualifiedName())
                .replaceAll(":httpTbl", TABLE_HTTP)
                .replaceAll(":tbl", TABLE_OPTION);

        String PATH = Http.PATH + "/with_http";
        Uri URI = Uri.withAppendedPath(AUTHORITY_BASE, PATH);

        StrColumn HTTP_KEY = HttpCol.KEY;
        CodeEnumColumn<HttpStatus> HTTP_STATUS = HttpCol.STATUS;
        LongColumn HTTP_TIMESTAMP = HttpCol.TIMESTAMP;
        LongColumn HTTP_TRANSACTION_ID = HttpCol.TRANSACTION_ID;

        String[] PROJECTION = DbUtil.qualifiedNames(ID, KEY, VAL, HTTP_KEY, HTTP_STATUS,
                HTTP_TIMESTAMP, HTTP_TRANSACTION_ID);
    }
}
